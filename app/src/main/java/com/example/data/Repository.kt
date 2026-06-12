package com.example.data

import kotlinx.coroutines.flow.Flow

class Repository(
    private val contactDao: ContactDao,
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao
) {
    val allContacts: Flow<List<Contact>> = contactDao.getAllContactsFlow()
    val allUsers: Flow<List<User>> = userDao.getAllUsersFlow()
    val allAuditLogs: Flow<List<AuditLog>> = auditLogDao.getAllLogsFlow()

    suspend fun insertContact(contact: Contact) {
        contactDao.insertContact(contact)
    }

    suspend fun insertAuditLog(log: AuditLog) {
        auditLogDao.insertLog(log)
    }

    suspend fun clearAllAuditLogs() {
        auditLogDao.clearAllLogs()
    }

    suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(contact)
    }

    suspend fun deleteContactById(id: Int) {
        contactDao.deleteContactById(id)
    }

    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun insertUser(user: User) {
        userDao.insertUser(user)
    }

    suspend fun deleteUserByUsername(username: String) {
        userDao.deleteUserByUsername(username)
    }

    suspend fun getContactCount(): Int {
        return contactDao.getContactCount()
    }

    suspend fun getUserCount(): Int {
        return userDao.getUserCount()
    }
}
