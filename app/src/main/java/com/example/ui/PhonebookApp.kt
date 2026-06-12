package com.example.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Contact
import com.example.data.User
import com.example.ui.theme.*


// Helper to trigger direct calls or open dialer
fun makePhoneCallOrDial(context: Context, number: String) {
    val cleanNumber = number.trim()
    val hasCallPermission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED

    if (hasCallPermission) {
        try {
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)
        } catch (e: Exception) {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
        }
    } else {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(dialIntent)
    }
}

// Helper to send SMS
fun sendDirectSms(context: Context, number: String) {
    val cleanNumber = number.trim()
    try {
        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanNumber")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(smsIntent)
    } catch (e: Exception) {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$cleanNumber")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(viewIntent)
    }
}

data class CardTheme(
    val bgBrush: Brush,
    val accentColor: Color,
    val textColor: Color,
    val subtitleColor: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val iconColor: Color
)

fun getCardTheme(department: String, isDark: Boolean): CardTheme {
    val cleanDept = department.trim().uppercase()
    val hash = cleanDept.hashCode() % 5
    val absHash = if (hash < 0) hash + 5 else hash
    
    return when (absHash) {
        0 -> CardTheme(
            bgBrush = if (isDark) {
                Brush.linearGradient(colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
            } else {
                Brush.linearGradient(colors = listOf(Color(0xFFF0FDFA), Color(0xFFCCFBF1))) // Soft Teal-Mint
            },
            accentColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488), // Teal 600
            textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF115E59), // Teal 800
            subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF0F766E), // Teal 700
            badgeBg = if (isDark) Color(0xFF115E59).copy(alpha = 0.4f) else Color(0xFF99F6E4), // Teal 200
            badgeText = if (isDark) Color(0xFF2DD4BF) else Color(0xFF115E59),
            iconColor = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488)
        )
        1 -> CardTheme(
            bgBrush = if (isDark) {
                Brush.linearGradient(colors = listOf(Color(0xFF1E293B), Color(0xFF1E1E38)))
            } else {
                Brush.linearGradient(colors = listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF))) // Soft Indigo-Lavender
            },
            accentColor = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5), // Indigo 600
            textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF3730A3), // Indigo 800
            subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF4338CA), // Indigo 700
            badgeBg = if (isDark) Color(0xFF3730A3).copy(alpha = 0.4f) else Color(0xFFC7D2FE), // Indigo 200
            badgeText = if (isDark) Color(0xFF818CF8) else Color(0xFF3730A3),
            iconColor = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
        )
        2 -> CardTheme(
            bgBrush = if (isDark) {
                Brush.linearGradient(colors = listOf(Color(0xFF1E293B), Color(0xFF2E1B2C)))
            } else {
                Brush.linearGradient(colors = listOf(Color(0xFFFDF2F8), Color(0xFFFCE7F3))) // Soft Rose-Coral
            },
            accentColor = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777), // Rose 600
            textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF9D174D), // Rose 800
            subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF86198F), // Rose 900
            badgeBg = if (isDark) Color(0xFF9D174D).copy(alpha = 0.4f) else Color(0xFFFBCFE8), // Rose 200
            badgeText = if (isDark) Color(0xFFF472B6) else Color(0xFF9D174D),
            iconColor = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777)
        )
        3 -> CardTheme(
            bgBrush = if (isDark) {
                Brush.linearGradient(colors = listOf(Color(0xFF1E293B), Color(0xFF2B1F11)))
            } else {
                Brush.linearGradient(colors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))) // Soft Amber-Gold
            },
            accentColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706), // Amber 600
            textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF92400E), // Amber 800
            subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF78350F), // Amber 900
            badgeBg = if (isDark) Color(0xFF92400E).copy(alpha = 0.4f) else Color(0xFFFDE68A), // Amber 200
            badgeText = if (isDark) Color(0xFFFBBF24) else Color(0xFF78350F),
            iconColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
        )
        else -> CardTheme(
            bgBrush = if (isDark) {
                Brush.linearGradient(colors = listOf(Color(0xFF1E293B), Color(0xFF0F243A)))
            } else {
                Brush.linearGradient(colors = listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE))) // Soft Azure-Sky
            },
            accentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7), // Sky 600
            textColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF075985), // Sky 800
            subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF0369A1), // Sky 700
            badgeBg = if (isDark) Color(0xFF075985).copy(alpha = 0.4f) else Color(0xFFBAE6FD), // Sky 200
            badgeText = if (isDark) Color(0xFF38BDF8) else Color(0xFF075985),
            iconColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
        )
    }
}

@Composable
fun getTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = if (MaterialTheme.colorScheme.background == SleekSecondary) Color(0xFF334155) else Color(0xFFE2E8F0),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun getSearchFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = if (MaterialTheme.colorScheme.background == SleekSecondary) Color(0xFF1E293B) else Color(0xFFF8FAFC),
    unfocusedContainerColor = if (MaterialTheme.colorScheme.background == SleekSecondary) Color(0xFF1E293B) else Color(0xFFF8FAFC),
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = if (MaterialTheme.colorScheme.background == SleekSecondary) Color(0xFF334155) else Color(0xFFE2E8F0),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun PhonebookMainApp(viewModel: PhonebookViewModel) {
    val lang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val screen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val translate = LocalizedStrings.get(lang)

    // Layout configuration support for Bilingual RTL and LTR
    val layoutDirection = if (lang == Language.PERSIAN) LayoutDirection.Rtl else LayoutDirection.Ltr

    // Toast and message notifications dispatcher
    LaunchedEffect(toastMsg) {
        toastMsg?.let { msg ->
            val resourceText = when {
                msg == "contact_saved" -> translate.contactSavedSuccess
                msg == "contact_deleted" -> translate.contactDeletedSuccess
                msg == "account_created" -> translate.accountCreatedSuccess
                msg == "account_deleted" -> translate.accountDeletedSuccess
                msg == "sync_success" -> translate.syncSuccess
                msg.startsWith("contact_imported_success_") -> {
                    val count = msg.substringAfterLast("_")
                    if (lang == Language.ENGLISH) "Successfully imported $count contacts!" else "تعداد $count مخاطب با موفقیت وارد گردید!"
                }
                msg.startsWith("imported_success_") -> {
                    val count = msg.substringAfterLast("_")
                    if (lang == Language.ENGLISH) "Successfully imported $count users!" else "تعداد $count حساب کاربری با موفقیت وارد گردید!"
                }
                else -> msg
            }
            Toast.makeText(context, resourceText, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "screen_navigation"
                ) { targetScreen ->
                    when (targetScreen) {
                        is Screen.Login -> LoginScreen(viewModel, translate)
                        is Screen.Dashboard -> DashboardScreen(viewModel, translate)
                        is Screen.UsersManagement -> UsersManagementScreen(viewModel, translate)
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: PhonebookViewModel, translation: Translation) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val activeLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val pillContainerColor = if (isDark) Color(0xFF1E293B) else Color.White
    val activePillColor = if (isDark) Color(0xFF3B82F6) else Color(0xFF1E293B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter // Shipped upwards!
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 20.dp), // Compact padding and shifted up!
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp) // More compact spacing!
        ) {
            // Elegant Sleek Layout Logo Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Circular White Logo with subtle border (highly compact logo)
                Box(
                    modifier = Modifier
                        .size(54.dp) // Reduced from 64.dp for compactness
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color.White)
                        .border(BorderStroke(1.dp, borderColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "App Logo",
                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0F172A),
                        modifier = Modifier.size(24.dp) // Reduced from 30.dp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "PERSIAN & ENGLISH",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB), // Blue 600
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        text = translation.appTitle,
                        style = MaterialTheme.typography.titleMedium, // Reduced from headlineMedium for sleekness
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Dual switcher row: Language Pill and Theme Pill side by side!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lang Selector Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(pillContainerColor)
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(24.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (activeLang == Language.ENGLISH) activePillColor else Color.Transparent)
                            .clickable { viewModel.setLanguage(Language.ENGLISH) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "EN",
                            color = if (activeLang == Language.ENGLISH) Color.White else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (activeLang == Language.PERSIAN) activePillColor else Color.Transparent)
                            .clickable { viewModel.setLanguage(Language.PERSIAN) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "FA",
                            color = if (activeLang == Language.PERSIAN) Color.White else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Theme Selector Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(pillContainerColor)
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(24.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!isDark) activePillColor else Color.Transparent)
                            .clickable { if (isDark) viewModel.toggleDarkMode() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (activeLang == Language.ENGLISH) "Light" else "روز",
                            color = if (!isDark) Color.White else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isDark) activePillColor else Color.Transparent)
                            .clickable { if (!isDark) viewModel.toggleDarkMode() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (activeLang == Language.ENGLISH) "Dark" else "شب",
                            color = if (isDark) Color.White else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Input Fields Card panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_card"),
                shape = RoundedCornerShape(20.dp), // more compact rounded corner
                border = BorderStroke(1.dp, borderColor), // Adaptive Card border
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Sleek flat card
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp), // reduced from 24.dp
                    verticalArrangement = Arrangement.spacedBy(10.dp) // reduced from 16.dp
                ) {
                    Text(
                        text = translation.login,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Wrap Username field in LTR layout provider to avoid RTL bidi character scrambled alignment
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(translation.username, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF94A3B8)) },
                            shape = RoundedCornerShape(16.dp),
                            colors = getTextFieldColors()
                        )
                    }

                    // Wrap Password field in LTR layout provider to avoid RTL bidi character scrambled alignment
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(translation.password, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("password_input"),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF94A3B8)) },
                            trailingIcon = {
                                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(if (passwordVisible) "HIDE" else "SHOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = getTextFieldColors()
                        )
                    }

                    if (loginError != null) {
                        Text(
                            text = translation.invalidCredentials,
                            color = Color(0xFFEF4444), // Red 500
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.login(username, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp) // more compact
                            .testTag("submit_button"),
                        shape = RoundedCornerShape(12.dp), // more compact rounded corner
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = translation.login,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Friendly configuration advice helpers
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Demo Sign In / ورود دمو",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• Admin:\n   User: admin / Pass: admin123",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "• Regular User (Hides mobile numbers):\n   User: user / Pass: user123",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

enum class DashboardTab {
    HOME, UNITS, ABOUT, LOGS, ADMIN
}

@Composable
fun DashboardScreen(viewModel: PhonebookViewModel, translation: Translation) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val contacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortOption.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(DashboardTab.HOME) }
    var showContactDialog by remember { mutableStateOf(false) }
    var selectedContactForEdit by remember { mutableStateOf<Contact?>(null) }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, translation.permissionCallRequired, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF3F5FA), // Slate Sleek Background
        topBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when (activeTab) {
                                    DashboardTab.HOME -> translation.appTitle
                                    DashboardTab.UNITS -> if (activeLang == Language.ENGLISH) "Organizational Units" else "واحدهای سازمانی"
                                    DashboardTab.ABOUT -> translation.aboutApp
                                    DashboardTab.LOGS -> translation.editLogs
                                    DashboardTab.ADMIN -> translation.adminPanel
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A) // Slate 900
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E)) // Green 500 active connection indicator
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSyncing) translation.syncingState else (if (activeLang == Language.ENGLISH) "Active Directory Sync Active" else "اتصال فعال به پایگاه داده سازمانی"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B) // Slate 500
                                )
                            }
                        }

                        // Switch lang & logout actions
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // LANG SWITCHER
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable {
                                        viewModel.setLanguage(if (activeLang == Language.ENGLISH) Language.PERSIAN else Language.ENGLISH)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (activeLang == Language.ENGLISH) "FA" else "EN",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            // LOGOUT
                            IconButton(onClick = { viewModel.logout() }) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    // Search field ONLY on Home sub-tab
                    if (activeTab == DashboardTab.HOME) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text(translation.searchPlaceholder, fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Start
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("search_input"),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF94A3B8)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp), tint = Color(0xFF64748B))
                                        }
                                    }
                                },
                                colors = getSearchFieldColors()
                            )

                            // Quick Sync Sync Button
                            val rotationAnim = rememberInfiniteTransition(label = "sync_rotation")
                            val angle by rotationAnim.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "degrees"
                            )

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .clickable(enabled = !isSyncing) { viewModel.syncAndRefresh() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync",
                                    tint = if (isSyncing) Color(0xFF3B82F6) else Color(0xFF0F172A),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(if (isSyncing) angle else 0f)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Sleek Custom Bottom Nav Bar
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TAB 1: HOME
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = DashboardTab.HOME }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Contacts",
                            tint = if (activeTab == DashboardTab.HOME) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (activeLang == Language.ENGLISH) "Contacts" else "مخاطبین",
                            fontSize = 10.sp,
                            fontWeight = if (activeTab == DashboardTab.HOME) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == DashboardTab.HOME) Color(0xFF2563EB) else Color(0xFF94A3B8)
                        )
                    }

                    // TAB 2: UNITS
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = DashboardTab.UNITS }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Units",
                            tint = if (activeTab == DashboardTab.UNITS) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (activeLang == Language.ENGLISH) "Units" else "واحدها",
                            fontSize = 10.sp,
                            fontWeight = if (activeTab == DashboardTab.UNITS) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == DashboardTab.UNITS) Color(0xFF2563EB) else Color(0xFF94A3B8)
                        )
                    }

                    // TAB 3: ABOUT
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = DashboardTab.ABOUT }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = if (activeTab == DashboardTab.ABOUT) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = translation.aboutApp,
                            fontSize = 10.sp,
                            fontWeight = if (activeTab == DashboardTab.ABOUT) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == DashboardTab.ABOUT) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (currentUser?.role == "admin" || currentUser?.role == "level_2") {
                        // TAB LOGS: EDIT LOG
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = DashboardTab.LOGS }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Audit Logs",
                                tint = if (activeTab == DashboardTab.LOGS) Color(0xFF2563EB) else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = translation.editLogs,
                                fontSize = 10.sp,
                                fontWeight = if (activeTab == DashboardTab.LOGS) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == DashboardTab.LOGS) Color(0xFF2563EB) else Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // TAB 4: ADMIN
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = DashboardTab.ADMIN }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Admin Area",
                            tint = if (activeTab == DashboardTab.ADMIN) Color(0xFF2563EB) else Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (activeLang == Language.ENGLISH) "Admin Panel" else "پنل مدیریت",
                            fontSize = 10.sp,
                            fontWeight = if (activeTab == DashboardTab.ADMIN) FontWeight.Bold else FontWeight.Medium,
                            color = if (activeTab == DashboardTab.ADMIN) Color(0xFF2563EB) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeTab == DashboardTab.HOME && currentUser?.role == "admin") {
                FloatingActionButton(
                    onClick = {
                        selectedContactForEdit = null
                        showContactDialog = true
                    },
                    containerColor = Color(0xFF0F172A), // Slate 900
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_contact_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add new contact",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            when (activeTab) {
                DashboardTab.HOME -> {
                    // Modern elegant sorting row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Sort Icon",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF64748B)
                        )
                        Text(
                            text = "${translation.sortBy}:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val options = listOf(
                                PhonebookViewModel.SortOption.NAME_ASC to translation.sortNameAsc,
                                PhonebookViewModel.SortOption.NAME_DESC to translation.sortNameDesc,
                                PhonebookViewModel.SortOption.DEPT_ASC to translation.sortDeptAsc,
                                PhonebookViewModel.SortOption.DEPT_DESC to translation.sortDeptDesc
                            )
                            items(options) { (option, label) ->
                                val isSelected = currentSort == option
                                val bgCol = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
                                val textCol = if (isSelected) Color.White else Color(0xFF475569)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(bgCol)
                                        .clickable { viewModel.updateSortOption(option) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = textCol
                                    )
                                }
                            }
                        }
                    }

                    if (contacts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = translation.noContactsFound,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                        ) {
                            items(contacts, key = { it.id }) { contact ->
                                val userRole = currentUser?.role
                                val canViewMobile = userRole == "admin" || userRole == "level_2"
                                val canCallAndSms = userRole == "admin" || userRole == "level_2" || userRole == "level_1"
                                ContactCard(
                                    contact = contact,
                                    canViewMobile = canViewMobile,
                                    canCallAndSms = canCallAndSms,
                                    isAdmin = userRole == "admin",
                                    translation = translation,
                                    onEdit = {
                                        selectedContactForEdit = contact
                                        showContactDialog = true
                                    },
                                    onDelete = {
                                        contactToDelete = contact
                                    },
                                    onRequestCall = {
                                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                            callPermissionLauncher.launch(android.Manifest.permission.CALL_PHONE)
                                        }
                                        makePhoneCallOrDial(context, contact.mobileNumber)
                                    },
                                    onRequestSms = {
                                        sendDirectSms(context, contact.mobileNumber)
                                    }
                                )
                            }
                        }
                    }
                }

                DashboardTab.UNITS -> {
                    val departmentsList = contacts.map { it.department }.distinct().sorted()
                    if (departmentsList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (activeLang == Language.ENGLISH) "No departments found." else "هیچ واحدی تعریف نشده است.",
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                        ) {
                            items(departmentsList) { dept ->
                                val deptCount = contacts.count { it.department == dept }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setSearchQuery(dept)
                                            activeTab = DashboardTab.HOME
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEFF6FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = null,
                                                tint = Color(0xFF3B82F6),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = dept,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = if (activeLang == Language.ENGLISH) "View directory" else "مشاهده دفترچه تلفن",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(30.dp))
                                                .background(Color(0xFFEFF6FF))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "$deptCount " + if (activeLang == Language.ENGLISH) "contacts" else "مورد",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2563EB)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                DashboardTab.ABOUT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Card 1: App Info Card with Sleek Blue Accent
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Corporate phone book representation icon
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF2563EB).copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Text(
                                    text = translation.appTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                )

                                Text(
                                    text = if (activeLang == Language.ENGLISH) "Version 1.2.0" else "نسخه ۱.۲.۰",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.08f))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                )

                                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                                if (activeLang == Language.PERSIAN) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = "ویژگی‌های کلیدی سامانه:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "• سامانه تلفن سازمانی شرکت عمران آذرستان (پروژه بوشهر) جهت ارتباط بلادرنگ درون کارگاهی.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF475569),
                                            lineHeight = 18.sp
                                        )
                                        Text(
                                            text = "• سطوح دسترسی چندگانه هوشمند جهت همگام‌سازی و نمایش انتخابی اطلاعات تماس همکاران.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF475569),
                                            lineHeight = 18.sp
                                        )
                                        Text(
                                            text = "• جستجوی پیشرفته، مرتب‌سازی دوطرفه الفبایی/واحدی، و ذخیره‌سازی محلی دیتابیس.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF475569),
                                            lineHeight = 18.sp
                                        )
                                        Text(
                                            text = "• ارسال مستقیم پیامک و شماره‌گیری سریع بدون نیاز به باز کردن دفترچه تلفن اصلی گوشی.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF475569),
                                            lineHeight = 18.sp
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = "Key System Features:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "• Designed to facilitate direct communication for Omran Azarestan Construction Company (Bushehr Project).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF475569),
                                            lineHeight = 18.sp
                                        )
                                        Text(
                                            text = "• Tiered security roles for live directory synchronization and selective mobile visibility.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF475569),
                                            lineHeight = 18.sp
                                        )
                                        Text(
                                            text = "• Offline-first SQLite persistence, instant multi-field sorting, and rapid SMS/Call actions.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF475569),
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Card 2: Developer Presentation (Mehdi Esmaeili)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Deep Slate 900
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar / Badge representation
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Text(
                                    text = if (activeLang == Language.ENGLISH) "DEVELOPER & PRODUCER" else "تهیه کننده سامانه",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8), // Cyan 400 accent
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = translation.developerName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )

                                Text(
                                    text = translation.developerTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))

                                // Projects / Location info tag
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (activeLang == Language.ENGLISH) "Bushehr Project" else "پروژه بوشهر",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                DashboardTab.LOGS -> {
                    val logs by viewModel.allAuditLogs.collectAsStateWithLifecycle()
                    if (currentUser?.role == "admin" || currentUser?.role == "level_2") {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Logs Header / Actions (Clear Logs option only for admin)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = translation.editLogs,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )

                                if (currentUser?.role == "admin") {
                                    Button(
                                        onClick = { viewModel.clearAllAuditLogs() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = translation.clearLogs,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            if (logs.isEmpty()) {
                                // Empty state
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.List,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = translation.noLogsFound,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(logs) { log ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val (label, bg, textCol) = when (log.actionType) {
                                                        "ADD_CONTACT", "ADD_USER" -> Triple(translation.logAdd, Color(0xFFDCFCE7), Color(0xFF15803D))
                                                        "DELETE_CONTACT", "DELETE_USER" -> Triple(translation.logDelete, Color(0xFFFEE2E2), Color(0xFFB91C1C))
                                                        else -> Triple(translation.logUpdate, Color(0xFFFEF9C3), Color(0xFFA16207))
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(30.dp))
                                                            .background(bg)
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = textCol
                                                        )
                                                    }

                                                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                                    val formattedTime = sdf.format(java.util.Date(log.timestamp))
                                                    Text(
                                                        text = formattedTime,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }

                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = log.itemName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF0F172A),
                                                        textAlign = if (activeLang == Language.PERSIAN) TextAlign.Right else TextAlign.Left,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    if (log.details.isNotEmpty()) {
                                                        Text(
                                                            text = log.details,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = Color(0xFF475569),
                                                            textAlign = if (activeLang == Language.PERSIAN) TextAlign.Right else TextAlign.Left,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }

                                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = Color(0xFF2563EB),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${translation.logPerformedBy}: ${log.performedBy}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF2563EB)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = if (activeLang == Language.ENGLISH) "Access Denied" else "عدم دسترسی کافی",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                DashboardTab.ADMIN -> {
                    if (currentUser?.role == "admin") {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Admin Active Info Card
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Slate 900
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = if (activeLang == Language.ENGLISH) "Administrator Active" else "نشست مدیریت فعال",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${currentUser?.username} • ${translation.adminRole}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            // Actions
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    // Action 1: Navigation to Account Manager
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.navigateTo(Screen.UsersManagement) }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEFF6FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color(0xFF3B82F6),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = translation.userAccountsManager,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = if (activeLang == Language.ENGLISH) "Create & manage phonebook user accounts" else "ایجاد و مدیریت حساب‌های کاربری سیستم",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    // Action 2: LDAP Synchronization
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(enabled = !isSyncing) { viewModel.syncAndRefresh() }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFECFDF5)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = translation.syncRefreshButton,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = if (isSyncing) translation.syncingState else (if (activeLang == Language.ENGLISH) "Synchronize contacts from organization database" else "ردیف‌سازی اطلاعات مخاطبین با سرور اصلی"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSyncing) Color(0xFF10B981) else Color(0xFF64748B)
                                            )
                                        }
                                        if (isSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF10B981)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val isDark = MaterialTheme.colorScheme.background == SleekSecondary

                            // CSV Bulk Import Card
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981).copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (activeLang == Language.ENGLISH) "Bulk Import Contacts (CSV)" else "بارگذاری گروهی اعضا (CSV)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (activeLang == Language.ENGLISH) "Upload complete list at once" else "افزودن کل اعضای تلفن سازمانی بصورت یکجا",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))

                                    // Columns description
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (activeLang == Language.ENGLISH) "CSV format columns (with or without headers):" else "ترتیب ستون‌های فایل CSV نمونه:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                        )
                                        Text(
                                            text = if (activeLang == Language.ENGLISH) 
                                                "1. Full Name | 2. Job Title | 3. Department | 4. Short Code | 5. Mobile"
                                                else "۱. نام و خانوادگی | ۲. پست سازمانی | ۳. واحد سازمانی | ۴. کد کوتاه | ۵. شماره موبایل",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Template File Content
                                    val csvContentTemplate = "Full Name,Job Title,Department,Short Code,Mobile\n" +
                                            "علیرضا احمدی,مدیر پروژه,فنی مهندسی,10201,+989120000001\n" +
                                            "حسین رضایی,سرپرست کارگاه,اجرایی,20302,+989120000002"

                                    val filePickerLauncher = rememberLauncherForActivityResult(
                                        contract = ActivityResultContracts.GetContent()
                                    ) { uri: Uri? ->
                                        uri?.let {
                                            try {
                                                val inputStream = context.contentResolver.openInputStream(uri)
                                                val text = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                                                viewModel.bulkImportContacts(text)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // 1. Download Template Button
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val file = java.io.File(context.cacheDir, "contacts_template.csv")
                                                    file.writeText(csvContentTemplate)
                                                    
                                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "com.example.fileprovider",
                                                        file
                                                    )
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "text/csv"
                                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(intent, "Share/Save Template"))
                                                } catch (e: Exception) {
                                                    // Clipboard backup
                                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    val clip = android.content.ClipData.newPlainText("CSV Template", csvContentTemplate)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, if (activeLang == Language.ENGLISH) "Template copied to clipboard!" else "قالب با موفقیت در کلیپ‌بورد کپی شد!", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                                            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (activeLang == Language.ENGLISH) "Get Template" else "دریافت فایل قالب",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // 2. Select File Button
                                        Button(
                                            onClick = {
                                                filePickerLauncher.launch("*/*")
                                            },
                                            modifier = Modifier.weight(1.3f).height(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.List,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (activeLang == Language.ENGLISH) "Upload CSV" else "بارگذاری فایل CSV",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFEF3C7)), // Warm yellow background
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706), // Warm orange lock link
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (activeLang == Language.ENGLISH) "Administrative Restricted Space" else "محدوده مدیریت - سطح دسترسی ناکافی",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = if (activeLang == Language.ENGLISH) "You are currently signed in as a regular employee. Access to user accounts database and syncing triggers are reserved for Administrators." else "شما با حساب کاربری عادی وارد شده‌اید. تغییرات بر روی کاربران و همگام‌سازی کلی اطلاعات تنها برای مدیران سیستم قابل دسترسی است.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 16.sp
                                        )
                                    }

                                    // Demo quick administrator session override trigger
                                    Button(
                                        onClick = { viewModel.login("admin", "admin123") },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0F172A),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(
                                            text = if (activeLang == Language.ENGLISH) "Elevate Privileges (Admin Session)" else "ارتقای سطح دسترسی (نشست ادمین دمو)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE OR EDIT CONTACT DIALOG
    if (showContactDialog) {
        ContactFormDialog(
            contact = selectedContactForEdit,
            translation = translation,
            onDismiss = { showContactDialog = false },
            onSave = { name, title, dept, code, mobile ->
                viewModel.saveContact(
                    id = selectedContactForEdit?.id ?: 0,
                    name = name,
                    title = title,
                    dept = dept,
                    code = code,
                    phone = mobile
                )
                showContactDialog = false
            }
        )
    }

    // CONFIRM DELETE CONTACT DIALOG
    if (contactToDelete != null) {
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text(translation.deleteContact) },
            text = { Text(translation.deleteContactConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        contactToDelete?.let { viewModel.deleteContact(it.id) }
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(translation.deleteContact)
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text(translation.cancel)
                }
            }
        )
    }
}

fun getAvatarColors(name: String): Pair<Color, Color> {
    val cleanName = name.trim().uppercase()
    val firstChar = cleanName.firstOrNull() ?: 'A'
    return when {
        firstChar in "AEIMQUY" || firstChar.toInt() in 1575..1582 -> Pair(Color(0xFFEEF2FF), Color(0xFF4F46E5)) // Indigo / Persian Alif group
        firstChar in "BFJNRVZ" || firstChar.toInt() in 1583..1590 -> Pair(Color(0xFFECFDF5), Color(0xFF059669)) // Emerald
        firstChar in "CGKOSWX" || firstChar.toInt() in 1591..1609 -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706)) // Amber
        else -> Pair(Color(0xFFEFF6FF), Color(0xFF2563EB)) // Blue
    }
}

@Composable
fun ContactCard(
    contact: Contact,
    canViewMobile: Boolean,
    canCallAndSms: Boolean, // New parameter for Level 1 support
    isAdmin: Boolean,
    translation: Translation,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRequestCall: () -> Unit,
    onRequestSms: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isDarkTheme = MaterialTheme.colorScheme.background == SleekSecondary
    val theme = getCardTheme(contact.department, isDarkTheme)
    val (avatarBg, avatarText) = getAvatarColors(contact.fullName)
    val firstLetter = contact.fullName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C"
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "ChevronRotation")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { expanded = !expanded }
            .testTag("contact_card_${contact.shortCode}"),
        shape = RoundedCornerShape(12.dp), // tight corners
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (expanded) theme.accentColor.copy(alpha = 0.5f) else if (isDarkTheme) Color(0xFF334155) else Color(0xFFF2F4F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 2.dp else 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (expanded) theme.accentColor.copy(alpha = 0.08f) else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 6.dp) // very compact padding!
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Subtle vertical colored indicator bar on the edge (highly distinctive and attractive!)
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(theme.accentColor)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Elegant Rounded Avatar (highly compact: 32.dp)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) avatarBg.copy(alpha = 0.25f) else avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = firstLetter,
                        color = if (isDarkTheme) theme.accentColor else avatarText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Body info - Name and Short Code
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = contact.fullName,
                        fontSize = 13.sp, // compact font size
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(1.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (translation.shortCode.contains("کد")) "کد داخلی:" else "Ext:",
                            fontSize = 10.sp,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                        Text(
                            text = contact.shortCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Call and SMS buttons right in the main row if canCallAndSms!
                if (canCallAndSms) {
                    // Call Button
                    IconButton(
                        onClick = { onRequestCall() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)) // Emerald Green
                            .testTag("call_button_quick_${contact.shortCode}"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // SMS Button
                    IconButton(
                        onClick = { onRequestSms() },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6)) // Indigo Blue
                            .testTag("sms_button_quick_${contact.shortCode}"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "SMS",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Modern Chevron icon indicating clickability / expanded details
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8),
                    modifier = Modifier
                        .rotate(rotation)
                        .size(18.dp)
                )
            }

            // Expanded content section containing extra details and admin edit/delete buttons
            if (expanded) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0).copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Job Title Detail
                    DetailItemRow(
                        icon = Icons.Default.Person,
                        label = translation.jobTitle,
                        value = contact.jobTitle,
                        accentColor = theme.accentColor
                    )

                    // Department Detail
                    DetailItemRow(
                        icon = Icons.Default.Home,
                        label = translation.department,
                        value = contact.department,
                        accentColor = theme.accentColor
                    )

                    // Mobile Number Detail
                    DetailItemRow(
                        icon = Icons.Default.Phone,
                        label = translation.mobileNumber,
                        value = if (canViewMobile) contact.mobileNumber else translation.mobileNumberHidden,
                        accentColor = theme.accentColor,
                        isDimmed = !canViewMobile
                    )
                }

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                contentColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit contact",
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        FilledIconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFFEF2F2),
                                contentColor = Color(0xFFEF4444)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete contact",
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* OUTDATED BUTTON BLOCK:
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Call button
                IconButton(
                    onClick = onRequestCall,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)) // Green 500
                        .testTag("call_button_${contact.shortCode}"),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = translation.callAction,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // SMS
                IconButton(
                    onClick = onRequestSms,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6)) // Blue 500
                        .testTag("sms_button_${contact.shortCode}"),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = translation.smsAction,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Admin (Edit / Delete)
                if (isAdmin) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit contact",
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFF475569)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF2F2))
                            .border(BorderStroke(1.dp, Color(0xFFFEE2E2)), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete contact",
                            modifier = Modifier.size(13.dp),
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}
*/

@Composable
fun ContactFormDialog(
    contact: Contact?,
    translation: Translation,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(contact?.fullName ?: "") }
    var title by remember { mutableStateOf(contact?.jobTitle ?: "") }
    var dept by remember { mutableStateOf(contact?.department ?: "") }
    var code by remember { mutableStateOf(contact?.shortCode ?: "") }
    var mobile by remember { mutableStateOf(contact?.mobileNumber ?: "") }

    var nameError by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf(false) }
    var deptError by remember { mutableStateOf(false) }
    var codeError by remember { mutableStateOf(false) }
    var mobileError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (contact == null) translation.addContact else translation.editContact,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text(translation.fullName) },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth().testTag("add_name_input"),
                    singleLine = true,
                    supportingText = { if (nameError) Text(translation.validationRequired) },
                    colors = getTextFieldColors()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = it.isBlank()
                    },
                    label = { Text(translation.jobTitle) },
                    isError = titleError,
                    modifier = Modifier.fillMaxWidth().testTag("add_title_input"),
                    singleLine = true,
                    supportingText = { if (titleError) Text(translation.validationRequired) },
                    colors = getTextFieldColors()
                )

                OutlinedTextField(
                    value = dept,
                    onValueChange = {
                        dept = it
                        deptError = it.isBlank()
                    },
                    label = { Text(translation.department) },
                    isError = deptError,
                    modifier = Modifier.fillMaxWidth().testTag("add_dept_input"),
                    singleLine = true,
                    supportingText = { if (deptError) Text(translation.validationRequired) },
                    colors = getTextFieldColors()
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        codeError = it.isBlank() || it.length != 5 || it.any { c -> !c.isDigit() }
                    },
                    label = { Text(translation.shortCode) },
                    isError = codeError,
                    modifier = Modifier.fillMaxWidth().testTag("add_code_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        if (codeError) {
                            Text(if (code.isBlank()) translation.validationRequired else "Must be 5 digits / باید ۵ رقمی باشد")
                        }
                    },
                    colors = getTextFieldColors()
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = {
                        mobile = it
                        mobileError = it.isBlank()
                    },
                    label = { Text(translation.mobileNumber) },
                    isError = mobileError,
                    modifier = Modifier.fillMaxWidth().testTag("add_mobile_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    supportingText = { if (mobileError) Text(translation.validationRequired) },
                    colors = getTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isNameErr = name.isBlank()
                    val isTitleErr = title.isBlank()
                    val isDeptErr = dept.isBlank()
                    val isCodeErr = code.isBlank() || code.length != 5 || code.any { !it.isDigit() }
                    val isMobileErr = mobile.isBlank()

                    nameError = isNameErr
                    titleError = isTitleErr
                    deptError = isDeptErr
                    codeError = isCodeErr
                    mobileError = isMobileErr

                    if (!isNameErr && !isTitleErr && !isDeptErr && !isCodeErr && !isMobileErr) {
                        onSave(name, title, dept, code, mobile)
                    }
                }
            ) {
                Text(translation.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(translation.cancel)
            }
        }
    )
}

private fun saveTemplateCsvToDownloads(context: Context): Boolean {
    val csvContent = "username,password,role\nsara_smith,sara123,user\nalireza_admin,admin987,admin\njohn_doe,doe555,user\n"
    return try {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "users_template.csv")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
        }
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(csvContent.toByteArray())
            }
            true
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

@Composable
fun UsersManagementScreen(viewModel: PhonebookViewModel, translation: Translation) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val activeLang by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("level_1") }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var editingUser by remember { mutableStateOf<User?>(null) }

    var usernameErr by remember { mutableStateOf(false) }
    var passwordErr by remember { mutableStateOf(false) }

    // User Directory Search State
    var userSearchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, userSearchQuery) {
        if (userSearchQuery.isBlank()) {
            users
        } else {
            users.filter { u ->
                u.username.contains(userSearchQuery, ignoreCase = true) ||
                u.role.contains(userSearchQuery, ignoreCase = true)
            }
        }
    }

    // Role-specific theme helper for UI decoration
    fun getRoleStyle(role: String): Triple<Color, Color, String> {
        return when (role) {
            "admin" -> Triple(
                Color(0xFF6366F1), // Indigo
                if (isDark) Color(0xFF1E1E38) else Color(0xFFEEF2FF), // Violet tint
                if (activeLang == Language.ENGLISH) "Administrator" else "مدیر ارشد سیستم (ادمین)"
            )
            "level_2" -> Triple(
                Color(0xFF10B981), // Emerald
                if (isDark) Color(0xFF132A21) else Color(0xFFECFDF5), // Green tint
                if (activeLang == Language.ENGLISH) "Level 2 Access" else "کاربر سطح ۲ (دسترسی کامل)"
            )
            else -> Triple(
                Color(0xFF3B82F6), // Blue / Legacy user / level_1
                if (isDark) Color(0xFF112240) else Color(0xFFEFF6FF), // Blue tint
                if (activeLang == Language.ENGLISH) "Level 1 Access" else "کاربر سطح ۱ (دسترسی محدود)"
            )
        }
    }

    // File picker launcher for CSV import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val csvContent = stream?.bufferedReader()?.use { it.readText() } ?: ""
                viewModel.bulkImportUsers(csvContent)
            } catch (e: Exception) {
                Toast.makeText(context, if (activeLang == Language.ENGLISH) "Error reading file" else "خطا در خواندن فایل", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                border = BorderStroke(1.dp, borderColor),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.navigateTo(Screen.Dashboard) }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to list",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = translation.userAccountsManager,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // EXPANDABLE PERMISSION LEVEL GUIDE PANEL
            var isGuideExpanded by remember { mutableStateOf(true) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFAFAFA)
                ),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isGuideExpanded = !isGuideExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (activeLang == Language.ENGLISH) "Access Level Visual Guide" else "راهنمای سطوح دسترسی کاربران سازمان",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isGuideExpanded) "Collapse" else "Expand",
                            tint = Color(0xFF64748B),
                            modifier = Modifier
                                .rotate(if (isGuideExpanded) 180f else 0f)
                                .size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isGuideExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                            
                            // Level 1 Info Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDark) Color(0xFF112240) else Color(0xFFEFF6FF))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (activeLang == Language.ENGLISH) "Lvl 1 / Limited" else "سطح ۱ / محدود",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3B82F6)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (activeLang == Language.ENGLISH) "Hidden mobile numbers" else "شماره‌های موبایل پنهان",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (activeLang == Language.ENGLISH) 
                                            "Only sees the name, department, job title, and 5-digit short code. Action utilities (Call/SMS) are completely hidden to enforce privacy." 
                                            else "فقط نام مخاطبان، واحد سازمانی، پست سازمانی و کد کوتاه ۵ رقمی را می‌بیند. دکمه‌های تماس مستقیم و ارسال پیامک حذف شده‌اند.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Level 2 Info Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDark) Color(0xFF132A21) else Color(0xFFECFDF5))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (activeLang == Language.ENGLISH) "Lvl 2 / Full" else "سطح ۲ / کامل",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (activeLang == Language.ENGLISH) "View directory + Mobile numbers" else "مشاهده مشخصات کامل با موبایل",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (activeLang == Language.ENGLISH) 
                                            "Can view full directory details including mobile numbers. Action utilities (Direct Call and Direct SMS) are completely functional." 
                                            else "می‌تواند مشخصات کامل را همراه با شماره موبایل مشاهده نماید. امکان برقراری تماس و ارسال پیامک برای وی باز است.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Admin Info Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDark) Color(0xFF1E1E38) else Color(0xFFEEF2FF))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (activeLang == Language.ENGLISH) "Admin" else "ادمین",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6366F1)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (activeLang == Language.ENGLISH) "Administrative capabilities" else "مدیر ارشد با تمام دسترسی‌ها",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (activeLang == Language.ENGLISH) 
                                            "All privileges enabled. Can insert, update, or delete directory contacts, as well as register, modify, or remove user credentials." 
                                            else "دارای کلیه اختیارات سیستم. ویرایش و حذف مخاطبان دفترچه تلفن سازمان + ثبت، ویرایش، و حذف کاربران سازمان.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // REGISTER / EDIT USER FORM PANEL
            Card(
                modifier = Modifier.fillMaxWidth().testTag("create_account_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (editingUser != null) {
                            if (activeLang == Language.ENGLISH) "Edit Account: ${editingUser?.username}" else "ویرایش حساب کاربری: ${editingUser?.username}"
                        } else {
                            translation.createAccountHeader
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Ensure English inputs are rendered left-to-right to avoid scrambled bidi typed characters
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = newUsername,
                            onValueChange = {
                                newUsername = it
                                usernameErr = it.isBlank()
                            },
                            label = { Text(translation.username) },
                            isError = usernameErr,
                            enabled = editingUser == null, // Prevent editing the username primary key
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_username_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = getSearchFieldColors()
                        )
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                passwordErr = it.isBlank()
                            },
                            label = { Text(translation.password) },
                            isError = passwordErr,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_password_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = getSearchFieldColors()
                        )
                    }

                    // Level Selection Options Custom Beautified Layout!
                    Text(
                        text = if (activeLang == Language.ENGLISH) "Select Level of Access:" else "تعیین سطح دسترسی و نقش کاربر:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val rolesList = listOf(
                            Triple("level_1", translation.level1Role, if (activeLang == Language.ENGLISH) "Only views basic details. Mobile number hidden. (No Call/SMS)" else "رویت نام، واحد، پست و کد کوتاه بدون شماره موبایل"),
                            Triple("level_2", translation.level2Role, if (activeLang == Language.ENGLISH) "Views basic details + Mobile. Can call/sms." else "رویت اطلاعات کامل همراه با موبایل + امکان تماس مستقیم و پیامک"),
                            Triple("admin", translation.adminRole, if (activeLang == Language.ENGLISH) "Full capabilities. Manages contacts & accounts." else "مدیر با کلیه اختیارات، مدیریت مخاطبان دفترچه و اکانت‌های کاربران")
                        )

                        rolesList.forEach { (roleKey, roleLabel, roleDesc) ->
                            val isSelected = selectedRole == roleKey
                            val selectionColor = when (roleKey) {
                                "admin" -> Color(0xFF6366F1) // Indigo
                                "level_2" -> Color(0xFF10B981) // Emerald
                                else -> Color(0xFF3B82F6) // Blue
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRole = roleKey }
                                    .testTag("role_${roleKey}_card"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) selectionColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) selectionColor else borderColor
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedRole = roleKey },
                                        colors = RadioButtonDefaults.colors(selectedColor = selectionColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = roleLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) selectionColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = roleDesc,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (editingUser != null) {
                            Button(
                                onClick = {
                                    editingUser = null
                                    newUsername = ""
                                    newPassword = ""
                                    selectedRole = "level_1"
                                    usernameErr = false
                                    passwordErr = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF334155) else Color(0xFF64748B),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (activeLang == Language.ENGLISH) "Cancel" else "لغو",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val userBlank = newUsername.isBlank()
                                val passBlank = newPassword.isBlank()
                                usernameErr = userBlank
                                passwordErr = passBlank

                                if (!userBlank && !passBlank) {
                                    if (editingUser != null) {
                                        viewModel.updateAccount(newUsername, newPassword, selectedRole)
                                    } else {
                                        viewModel.createAccount(newUsername, newPassword, selectedRole)
                                    }
                                    newUsername = ""
                                    newPassword = ""
                                    selectedRole = "level_1"
                                    editingUser = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(if (editingUser != null) 1.5f else 1f).height(48.dp).testTag("create_account_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (editingUser != null) {
                                    if (activeLang == Language.ENGLISH) "Save Edit" else "ذخیره تغییرات"
                                } else {
                                    translation.createAccount
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // CSV GROUP IMPORT PANEL (یک فایل تمپلیت ارائه دهد)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("bulk_upload_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (activeLang == Language.ENGLISH) "Bulk Import User Accounts" else "بارگذاری گروهی حساب‌های کاربری",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = if (activeLang == Language.ENGLISH) 
                            "Import users from CSV. File structure should be as follows on each row (omit headers if desired):\nusername,password,role (admin / level_1 / level_2)"
                            else "بارگذاری حساب‌های کاربری از طریق فایل متنی CSV. ساختار ستون‌های هر ردیف به این ترتیب می‌باشد:\nusername,password,role (admin / level_1 / level_2)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .border(BorderStroke(0.5.dp, borderColor), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "username,password,role\nsara_smith,sara123,level_1\nalireza_admin,admin987,admin\njohn_doe,doe555,level_2",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Users CSV Template", "username,password,role\nsara123,pass123,level_1\nalireza_admin,admin987,admin\nmaryam_rad,maryam543,level_2")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, if (activeLang == Language.ENGLISH) "Template copied to clipboard!" else "قالب الگو کپی گردید!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (activeLang == Language.ENGLISH) "Copy Template" else "کپی قالب فایل الگو",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Button(
                            onClick = {
                                filePickerLauncher.launch("text/*")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (activeLang == Language.ENGLISH) "Upload CSV" else "بارگذاری فایل CSV",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // LIST REGISTERED USERS DIRECTORY HEADER + SEARCH FILTER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = translation.accountList,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Render dynamic counter badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${filteredUsers.size} " + if (activeLang == Language.ENGLISH) "accounts" else "حساب",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Interactive Live Account Search Bar (Reduces layout clutter dynamically!)
            OutlinedTextField(
                value = userSearchQuery,
                onValueChange = { userSearchQuery = it },
                placeholder = {
                    Text(
                        text = if (activeLang == Language.ENGLISH) "Search accounts by username or role..." else "جستجوی حساب‌ها با نام کاربری یا سطح دسترسی...",
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (userSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { userSearchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("user_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = getSearchFieldColors()
            )

            // Dynamic User Database List
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (filteredUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (userSearchQuery.isEmpty()) translation.noUsersFound else (if (activeLang == Language.ENGLISH) "No matching accounts found." else "هیچ حسابی منطبق بر جستجوی شما یافت نشد."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column {
                        filteredUsers.forEachIndexed { index, user ->
                            val (roleColor, roleBg, roleLabel) = getRoleStyle(user.role)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left details column with neat icons
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Colored profile bubble representing the user
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(roleColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (user.role) {
                                                "admin" -> Icons.Default.Star
                                                "level_2" -> Icons.Default.Phone
                                                else -> Icons.Default.Lock
                                            },
                                            contentDescription = null,
                                            tint = roleColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column {
                                        Text(
                                            text = user.username,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Visual Badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(roleBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = roleLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = roleColor
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Custom edit account button
                                    IconButton(onClick = {
                                        editingUser = user
                                        newUsername = user.username
                                        newPassword = user.password
                                        selectedRole = user.role
                                        usernameErr = false
                                        passwordErr = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit account",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Prevent deleting default "admin" account or current active user
                                    if (user.username != "admin") {
                                        IconButton(onClick = { userToDelete = user }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete account",
                                                tint = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                            if (index < filteredUsers.size - 1) {
                                HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    // CONFIRM DELETE ACCOUNT DIALOG
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text(translation.deleteContact) },
            text = { Text(translation.deleteAccountConfirm) },
            confirmButton = {
                TextButton(
                    onClick = {
                        userToDelete?.let { viewModel.deleteAccount(it.username) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text(translation.deleteContact)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text(translation.cancel)
                }
            }
        )
    }
}

@Composable
fun DetailItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    isBadge: Boolean = false,
    badgeBg: Color = Color.Transparent,
    badgeText: Color = Color.Unspecified,
    isDimmed: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        if (isBadge) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeText
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 12.sp,
                color = if (isDimmed) Color(0xFF94A3B8) else Color(0xFF1E293B),
                fontWeight = if (isDimmed) FontWeight.Normal else FontWeight.SemiBold
            )
        }
    }
}
