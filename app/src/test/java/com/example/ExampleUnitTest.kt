package com.example

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

class ExampleUnitTest {
  private val client = OkHttpClient()
  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

  @Test
  fun testVerifyBucket() {
    try {
      println("RUNNING_FRESH_TEST_TOKEN_XYZ_3")
      val request = Request.Builder()
        .url("https://api.keyvalue.xyz/new")
        .post(FormBody.Builder().build())
        .build()
      
      client.newCall(request).execute().use { response ->
        val bodyStr = response.body?.string()?.trim() ?: ""
        val errorMsg = "KEYVALUE_XYZ: Successful=${response.isSuccessful} | HTTP=${response.code} | Token=$bodyStr"
        File("kvdb_error.txt").writeText(errorMsg)
      }
    } catch (e: Exception) {
      File("kvdb_error.txt").writeText("KEYVALUE_EXCEPTION: ${e.message}\n${e.stackTraceToString()}")
    }
  }
}















