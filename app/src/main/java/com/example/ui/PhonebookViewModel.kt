package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Contact
import com.example.data.Repository
import com.example.data.User
import com.example.data.AuditLog
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
        NAME_ASC,
        NAME_DESC,
        DEPT_ASC,
        DEPT_DESC
    }

    private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
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

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun updateAccount(usernameInput: String, passwordInput: String, roleInput: String) {
        viewModelScope.launch {
            if (usernameInput.trim().isEmpty() || passwordInput.isEmpty()) {
                return@launch
            }
            val updatedUser = User(
                username = usernameInput.trim().lowercase(),
                password = passwordInput,
                role = roleInput
            )
            repository.insertUser(updatedUser)
            repository.insertAuditLog(
                AuditLog(
                    actionType = "UPDATE_USER",
                    itemName = updatedUser.username,
                    details = "Role: $roleInput",
                    performedBy = _currentUser.value?.username ?: "System"
                )
            )
            showToast("account_updated")
            if (usernameInput.trim().lowercase() == _currentUser.value?.username) {
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
                    if (name.contains("Name", ignoreCase = true) || name.contains("نام")) continue
                    if (name.isEmpty() || code.isEmpty() || phone.isEmpty()) continue
                    
                    val contact = Contact(
                        fullName = name,
                        jobTitle = title,
                        department = dept,
                        shortCode = code,
                        mobileNumber = phone
                    )
                    repository.insertContact(contact)
                    importCount++
                }
            }
            if (importCount > 0) {
                showToast("contact_imported_success_$importCount")
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
            } catch (e: Exception) {
                // Handle seeding errors gracefully
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
    fun saveContact(id: Int, name: String, title: String, dept: String, code: String, phone: String) {
        viewModelScope.launch {
            val contact = Contact(
                id = if (id == 0) 0 else id,
                fullName = name.trim(),
                jobTitle = title.trim(),
                department = dept.trim(),
                shortCode = code.trim(),
                mobileNumber = phone.trim()
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
        }
    }

    fun clearAllAuditLogs() {
        viewModelScope.launch {
            repository.clearAllAuditLogs()
            showToast("Logs cleared")
        }
    }

    // REFRESH & SYNC ACTION (Simulated cloud backup sync)
    fun syncAndRefresh() {
        viewModelScope.launch {
            _isSyncing.value = true
            // Dynamic delay simulation for internet handshake
            delay(1500)
            
            // Re-seed default contacts if database got cleared, or add a new updated record to demonstrate updates
            val count = repository.getContactCount()
            if (count == 0) {
                seedDefaultContacts()
            } else {
                // Ensure a nice demo sync item: adds "Simulated Sync Contact" which proves synchronizing updates
                val syncDemo = Contact(
                    fullName = "Cloud Sync Bot / بات سرور مرکزی",
                    jobTitle = "Automated System / هسته مرکزی همگام‌ساز",
                    department = "IT Support / واحد فناوری اطلاعات",
                    shortCode = "99999",
                    mobileNumber = "+989000000000"
                )
                repository.insertContact(syncDemo)
            }
            
            _isSyncing.value = false
            showToast("sync_success")
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
