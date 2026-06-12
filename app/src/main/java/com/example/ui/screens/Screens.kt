package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Shadow
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Clip
import com.example.data.model.VideoProject
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClipForgeViewModel
import com.example.ui.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.*

data class SubtitleWord(val word: String, var start: Double, var end: Double)

fun parseSubtitles(json: String): List<SubtitleWord> {
    val list = mutableListOf<SubtitleWord>()
    try {
        val regex = Regex("""\{"word":"([^"]*)","start":([0-9.]+),"end":([0-9.]+)\}""")
        val matches = regex.findAll(json)
        for (match in matches) {
            val word = match.groupValues[1]
            val start = match.groupValues[2].toDoubleOrNull() ?: 0.0
            val end = match.groupValues[3].toDoubleOrNull() ?: 0.0
            list.add(SubtitleWord(word, start, end))
        }
    } catch (e: Exception) {
        // Fallback
    }
    if (list.isEmpty()) {
        val phrases = listOf("Let's", "build", "something", "amazing", "using", "automated", "AI", "video", "clipping", "technology!")
        var current = 0.0
        phrases.forEach {
            list.add(SubtitleWord(it, current, current + 0.5))
            current += 0.6
        }
    }
    return list
}

fun formatSubtitlesToJson(list: List<SubtitleWord>): String {
    return list.joinToString(prefix = "[", postfix = "]") { word ->
        """{"word":"${word.word.replace("\"", "\\\"")}","start":${word.start},"end":${word.end}}"""
    }
}

// Helper gradients for thumbnail simulation
val ThumbGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))),
    Brush.linearGradient(listOf(Color(0xFF00B4DB), Color(0xFF0083B0))),
    Brush.linearGradient(listOf(Color(0xFF11998e), Color(0xFF38ef7d))),
    Brush.linearGradient(listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))),
    Brush.linearGradient(listOf(Color(0xFF1F1C2C), Color(0xFF928DAB))),
    Brush.linearGradient(listOf(Color(0xFFF3904F), Color(0xFF3B4371)))
)

@Composable
fun AppNavigationWrapper(viewModel: ClipForgeViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        bottomBar = {
            if (currentScreen != Screen.LANDING && currentScreen != Screen.LOGIN && currentScreen != Screen.REGISTER) {
                BottomMenuBar(currentScreen = currentScreen, onSelect = { viewModel.navigateTo(it) }, user = currentUser)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.LANDING -> LandingScreen(
                        onStart = { viewModel.navigateTo(Screen.LOGIN) },
                        onExplore = { viewModel.navigateTo(Screen.LIBRARY) }
                    )
                    Screen.LOGIN -> LoginScreen(
                        errorMessage = errorMessage,
                        onLogin = { email, pass -> viewModel.login(email, pass, {}) },
                        onRegisterNavigate = { viewModel.navigateTo(Screen.REGISTER) }
                    )
                    Screen.REGISTER -> RegisterScreen(
                        errorMessage = errorMessage,
                        onRegister = { email, pass -> viewModel.register(email, pass, {}) },
                        onLoginNavigate = { viewModel.navigateTo(Screen.LOGIN) }
                    )
                    Screen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel
                    )
                    Screen.UPLOAD -> UploadScreen(
                        onUpload = { title, url, desc ->
                            viewModel.importVideo(title, url, "UPLOAD", "10:30", desc)
                        },
                        errorMessage = errorMessage
                    )
                    Screen.LIBRARY -> LibraryScreen(
                        viewModel = viewModel
                    )
                    Screen.EDITOR -> EditorScreen(
                        viewModel = viewModel
                    )
                    Screen.ANALYTICS -> AnalyticsScreen()
                    Screen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel
                    )
                    Screen.BILLING -> BillingScreen(
                        viewModel = viewModel
                    )
                    Screen.ADMIN -> AdminScreen()
                }
            }

            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NeonGreen,
                            strokeWidth = 5.dp,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "ClipForge AI Engine Active",
                            style = TextStyle(color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Transcribing narrative, pinpointing audience reactions, detecting high-energy moments, and computing vertical dynamic reframes. Hang tight!",
                            style = TextStyle(color = MutedText, fontSize = 14.sp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomMenuBar(currentScreen: Screen, onSelect: (Screen) -> Unit, user: com.example.data.model.User?) {
    NavigationBar(
        containerColor = DarkSurface,
        tonalElevation = 12.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.DASHBOARD || currentScreen == Screen.UPLOAD,
            onClick = { onSelect(Screen.DASHBOARD) },
            icon = { Icon(Icons.Outlined.Home, contentDescription = "Dashboard") },
            label = { Text("Hub", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = NeonGreen,
                indicatorColor = NeonGreen,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier.testTag("nav_hub_tab")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.LIBRARY || currentScreen == Screen.EDITOR,
            onClick = { onSelect(Screen.LIBRARY) },
            icon = { Icon(Icons.Outlined.List, contentDescription = "Library") },
            label = { Text("Library", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = NeonGreen,
                indicatorColor = NeonGreen,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier.testTag("nav_library_tab")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.ANALYTICS,
            onClick = { onSelect(Screen.ANALYTICS) },
            icon = { Icon(Icons.Filled.List, contentDescription = "Analytics") },
            label = { Text("Metrics", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = NeonGreen,
                indicatorColor = NeonGreen,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier.testTag("nav_metrics_tab")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.SETTINGS || currentScreen == Screen.BILLING || currentScreen == Screen.ADMIN,
            onClick = { onSelect(Screen.SETTINGS) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = NeonGreen,
                indicatorColor = NeonGreen,
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier.testTag("nav_settings_tab")
        )
    }
}

// --- LANDING SCREEN ---
@Composable
fun LandingScreen(onStart: () -> Unit, onExplore: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.radialGradient(listOf(NeonGreen, NeonCyan))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Logo icon",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "ClipForge AI",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Convert Raw Videos into Viral Shorts in Seconds",
            style = TextStyle(
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                lineHeight = 38.sp,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Automated engaging moment detection, vertical dynamic cropping (9:16), animated subtitles, and 1-click export to TikTok, YouTube and Instagram.",
            style = TextStyle(
                fontSize = 15.sp,
                color = MutedText,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_now_button")
        ) {
            Text("Open Workspace", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedButton(
            onClick = onExplore,
            border = BorderStroke(1.5.dp, DarkBorder),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("explore_samples_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text("View Sample Library", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(48.dp))
        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("98%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Text("Engagement", fontSize = 12.sp, color = MutedText)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("10x", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                Text("Faster Clips", fontSize = 12.sp, color = MutedText)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("24/7", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = HotPink)
                Text("Automated", fontSize = 12.sp, color = MutedText)
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

// --- AUTHENTICATION: LOGIN SCREEN ---
@Composable
fun LoginScreen(errorMessage: String?, onLogin: (String, String) -> Unit, onRegisterNavigate: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to ClipForge", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Provide your credentials to manage active workspaces", fontSize = 14.sp, color = MutedText)
        Spacer(modifier = Modifier.height(32.dp))

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = HotPink.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, HotPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = HotPink,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Workspace Email") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = DarkBorder
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Your Display Name") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = DarkBorder
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { onLogin(email, password) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("login_submit")
        ) {
            Text("Enter Dashboard", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(onClick = onRegisterNavigate) {
            Text("Don't have an account? Sign up with Premium tier", color = NeonCyan, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCard)
                .clickable { onLogin("google_session@clipforge.ai", "Google Pilot") }
                .padding(14.dp)
        ) {
            Icon(Icons.Filled.AccountBox, contentDescription = "Google Sign In", tint = NeonCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Instant Sign In with Google", color = Color.White, fontSize = 14.sp)
        }
    }
}

// --- AUTHENTICATION: REGISTER SCREEN ---
@Composable
fun RegisterScreen(errorMessage: String?, onRegister: (String, String) -> Unit, onLoginNavigate: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Premium Account", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Get 100 free AI video conversion credits instantly", fontSize = 14.sp, color = MutedText)
        Spacer(modifier = Modifier.height(32.dp))

        if (errorMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = HotPink.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, HotPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = HotPink,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Workspace Email") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_email"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = DarkBorder
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Display Name") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_name"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = DarkBorder
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { onRegister(email, name) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("register_submit")
        ) {
            Text("Sign Up with Google Credits", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(onClick = onLoginNavigate) {
            Text("Already registered? Login to existing workspace", color = NeonCyan, fontSize = 13.sp)
        }
    }
}

// --- PRIMARY DASHBOARD ---
@Composable
fun DashboardScreen(viewModel: ClipForgeViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val projects by viewModel.allProjects.collectAsState()
    var pastedUrl by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    var contextText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, ${user?.email?.split("@")?.firstOrNull() ?: "Creator"}",
                        style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Text(
                        text = "Plan Tier: ${user?.tier ?: "Free"}",
                        style = TextStyle(fontSize = 13.sp, color = NeonGreen, fontWeight = FontWeight.SemiBold)
                    )
                }
                IconButton(onClick = { viewModel.logout() }) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = "Log Out", tint = HotPink)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Usage", fontSize = 11.sp, color = MutedText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${user?.usageCount ?: 0} Min", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Queue Node", fontSize = 11.sp, color = MutedText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Active (Ok)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Socials", fontSize = 11.sp, color = MutedText)
                        Spacer(modifier = Modifier.height(4.dp))
                        val socialCount = (if (user?.connectedYoutube == true) 1 else 0) +
                                (if (user?.connectedTiktok == true) 1 else 0) +
                                (if (user?.connectedInstagram == true) 1 else 0)
                        Text("$socialCount / 3", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HotPink)
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))

            // Paste YouTube or Import File Section
            Text("Transform New Video", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Paste any YouTube, TikTok or Vimeo video URL below. ClipForge AI will download and extract highlight clips instantly.", fontSize = 13.sp, color = MutedText)
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                label = { Text("Video Title") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_input_title"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = DarkBorder
                ),
                singleLine = true,
                placeholder = { Text("e.g. Space Odyssey Pod", color = Color.White.copy(alpha = 0.4f)) }
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = pastedUrl,
                onValueChange = { pastedUrl = it },
                label = { Text("YouTube / Vimeo / TikTok URL") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_input_url"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = DarkBorder
                ),
                singleLine = true,
                placeholder = { Text("https://www.youtube.com/watch?v=...", color = Color.White.copy(alpha = 0.4f)) }
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = contextText,
                onValueChange = { contextText = it },
                label = { Text("AI Context & Prompt Hints (Optional)") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = DarkBorder
                ),
                placeholder = { Text("Focus on energetic speech segments...", color = Color.White.copy(alpha = 0.4f)) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (pastedUrl.isNotBlank() && customTitle.isNotBlank()) {
                        viewModel.importVideo(customTitle, pastedUrl, "YOUTUBE", "15:20", contextText)
                        customTitle = ""
                        pastedUrl = ""
                        contextText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("transform_submit_btn"),
                enabled = pastedUrl.isNotBlank() && customTitle.isNotBlank()
            ) {
                Icon(Icons.Filled.AddCircle, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Process with Gemini AI", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Or Upload Mock Button
            OutlinedButton(
                onClick = {
                    viewModel.navigateTo(Screen.UPLOAD)
                },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.5.dp, DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Local Video File")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recent Projects
            Text("Project Workspace Library", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (projects.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No video projects. Import above to get started!", color = MutedText, fontSize = 14.sp)
                }
            }
        } else {
            items(projects) { project ->
                ProjectCardItem(
                    project = project,
                    onSelect = { viewModel.selectProject(project) },
                    onDelete = { viewModel.deleteProject(project) }
                )
            }
        }
    }
}

@Composable
fun ProjectCardItem(project: VideoProject, onSelect: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail Mock
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ThumbGradients[project.thumbnailIndex % ThumbGradients.size]),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(project.sourceType, fontSize = 9.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Duration: ${project.duration}", fontSize = 11.sp, color = MutedText)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = HotPink.copy(alpha = 0.7f))
            }
        }
    }
}

// --- FILE UPLOAD COMPONENT ---
@Composable
fun UploadScreen(onUpload: (String, String, String) -> Unit, errorMessage: String?) {
    var title by remember { mutableStateOf("") }
    var filepath by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Upload Video Engine", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Select and push local video files up to our Supabase processing node. We then transcribe & extract highlight candidates automatically.",
            fontSize = 13.sp, color = MutedText, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Narrative / File Designation Title") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = DarkBorder
            ),
            singleLine = true,
            placeholder = { Text("e.g. Product Demo Clip Raw", color = Color.White.copy(alpha = 0.3f)) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = filepath,
            onValueChange = { filepath = it },
            label = { Text("Simulated Local Filepath") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("upload_file_path"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = DarkBorder
            ),
            singleLine = true,
            placeholder = { Text("e.g. /downloads/raw_record_01.mp4", color = Color.White.copy(alpha = 0.3f)) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("Detailed description (Guides AI)") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonGreen,
                unfocusedBorderColor = DarkBorder
            )
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (title.isNotBlank() && filepath.isNotBlank()) {
                    onUpload(title, filepath, desc)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("upload_action_submit"),
            enabled = title.isNotBlank() && filepath.isNotBlank()
        ) {
            Text("Compile & Extract Clips", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// --- VIDEO LIBRARY SCREEN ---
@Composable
fun LibraryScreen(viewModel: ClipForgeViewModel) {
    val selectedProject by viewModel.selectedProject.collectAsState()
    val clips by viewModel.projectClips.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        if (selectedProject == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = HotPink, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select a video project from Hub first!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.navigateTo(Screen.DASHBOARD) }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)) {
                        Text("Go to Hub", color = Color.Black)
                    }
                }
            }
        } else {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { viewModel.navigateTo(Screen.DASHBOARD) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = selectedProject!!.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("Detected AI Viral Moments", fontSize = 12.sp, color = NeonGreen)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            if (clips.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(clips) { clip ->
                        ClipRowItem(
                            clip = clip,
                            onExplore = { viewModel.selectClip(clip) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClipRowItem(clip: Clip, onExplore: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onExplore() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clip.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(HotPink.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("Score: ${clip.engagementRate}%", fontSize = 11.sp, color = HotPink, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scene: ${clip.startSeconds}s to ${clip.endSeconds}s (${clip.endSeconds - clip.startSeconds}s length)",
                fontSize = 12.sp,
                color = MutedText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = clip.description,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clip.exportStatus,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (clip.exportStatus) {
                        "PUBLISHED" -> NeonGreen
                        "SCHEDULED" -> NeonCyan
                        "EXPORTED" -> Color.White
                        else -> MutedText
                    }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Edit & Render", fontSize = 12.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = NeonGreen)
                }
            }
        }
    }
}

// --- MAIN VIDEO EDITOR & TIMELINE ---
@Composable
fun EditorScreen(viewModel: ClipForgeViewModel) {
    val clip by viewModel.selectedClip.collectAsState()
    val exportStates by viewModel.clipExportStates.collectAsState()

    if (clip == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a highlight clip from Library first!", color = Color.White)
        }
        return
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Timeline Trans, 1: Captions, 2: Post Specs

    // Edit variables
    var currentStart by remember(clip) { mutableStateOf(clip!!.startSeconds) }
    var currentEnd by remember(clip) { mutableStateOf(clip!!.endSeconds) }
    var selectedStyle by remember(clip) { mutableStateOf(clip!!.captionsStyle) }
    var selectedFontSize by remember(clip) { mutableStateOf(clip!!.fontSize) }
    var watermarkVal by remember(clip) { mutableStateOf(clip!!.watermarkText) }
    var selectedMusic by remember(clip) { mutableStateOf(clip!!.musicTrack) }

    // Caption word corrections
    val wordsList = remember(clip) { parseSubtitles(clip!!.subtitlesJson).toMutableStateList() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top back bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(Screen.LIBRARY) }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Clip Studio & Reframing", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(clip!!.title, fontSize = 12.sp, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = {
                    val updated = clip!!.copy(
                        startSeconds = currentStart,
                        endSeconds = currentEnd,
                        captionsStyle = selectedStyle,
                        fontSize = selectedFontSize,
                        watermarkText = watermarkVal,
                        musicTrack = selectedMusic,
                        subtitlesJson = formatSubtitlesToJson(wordsList)
                    )
                    viewModel.saveClipEdits(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, NeonGreen)
            ) {
                Text("Save", color = NeonGreen)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            // LEFT SIDE: Interactive 9:16 vertical preview frame! Only 44% of available width to avoid truncation
            Column(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                // 9:16 Canvas Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.5625f) // 9:16 aspect ratio
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .border(1.5.dp, DarkBorder, RoundedCornerShape(12.dp))
                ) {
                    // Simulated character speaking
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawCircle(
                                    color = NeonCyan.copy(alpha = 0.15f),
                                    radius = size.width / 2.5f,
                                    center = Offset(size.width / 2, size.height / 3),
                                    style = Stroke(width = 4f)
                                )
                            }
                    ) {
                        // Dynamic face/speaker avatar
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-40).dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(HotPink.copy(alpha = 0.2f))
                                    .border(2.dp, HotPink, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = HotPink, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Face Track Active", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.SemiBold)
                        }

                        // Watermark Overlay
                        if (watermarkVal.isNotBlank()) {
                            Text(
                                text = "© $watermarkVal",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopStart)
                            )
                        }

                        // Bottom dynamic captions simulation showing first word
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 36.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = selectedStyle.uppercase(),
                                fontSize = 9.sp,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Renders subtitles word-by-word highlighted
                            Text(
                                text = buildAnnotatedString {
                                    wordsList.take(3).forEachIndexed { index, subword ->
                                        if (index == 1) { // Highlighting the pseudo active word
                                            withStyle(style = SpanStyle(color = Color(0xFFFFDE4D), fontWeight = FontWeight.Black)) {
                                                append("${subword.word.uppercase()} ")
                                            }
                                        } else {
                                            withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                                append("${subword.word} ")
                                            }
                                        }
                                    }
                                },
                                style = TextStyle(
                                    fontSize = selectedFontSize.sp,
                                    textAlign = TextAlign.Center,
                                    shadow = Shadow(color = Color.Black, blurRadius = 8f)
                                ),
                                maxLines = 2
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Simple play simulation button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkCard)
                        .padding(6.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Playing live: ${currentStart}s - ${currentEnd}s", fontSize = 10.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // RIGHT SIDE: Tabbed control panel for editing
            Column(
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
            ) {
                // Tab switcher Row
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                            .drawBehind {
                                if (selectedTab == 0) drawLine(
                                    color = NeonGreen,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Timeline", fontSize = 11.sp, color = if (selectedTab == 0) NeonGreen else MutedText, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 }
                            .drawBehind {
                                if (selectedTab == 1) drawLine(
                                    color = NeonGreen,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Captions", fontSize = 11.sp, color = if (selectedTab == 1) NeonGreen else MutedText, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 2 }
                            .drawBehind {
                                if (selectedTab == 2) drawLine(
                                    color = NeonGreen,
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Publish", fontSize = 11.sp, color = if (selectedTab == 2) NeonGreen else MutedText, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> {
                            // Timeline & Trim controls
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text("Trimming Bounds", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Start Time", fontSize = 10.sp, color = MutedText)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { if (currentStart > 0) currentStart-- }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = null, tint = Color.White)
                                            }
                                            Text("${currentStart}s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            IconButton(onClick = { if (currentStart < currentEnd - 5) currentStart++ }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                                            }
                                        }
                                    }
                                    Column {
                                        Text("End Time", fontSize = 10.sp, color = MutedText)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { if (currentEnd > currentStart + 5) currentEnd-- }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = null, tint = Color.White)
                                            }
                                            Text("${currentEnd}s", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            IconButton(onClick = { currentEnd++ }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))

                                Text("Subtitles Styling", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                val styles = listOf("Classic Karaoke", "Retro Neon", "Cinematic", "Word-by-Word")
                                styles.forEach { style ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (selectedStyle == style) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable { selectedStyle = style }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (selectedStyle == style) Icons.Filled.CheckCircle else Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = if (selectedStyle == style) NeonCyan else DarkBorder,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(style, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Font Size Highlight (${selectedFontSize}sp)", fontSize = 11.sp, color = MutedText)
                                Slider(
                                    value = selectedFontSize.toFloat(),
                                    onValueChange = { selectedFontSize = it.toInt() },
                                    valueRange = 16f..36f,
                                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Background Audio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                val tracks = listOf("None", "Dynamic Lofi", "Upbeat Pop", "Cinematic Orchestral")
                                tracks.forEach { tr ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedMusic = tr }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = selectedMusic == tr, onClick = { selectedMusic = tr }, colors = RadioButtonDefaults.colors(selectedColor = NeonGreen))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(tr, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Editable Transcription Word-List
                            Column {
                                Text("Correction Editor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap any generated word block to adjust spelling or fine-tune exact timeline seconds.", fontSize = 10.sp, color = MutedText)
                                Spacer(modifier = Modifier.height(8.dp))

                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(wordsList.toList()) { subword ->
                                        var wordEditing by remember { mutableStateOf(subword.word) }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = wordEditing,
                                                onValueChange = {
                                                    wordEditing = it
                                                    subword.start = subword.start // Trigger refresh
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(42.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedBorderColor = NeonGreen,
                                                    unfocusedBorderColor = DarkBorder
                                                ),
                                                textStyle = TextStyle(fontSize = 11.sp),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${String.format("%.1f", subword.start)}s",
                                                fontSize = 11.sp,
                                                color = NeonCyan,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Direct Publish tab
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text("AI Social Metadata", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                                    border = BorderStroke(1.dp, DarkBorder)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("AI Title Proposal", fontSize = 9.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                                        Text(clip!!.titleSuggestion.ifBlank { "Unleash your potential today!" }, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("AI Description", fontSize = 9.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                                        Text(clip!!.description, fontSize = 11.sp, color = MutedText)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("AI Auto Tags", fontSize = 9.sp, color = HotPink, fontWeight = FontWeight.Bold)
                                        Text(clip!!.hashtags, fontSize = 11.sp, color = NeonCyan)
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))

                                Text("Render & Social Push", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(6.dp))

                                val progress = exportStates[clip!!.id]
                                if (progress != null) {
                                    Column {
                                        Text("Rendering Node Progress: $progress%", fontSize = 11.sp, color = NeonCyan)
                                        LinearProgressIndicator(progress = progress.toFloat() / 100f, color = NeonCyan, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                                    }
                                } else if (clip!!.exportStatus == "DRAFT") {
                                    Button(
                                        onClick = { viewModel.triggerClipExport(clip!!) },
                                        modifier = Modifier.fillMaxWidth().testTag("render_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                                    ) {
                                        Text("Compile vertical high-def (1080p)", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("Export Complete: MP4 is ready!", fontSize = 11.sp, color = NeonGreen)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.scheduleOrPublishClip(clip!!, "TikTok") },
                                            modifier = Modifier.weight(1f).testTag("publish_tiktok_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = HotPink)
                                        ) {
                                            Text("TikTok", color = Color.White, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.scheduleOrPublishClip(clip!!, "YouTube Shorts") },
                                            modifier = Modifier.weight(1f).testTag("publish_shorts_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                        ) {
                                            Text("Shorts", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ANALYTICS DASHBOARD ---
@Composable
fun AnalyticsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("AI SaaS Performance Dashboard", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("Real-time aggregated social analytics from connected accounts.", fontSize = 13.sp, color = MutedText)
        Spacer(modifier = Modifier.height(24.dp))

        // Total metrics cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Total Views", fontSize = 10.sp, color = MutedText)
                    Text("42.8M", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Avg Retention", fontSize = 10.sp, color = MutedText)
                    Text("84.5%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Viral Clips", fontSize = 10.sp, color = MutedText)
                    Text("24", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HotPink)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Simulated Bar Graph using DrawScope Canvas
        Text("Conversion Views Growth Index (Monthly)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder)
                .padding(12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pointPadding = size.width / 5f
                val dataPoints = listOf(35f, 65f, 48f, 92f, 78f)
                val maxDatapoint = 100f
                val strokeHeight = size.height

                for (i in 0 until 5) {
                    val x = pointPadding * i + (pointPadding / 2f)
                    val barHeight = (dataPoints[i] / maxDatapoint) * (strokeHeight * 0.8f)
                    val y = strokeHeight - barHeight

                    drawRect(
                        brush = Brush.verticalGradient(listOf(NeonGreen, NeonCyan)),
                        topLeft = Offset(x - 14.dp.toPx(), y),
                        size = androidx.compose.ui.geometry.Size(28.dp.toPx(), barHeight)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(28.dp))

        // Top Performing Clips list
        Text("Global Performance Leaderboard", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))
        val listClips = listOf(
            Triple("Ultimate AI SaaS Guide guide clip", "98.4K views", "TikTok"),
            Triple("Creative Focus zen moments", "42.1K views", "YouTube Shorts"),
            Triple("Secret habit hack highlights", "12.5K views", "Instagram Reels")
        )
        listClips.forEach { (clipTitle, viewCount, social) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(clipTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Active on: $social", fontSize = 11.sp, color = MutedText)
                    }
                    Text(viewCount, fontSize = 13.sp, color = NeonCyan, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

// --- SETTINGS SCREEN ---
@Composable
fun SettingsScreen(viewModel: ClipForgeViewModel) {
    val user by viewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("ClipForge Workspace Configurations", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Account metadata and active social publication nodes.", fontSize = 13.sp, color = MutedText)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Active Social Accounts Integration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))

        SocialToggleCard(
            platformName = "YouTube Publisher Node",
            isActive = user?.connectedYoutube == true,
            onToggle = { viewModel.toggleSocialConnection("YOUTUBE") },
            iconRes = Icons.Filled.AccountBox,
            color = Color.Red
        )
        SocialToggleCard(
            platformName = "TikTok Short-form Sync Node",
            isActive = user?.connectedTiktok == true,
            onToggle = { viewModel.toggleSocialConnection("TIKTOK") },
            iconRes = Icons.Filled.AccountBox,
            color = HotPink
        )
        SocialToggleCard(
            platformName = "Instagram Meta Reels Sync",
            isActive = user?.connectedInstagram == true,
            onToggle = { viewModel.toggleSocialConnection("INSTAGRAM") },
            iconRes = Icons.Filled.AccountBox,
            color = NeonCyan
        )
        Spacer(modifier = Modifier.height(28.dp))

        // Subscription overview
        Text("Active Billing & Tier", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Current Workspace plan is [${user?.tier?.uppercase() ?: "FREE"}]", fontSize = 13.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Includes default processing nodes, Whisper transcription parsing, and classic styling presets.", fontSize = 12.sp, color = MutedText)
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { viewModel.navigateTo(Screen.BILLING) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.fillMaxWidth().testTag("billing_nav_btn")
                ) {
                    Text("Change Plan & Buy Credits", color = Color.Black)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Admin shortcut access
        Button(
            onClick = { viewModel.navigateTo(Screen.ADMIN) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, NeonCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Build, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Operator Dashboard (Admin)", color = NeonCyan)
        }
    }
}

@Composable
fun SocialToggleCard(platformName: String, isActive: Boolean, onToggle: () -> Unit, iconRes: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(iconRes, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(platformName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            Switch(checked = isActive, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen))
        }
    }
}

// --- BILLING / CHECHOUT ---
@Composable
fun BillingScreen(viewModel: ClipForgeViewModel) {
    val user by viewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("ClipForge Pricing Plans", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Get more compilation capacity & ultra high-speed rendering threads.", fontSize = 13.sp, color = MutedText)
        Spacer(modifier = Modifier.height(20.dp))

        val tiers = listOf(
            Triple("Free Plan", "${0}$/Mo", "Free defaults and 3 standard rendering exports"),
            Triple("Creator Plan", "${19}$/Mo", "100 AI processing credits with word-by-word dynamic presets"),
            Triple("Pro Plan", "${49}$/Mo", "Unlimited exports, 1080p, custom watermarks and soundscapes"),
            Triple("Enterprise Plan", "${199}$/Mo", "Dedicated backend thread nodes, Stripe contract APIs")
        )

        tiers.forEach { (name, price, desc) ->
            val isCurrent = user?.tier?.lowercase() == name.split(" ").first().lowercase()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = if (isCurrent) DarkCard else DarkSurface),
                border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) NeonGreen else DarkBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(price, fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonCyan)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, fontSize = 12.sp, color = MutedText)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.updateUserTier(name.split(" ").first()) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isCurrent) MutedText else NeonGreen),
                        modifier = Modifier.fillMaxWidth().testTag("billing_plan_${name.split(" ").first()}"),
                        enabled = !isCurrent
                    ) {
                        Text(if (isCurrent) "Active Workspace Plan" else "Subscribe via Stripe", color = if (isCurrent) Color.White else Color.Black)
                    }
                }
            }
        }
    }
}

// --- ADMIN DASHBOARD ---
@Composable
fun AdminScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("ClipForge Operations Center", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text("Monitor back-end transcription engines, file hosting stats, and compile pipelines.", fontSize = 12.sp, color = MutedText)
        Spacer(modifier = Modifier.height(24.dp))

        // System resources card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Platform System Node Resources", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                AdminStatRow("Active Render Threads", "12 / 16", NeonGreen)
                AdminStatRow("Whisper API node CPU", "42%", NeonCyan)
                AdminStatRow("S3 Storage consumption", "1.4 TB", HotPink)
                AdminStatRow("Supabase connections", "98.4%", NeonGreen)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Action queue switches
        Text("Active Worker Nodes Operator", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(10.dp))
        var videoEngNode by remember { mutableStateOf(true) }
        var audioAlignNode by remember { mutableStateOf(true) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MoviePy & FFmpeg Conversion Thread Pool", fontSize = 12.sp, color = Color.White)
            Switch(checked = videoEngNode, onCheckedChange = { videoEngNode = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("OpenAI Whisper alignment and word timing tokenizers", fontSize = 12.sp, color = Color.White)
            Switch(checked = audioAlignNode, onCheckedChange = { audioAlignNode = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen))
        }
    }
}

@Composable
fun AdminStatRow(label: String, valStr: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MutedText)
        Text(valStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
    }
}
