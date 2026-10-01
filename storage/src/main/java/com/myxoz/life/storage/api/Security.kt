package com.myxoz.life.storage.api

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import java.time.Instant
import javax.net.ssl.HttpsURLConnection

internal class Security(
    private val alias: String = "private_token"
) {
    init {
        ensureKeypair()
    }

    suspend fun send(
        urlString: String,
        lastUpdate: Instant,
        currentTime: Instant,
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
                os.write(signed.asString().toByteArray(Charsets.UTF_8))
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
        lastUpdate: Instant,
        currentTime: Instant,
        bodyStr: String,
        method: API.Method,
        offset: Int?,
        publicKey: String = getBase64Public()
    ): LifeJSONObject {
        val sha = sha256Hex(bodyStr)

        val canonical = buildString {
            append(currentTime.toEpochMilli())
            append("\n")
            append(lastUpdate.toEpochMilli())
            append("\n")
            append(method.method)
            append("\n")
            append(offset?:-1)
            append("\n")
            append(sha)
        }

        val signature = sign(canonical.toByteArray(Charsets.UTF_8))
        val signatureB64 = Base64.encodeToString(signature, Base64.NO_WRAP)

        return LifeJSONObject()
            .put("public_key", publicKey)
            .put("timestamp", currentTime)
            .put("last_update", lastUpdate)
            .put("signature", signatureB64)
            .put("method", method.method)
            .put("offset", offset?:-1)
            .put("body", bodyStr)
    }

    fun getBase64Public(): String = Base64.encodeToString(getPublicKey().encoded, Base64.NO_WRAP)

    private fun ensureKeypair() {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
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
}