package com.myxoz.life.api

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.UnknownHostException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import javax.net.ssl.HttpsURLConnection
import kotlin.random.Random

class Security(
    private val alias: String = "private_token"
) {
    init {
        ensureKeypair()
    }

    suspend fun send(
        urlString: String,
        lastUpdate: Long,
        currentTime: Long,
        data: String,
        method: API.Method,
        offset: Int?,
    ): String? = withContext(Dispatchers.IO) {
        val signed = generateSignedBody(lastUpdate, currentTime, data, method, offset)
        val url = URL(urlString)
        return@withContext try {
            val conn = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            conn.outputStream.use { os ->
                os.write(signed.toString().toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val result = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            conn.disconnect()
            result
        } catch (_: UnknownHostException) {
            Log.w(API.LOGTAG, "Offline")
            null
        } catch (e: Exception) {
            Log.e(API.LOGTAG,"Exception while fetching", e)
            e.printStackTrace()
            null
        }
    }

    fun generateSignedBody(
        lastUpdate: Long,
        currentTime: Long,
        bodyStr: String,
        method: API.Method,
        offset: Int?,
        publicKey: String = getBase64Public()
    ): JSONObject {
        val sha = sha256Hex(bodyStr)

        val canonical = buildString {
            append(currentTime)
            append("\n")
            append(lastUpdate)
            append("\n")
            append(method.method)
            append("\n")
            append(offset?:-1)
            append("\n")
            append(sha)
        }

        val signature = sign(canonical.toByteArray(Charsets.UTF_8))
        val signatureB64 = Base64.encodeToString(signature, Base64.NO_WRAP)

        val out = JSONObject()
        out.put("public_key", publicKey)
        out.put("timestamp", currentTime)
        out.put("last_update", lastUpdate)
        out.put("signature", signatureB64)
        out.put("method", method.method)
        out.put("offset", offset?:-1)
        out.put("body", bodyStr)

        return out
    }

    fun getBase64Public(): String = Base64.encodeToString(getPublicKey().encoded, Base64.NO_WRAP)

    private fun ensureKeypair() {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val exists = ks.containsAlias(alias)
        if (!exists) generateKeypair()
    }

    private fun generateKeypair() {
        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )

        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .build()

        kpg.initialize(spec)
        kpg.generateKeyPair()
    }

    private fun getPrivateKey(): PrivateKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return ks.getKey(alias, null) as PrivateKey
    }

    fun getPublicKey(): PublicKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val cert = ks.getCertificate(alias)
        return cert.publicKey
    }

    private fun sign(bytes: ByteArray): ByteArray {
        val privateKey = getPrivateKey()
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(privateKey)
        sig.update(bytes)
        return sig.sign()
    }

    private fun sha256Hex(s: String): String {
        val d = MessageDigest.getInstance("SHA-256")
        val b = d.digest(s.toByteArray(Charsets.UTF_8))
        return b.joinToString("") { "%02x".format(it) }
    }

    suspend fun testSign(urlString: String, lastUpdate: Long, currentTimeMillis: Long) = withContext(Dispatchers.IO) {
        val baseBody = """{"ping":${Random.nextInt()}}"""
        val offset = 0
        val method = API.Method.Verify

        fun sendRaw(json: JSONObject): Boolean? {
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            conn.outputStream.use {
                it.write(json.toString().toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            val codeIsSuccess = code in 200..299
            try {
                BufferedReader(InputStreamReader(
                    if (codeIsSuccess) conn.inputStream else conn.errorStream
                )).readText()
            } catch (_: Exception) {
                return null
            }
            conn.disconnect()
            return codeIsSuccess
        }

        run {
            val body = generateSignedBody(lastUpdate, currentTimeMillis, baseBody, method, offset)
            val resp = sendRaw(body)
            if(resp != true){
                return@withContext "Server rejected valid signature. Make sure to add the device."
            }
        }

        // 2. Modified body (must fail)
        run {
            val body = generateSignedBody(lastUpdate, currentTimeMillis, baseBody, method, offset)
            body.put("body", """{"ping":${Random.nextInt()}}""")
            val resp = sendRaw(body)
            if(resp != false){
                return@withContext "Server didnt reject invalid sign for body."
            }
        }

        run {
            val body = generateSignedBody(lastUpdate, currentTimeMillis, baseBody, method, offset)
            body.put("method", "r")
            val resp = sendRaw(body)
            if(resp != false){
                return@withContext "Server didn't reject wrong method"
            }
        }

        run {
            val pk = getBase64Public()
            val corruptedPk = pk.toCharArray().apply { shuffle() }.joinToString("")
            val body = generateSignedBody(
                lastUpdate, currentTimeMillis, baseBody, method, offset, corruptedPk
            )
            val resp = sendRaw(body)
            if(resp != false){
                return@withContext "Server didnt reject altered public key"
            }
        }

        run {
            val body = generateSignedBody(lastUpdate, currentTimeMillis, baseBody, method, offset)
            val sig = body.getString("signature")
            val corruptedSig = sig.toCharArray().apply { shuffle() }.joinToString("")
            body.put("signature", corruptedSig)
            val resp = sendRaw(body)
            if(resp != false){
                return@withContext "Server didnt reject altered signature"
            }
        }

        run {
            val body = generateSignedBody(
                lastUpdate,
                currentTimeMillis - 600_000, // 10 minutes old
                baseBody,
                method,
                offset
            )
            val resp = sendRaw(body)
            if(resp != false){
                return@withContext "Server didnt reject outdates ts"
            }
        }

        return@withContext null
    }
}