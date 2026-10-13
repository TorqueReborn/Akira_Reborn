package com.ghostreborn.akira

import com.ghostreborn.akira.core.config.MkissaBundleParser
import com.ghostreborn.akira.core.config.MkissaClientConfig
import com.ghostreborn.akira.features.player.services.CryptoBootstrap
import com.ghostreborn.akira.features.player.services.Decryptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Test
import java.io.File

class MkissaIntegrationUnitTest {

    @Test
    fun test15_bundleParserDeobfuscatesReferenceBundle() {
        val resourceStream = javaClass.classLoader?.getResourceAsStream("fixtures/BQqtsmMs.js")
        val file = File("app/src/test/resources/fixtures/BQqtsmMs.js")
        
        if (resourceStream == null && !file.exists()) {
            return // Skip test if offline JS fixture is not present
        }

        val stream = resourceStream ?: file.inputStream()
        val content = stream.bufferedReader().use { it.readText() }
        val extracted = MkissaBundleParser.parseCryptoParams(content)
        assertNotNull(extracted)
        assertEquals(12, extracted!!.saltMul)
        assertEquals(227, extracted.saltAdd)
        assertEquals(32, extracted.fragMul)
        assertEquals(217, extracted.fragAdd)
        assertEquals("+", extracted.joinDelimiter)
        assertEquals("NmmeXF:", extracted.bootPrefix)
        assertEquals(listOf("lane", "buildId", "host", "group", "epoch"), extracted.parts)
        assertEquals(4, extracted.fragments.size)
        assertEquals(604800000L, extracted.epochMs)
        assertEquals(86400000L, extracted.graceMs)
    }
}
