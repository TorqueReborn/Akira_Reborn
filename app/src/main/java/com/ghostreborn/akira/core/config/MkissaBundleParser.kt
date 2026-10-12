package com.ghostreborn.akira.core.config

import com.ghostreborn.akira.core.crypto.SafeBase64
import java.util.regex.Pattern

/**
 * Robust extractor for build-dependent cryptographic parameters from Mkissa frontend bundles.
 * Parses the obfuscated client crypto chunk (e.g. BQqtsmMs.js) using deterministic string deobfuscation
 * and an arithmetic expression parser handling signed integers, multiplication, and addition.
 */
data class ExtractedCryptoParams(
    val saltMul: Int,
    val saltAdd: Int,
    val fragMul: Int,
    val fragAdd: Int,
    val bootPrefix: String,
    val joinDelimiter: String,
    val parts: List<String>,
    val fragments: List<ByteArray>,
    val epochMs: Long,
    val graceMs: Long
)

object MkissaBundleParser {

    /**
     * Evaluates signed arithmetic expressions present in frontend bundles, e.g.:
     * "1870*-1+-1*-9874+-7663" -> 341
     * "-2*97429255+519*-1699713+1681809557" -> 604800000
     * Unary +/- signs, binary *, /, +, -, and whitespaces are handled strictly.
     * Rejects empty or invalid syntax safely without arbitrary eval.
     */
    fun evaluateArithmetic(expr: String): Long {
        val clean = expr.replace(" ", "")
        require(clean.isNotBlank()) { "Arithmetic expression cannot be empty" }

        val tokens = mutableListOf<Any>() // Long or Char ('+', '-', '*', '/')
        var i = 0
        val n = clean.length

        while (i < n) {
            val c = clean[i]
            if (c == '+' || c == '-' || c == '*' || c == '/') {
                val isPrevOp = tokens.isEmpty() || (tokens.last() is Char)
                if ((c == '+' || c == '-') && isPrevOp) {
                    val sign = if (c == '+') 1L else -1L
                    i++
                    val start = i
                    while (i < n && clean[i].isDigit()) {
                        i++
                    }
                    require(start < i) { "Expected number following unary '$c' at position $start in expression '$expr'" }
                    val num = sign * clean.substring(start, i).toLong()
                    tokens.add(num)
                } else {
                    tokens.add(c)
                    i++
                }
            } else if (c.isDigit()) {
                val start = i
                while (i < n && clean[i].isDigit()) {
                    i++
                }
                tokens.add(clean.substring(start, i).toLong())
            } else {
                throw IllegalArgumentException("Unsupported character '$c' in arithmetic expression '$expr'")
            }
        }

        // Pass 1: Multiplication and Division (left to right)
        val values = mutableListOf<Long>()
        val addSubOps = mutableListOf<Char>()
        var idx = 0

        while (idx < tokens.size) {
            val tok = tokens[idx]
            when (tok) {
                is Long -> values.add(tok)
                '*', '/' -> {
                    idx++
                    require(idx < tokens.size && tokens[idx] is Long) { "Expected number after operator '$tok'" }
                    val nextVal = tokens[idx] as Long
                    require(values.isNotEmpty()) { "Missing left operand for operator '$tok'" }
                    val prevVal = values.removeAt(values.size - 1)
                    if (tok == '*') {
                        values.add(prevVal * nextVal)
                    } else {
                        require(nextVal != 0L) { "Division by zero in expression '$expr'" }
                        values.add(prevVal / nextVal)
                    }
                }
                '+', '-' -> addSubOps.add(tok as Char)
                else -> throw IllegalArgumentException("Unexpected token $tok in expression '$expr'")
            }
            idx++
        }

        require(values.isNotEmpty()) { "No values to evaluate in expression '$expr'" }
        require(values.size == addSubOps.size + 1) { "Mismatched operators and operands in expression '$expr'" }

        // Pass 2: Addition and Subtraction (left to right)
        var result = values[0]
        for (k in addSubOps.indices) {
            val op = addSubOps[k]
            val nextVal = values[k + 1]
            if (op == '+') {
                result += nextVal
            } else {
                result -= nextVal
            }
        }

        return result
    }

    /**
     * Parses the string table and deobfuscation logic from the crypto chunk JS bundle.
     */
    fun parseCryptoParams(jsContent: String): ExtractedCryptoParams? {
        try {
            if (!jsContent.contains("saltMul:") || !jsContent.contains("function tu()")) {
                return null
            }

            // 1. Extract string array from function tu()
            val tuStart = jsContent.indexOf("function tu()")
            if (tuStart == -1) return null
            val tuEnd = jsContent.indexOf("return tu=function()", tuStart)
            if (tuEnd == -1) return null
            val tuBody = jsContent.substring(tuStart, tuEnd)
            val arrOpen = tuBody.indexOf('[')
            val arrClose = tuBody.lastIndexOf(']')
            if (arrOpen == -1 || arrClose == -1) return null

            val rawArrayItems = tuBody.substring(arrOpen + 1, arrClose)
            val stringArray = mutableListOf<String>()
            val strPattern = Pattern.compile("\"([^\"]*)\"|'([^']*)'")
            val strMatcher = strPattern.matcher(rawArrayItems)
            while (strMatcher.find()) {
                val s = strMatcher.group(1) ?: strMatcher.group(2) ?: ""
                stringArray.add(s)
            }
            if (stringArray.size < 50) return null

            // 2. Extract target integer and offset for qa()
            val qaStart = jsContent.indexOf("function qa(")
            if (qaStart == -1) return null

            val targetMatcher = Pattern.compile("""\}\)\(tu,\s*([^\)]+)\)""").matcher(jsContent.substring(0, qaStart))
            var targetExpr = ""
            var targetEndPos = 0
            while (targetMatcher.find()) {
                targetExpr = targetMatcher.group(1) ?: ""
                targetEndPos = targetMatcher.start()
            }
            if (targetExpr.isBlank()) return null
            val targetVal = evaluateArithmetic(targetExpr)

            val offsetMatcher = Pattern.compile("""function qa\(e,t\)\{return e=e-\(([^\)]+)\)""").matcher(jsContent)
            if (!offsetMatcher.find()) return null
            val offsetValStr = offsetMatcher.group(1) ?: return null
            val offsetVal = evaluateArithmetic(offsetValStr).toInt()

            // 3. Extract IIFE loop before targetEndPos
            val iifeStart = jsContent.lastIndexOf("(function(", targetEndPos)
            if (iifeStart == -1) return null
            val iifeCode = jsContent.substring(iifeStart, targetEndPos)

            val rMatcher = Pattern.compile("""const\s+r\s*=\s*\{([^}]+)\}""").matcher(iifeCode)
            if (!rMatcher.find()) return null
            val rTable = mutableMapOf<String, Int>()
            val rGroup = rMatcher.group(1) ?: return null
            for (part in rGroup.split(",")) {
                val kv = part.split(":")
                if (kv.size == 2) {
                    rTable[kv[0].trim()] = kv[1].trim().toInt()
                }
            }

            val nMatcher = Pattern.compile("""function\s+n\([^\)]*\)\{return\s+qa\(o-\s*([^\)]+)\)""").matcher(iifeCode)
            val aMatcher = Pattern.compile("""function\s+a\([^\)]*\)\{return\s+qa\(o-\s*([^\)]+)\)""").matcher(iifeCode)
            if (!nMatcher.find() || !aMatcher.find()) return null
            val nSubStr = nMatcher.group(1) ?: return null
            val aSubStr = aMatcher.group(1) ?: return null
            val nSub = evaluateArithmetic(nSubStr).toInt()
            val aSub = evaluateArithmetic(aSubStr).toInt()

            val checkMatcher = Pattern.compile("""if\(([^=]+)===\s*t\)\s*break""").matcher(iifeCode)
            if (!checkMatcher.find()) return null
            val checkExpr = checkMatcher.group(1)?.trim() ?: return null

            // 4. Simulate the shuffle loop
            val arr = ArrayList(stringArray)

            fun parseLeadingInt(str: String): Int {
                val m = Pattern.compile("""^-?\d+""").matcher(str)
                return if (m.find()) m.group(0)?.toInt() ?: 0 else 0
            }

            fun qa(e: Int): String {
                val idx = e - offsetVal
                return if (idx in 0 until arr.size) arr[idx] else ""
            }

            fun n(o: Int): String = qa(o - nSub)
            fun a(o: Int): String = qa(o - aSub)

            // Generic evaluator for check expression terms:
            // Term pattern: [sign]parseInt(fn(arg1, arg2))/div [* [sign]parseInt(fn(arg1, arg2))/div]
            fun evalCall(callStr: String): Long {
                val cleanCall = callStr.replace(" ", "").trim('(', ')')
                val m = Pattern.compile("""(-?)parseInt\((a|n)\(([^)]+)\)\)/(\d+)""").matcher(cleanCall)
                if (!m.find()) return 0L
                val sign = if (m.group(1) == "-") -1L else 1L
                val fnType = m.group(2)
                val argsRaw = (m.group(3) ?: "").split(",")
                val div = (m.group(4) ?: "1").toLong()

                fun resolveArg(rawArg: String): Int {
                    var exprA = rawArg.trim()
                    for ((k, v) in rTable) {
                        exprA = exprA.replace("r.$k", v.toString())
                    }
                    return evaluateArithmetic(exprA).toInt()
                }

                // In JS: a(s, o) returns qa(o - aSub). The 2nd argument 'o' determines the string lookup index!
                val oArg = if (argsRaw.size > 1) resolveArg(argsRaw[1]) else resolveArg(argsRaw[0])
                val rawStr = if (fnType == "a") a(oArg) else n(oArg)
                val num = parseLeadingInt(rawStr).toLong()
                return sign * (num / div)
            }

            fun evaluateCheck(): Long {
                val terms = checkExpr.split("+")
                var total = 0L
                for (termStr in terms) {
                    val factors = termStr.split("*")
                    var prod = 1L
                    for (f in factors) {
                        prod *= evalCall(f)
                    }
                    total += prod
                }
                return total
            }

            for (step in 0 until arr.size) {
                try {
                    if (evaluateCheck() == targetVal) {
                        break
                    }
                } catch (_: Exception) {}
                arr.add(arr.removeAt(0))
            }

            // Dynamically discover all accessor helper functions like `function At(e){return qa(e-438)}`
            val helperOffsetMap = mutableMapOf<String, Int>()
            val helperMatcher = Pattern.compile("""function\s+([A-Za-z0-9_]+)\(e\)\s*\{\s*return\s+qa\(e\s*-\s*([^\)]+)\)\s*\}""").matcher(jsContent)
            while (helperMatcher.find()) {
                val fnName = helperMatcher.group(1) ?: continue
                val offExpr = helperMatcher.group(2) ?: continue
                val offVal = evaluateArithmetic(offExpr).toInt()
                helperOffsetMap[fnName] = offVal
            }

            fun resolveHelperCall(fnName: String, arg: Int): String {
                val offset = helperOffsetMap[fnName] ?: when (fnName) {
                    "At" -> 438
                    "lr" -> 452
                    else -> 0
                }
                return qa(arg - offset)
            }

            val helperNamesPattern = if (helperOffsetMap.isNotEmpty()) {
                helperOffsetMap.keys.joinToString("|")
            } else {
                "At|lr"
            }

            // 5. Extract Df params
            val paramMatcher = Pattern.compile("""saltMul:\s*(\d+)\s*,\s*saltAdd:\s*(\d+)\s*,\s*fragMul:\s*(\d+)\s*,\s*fragAdd:\s*(\d+)""").matcher(jsContent)
            if (!paramMatcher.find()) return null
            val saltMul = (paramMatcher.group(1) ?: return null).toInt()
            val saltAdd = (paramMatcher.group(2) ?: return null).toInt()
            val fragMul = (paramMatcher.group(3) ?: return null).toInt()
            val fragAdd = (paramMatcher.group(4) ?: return null).toInt()

            val joinMatcher = Pattern.compile("""join:\s*["']([^"']+)["']""").matcher(jsContent)
            val joinDelim = if (joinMatcher.find()) (joinMatcher.group(1) ?: "+") else "+"

            val epochMatcher = Pattern.compile("""tT\s*=\s*Number\(([^\)]+)\)""").matcher(jsContent)
            val epochMs = if (epochMatcher.find()) evaluateArithmetic(epochMatcher.group(1) ?: "") else 604800000L

            val graceMatcher = Pattern.compile("""rT\s*=\s*Number\(([^\)]+)\)""").matcher(jsContent)
            val graceMs = if (graceMatcher.find()) evaluateArithmetic(graceMatcher.group(1) ?: "") else 86400000L

            // Extract fragments (_m)
            val mMatcher = Pattern.compile("""_m\s*=\s*\[([^\]]+)\]""").matcher(jsContent)
            val fragments = mutableListOf<ByteArray>()
            if (mMatcher.find()) {
                val fragExprs = (mMatcher.group(1) ?: "").split(",")
                val callPattern = Pattern.compile("""($helperNamesPattern)\((\d+)\)""")
                for (expr in fragExprs) {
                    val fnMatcher = callPattern.matcher(expr)
                    val sb = StringBuilder()
                    while (fnMatcher.find()) {
                        val fn = fnMatcher.group(1) ?: ""
                        val arg = (fnMatcher.group(2) ?: "0").toInt()
                        sb.append(resolveHelperCall(fn, arg))
                    }
                    val b64 = sb.toString()
                    val decoded = SafeBase64.decode(b64)
                    if (decoded.size == 8) {
                        fragments.add(decoded)
                    }
                }
            }

            // Extract bootPrefix
            val bpMatcher = Pattern.compile("""bootPrefix:\s*([^,]+),""").matcher(jsContent)
            var bootPrefix = "NmmeXF:"
            if (bpMatcher.find()) {
                val expr = bpMatcher.group(1) ?: ""
                val callPattern = Pattern.compile("""($helperNamesPattern)\((\d+)\)""")
                val fnMatcher = callPattern.matcher(expr)
                val sb = StringBuilder()
                while (fnMatcher.find()) {
                    val fn = fnMatcher.group(1) ?: ""
                    val arg = (fnMatcher.group(2) ?: "0").toInt()
                    sb.append(resolveHelperCall(fn, arg))
                }
                sb.append(":")
                bootPrefix = sb.toString()
            }

            // Extract parts
            val partsMatcher = Pattern.compile("""parts:\s*\[([^\]]+)\]""").matcher(jsContent)
            val parts = mutableListOf<String>()
            if (partsMatcher.find()) {
                val partsExprs = (partsMatcher.group(1) ?: "").split(",")
                val partCallPattern = Pattern.compile("""(?:($helperNamesPattern)\((\d+)\)|"([^"]+)")""")
                for (expr in partsExprs) {
                    val fnMatcher = partCallPattern.matcher(expr)
                    val sb = StringBuilder()
                    while (fnMatcher.find()) {
                        val fn = fnMatcher.group(1)
                        if (fn != null) {
                            val arg = (fnMatcher.group(2) ?: "0").toInt()
                            sb.append(resolveHelperCall(fn, arg))
                        } else {
                            sb.append(fnMatcher.group(3) ?: "")
                        }
                    }
                    parts.add(sb.toString())
                }
            }

            if (fragments.size == 4 && parts.isNotEmpty()) {
                return ExtractedCryptoParams(
                    saltMul = saltMul,
                    saltAdd = saltAdd,
                    fragMul = fragMul,
                    fragAdd = fragAdd,
                    bootPrefix = bootPrefix,
                    joinDelimiter = joinDelim,
                    parts = parts,
                    fragments = fragments,
                    epochMs = epochMs,
                    graceMs = graceMs
                )
            }
            return null
        } catch (_: Exception) {
            return null
        }
    }
}
