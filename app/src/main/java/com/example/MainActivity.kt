package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.AppDatabase
import com.example.data.Repository
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PhonebookMainApp
import com.example.ui.PhonebookViewModel
import com.example.ui.PhonebookViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AppSettings & Room Database, DAO and Repository
        com.example.data.AppSettings.init(applicationContext)
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = Repository(database.contactDao(), database.userDao(), database.auditLogDao())

        // Create the Phonebook ViewModel using construction factory pattern
        val viewModel: PhonebookViewModel by viewModels {
            PhonebookViewModelFactory(repository)
        }

        enableEdgeToEdge()
        setContent {
            val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = isDark) {
                PhonebookMainApp(viewModel = viewModel)
            }
        }
    }
}
