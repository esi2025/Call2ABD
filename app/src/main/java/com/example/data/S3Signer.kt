package com.example.data

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import okhttp3.Request
import okio.Buffer

object S3Signer {

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun sha256Hex(data: String): String {
        return sha256Hex(data.toByteArray(Charsets.UTF_8))
    }

    private fun hmacSHA256(key: ByteArray, data: String): ByteArray {
        val algorithm = "HmacSHA256"
        val keySpec = SecretKeySpec(key, algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(keySpec)
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun getSigningKey(secretKey: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
        val kSecret = ("AWS4$secretKey").toByteArray(Charsets.UTF_8)
        val kDate = hmacSHA256(kSecret, dateStamp)
        val kRegion = hmacSHA256(kDate, regionName)
        val kService = hmacSHA256(kRegion, serviceName)
        return hmacSHA256(kService, "aws4_request")
    }

    fun signRequest(
        request: Request,
        accessKey: String,
        secretKey: String,
        region: String,
        bucket: String,
        key: String,
        endpointHost: String
    ): Request {
        if (accessKey.isBlank() || secretKey.isBlank()) return request

        val service = "s3"
        val now = Date()
        
        // Date formats
        val amzFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val stampFormat = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        val amzDate = amzFormat.format(now)
        val dateStamp = stampFormat.format(now)

        val method = request.method
        
        // Canonical URI and Query
        // For Path-Style S3: CanonicalURI is "/bucket/key"
        val canonicalUri = "/$bucket/$key"
        val canonicalQuery = "" // empty query parameters

        // Payload hash
        val bodyBytes = request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readByteArray()
        } ?: ByteArray(0)
        val payloadHash = sha256Hex(bodyBytes)

        // Clean Host header
        val host = endpointHost.removePrefix("https://").removePrefix("http://").substringBefore("/")
        
        val headersMap = sortedMapOf<String, String>()
        headersMap["host"] = host
        headersMap["x-amz-content-sha256"] = payloadHash
        headersMap["x-amz-date"] = amzDate

        // Canonical Headers & Signed Headers
        val canonicalHeaders = headersMap.entries.joinToString("") { "${it.key}:${it.value}\n" }
        val signedHeaders = headersMap.keys.joinToString(";")

        // Canonical request
        val canonicalRequest = "$method\n$canonicalUri\n$canonicalQuery\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
        val hashedCanonicalRequest = sha256Hex(canonicalRequest)

        // String to sign
        val credentialScope = "$dateStamp/$region/$service/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n$hashedCanonicalRequest"

        // Signature calculation
        val signingKey = getSigningKey(secretKey, dateStamp, region, service)
        val signatureBytes = hmacSHA256(signingKey, stringToSign)
        val signature = signatureBytes.joinToString("") { "%02x".format(it) }

        // Authorization header
        val authorization = "AWS4-HMAC-SHA256 Credential=$accessKey/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"

        // Build signed request
        val builder = request.newBuilder()
            .header("Host", host)
            .header("X-Amz-Date", amzDate)
            .header("X-Amz-Content-SHA256", payloadHash)
            .header("Authorization", authorization)

        if (request.body != null) {
            builder.header("Content-Type", "application/json")
        }
        
        return builder.build()
    }
}
