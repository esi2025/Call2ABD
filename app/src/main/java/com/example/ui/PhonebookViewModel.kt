package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Contact
import com.example.data.Repository
import com.example.data.User
import com.example.data.AuditLog
import com.example.data.CloudDbService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    object Login : Screen()
    object Dashboard : Screen()
    object UsersManagement : Screen()
}

class PhonebookViewModel(private val repository: Repository) : ViewModel() {

    private val _currentLanguage = MutableStateFlow(Language.ENGLISH)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    enum class SortOption {
        CODE_ASC,
        CODE_DESC,
        NAME_ASC,
        NAME_DESC,
        DEPT_ASC,
        DEPT_DESC
    }

    private val _sortOption = MutableStateFlow(SortOption.CODE_ASC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0)
    val syncProgress: StateFlow<Int> = _syncProgress.asStateFlow()

    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun updateAccount(oldUsername: String, usernameInput: String, passwordInput: String, roleInput: String) {
        viewModelScope.launch {
            val cleanOld = oldUsername.trim().lowercase()
            val cleanNew = usernameInput.trim().lowercase()
            if (cleanNew.isEmpty() || passwordInput.isEmpty()) {
                return@launch
            }
            if (cleanOld != cleanNew) {
                repository.deleteUserByUsername(cleanOld)
            }
            val updatedUser = User(
                username = cleanNew,
                password = passwordInput,
                role = roleInput
            )
            repository.insertUser(updatedUser)
            repository.insertAuditLog(
                AuditLog(
                    actionType = "UPDATE_USER",
                    itemName = cleanNew,
                    details = if (cleanOld != cleanNew) "Renamed from $cleanOld. Role: $roleInput" else "Role: $roleInput",
                    performedBy = _currentUser.value?.username ?: "System"
                )
            )
            showToast("account_updated")
            pushLocalStateToCloud()
            if (cleanOld == _currentUser.value?.username) {
                _currentUser.value = updatedUser
            }
        }
    }

    fun bulkImportUsers(csvContent: String) {
        viewModelScope.launch {
            var importCount = 0
            val lines = csvContent.lines()
            for (line in lines) {
                if (line.trim().isBlank()) continue
                val parts = line.split(",").map { it.trim() }
                if (parts.size >= 2) {
                    val usernameInput = parts[0].lowercase()
                    val passwordInput = parts[1]
                    if (usernameInput == "username" && passwordInput == "password") continue
                    if (usernameInput.isEmpty() || passwordInput.isEmpty()) continue
                    
                    val roleInput = if (parts.size >= 3) {
                        val r = parts[2].lowercase()
                        when (r) {
                            "admin" -> "admin"
                            "level_2" -> "level_2"
                            else -> "level_1"
                        }
                    } else "level_1"
                    
                    val user = User(username = usernameInput, password = passwordInput, role = roleInput)
                    repository.insertUser(user)
                    importCount++
                }
            }
            if (importCount > 0) {
                showToast("imported_success_$importCount")
                pushLocalStateToCloud()
            } else {
                showToast("No valid users found.")
            }
        }
    }

    fun bulkImportContacts(csvContent: String) {
        viewModelScope.launch {
            var importCount = 0
            val lines = csvContent.lines()
            for (line in lines) {
                if (line.trim().isBlank()) continue
                val delimiter = if (line.contains(";")) ";" else ","
                val parts = line.split(delimiter).map { it.trim() }
                if (parts.size >= 5) {
                    val name = parts[0]
                    val title = parts[1]
                    val dept = parts[2]
                    val code = parts[3]
                    val phone = parts[4]
                    val announced = if (parts.size >= 6) parts[5] else ""
                    if (name.contains("Name", ignoreCase = true) || name.contains("نام")) continue
                    if (name.isEmpty() || code.isEmpty() || phone.isEmpty()) continue
                    
                    val contact = Contact(
                        fullName = name,
                        jobTitle = title,
                        department = dept,
                        shortCode = code,
                        mobileNumber = phone,
                        announcedNumber = announced
                    )
                    repository.insertContact(contact)
                    importCount++
                }
            }
            if (importCount > 0) {
                showToast("contact_imported_success_$importCount")
                pushLocalStateToCloud()
                repository.insertAuditLog(
                    AuditLog(
                        actionType = "BULK_IMPORT_CONTACTS",
                        itemName = "Contacts Bulk Upload",
                        details = "Imported $importCount contacts successfully.",
                        performedBy = _currentUser.value?.username ?: "System"
                    )
                )
            } else {
                showToast("No valid contacts found in the loaded file.")
            }
        }
    }

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredContacts: StateFlow<List<Contact>> = combine(
        repository.allContacts,
        _searchQuery,
        _sortOption
    ) { contacts, query, sortOpt ->
        val filtered = if (query.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.fullName.contains(query, ignoreCase = true) ||
                contact.jobTitle.contains(query, ignoreCase = true) ||
                contact.department.contains(query, ignoreCase = true) ||
                contact.shortCode.contains(query, ignoreCase = true) ||
                contact.mobileNumber.contains(query)
            }
        }
        when (sortOpt) {
            SortOption.CODE_ASC -> filtered.sortedBy { it.shortCode }
            SortOption.CODE_DESC -> filtered.sortedByDescending { it.shortCode }
            SortOption.NAME_ASC -> filtered.sortedBy { it.fullName }
            SortOption.NAME_DESC -> filtered.sortedByDescending { it.fullName }
            SortOption.DEPT_ASC -> filtered.sortedBy { it.department }
            SortOption.DEPT_DESC -> filtered.sortedByDescending { it.department }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Seeding database with default accounts and sample bilingual contacts if empty
        viewModelScope.launch {
            try {
                if (repository.getUserCount() == 0) {
                    repository.insertUser(User("admin", "admin123", "admin"))
                    repository.insertUser(User("user", "user123", "level_1"))
                }
                if (repository.getContactCount() == 0) {
                    seedDefaultContacts()
                }
                // Automatically retrieve fresh updates from cloud database on start if cache has expired
                syncAndRefreshIfNeeded()
            } catch (e: Exception) {
                // Handle seeding errors gracefully
            }
        }
    }

    // Checks if the local cache has expired (e.g., 24 hours / once a day) before performing active sync on start
    fun syncAndRefreshIfNeeded() {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val lastSync = com.example.data.AppSettings.lastSyncTimestamp
            val intervalMillis = com.example.data.AppSettings.syncIntervalHours * 60 * 60 * 1000L
            
            // Check if Room database has actual cached contacts & users
            val hasCachedContacts = repository.getContactCount() > 0
            val hasCachedUsers = repository.getUserCount() > 0
            
            val cacheExpired = (currentTime - lastSync) >= intervalMillis
            
            if (!hasCachedContacts || !hasCachedUsers || cacheExpired) {
                println("PhonebookViewModel: Cache expired or empty. Triggering background sync.")
                syncAndRefresh(showToastOnCompletion = false)
            } else {
                println("PhonebookViewModel: Local database cache is fresh. Skipping network sync on startup.")
            }
        }
    }

    private suspend fun seedDefaultContacts() {
        val sampleContacts = listOf(
            Contact(
                fullName = "Dr. Alireza Hosseini / علیرضا حسینی",
                jobTitle = "Engineering Director / مدیر فنی مهندسی",
                department = "R&D / توسعه محصول",
                shortCode = "10021",
                mobileNumber = "+989123456789"
            ),
            Contact(
                fullName = "Sarah Connor / سارا کانر",
                jobTitle = "Operations Manager / مدیر عملیات",
                department = "Operations / واحد عملیات",
                shortCode = "10025",
                mobileNumber = "+15550199120"
            ),
            Contact(
                fullName = "Maryam Rad / مریم راد",
                jobTitle = "HR Lead / سرپرست منابع انسانی",
                department = "Human Resources / منابع انسانی",
                shortCode = "20012",
                mobileNumber = "+989351234567"
            ),
            Contact(
                fullName = "John Doe / جان دو",
                jobTitle = "Security Officer / مسئول حراست",
                department = "Security / حراست سازمان",
                shortCode = "33010",
                mobileNumber = "+15551234567"
            ),
            Contact(
                fullName = "Nima Pakzad / نیما پاکزاد",
                jobTitle = "PR Specialist / کارشناس روابط عمومی",
                department = "Public Relations / روابط عمومی",
                shortCode = "44501",
                mobileNumber = "+989121111111"
            )
        )
        for (contact in sampleContacts) {
            repository.insertContact(contact)
        }
    }

    fun setLanguage(language: Language) {
        _currentLanguage.value = language
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showToast(msg: String?) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    // AUTH ACTIONS
    fun login(usernameInput: String, passwordInput: String) {
        viewModelScope.launch {
            _loginError.value = null
            val user = repository.getUserByUsername(usernameInput.trim())
            if (user != null && user.password == passwordInput) {
                _currentUser.value = user
                _currentScreen.value = Screen.Dashboard
            } else {
                _loginError.value = "invalid" // Will resolve to local language translated string
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _searchQuery.value = ""
        _currentScreen.value = Screen.Login
    }

    // CONTACT ACTIONS (CRUD for Admin / View for Admin)
    fun saveContact(id: Int, name: String, title: String, dept: String, code: String, phone: String, announced: String = "") {
        viewModelScope.launch {
            val contact = Contact(
                id = if (id == 0) 0 else id,
                fullName = name.trim(),
                jobTitle = title.trim(),
                department = dept.trim(),
                shortCode = code.trim(),
                mobileNumber = phone.trim(),
                announcedNumber = announced.trim()
            )
            if (id == 0) {
                repository.insertContact(contact)
                repository.insertAuditLog(
                    AuditLog(
                        actionType = "ADD_CONTACT",
                        itemName = name.trim(),
                        details = "$title ($dept), Short Code: $code",
                        performedBy = _currentUser.value?.username ?: "System"
                    )
                )
            } else {
                repository.updateContact(contact)
                repository.insertAuditLog(
                    AuditLog(
                        actionType = "UPDATE_CONTACT",
                        itemName = name.trim(),
                        details = "$title ($dept), Short Code: $code",
                        performedBy = _currentUser.value?.username ?: "System"
                    )
                )
            }
            showToast("contact_saved")
            pushLocalStateToCloud()
        }
    }

    fun deleteContact(id: Int) {
        viewModelScope.launch {
            try {
                val contactList = repository.allContacts.first()
                val contact = contactList.find { it.id == id }
                val cName = contact?.fullName ?: "ID: $id"
                val cDetails = contact?.let { "${it.jobTitle} (${it.department})" } ?: ""
                repository.deleteContactById(id)
                repository.insertAuditLog(
                    AuditLog(
                        actionType = "DELETE_CONTACT",
                        itemName = cName,
                        details = cDetails,
                        performedBy = _currentUser.value?.username ?: "System"
                    )
                )
            } catch (e: Exception) {
                repository.deleteContactById(id)
            }
            showToast("contact_deleted")
            pushLocalStateToCloud()
        }
    }

    // USER ACCOUNTS ACTIONS (CRUD for Admin)
    fun createAccount(usernameInput: String, passwordInput: String, roleInput: String) {
        viewModelScope.launch {
            if (usernameInput.trim().isEmpty() || passwordInput.isEmpty()) {
                return@launch
            }
            val newUser = User(
                username = usernameInput.trim().lowercase(),
                password = passwordInput,
                role = roleInput
            )
            repository.insertUser(newUser)
            repository.insertAuditLog(
                AuditLog(
                    actionType = "ADD_USER",
                    itemName = newUser.username,
                    details = "Role: $roleInput",
                    performedBy = _currentUser.value?.username ?: "System"
                )
            )
            showToast("account_created")
            pushLocalStateToCloud()
        }
    }

    fun deleteAccount(usernameToDelete: String) {
        viewModelScope.launch {
            if (usernameToDelete == "admin" || usernameToDelete == _currentUser.value?.username) {
                // Protect default admin and active user from self-deletion
                showToast("Cannot delete default admin or active administrator account.")
                return@launch
            }
            repository.deleteUserByUsername(usernameToDelete)
            repository.insertAuditLog(
                AuditLog(
                    actionType = "DELETE_USER",
                    itemName = usernameToDelete,
                    details = "Deleted user account",
                    performedBy = _currentUser.value?.username ?: "System"
                )
            )
            showToast("account_deleted")
            pushLocalStateToCloud()
        }
    }

    fun clearAllAuditLogs() {
        viewModelScope.launch {
            repository.clearAllAuditLogs()
            showToast("Logs cleared")
        }
    }

    fun clearErrorLogs() {
        viewModelScope.launch {
            repository.clearAllAuditLogs()
            CloudDbService.clearDiagnosticLogs()
            showToast("Error logs cleared")
        }
    }

    // REFRESH & SYNC ACTION (Real KVDB/S3 cloud directory sync with connection test, progress and Persian status)
    fun syncAndRefresh(showToastOnCompletion: Boolean = true) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncProgress.value = 10
            _syncStatus.value = "در حال برقراری ارتباط با دیتابیس ابری..."
            
            val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    // Step 1: Ping / Test connections
                    val connResult = CloudDbService.testConnection()
                    if (!connResult.success) {
                        _syncProgress.value = 0
                        _syncStatus.value = "خطا در برقراری ارتباط با دیتابیس ابری!"
                        repository.insertAuditLog(
                            AuditLog(
                                actionType = "ERROR_SYNC",
                                itemName = "Connection Check Failed",
                                details = "Message: ${connResult.message}\nDetails: ${connResult.details}",
                                performedBy = "System"
                            )
                        )
                        return@withContext false
                    }
                    
                    // Signal Connection Success
                    _syncStatus.value = "اتصال موفق به دیتابیس برقرار شد."
                    _syncProgress.value = 35
                    kotlinx.coroutines.delay(850) // Small visual delay to appreciate success message
                    
                    // Step 2: Download Contacts
                    _syncStatus.value = "در حال دریافت لیست مخاطبین..."
                    _syncProgress.value = 55
                    val cloudContacts = CloudDbService.downloadContacts()
                    kotlinx.coroutines.delay(650)
                    
                    // Step 3: Download Users
                    _syncStatus.value = "در حال دریافت حساب‌های کاربری..."
                    _syncProgress.value = 75
                    val cloudUsers = CloudDbService.downloadUsers()
                    kotlinx.coroutines.delay(500)
                    
                    if (cloudContacts == null || cloudUsers == null) {
                        _syncStatus.value = "خطا در دریافت فایل‌های ابری."
                        _syncProgress.value = 0
                        val detailsBuilder = StringBuilder()
                        detailsBuilder.append("Failed to download database files from Cloud.\n")
                        detailsBuilder.append("S3 Enabled: ${com.example.data.AppSettings.s3Enabled}\n")
                        if (com.example.data.AppSettings.s3Enabled) {
                            detailsBuilder.append("Endpoint: ${com.example.data.AppSettings.s3Endpoint}\n")
                            detailsBuilder.append("Bucket: ${com.example.data.AppSettings.s3BucketName}\n")
                            detailsBuilder.append("Access Key: ${com.example.data.AppSettings.s3AccessKey.take(4)}***\n")
                        } else {
                            detailsBuilder.append("KVDB Endpoint\n")
                        }
                        repository.insertAuditLog(
                            AuditLog(
                                actionType = "ERROR_SYNC",
                                itemName = "Sync Download Null",
                                details = detailsBuilder.toString(),
                                performedBy = "System"
                            )
                        )
                        false
                    } else if (cloudContacts.isEmpty() && cloudUsers.isEmpty()) {
                        _syncStatus.value = "دیتابیس خالی است. در حال آغاز راه‌اندازی اولیه..."
                        _syncProgress.value = 85
                        // Cloud has never been initialized. Seed the cloud!
                        val localContacts = repository.allContacts.first()
                        val localUsers = repository.allUsers.first()
                        
                        var contactsToUpload = localContacts
                        if (contactsToUpload.isEmpty()) {
                            seedDefaultContacts()
                            contactsToUpload = repository.allContacts.first()
                        }
                        
                        var usersToUpload = localUsers
                        if (usersToUpload.isEmpty()) {
                            repository.insertUser(User("admin", "admin123", "admin"))
                            repository.insertUser(User("user", "user123", "level_1"))
                            usersToUpload = repository.allUsers.first()
                        }
                        
                        val uploadContactsSuccess = CloudDbService.uploadContacts(contactsToUpload)
                        val uploadUsersSuccess = CloudDbService.uploadUsers(usersToUpload)
                        val ok = uploadContactsSuccess && uploadUsersSuccess
                        if (ok) {
                            com.example.data.AppSettings.lastSyncTimestamp = System.currentTimeMillis()
                            _syncStatus.value = "بروزرسانی با موفقیت انجام شد."
                            _syncProgress.value = 100
                        } else {
                            _syncStatus.value = "خطا در آپلود اطلاعات اولیه به ابر."
                            _syncProgress.value = 0
                            repository.insertAuditLog(
                                AuditLog(
                                    actionType = "ERROR_SYNC",
                                    itemName = "Cloud Initial Seed Failed",
                                    details = "Contacts uploaded: $uploadContactsSuccess, Users uploaded: $uploadUsersSuccess",
                                    performedBy = "System"
                                )
                            )
                        }
                        ok
                    } else {
                        _syncStatus.value = "در حال بازنویسی و همگام‌سازی اطلاعات محلی..."
                        _syncProgress.value = 90
                        // Override local DB content with downloaded cloud contents as the true directory source
                        repository.deleteAllContacts()
                        repository.insertContacts(cloudContacts)
                        
                        repository.deleteAllUsers()
                        repository.insertUsers(cloudUsers)
                        
                        // Update cache timestamp upon successful down-sync
                        com.example.data.AppSettings.lastSyncTimestamp = System.currentTimeMillis()
                        _syncStatus.value = "بروزرسانی با موفقیت انجام شد."
                        _syncProgress.value = 100
                        true
                    }
                } catch (e: Exception) {
                    _syncProgress.value = 0
                    _syncStatus.value = "خطا: ${e.localizedMessage ?: e.message ?: "مشکل ارتباطی"}"
                    val errMsg = e.localizedMessage ?: e.message ?: "Unknown Exception"
                    repository.insertAuditLog(
                        AuditLog(
                            actionType = "ERROR_SYNC",
                            itemName = "Sync Exception Caught",
                            details = "Message: $errMsg\n\nStacktrace:\n${e.stackTraceToString()}",
                            performedBy = "System"
                        )
                    )
                    e.printStackTrace()
                    false
                }
            }
            
            _isSyncing.value = false
            if (success) {
                if (showToastOnCompletion) {
                    showToast("sync_success")
                }
            } else {
                if (showToastOnCompletion) {
                    showToast("cloud_sync_failed")
                }
            }
        }
    }

    private fun pushLocalStateToCloud() {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val localContacts = repository.allContacts.first()
                    val localUsers = repository.allUsers.first()
                    val contactsOk = CloudDbService.uploadContacts(localContacts)
                    val usersOk = CloudDbService.uploadUsers(localUsers)
                    if (!contactsOk || !usersOk) {
                        repository.insertAuditLog(
                            AuditLog(
                                actionType = "ERROR_SYNC",
                                itemName = "Bulk Auto-Push Failed",
                                details = "Contacts uploaded: $contactsOk, Users uploaded: $usersOk",
                                performedBy = "System"
                            )
                        )
                    }
                } catch (e: Exception) {
                    val errMsg = e.localizedMessage ?: e.message ?: "Unknown Exception"
                    repository.insertAuditLog(
                        AuditLog(
                            actionType = "ERROR_SYNC",
                            itemName = "Bulk Auto-Push Exception",
                            details = "Message: $errMsg\n\nStacktrace:\n${e.stackTraceToString()}",
                            performedBy = "System"
                        )
                    )
                    e.printStackTrace()
                }
            }
        }
    }
}

class PhonebookViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhonebookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhonebookViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
