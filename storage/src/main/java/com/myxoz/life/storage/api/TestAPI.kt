package com.myxoz.life.storage.api

import com.myxoz.life.core.minus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.time.Instant
import javax.net.ssl.HttpsURLConnection
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

internal object TestAPI {
    suspend fun testSign(
        security: Security,
        urlString: String,
        lastUpdate: Instant,
        now: Instant
    ) = withContext(Dispatchers.IO) {
        val baseBody = """{"ping":${Random.nextInt()}}"""
        val offset = 0
        val method = API.Method.Verify

        fun sendRaw(json: LifeJSONObject): Boolean? {
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            conn.outputStream.use {
                it.write(json.asString().toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            val codeIsSuccess = code in 200..299
            try {
                BufferedReader(
                    InputStreamReader(
                        if (codeIsSuccess) conn.inputStream else conn.errorStream
                    )
                ).readText()
            } catch (_: Exception) {
                return null
            }
            conn.disconnect()
            return codeIsSuccess
        }

        run {
            val body =
                security.generateSignedBody(lastUpdate, now, baseBody, method, offset)
            val resp = sendRaw(body)
            if (resp != true) {
                return@withContext "Server rejected valid signature. Make sure to add the device."
            }
        }

        // 2. Modified body (must fail)
        run {
            val body =
                security.generateSignedBody(lastUpdate, now, baseBody, method, offset)
            body.put("body", """{"ping":${Random.nextInt()}}""")
            val resp = sendRaw(body)
            if (resp != false) {
                return@withContext "Server didnt reject invalid sign for body."
            }
        }

        run {
            val body =
                security.generateSignedBody(lastUpdate, now, baseBody, method, offset)
            body.put("method", "r")
            val resp = sendRaw(body)
            if (resp != false) {
                return@withContext "Server didn't reject wrong method"
            }
        }

        run {
            val pk = security.getBase64Public()
            val corruptedPk = pk.toCharArray().apply { shuffle() }.joinToString("")
            val body = security.generateSignedBody(
                lastUpdate, now, baseBody, method, offset, corruptedPk
            )
            val resp = sendRaw(body)
            if (resp != false) {
                return@withContext "Server didnt reject altered public key"
            }
        }

        run {
            val body =
                security.generateSignedBody(lastUpdate, now, baseBody, method, offset)
            val sig = body.getString("signature")
            val corruptedSig = sig.toCharArray().apply { shuffle() }.joinToString("")
            body.put("signature", corruptedSig)
            val resp = sendRaw(body)
            if (resp != false) {
                return@withContext "Server didnt reject altered signature"
            }
        }

        run {
            val body = security.generateSignedBody(
                lastUpdate,
                now - 10.minutes, // 10 minutes old
                baseBody,
                method,
                offset
            )
            val resp = sendRaw(body)
            if (resp != false) {
                return@withContext "Server didnt reject outdates ts"
            }
        }
        return@withContext null
    }
}