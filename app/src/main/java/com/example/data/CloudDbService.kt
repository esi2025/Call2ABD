package com.example.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object FallbackDns : Dns {
    private val systemDns = Dns.SYSTEM

    override fun lookup(hostname: String): List<InetAddress> {
        // Prevent recursive resolver loops on DoH servers
        if (hostname == "dns.google" || hostname == "cloudflare-dns.com" || hostname == "one.one.one.one") {
            return systemDns.lookup(hostname)
        }

        // Try standard system DNS first
        try {
            val addresses = systemDns.lookup(hostname)
            if (addresses.isNotEmpty()) {
                return addresses
            }
        } catch (e: Exception) {
            // Standard system DNS failed, proceed to try Iran Telecommunication (TCI) DNS
            CloudDbService.logDiagnostic(
                "DNS_SYSTEM_FAILED",
                "WARNING",
                "System DNS failed to resolve '$hostname'",
                "Error: ${e.message}\nBypassing and attempting Iran Telecommunication (TCI) DNS over UDP..."
            )
        }

        // Try query via custom configured DNS servers over UDP
        val tciDnsList = listOf(AppSettings.dns1, AppSettings.dns2).filter { it.isNotBlank() }
        for (dnsIp in tciDnsList) {
            try {
                val ip = resolveWithUdpDns(hostname, dnsIp)
                if (ip != null) {
                    val numericAddress = InetAddress.getByName(ip)
                    val resolved = InetAddress.getByAddress(hostname, numericAddress.address)
                    CloudDbService.logDiagnostic(
                        "DNS_TCI_SUCCESS",
                        "SUCCESS",
                        "Iran Telecommunication DNS ($dnsIp) resolved '$hostname' to '$ip'",
                        "Hostname: $hostname\nResolved IP: $ip"
                    )
                    return listOf(resolved)
                }
            } catch (ex: Exception) {
                CloudDbService.logDiagnostic(
                    "DNS_TCI_FAILED",
                    "WARNING",
                    "Iran Telecommunication DNS ($dnsIp) query failed for '$hostname'",
                    ex.stackTraceToString()
                )
            }
        }

        // Run secure DNS-over-HTTPS (DoH) lookup
        try {
            val ip = resolveWithDoh(hostname)
            if (ip != null) {
                val numericAddress = InetAddress.getByName(ip)
                val resolved = InetAddress.getByAddress(hostname, numericAddress.address)
                CloudDbService.logDiagnostic(
                    "DNS_DOH_SUCCESS",
                    "SUCCESS",
                    "Secure DNS-over-HTTPS successfully resolved '$hostname' to '$ip'",
                    "Hostname: $hostname\nResolved IP Address: $ip"
                )
                return listOf(resolved)
            }
        } catch (dohEx: Exception) {
            CloudDbService.logDiagnostic(
                "DNS_DOH_FAILED",
                "ERROR",
                "Secure DoH fallback failed resolving '$hostname'",
                dohEx.stackTraceToString()
            )
        }

        throw UnknownHostException("FallbackDns: Unable to resolve hostname '$hostname' after all system DNS, TCI UDP, and secure DoH fallback attempts.")
    }

    private fun resolveWithUdpDns(hostname: String, dnsServerIp: String): String? {
        var socket: java.net.DatagramSocket? = null
        try {
            socket = java.net.DatagramSocket().apply {
                soTimeout = 3000
            }
            val serverAddr = java.net.InetAddress.getByName(dnsServerIp)
            
            // Build raw DNS Query
            val baos = java.io.ByteArrayOutputStream()
            baos.write(byteArrayOf(0x12, 0x34)) // Transaction ID
            baos.write(byteArrayOf(0x01, 0x00)) // Flags: standard query, recursion desired
            baos.write(byteArrayOf(0x00, 0x01)) // Questions: 1
            baos.write(byteArrayOf(0x00, 0x00)) // Answer RRs: 0
            baos.write(byteArrayOf(0x00, 0x00)) // Authority RRs: 0
            baos.write(byteArrayOf(0x00, 0x00)) // Additional RRs: 0
            
            // Hostname labels
            val parts = hostname.split(".")
            for (part in parts) {
                val bytes = part.toByteArray(Charsets.US_ASCII)
                baos.write(bytes.size)
                baos.write(bytes)
            }
            baos.write(0) // Null byte terminating domain name
            
            baos.write(byteArrayOf(0x00, 0x01)) // Type: A
            baos.write(byteArrayOf(0x00, 0x01)) // Class: IN
            
            val requestData = baos.toByteArray()
            val requestPacket = java.net.DatagramPacket(requestData, requestData.size, serverAddr, 53)
            socket.send(requestPacket)
            
            val responseData = ByteArray(512)
            val responsePacket = java.net.DatagramPacket(responseData, responseData.size)
            socket.receive(responsePacket)
            
            // Ensure Transaction ID matches
            if (responseData[0] != 0x12.toByte() || responseData[1] != 0x34.toByte()) {
                return null
            }
            
            val answerCount = ((responseData[6].toInt() and 0xFF) shl 8) or (responseData[7].toInt() and 0xFF)
            if (answerCount == 0) return null
            
            // Skip header (12 bytes) and question details
            var ptr = 12
            while (responseData[ptr].toInt() != 0) {
                ptr += (responseData[ptr].toInt() and 0xFF) + 1
                if (ptr >= responseData.size) return null
            }
            ptr += 1 // skip closing null
            ptr += 4 // skip type and class (4 bytes)
            
            // Answers start here
            for (i in 0 until answerCount) {
                if (ptr >= responseData.size) return null
                val b = responseData[ptr].toInt() and 0xFF
                if (b >= 192) {
                    ptr += 2 // Pointer is 2 bytes
                } else {
                    while (responseData[ptr].toInt() != 0) {
                        ptr += (responseData[ptr].toInt() and 0xFF) + 1
                        if (ptr >= responseData.size) return null
                    }
                    ptr += 1
                }
                
                val type = ((responseData[ptr].toInt() and 0xFF) shl 8) or (responseData[ptr+1].toInt() and 0xFF)
                val clazz = ((responseData[ptr+2].toInt() and 0xFF) shl 8) or (responseData[ptr+3].toInt() and 0xFF)
                ptr += 8 // skip type (2), class (2), TTL (4)
                val dataLen = ((responseData[ptr].toInt() and 0xFF) shl 8) or (responseData[ptr+1].toInt() and 0xFF)
                ptr += 2
                
                if (type == 1 && clazz == 1 && dataLen == 4) { // Type A, IN class, length 4 (IPv4)
                    val ipBytes = byteArrayOf(
                        responseData[ptr],
                        responseData[ptr+1],
                        responseData[ptr+2],
                        responseData[ptr+3]
                    )
                    return java.net.InetAddress.getByAddress(ipBytes).hostAddress
                }
                ptr += dataLen
            }
        } catch (e: Exception) {
            // Squelch and return null
        } finally {
            socket?.close()
        }
        return null
    }

    private fun resolveWithDoh(hostname: String): String? {
        val client = OkHttpClient.Builder()
            .dns(systemDns) // Use standard system DNS resolver for resolving DoH API endpoints to avoid cyclic loops
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        // List of DNS-over-HTTPS engines to query sequentially
        val providers = listOf(
            "https://dns.google/resolve?name=$hostname&type=A",
            "https://cloudflare-dns.com/dns-query?name=$hostname&type=A",
            "https://8.8.8.8/resolve?name=$hostname&type=A",
            "https://1.1.1.1/dns-query?name=$hostname&type=A"
        )

        for (url in providers) {
            try {
                val reqBuilder = Request.Builder().url(url)
                if (url.contains("dns-query")) {
                    reqBuilder.header("accept", "application/dns-json")
                }
                val request = reqBuilder.build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val regex = """"data"\s*:\s*"([0-9.]+)"""".toRegex()
                        val match = regex.find(body)
                        if (match != null) {
                            return match.groupValues[1]
                        }
                    }
                }
            } catch (e: Exception) {
                // Squelch and try next provider
            }
        }
        return null
    }
}

object CloudDbService {
    private val client = OkHttpClient.Builder()
        .dns(FallbackDns)
        .build()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    
    private const val BUCKET_NAME = "TTcinKc3VEmTiecanbPpbr"
    private const val BASE_URL = "https://kvdb.io/$BUCKET_NAME"

    private val contactsType = Types.newParameterizedType(List::class.java, Contact::class.java)
    private val contactsAdapter = moshi.adapter<List<Contact>>(contactsType)

    private val usersType = Types.newParameterizedType(List::class.java, User::class.java)
    private val usersAdapter = moshi.adapter<List<User>>(usersType)

    // Thread-safe in-memory log list for instant diagnostic reporting
    val diagnosticLogs = CopyOnWriteArrayList<DiagnosticLog>()

    data class DiagnosticLog(
        val timestamp: Long = System.currentTimeMillis(),
        val type: String, // "UPLOAD_CONTACTS", "DOWNLOAD_CONTACTS", "UPLOAD_USERS", "DOWNLOAD_USERS", "TEST_CONNECTION"
        val status: String, // "SUCCESS", "FAILED", "EXCEPTION"
        val message: String,
        val details: String
    )

    fun logDiagnostic(type: String, status: String, message: String, details: String) {
        diagnosticLogs.add(0, DiagnosticLog(type = type, status = status, message = message, details = details))
        if (diagnosticLogs.size > 100) {
            diagnosticLogs.removeAt(diagnosticLogs.size - 1)
        }
    }

    fun clearDiagnosticLogs() {
        diagnosticLogs.clear()
    }

    fun testConnection(): ConnectionResult {
        return try {
            val url = if (AppSettings.s3Enabled) {
                val host = AppSettings.s3Endpoint.removePrefix("https://").removePrefix("http://")
                "https://$host/${AppSettings.s3BucketName}/${AppSettings.s3ContactsKey}"
            } else {
                "$BASE_URL/contacts"
            }
            
            val rawRequest = Request.Builder()
                .url(url)
                .head()
                .build()
                
            val request = if (AppSettings.s3Enabled) {
                val host = AppSettings.s3Endpoint.removePrefix("https://").removePrefix("http://")
                S3Signer.signRequest(
                    rawRequest,
                    accessKey = AppSettings.s3AccessKey,
                    secretKey = AppSettings.s3SecretKey,
                    region = AppSettings.s3Region,
                    bucket = AppSettings.s3BucketName,
                    key = AppSettings.s3ContactsKey,
                    endpointHost = host
                )
            } else {
                rawRequest
            }
            
            client.newCall(request).execute().use { response ->
                val code = response.code
                val msg = response.message
                val bodySnippet = try { response.peekBody(1024).string() } catch (ex: Exception) { "" }
                
                if (response.isSuccessful || code == 404) {
                    val successMsg = "Connection OK. HTTP Status: $code ($msg)"
                    val details = "Response head/snippet:\n$bodySnippet\n\nHeaders:\n${response.headers}"
                    logDiagnostic("TEST_CONNECTION", "SUCCESS", successMsg, details)
                    ConnectionResult(true, successMsg, details)
                } else {
                    val errMsg = "HTTP error code: $code ($msg)"
                    val details = "Response Body:\n$bodySnippet\n\nHeaders:\n${response.headers}"
                    logDiagnostic("TEST_CONNECTION", "FAILED", errMsg, details)
                    ConnectionResult(false, errMsg, details)
                }
            }
        } catch (e: Exception) {
            val errMsg = e.message ?: "Unknown socket/network exception"
            val trace = e.stackTraceToString()
            logDiagnostic("TEST_CONNECTION", "EXCEPTION", errMsg, trace)
            ConnectionResult(false, errMsg, trace)
        }
    }

    data class ConnectionResult(val success: Boolean, val message: String, val details: String)

    fun uploadContacts(contacts: List<Contact>): Boolean {
        return try {
            val json = contactsAdapter.toJson(contacts)
            val requestBody = json.toRequestBody("application/json".toMediaType())
            
            val request = if (AppSettings.s3Enabled) {
                val host = AppSettings.s3Endpoint.removePrefix("https://").removePrefix("http://")
                val url = "https://$host/${AppSettings.s3BucketName}/${AppSettings.s3ContactsKey}"
                val rawRequest = Request.Builder()
                    .url(url)
                    .put(requestBody)
                    .build()
                S3Signer.signRequest(
                    rawRequest,
                    accessKey = AppSettings.s3AccessKey,
                    secretKey = AppSettings.s3SecretKey,
                    region = AppSettings.s3Region,
                    bucket = AppSettings.s3BucketName,
                    key = AppSettings.s3ContactsKey,
                    endpointHost = host
                )
            } else {
                Request.Builder()
                    .url("$BASE_URL/contacts")
                    .put(requestBody)
                    .build()
            }

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodySnippet = try { response.peekBody(1024).string() } catch (ex: Exception) { "" }
                    val message = "FAILED. HTTP ${response.code} ${response.message}"
                    val details = "Response Body:\n$bodySnippet\n\nHeaders:\n${response.headers}"
                    logDiagnostic("UPLOAD_CONTACTS", "FAILED", message, details)
                } else {
                    logDiagnostic("UPLOAD_CONTACTS", "SUCCESS", "Uploaded ${contacts.size} contacts successfully", "HTTP ${response.code}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            val message = "Exception: ${e.message}"
            logDiagnostic("UPLOAD_CONTACTS", "EXCEPTION", message, e.stackTraceToString())
            e.printStackTrace()
            false
        }
    }

    fun downloadContacts(): List<Contact>? {
        return try {
            val request = if (AppSettings.s3Enabled) {
                val host = AppSettings.s3Endpoint.removePrefix("https://").removePrefix("http://")
                val url = "https://$host/${AppSettings.s3BucketName}/${AppSettings.s3ContactsKey}"
                val rawRequest = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                S3Signer.signRequest(
                    rawRequest,
                    accessKey = AppSettings.s3AccessKey,
                    secretKey = AppSettings.s3SecretKey,
                    region = AppSettings.s3Region,
                    bucket = AppSettings.s3BucketName,
                    key = AppSettings.s3ContactsKey,
                    endpointHost = host
                )
            } else {
                Request.Builder()
                    .url("$BASE_URL/contacts")
                    .get()
                    .build()
            }

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    val result = if (!bodyString.isNullOrEmpty()) {
                        contactsAdapter.fromJson(bodyString)
                    } else {
                        emptyList()
                    }
                    logDiagnostic("DOWNLOAD_CONTACTS", "SUCCESS", "Downloaded ${result?.size ?: 0} contacts successfully", "HTTP ${response.code}")
                    result
                } else if (response.code == 404) {
                    logDiagnostic("DOWNLOAD_CONTACTS", "SUCCESS", "Downloaded empty list (HTTP 404 Not Found is treated as initial state)", "HTTP 404")
                    emptyList() // Bucket key not created yet
                } else {
                    val bodySnippet = try { response.peekBody(1024).string() } catch (ex: Exception) { "" }
                    val message = "FAILED. HTTP ${response.code} ${response.message}"
                    val details = "Response Body:\n$bodySnippet\n\nHeaders:\n${response.headers}"
                    logDiagnostic("DOWNLOAD_CONTACTS", "FAILED", message, details)
                    null
                }
            }
        } catch (e: Exception) {
            val message = "Exception: ${e.message}"
            logDiagnostic("DOWNLOAD_CONTACTS", "EXCEPTION", message, e.stackTraceToString())
            e.printStackTrace()
            null
        }
    }

    fun uploadUsers(users: List<User>): Boolean {
        return try {
            val json = usersAdapter.toJson(users)
            val requestBody = json.toRequestBody("application/json".toMediaType())
            
            val request = if (AppSettings.s3Enabled) {
                val host = AppSettings.s3Endpoint.removePrefix("https://").removePrefix("http://")
                val url = "https://$host/${AppSettings.s3BucketName}/${AppSettings.s3UsersKey}"
                val rawRequest = Request.Builder()
                    .url(url)
                    .put(requestBody)
                    .build()
                S3Signer.signRequest(
                    rawRequest,
                    accessKey = AppSettings.s3AccessKey,
                    secretKey = AppSettings.s3SecretKey,
                    region = AppSettings.s3Region,
                    bucket = AppSettings.s3BucketName,
                    key = AppSettings.s3UsersKey,
                    endpointHost = host
                )
            } else {
                Request.Builder()
                    .url("$BASE_URL/users")
                    .put(requestBody)
                    .build()
            }

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodySnippet = try { response.peekBody(1024).string() } catch (ex: Exception) { "" }
                    val message = "FAILED. HTTP ${response.code} ${response.message}"
                    val details = "Response Body:\n$bodySnippet\n\nHeaders:\n${response.headers}"
                    logDiagnostic("UPLOAD_USERS", "FAILED", message, details)
                } else {
                    logDiagnostic("UPLOAD_USERS", "SUCCESS", "Uploaded ${users.size} user accounts successfully", "HTTP ${response.code}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            val message = "Exception: ${e.message}"
            logDiagnostic("UPLOAD_USERS", "EXCEPTION", message, e.stackTraceToString())
            e.printStackTrace()
            false
        }
    }

    fun downloadUsers(): List<User>? {
        return try {
            val request = if (AppSettings.s3Enabled) {
                val host = AppSettings.s3Endpoint.removePrefix("https://").removePrefix("http://")
                val url = "https://$host/${AppSettings.s3BucketName}/${AppSettings.s3UsersKey}"
                val rawRequest = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                S3Signer.signRequest(
                    rawRequest,
                    accessKey = AppSettings.s3AccessKey,
                    secretKey = AppSettings.s3SecretKey,
                    region = AppSettings.s3Region,
                    bucket = AppSettings.s3BucketName,
                    key = AppSettings.s3UsersKey,
                    endpointHost = host
                )
            } else {
                Request.Builder()
                    .url("$BASE_URL/users")
                    .get()
                    .build()
            }

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    val result = if (!bodyString.isNullOrEmpty()) {
                        usersAdapter.fromJson(bodyString)
                    } else {
                        emptyList()
                    }
                    logDiagnostic("DOWNLOAD_USERS", "SUCCESS", "Downloaded ${result?.size ?: 0} users successfully", "HTTP ${response.code}")
                    result
                } else if (response.code == 404) {
                    logDiagnostic("DOWNLOAD_USERS", "SUCCESS", "Downloaded empty list (HTTP 404 Not Found is treated as initial state)", "HTTP 404")
                    emptyList() // Bucket key not created yet
                } else {
                    val bodySnippet = try { response.peekBody(1024).string() } catch (ex: Exception) { "" }
                    val message = "FAILED. HTTP ${response.code} ${response.message}"
                    val details = "Response Body:\n$bodySnippet\n\nHeaders:\n${response.headers}"
                    logDiagnostic("DOWNLOAD_USERS", "FAILED", message, details)
                    null
                }
            }
        } catch (e: Exception) {
            val message = "Exception: ${e.message}"
            logDiagnostic("DOWNLOAD_USERS", "EXCEPTION", message, e.stackTraceToString())
            e.printStackTrace()
            null
        }
    }
}
