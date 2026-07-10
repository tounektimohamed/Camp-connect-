package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CampNotification
import com.example.data.CampSettings
import com.example.data.CampUser
import com.example.data.CampAlbumPost
import com.example.data.AlbumComment
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CampViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: CampViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Force Right-to-Left layout flow for Arabic support
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("main_scaffold")
                    ) { innerPadding ->
                        CampConnectApp(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CampConnectApp(
    viewModel: CampViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf("splash") }

    // Display toasts for Firestore transaction results or errors
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    // Handle initial routing in Splash screen
    LaunchedEffect(currentScreen) {
        if (currentScreen == "splash") {
            delay(1800)
            if (isAdmin) {
                currentScreen = "admin"
            } else if (currentUser != null) {
                currentScreen = if (currentUser?.status == "approved") "dashboard" else "pending"
            } else {
                currentScreen = "login"
            }
        }
    }

    // Real-time listener reaction for active user status updates
    LaunchedEffect(currentUser, isAdmin) {
        if (currentScreen != "splash" && !isAdmin) {
            val user = currentUser
            if (user != null) {
                currentScreen = if (user.status == "approved") "dashboard" else "pending"
            } else {
                currentScreen = "login"
            }
        } else if (isAdmin) {
            currentScreen = "admin"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                "splash" -> SplashScreen()
                "login" -> LoginScreen(viewModel = viewModel)
                "pending" -> PendingScreen(viewModel = viewModel)
                "dashboard" -> ParentDashboardScreen(viewModel = viewModel)
                "admin" -> AdminDashboardScreen(viewModel = viewModel)
            }
        }
    }
}

// ============================================================================
// 1. SPLASH SCREEN
// ============================================================================
@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(8.dp, CircleShape)
                .background(Color.White, CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_camp_logo_1783707890473),
                contentDescription = "Camp Connect Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Camp Connect",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "صلة الوصل بين الولي والمخيم الصيفي",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
        )
    }
}

// ============================================================================
// 2. LOGIN & REGISTRATION SCREEN
// ============================================================================
@Composable
fun LoginScreen(viewModel: CampViewModel) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var isRegisterTab by remember { mutableStateOf(true) } // default to new parent registration

    var parentName by remember { mutableStateOf("") }
    var childName by remember { mutableStateOf("") }

    var showAdminDialog by remember { mutableStateOf(false) }
    var adminPin by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Image Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_camp_hero_1783707876220),
                    contentDescription = "Camp Hero Graphic",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Welcome Texts
        item {
            Text(
                text = "مرحباً بكم في Camp Connect",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "تابع مغامرات طفلك اليومية ونظامه الغذائي والأنشطة بكل سهولة واطمئن عليه",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Segmented Tabs for Registration vs Log In
        item {
            TabRow(
                selectedTabIndex = if (isRegisterTab) 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .shadow(2.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = isRegisterTab,
                    onClick = { isRegisterTab = true },
                    text = {
                        Text(
                            "تسجيل كولي جديد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                )
                Tab(
                    selected = !isRegisterTab,
                    onClick = { isRegisterTab = false },
                    text = {
                        Text(
                            "دخول سريع (مسجل مسبقاً)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Form fields
        item {
            OutlinedTextField(
                value = parentName,
                onValueChange = { parentName = it },
                label = { Text("اسم الوليّ الكامل (الأب / الأم)") },
                placeholder = { Text("مثال: أحمد الصالح") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parent_name_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = childName,
                onValueChange = { childName = it },
                label = { Text("اسم الابن / البنت المشارك") },
                placeholder = { Text("مثال: يوسف") },
                leadingIcon = { Icon(Icons.Default.ChildCare, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("child_name_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))
        }

        // Action Button
        item {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (isRegisterTab) {
                            viewModel.registerUser(parentName, childName) {}
                        } else {
                            viewModel.loginUser(parentName, childName) {}
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("login_submit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRegisterTab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = if (isRegisterTab) Icons.Default.PersonAdd else Icons.Default.Login,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRegisterTab) "تسجيل وإرسال للموافقة" else "تسجيل الدخول السريع",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Admin Access Section
        item {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { showAdminDialog = true },
                modifier = Modifier.testTag("admin_login_trigger")
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "الدخول كمسؤول المخيم (Admin)",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }

    // Simple Admin PIN Dialog
    if (showAdminDialog) {
        Dialog(onDismissRequest = { showAdminDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "دخول الإدارة والمسؤولين",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "الرجاء إدخال رمز المرور السري الخاص بالمسؤول لتفعيل لوحة التحكم",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = adminPin,
                        onValueChange = { adminPin = it },
                        label = { Text("رمز المرور السري (PIN)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showAdminDialog = false }) {
                            Text("إلغاء", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                viewModel.loginAdmin(adminPin) {
                                    showAdminDialog = false
                                }
                            }
                        ) {
                            Text("تأكيد الدخول")
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. PENDING APPROVAL SCREEN
// ============================================================================
@Composable
fun PendingScreen(viewModel: CampViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "Waiting icon",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(90.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "بانتظار تفعيل الحساب من الادمين ⏳",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "الاسم المسجل للولي: ${currentUser?.parentName ?: ""}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "اسم الابن المشارك: ${currentUser?.childName ?: ""}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "شكراً لتسجيلك! لم يتم تفعيل حسابك من قبل الإدارة بعد لحماية خصوصية بيانات الأطفال وأنشطتهم. بمجرد تفعيل حسابك، ستتمكن تلقائياً من مشاهدة صور المخيم والبرامج اليومية والغذائية وتلقي الإشعارات.",
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                // Clicking refresh retrieves user again, although snap listener handles live updates
                Toast.makeText(viewModel.getApplication(), "جاري التحقق من حالة الموافقة...", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("تحديث حالة الحساب")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("الخروج وتسجيل حساب آخر")
        }
    }
}

// ============================================================================
// 4. PARENT DASHBOARD SCREEN (MAIN INTERFACE)
// ============================================================================
@Composable
fun ParentDashboardScreen(viewModel: CampViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val campSettings by viewModel.campSettings.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val albumPosts by viewModel.albumPosts.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Home/Photos, 1: Schedule, 2: Dietary, 3: Alerts

    val context = LocalContext.current

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Camp Connect",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("parent_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Color.Red
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("الرئيسية") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    label = { Text("البرنامج اليومي") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    label = { Text("الوجبات الغذائية") }
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = {
                        BadgedBox(badge = {
                            if (notifications.isNotEmpty()) {
                                Badge { Text(notifications.size.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    },
                    label = { Text("الإشعارات") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Welcome Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChildCare,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "مرحباً بك، السيد/ة ${currentUser?.parentName ?: ""}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "طفلك المشترك: ${currentUser?.childName ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = activeTab,
                label = "TabTransition"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> HomeTab(
                        campSettings = campSettings,
                        albumPosts = albumPosts,
                        isAdmin = false,
                        currentUser = currentUser,
                        onAddComment = { postId, text, isPrivate -> viewModel.addCommentToAlbum(postId, text, isPrivate) },
                        onDeletePost = { postId -> viewModel.deleteAlbumPost(postId) },
                        context = context
                    )
                    1 -> ScheduleTab(campSettings = campSettings)
                    2 -> DietaryTab(campSettings = campSettings)
                    3 -> AlertsTab(notifications = notifications)
                }
            }
        }
    }
}

// 4.1 HomeTab (Google Photos Albums Feed & Activities)
@Composable
fun HomeTab(
    campSettings: CampSettings,
    albumPosts: List<CampAlbumPost>,
    isAdmin: Boolean,
    currentUser: CampUser?,
    onAddComment: (String, String, Boolean) -> Unit,
    onDeletePost: (String) -> Unit,
    onPublishPost: ((String, String) -> Unit)? = null,
    context: Context
) {
    var adminAlbumName by remember { mutableStateOf("") }
    var adminAlbumUrl by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin Publisher Section
        if (isAdmin && onPublishPost != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "نشر ألبوم صور جديد 📸",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = adminAlbumName,
                            onValueChange = { adminAlbumName = it },
                            label = { Text("اسم الألبوم أو الفعالية (مثال: رحلة المسبح)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = adminAlbumUrl,
                            onValueChange = { adminAlbumUrl = it },
                            label = { Text("رابط ألبوم قوقل فوتو (Google Photos URL)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (adminAlbumName.isNotBlank() && adminAlbumUrl.isNotBlank()) {
                                    onPublishPost(adminAlbumName, adminAlbumUrl)
                                    adminAlbumName = ""
                                    adminAlbumUrl = ""
                                } else {
                                    Toast.makeText(context, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Publish, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("نشر الألبوم الآن")
                        }
                    }
                }
            }
        }

        // Photo Album Feed Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ألبومات صور المخيم التفاعلية 📸",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // List of Albums (Facebook Status style)
        if (albumPosts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoPhotography,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "لم يتم نشر ألبومات صور بعد",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "بمجرد قيام الإدارة بنشر صور الفعاليات اليومية، ستظهر هنا فوراً حيث يمكنك تصفحها والتعليق عليها.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(albumPosts) { post ->
                AlbumPostItem(
                    post = post,
                    isAdmin = isAdmin,
                    currentUser = currentUser,
                    onAddComment = onAddComment,
                    onDeletePost = onDeletePost,
                    context = context
                )
            }
        }

        // Key Activities Card at the bottom of the feed
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "أهم الأنشطة والرحلات 🏆",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = campSettings.activities,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumPostItem(
    post: CampAlbumPost,
    isAdmin: Boolean,
    currentUser: CampUser?,
    onAddComment: (String, String, Boolean) -> Unit,
    onDeletePost: (String) -> Unit,
    context: Context
) {
    var commentText by remember { mutableStateOf("") }
    var isPrivateComment by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Post Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "إدارة المخيم 👑",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()) }
                        val dateStr = remember(post.createdAt) { sdf.format(java.util.Date(post.createdAt)) }
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                if (isAdmin) {
                    IconButton(onClick = { onDeletePost(post.id) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف المنشور",
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post Body - Album title and info
            Text(
                text = post.albumName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Album Link Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4))))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.albumUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "الرابط غير صالح", Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تصفح صور الألبوم بالكامل 📸",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "جوجل فوتو (Google Photos)",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Filter comments based on privacy status (Public vs Private)
            val visibleComments = remember(post.comments, currentUser, isAdmin) {
                post.comments.filter { comment ->
                    isAdmin || !comment.isPrivate ||
                    (currentUser != null && comment.parentName == currentUser.parentName && comment.childName == currentUser.childName)
                }
            }

            // Comments Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التعليقات (${visibleComments.size}) 💬",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                // List of visible comments
                if (visibleComments.isEmpty()) {
                    Text(
                        text = "لا توجد تعليقات بعد. كن أول من يعلق!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    visibleComments.forEach { comment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = comment.parentName,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (comment.childName.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(والد/ة: ${comment.childName})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                        if (comment.isPrivate) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "تعليق خاص بالإدارة",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    val sdfComment = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                                    val timeStr = remember(comment.createdAt) { sdfComment.format(java.util.Date(comment.createdAt)) }
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Comment input box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("اكتب تعليقاً...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { isPrivateComment = !isPrivateComment }
                                    .padding(end = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPrivateComment) Icons.Default.Lock else Icons.Default.Public,
                                    contentDescription = "قفل الخصوصية",
                                    tint = if (isPrivateComment) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isPrivateComment) "خاص" else "عام",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPrivateComment) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )

                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onAddComment(post.id, commentText, isPrivateComment)
                                commentText = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "إرسال"
                        )
                    }
                }
            }
        }
    }
}

// 4.2 ScheduleTab (Daily Program)
@Composable
fun ScheduleTab(campSettings: CampSettings) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "البرنامج اليومي التفصيلي 📅",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = campSettings.dailyProgram,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp
                    )
                }
            }
        }
    }
}

// 4.3 DietaryTab (Dietary Program)
@Composable
fun DietaryTab(campSettings: CampSettings) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "النظام الغذائي والوجبات 🍉",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFE65100)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = campSettings.dietaryMenu,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp
                    )
                }
            }
        }
    }
}

// 4.4 AlertsTab (Notifications Inbox)
@Composable
fun AlertsTab(notifications: List<CampNotification>) {
    if (notifications.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "صندوق الإشعارات فارغ",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Gray
            )
            Text(
                "سيقوم مشرفو المخيم بإرسال التنبيهات المباشرة هنا في حال وجود أي جديد يخص الأطفال.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "الإشعارات الخاصة والتعميمات 📢",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            items(notifications) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = alert.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                            val dateStr = remember(alert.createdAt) { sdf.format(Date(alert.createdAt)) }
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = alert.body,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. ADMINISTRATOR DASHBOARD SCREEN
// ============================================================================
@Composable
fun AdminDashboardScreen(viewModel: CampViewModel) {
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val campSettings by viewModel.campSettings.collectAsStateWithLifecycle()
    val albumPosts by viewModel.albumPosts.collectAsStateWithLifecycle()

    var activeAdminTab by remember { mutableStateOf(0) } // 0: Approvals, 1: Update Info, 2: Photo Albums, 3: Push Notifications

    // Editing local states
    var editAlbumUrl by remember { mutableStateOf("") }
    var editDailyProg by remember { mutableStateOf("") }
    var editDietary by remember { mutableStateOf("") }
    var editActivities by remember { mutableStateOf("") }

    // Initialize editing values once database loads
    LaunchedEffect(campSettings) {
        editAlbumUrl = campSettings.photoAlbumUrl
        editDailyProg = campSettings.dailyProgram
        editDietary = campSettings.dietaryMenu
        editActivities = campSettings.activities
    }

    var notifTitle by remember { mutableStateOf("") }
    var notifBody by remember { mutableStateOf("") }

    val pendingUsers = remember(allUsers) { allUsers.filter { it.status == "pending" } }
    val approvedUsers = remember(allUsers) { allUsers.filter { it.status == "approved" } }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "لوحة تحكم المسؤول ⚙️",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.logoutAdmin() },
                        modifier = Modifier.testTag("admin_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout Admin",
                            tint = Color.Red
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeAdminTab == 0,
                    onClick = { activeAdminTab = 0 },
                    icon = {
                        BadgedBox(badge = {
                            if (pendingUsers.isNotEmpty()) {
                                Badge { Text(pendingUsers.size.toString()) }
                            }
                        }) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null)
                        }
                    },
                    label = { Text("الموافقة") }
                )
                NavigationBarItem(
                    selected = activeAdminTab == 1,
                    onClick = { activeAdminTab = 1 },
                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    label = { Text("تعديل البرامج") }
                )
                NavigationBarItem(
                    selected = activeAdminTab == 2,
                    onClick = { activeAdminTab = 2 },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    label = { Text("ألبومات الصور") }
                )
                NavigationBarItem(
                    selected = activeAdminTab == 3,
                    onClick = { activeAdminTab = 3 },
                    icon = { Icon(Icons.Default.Publish, contentDescription = null) },
                    label = { Text("إشعارات") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = activeAdminTab,
                label = "AdminTabTransition"
            ) { adminTab ->
                when (adminTab) {
                    0 -> ApprovalsTab(
                        pendingUsers = pendingUsers,
                        approvedUsers = approvedUsers,
                        viewModel = viewModel
                    )
                    1 -> UpdateInfoTab(
                        albumUrl = editAlbumUrl,
                        onAlbumUrlChange = { editAlbumUrl = it },
                        dailyProg = editDailyProg,
                        onDailyProgChange = { editDailyProg = it },
                        dietary = editDietary,
                        onDietaryChange = { editDietary = it },
                        activities = editActivities,
                        onActivitiesChange = { editActivities = it },
                        onSave = {
                            viewModel.updateCampContent(
                                editAlbumUrl,
                                editDailyProg,
                                editDietary,
                                editActivities
                            )
                        }
                    )
                    2 -> {
                        val context = LocalContext.current
                        HomeTab(
                            campSettings = campSettings,
                            albumPosts = albumPosts,
                            isAdmin = true,
                            currentUser = null,
                            onAddComment = { postId, text, isPrivate -> viewModel.addCommentToAlbum(postId, text, isPrivate) },
                            onDeletePost = { postId -> viewModel.deleteAlbumPost(postId) },
                            onPublishPost = { name, url -> viewModel.createAlbumPost(name, url) },
                            context = context
                        )
                    }
                    3 -> PushNotificationsTab(
                        title = notifTitle,
                        onTitleChange = { notifTitle = it },
                        body = notifBody,
                        onBodyChange = { notifBody = it },
                        onSend = {
                            viewModel.sendNotification(notifTitle, notifBody)
                            notifTitle = ""
                            notifBody = ""
                        }
                    )
                }
            }
        }
    }
}

// 5.1 Admin: Approvals Tab
@Composable
fun ApprovalsTab(
    pendingUsers: List<CampUser>,
    approvedUsers: List<CampUser>,
    viewModel: CampViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pending approval header
        item {
            Text(
                text = "طلبات تسجيل معلقة (${pendingUsers.size}) ⏳",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (pendingUsers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "لا توجد أي طلبات تسجيل معلقة حالياً.",
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(pendingUsers) { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_request_${user.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "الوليّ: ${user.parentName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اسم الابن: ${user.childName}",
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.approveUser(user.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("approve_btn_${user.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تفعيل الحساب")
                            }

                            OutlinedButton(
                                onClick = { viewModel.rejectUser(user.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reject_btn_${user.id}"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("رفض")
                            }
                        }
                    }
                }
            }
        }

        // Approved active parents header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "الحسابات النشطة والمفعلة (${approvedUsers.size}) ✅",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (approvedUsers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "لا توجد أي حسابات مفعلة بعد.",
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(approvedUsers) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = user.parentName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "ابنه: ${user.childName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteUser(user.id) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف الولي",
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}

// 5.2 Admin: Update Info Tab
@Composable
fun UpdateInfoTab(
    albumUrl: String,
    onAlbumUrlChange: (String) -> Unit,
    dailyProg: String,
    onDailyProgChange: (String) -> Unit,
    dietary: String,
    onDietaryChange: (String) -> Unit,
    activities: String,
    onActivitiesChange: (String) -> Unit,
    onSave: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "تعديل محتوى وتفاصيل المخيم 📝",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            OutlinedTextField(
                value = albumUrl,
                onValueChange = onAlbumUrlChange,
                label = { Text("رابط ألبوم صور قوقل فوتو (Google Photos URL)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_album_url_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = dailyProg,
                onValueChange = onDailyProgChange,
                label = { Text("البرنامج اليومي ومواعيد الأنشطة") },
                minLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_daily_prog_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = dietary,
                onValueChange = onDietaryChange,
                label = { Text("البرنامج الغذائي الأسبوعي والوجبات") },
                minLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_dietary_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = activities,
                onValueChange = onActivitiesChange,
                label = { Text("أهم الأنشطة والفعاليات الجارية") },
                minLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_activities_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("admin_save_settings_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "حفظ وتحديث المحتوى المباشر",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// 5.3 Admin: Push Notifications Tab
@Composable
fun PushNotificationsTab(
    title: String,
    onTitleChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    onSend: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "إرسال إشعار فوري جديد للأولياء 📢",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "بمجرد الضغط على إرسال، سيظهر الإشعار فوراً في صفحة الإشعارات لكل الأولياء الذين يملكون حسابات مفعلة.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("عنوان التنبيه / الإشعار") },
                placeholder = { Text("مثال: تغيير موعد رحلة غد أو تنبيه هام") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_notif_title_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = body,
                onValueChange = onBodyChange,
                label = { Text("تفاصيل الرسالة وملاحظات الإدارة") },
                placeholder = { Text("مثال: يرجى إحضار ملابس إضافية للسباحة غداً وقبعة للوقاية من الشمس...") },
                minLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_notif_body_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Button(
                onClick = onSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("admin_send_notif_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "بث الإشعار الآن",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
