package com.example.data

import android.content.Context
import android.content.SharedPreferences

object AppSettings {
    private const val PREFS_NAME = "corporate_phonebook_prefs"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var s3Enabled: Boolean
        get() = prefs.getBoolean("s3_enabled", false)
        set(value) = prefs.edit().putBoolean("s3_enabled", value).apply()

    var s3Endpoint: String
        get() = prefs.getString("s3_endpoint", "s3.ir-tb-1.arvanstorage.ir") ?: "s3.ir-tb-1.arvanstorage.ir"
        set(value) = prefs.edit().putString("s3_endpoint", value).apply()

    var s3AccessKey: String
        get() = prefs.getString("s3_access_key", "") ?: ""
        set(value) = prefs.edit().putString("s3_access_key", value).apply()

    var s3SecretKey: String
        get() = prefs.getString("s3_secret_key", "") ?: ""
        set(value) = prefs.edit().putString("s3_secret_key", value).apply()

    var s3BucketName: String
        get() = prefs.getString("s3_bucket_name", "phonebook") ?: "phonebook"
        set(value) = prefs.edit().putString("s3_bucket_name", value).apply()

    var s3Region: String
        get() = prefs.getString("s3_region", "ir-tb-1") ?: "ir-tb-1"
        set(value) = prefs.edit().putString("s3_region", value).apply()

    var s3ContactsKey: String
        get() = prefs.getString("s3_contacts_key", "contacts.json") ?: "contacts.json"
        set(value) = prefs.edit().putString("s3_contacts_key", value).apply()

    var s3UsersKey: String
        get() = prefs.getString("s3_users_key", "users.json") ?: "users.json"
        set(value) = prefs.edit().putString("s3_users_key", value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong("last_sync_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_sync_timestamp", value).apply()

    var syncIntervalHours: Int
        get() = prefs.getInt("sync_interval_hours", 24)
        set(value) = prefs.edit().putInt("sync_interval_hours", value).apply()

    var dns1: String
        get() = try { prefs.getString("dns_1", "217.218.127.127") ?: "217.218.127.127" } catch (e: Exception) { "217.218.127.127" }
        set(value) = try { prefs.edit().putString("dns_1", value).apply() } catch (e: Exception) {}

    var dns2: String
        get() = try { prefs.getString("dns_2", "217.218.155.155") ?: "217.218.155.155" } catch (e: Exception) { "217.218.155.155" }
        set(value) = try { prefs.edit().putString("dns_2", value).apply() } catch (e: Exception) {}
}
