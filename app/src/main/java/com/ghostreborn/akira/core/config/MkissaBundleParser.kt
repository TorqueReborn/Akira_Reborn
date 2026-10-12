package com.ghostreborn.akira.core.config

import com.ghostreborn.akira.core.crypto.SafeBase64
import java.util.regex.Pattern

/**
 * Robust extractor for build-dependent cryptographic parameters from Mkissa frontend bundles.
 * Parses the obfuscated client crypto chunk (e.g. BQqtsmMs.js) using the exact string deobfuscation
 * mechanism proven in the working reference build.
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
     * Evaluates simple arithmetic expressions like "-2*97429255+519*-1699713+1681809557"
     */
    fun evaluateArithmetic(expr: String): Long {
        val clean = expr.replace(" ", "")
        val terms = mutableListOf<String>()
        var cur = StringBuilder()
        var i = 0
        while (i < clean.length) {
            val c = clean[i]
            if ((c == '+' || c == '-') && cur.isNotEmpty() && cur.last() != '*' && cur.last() != '/') {
                terms.add(cur.toString())
                cur = StringBuilder()
            }
            cur.append(c)
            i++
        }
        if (cur.isNotEmpty()) terms.add(cur.toString())

        var total = 0L
        for (term in terms) {
            val trimmed = term.trim()
            if (trimmed.isEmpty()) continue
            val factors = trimmed.split("*")
            var product = 1L
            for (f in factors) {
                if (f.isNotEmpty()) {
                    product *= f.toLong()
                }
            }
            total += product
        }
        return total
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

            fun evaluateCheck(): Long {
                val r459 = rTable["_0x459f97"] ?: 87
                val r90 = rTable["_0x90dfba"] ?: 11
                val r160 = rTable["_0x160be5"] ?: 119
                val r59 = rTable["_0x59ff6d"] ?: 63
                val r33 = rTable["_0x33dd48"] ?: 292
                val r24 = rTable["_0x24ba69"] ?: 581
                val r4c = rTable["_0x4c76c2"] ?: 307
                val r5c = rTable["_0x5c1e15"] ?: 221
                val rf2 = rTable["_0xf2d97c"] ?: 354
                val r15 = rTable["_0x155d33"] ?: 91
                val r4b = rTable["_0x4bf52c"] ?: 73

                val v1 = parseLeadingInt(a(35)) / 1L
                val v2 = -parseLeadingInt(a(-r90)) / 2L * (-parseLeadingInt(n(468)) / 3L)
                val v3 = -parseLeadingInt(a(218)) / 4L * (-parseLeadingInt(a(r59)) / 5L)
                val v4 = parseLeadingInt(n(r33)) / 6L
                val v5 = -parseLeadingInt(n(460)) / 7L * (parseLeadingInt(n(r4c)) / 8L)
                val v6 = -parseLeadingInt(n(rf2)) / 9L * (-parseLeadingInt(a(6)) / 10L)
                val v7 = -parseLeadingInt(a(r4b)) / 11L
                return v1 + v2 + v3 + v4 + v5 + v6 + v7
            }

            for (step in 0 until arr.size) {
                try {
                    if (evaluateCheck() == targetVal) {
                        break
                    }
                } catch (_: Exception) {}
                arr.add(arr.removeAt(0))
            }

            fun At(e: Int): String = qa(e - 438)
            fun lr(e: Int): String = qa(e - 452)

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
                for (expr in fragExprs) {
                    val fnMatcher = Pattern.compile("""(At|lr)\((\d+)\)""").matcher(expr)
                    val sb = StringBuilder()
                    while (fnMatcher.find()) {
                        val fn = fnMatcher.group(1)
                        val arg = (fnMatcher.group(2) ?: "0").toInt()
                        sb.append(if (fn == "At") At(arg) else lr(arg))
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
                val fnMatcher = Pattern.compile("""(At|lr)\((\d+)\)""").matcher(expr)
                val sb = StringBuilder()
                while (fnMatcher.find()) {
                    val fn = fnMatcher.group(1)
                    val arg = (fnMatcher.group(2) ?: "0").toInt()
                    sb.append(if (fn == "At") At(arg) else lr(arg))
                }
                sb.append(":")
                bootPrefix = sb.toString()
            }

            // Extract parts
            val partsMatcher = Pattern.compile("""parts:\s*\[([^\]]+)\]""").matcher(jsContent)
            val parts = mutableListOf<String>()
            if (partsMatcher.find()) {
                val partsExprs = (partsMatcher.group(1) ?: "").split(",")
                for (expr in partsExprs) {
                    val fnMatcher = Pattern.compile("""(?:(At|lr)\((\d+)\)|"([^"]+)")""").matcher(expr)
                    val sb = StringBuilder()
                    while (fnMatcher.find()) {
                        val fn = fnMatcher.group(1)
                        if (fn != null) {
                            val arg = (fnMatcher.group(2) ?: "0").toInt()
                            sb.append(if (fn == "At") At(arg) else lr(arg))
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
