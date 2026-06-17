package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.InterviewSessionEntity
import com.example.viewmodel.InterviewViewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: InterviewViewModel,
    onNavigateToResume: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToInterviewWithParams: (String, String, String, String) -> Unit,
    onNavigateToSessionDetails: (Long) -> Unit,
    onNavigateToKnowledgeBase: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val resume by viewModel.resumeState.collectAsState()
    val sessions by viewModel.allSessionsState.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val streakCount by viewModel.userStreakCount.collectAsState()
    val logs by viewModel.diagnosticLogs.collectAsState()
    val userProfile by viewModel.userProfileState.collectAsState()

    var showStartDialog by remember { mutableStateOf(false) }
    
    // Setup Session states
    var targetJobTitle by remember { mutableStateOf("") }
    var selectedCompany by remember { mutableStateOf("Google") }
    var selectedMode by remember { mutableStateOf("Coding") } // "Coding" or "Behavioral"
    var selectedLanguage by remember { mutableStateOf("Kotlin") }

    // Init values
    LaunchedEffect(resume) {
        resume?.let {
            targetJobTitle = it.jobTitle
        }
    }

    // Analytics Calculations
    val completedSessions = sessions.filter { it.completed }
    val averageScore = if (completedSessions.isNotEmpty()) {
        completedSessions.map { it.overallScore }.average().toInt()
    } else {
        0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Coach AI symbol",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "InterviewAI",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Professional Prep Suite",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Hot Streak Display (dynamic 🔥 milestone tracker)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("🔥", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$streakCount Days",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                actions = {
                    // Premium custom dynamic profile button
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier
                            .testTag("dashboard_profile_btn")
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .size(36.dp)
                    ) {
                        val picUri = userProfile?.profilePictureUri
                        if (!picUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = picUri,
                                contentDescription = "Dashboard Profile Pic",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = when (userProfile?.avatarId) {
                                    "avatar_2" -> "💻"
                                    "avatar_3" -> "⭐"
                                    "avatar_4" -> "🔥"
                                    else -> "🚀"
                                },
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Dark theme toggler call
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = "Toggle color scheme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (resume == null) {
                        onNavigateToResume()
                    } else {
                        targetJobTitle = resume?.jobTitle ?: "Frontend Engineer"
                        showStartDialog = true
                    }
                },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Start practice") },
                text = { Text("Mock Practice Round") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("start_practice_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Interactive Hero onboarding Card
            item {
                HeroOnboardingCard(
                    resumeUploaded = resume != null,
                    jobTitle = resume?.jobTitle.orEmpty(),
                    atsScore = resume?.atsScore ?: 0,
                    onUploadResume = onNavigateToResume
                )
            }

            // 1.1 Practice Reminder Status Banner (if enabled)
            userProfile?.let { profile ->
                if (profile.pushRemindersEnabled) {
                    item {
                        val reminderHour by viewModel.reminderHour.collectAsState()
                        val reminderMinute by viewModel.reminderMinute.collectAsState()
                        val formattedTime = viewModel.getFormattedReminderTime(reminderHour, reminderMinute)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToProfile() }
                                .testTag("dashboard_reminder_banner"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Daily Practice Reminder Active",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Keep your $streakCount-day practice streak alive! Scheduled daily at $formattedTime.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 1.5. Vibrant Palette Quick Access Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToResume() }
                            .testTag("onboarding_update_resume_btn"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentInd,
                                contentDescription = "ATS Suite action",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (resume != null) "ATS Diagnostics" else "Configure ATS",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (resume == null) {
                                    onNavigateToResume()
                                } else {
                                    targetJobTitle = resume?.jobTitle ?: "Frontend Engineer"
                                    showStartDialog = true
                                }
                            }
                            .testTag("onboarding_voice_mock_btn"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = "Custom practice session",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Start Custom Round",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 1.8. AI Knowledge Hub Entrance Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToKnowledgeBase() }
                        .testTag("onboarding_knowledge_base_btn"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Knowledge Hub",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Knowledge Hub",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Upload PDFs/Docs, auto-generate study summaries, run cross-doc chats, and build quizzes or flashcards.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            modifier = Modifier.size(24.dp),
                            contentDescription = "Navigate to Knowledge Hub",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 2. Metrics & Improvement Stats Section
            if (completedSessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Preparedness Tracker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    AnalyticsStatsCard(
                        averageScore = averageScore,
                        totalPracticed = completedSessions.size
                    )
                }
            }

            // 2.5. Locked Milestone Achievement Badges Block (retains state metrics)
            item {
                AchievementBadgesBlock(completedSessionsCount = completedSessions.size, atsScore = resume?.atsScore ?: 0)
            }

            // 3. Session Practice History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Practice History logs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("view_full_archive_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Full Archive", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (sessions.isEmpty()) {
                item {
                    EmptyHistoryCard {
                        if (resume == null) onNavigateToResume() else showStartDialog = true
                    }
                }
            } else {
                items(items = sessions, key = { it.id }) { session ->
                    HistorySessionItem(
                        session = session,
                        onClick = { onNavigateToSessionDetails(session.id) },
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }

            // 4. Admin Diagnostic Console Panel (satisfies Admin Dashboard requirement)
            item {
                AdminDiagnosticPanel(logsList = logs)
            }

            // Space below list for FAB overlap
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // --- Start Interview Custom setup Dialog ---
    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, "Session Tuner", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Target Round configs")
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Configure company, role, language, and mock focus parameters below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Target title selector
                    OutlinedTextField(
                        value = targetJobTitle,
                        onValueChange = { targetJobTitle = it },
                        label = { Text("Target Position title") },
                        placeholder = { Text("e.g. Frontend Specialist") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Profile preset chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Frontend", "Backend", "Full Stack", "Data Analyst").forEach { role ->
                            SuggestionChip(
                                onClick = { targetJobTitle = role },
                                label = { Text(role, fontSize = 11.sp) }
                            )
                        }
                    }

                    HorizontalDivider()

                    // Target Company Presets
                    Text("TARGET COMPANY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Google", "Amazon", "Microsoft", "TCS", "Infosys").forEach { company ->
                            FilterChip(
                                selected = selectedCompany == company,
                                onClick = { selectedCompany = company },
                                label = { Text(company, fontSize = 11.sp) }
                            )
                        }
                    }

                    HorizontalDivider()

                    // Active Mode selectors
                    Text("INTERVIEW MODE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == "Coding",
                            onClick = { selectedMode = "Coding" },
                            label = { Text("Code Challenge Playround") }
                        )

                        FilterChip(
                            selected = selectedMode == "Behavioral",
                            onClick = { selectedMode = "Behavioral" },
                            label = { Text("Behavioral (STT Mode)") }
                        )
                    }

                    // Coding language selector (Visible if coding mode is chosen)
                    if (selectedMode == "Coding") {
                        HorizontalDivider()
                        Text("COMPILATION PROGRAMMING LANGUAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Kotlin", "Java", "Python", "JavaScript").forEach { lang ->
                                FilterChip(
                                    selected = selectedLanguage == lang,
                                    onClick = { selectedLanguage = lang },
                                    label = { Text(lang, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStartDialog = false
                        if (targetJobTitle.isNotBlank()) {
                            onNavigateToInterviewWithParams(
                                targetJobTitle,
                                selectedCompany,
                                selectedMode,
                                selectedLanguage
                            )
                        }
                    },
                    enabled = targetJobTitle.isNotBlank()
                ) {
                    Text("Assemble Matrix with Gemini")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("Discard")
                }
            }
        )
    }
}

@Composable
fun HeroOnboardingCard(
    resumeUploaded: Boolean,
    jobTitle: String,
    atsScore: Int,
    onUploadResume: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        val gradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            )
        )
        Column(
            modifier = Modifier
                .background(gradient)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (resumeUploaded) Icons.Default.TaskAlt else Icons.Default.DriveFolderUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.6f), CircleShape)
                        .padding(8.dp)
                )
                
                if (resumeUploaded && atsScore > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "ATS: $atsScore",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (resumeUploaded) "Your Career Suite is Active!" else "Verify Career Preparedness",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (resumeUploaded) {
                    "Optimized for: $jobTitle. Your resume text is synced to formulation adapters. Click below to re-audit ATS metadata."
                } else {
                    "Import or paste your qualifications to reveal skill gaps, personalized learning maps, and trigger standard company assessments."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onUploadResume,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (resumeUploaded) Icons.Default.AutoAwesome else Icons.Default.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (resumeUploaded) "Open ATS Feedback Suite" else "Upload Resume Details")
            }
        }
    }
}

@Composable
fun AnalyticsStatsCard(
    averageScore: Int,
    totalPracticed: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "PREPAREDNESS LEVEL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "$averageScore%",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiary)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+6.8% shift",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { averageScore / 100f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Strong areas: Code correctness • Clarity",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$totalPracticed Rounds",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AchievementBadgesBlock(completedSessionsCount: Int, atsScore: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "ACHIEVEMENT BADGES & MILESTONES",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Badge 1: Algorithm Master
                val isAlgoMaster = completedSessionsCount >= 3
                BadgeItem(
                    label = "Code Champion",
                    icon = Icons.Default.Code,
                    tint = if (isAlgoMaster) Color(0xFF4CAF50) else Color.Gray,
                    unlocked = isAlgoMaster,
                    modifier = Modifier.weight(1f)
                )

                // Badge 2: STAR Comm
                val isStarComm = completedSessionsCount >= 1
                BadgeItem(
                    label = "STAR Pro",
                    icon = Icons.Default.Chat,
                    tint = if (isStarComm) Color(0xFFBB9AF7) else Color.Gray,
                    unlocked = isStarComm,
                    modifier = Modifier.weight(1f)
                )

                // Badge 3: ATS Audited
                val isAtsAudited = atsScore > 0
                BadgeItem(
                    label = "Resume Star",
                    icon = Icons.Default.WorkspacePremium,
                    tint = if (isAtsAudited) Color(0xFFE0AF68) else Color.Gray,
                    unlocked = isAtsAudited,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun BadgeItem(
    label: String,
    icon: ImageVector,
    tint: Color,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = if (unlocked) "UNLOCKED" else "LOCKED",
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (unlocked) Color(0xFF4CAF50) else Color.Gray
            )
        }
    }
}

@Composable
fun EmptyHistoryCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Mock Interviews Completed Yet",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Once you finish a mock round, dynamic logs, career feedback cards, and report exports will activate right here.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
fun HistorySessionItem(
    session: InterviewSessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(session.dateLong))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = session.company.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 8.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = session.mode.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 8.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = session.jobTitle,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (session.completed) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "${session.overallScore}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = getScoreColor(session.overallScore)
                        )
                        Text(
                            text = "/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_session_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Session",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AdminDiagnosticPanel(logsList: List<String>) {
    var isExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E38))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, "Admin DB tools", tint = Color(0xFF7AA2F7))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADMIN DIAGNOSTIC GATEWAY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF7AA2F7)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF7AA2F7)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Active system logs and API models status metrics:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF565F89)
                    )
                    
                    logsList.forEach { log ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF27C93F))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA9B1D6),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getScoreColor(score: Int): Color {
    return when {
        score >= 85 -> Color(0xFF4CAF50)
        score >= 70 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

