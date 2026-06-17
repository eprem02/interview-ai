package com.example.ui.screens

import android.widget.Toast
import android.net.Uri
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import coil.compose.AsyncImage
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.UserProfileEntity
import com.example.viewmodel.InterviewViewModel
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    viewModel: InterviewViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val profile by viewModel.userProfileState.collectAsState()
    val sessions by viewModel.allSessionsState.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Briefing, 1: Prep & Goals, 2: AI Companion, 3: Advanced

    // Dialog state controllers
    var showEditIdentityDialog by remember { mutableStateOf(false) }
    var showEditEduDialog by remember { mutableStateOf(false) }
    var showEditWorkDialog by remember { mutableStateOf(false) }
    var showEditCertDialog by remember { mutableStateOf(false) }
    var showEditLinkDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteAccountWarning by remember { mutableStateOf(false) }

    if (profile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentProfile = profile!!

    var showPhotoOptionDialog by remember { mutableStateOf(false) }

    val GalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = saveUriToLocalFile(context, uri)
            if (path != null) {
                viewModel.saveUserProfile(currentProfile.copy(profilePictureUri = path))
                Toast.makeText(context, "Profile picture updated from gallery!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to process selected image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val path = saveBitmapToLocalFile(context, bitmap)
            if (path != null) {
                viewModel.saveUserProfile(currentProfile.copy(profilePictureUri = path))
                Toast.makeText(context, "Profile picture updated from camera!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to save camera photo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Parse skills with safe array parsing
    val parsedSkills = remember(currentProfile.skills) {
        try {
            val arr = JSONArray(currentProfile.skills)
            MutableList(arr.length()) { i -> arr.getString(i) }
        } catch (e: Exception) {
            mutableListOf("Kotlin", "Android", "Jetpack Compose")
        }
    }

    // Parse education JSON
    val parsedEdu = remember(currentProfile.educationJson) {
        try {
            val arr = JSONArray(currentProfile.educationJson)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                EduItem(obj.optString("school"), obj.optString("degree"), obj.optString("year"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Parse experience JSON
    val parsedWork = remember(currentProfile.workExperienceJson) {
        try {
            val arr = JSONArray(currentProfile.workExperienceJson)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                WorkItem(obj.optString("company"), obj.optString("role"), obj.optString("duration"), obj.optString("highlights"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Parse certifications JSON
    val parsedCerts = remember(currentProfile.certificationsJson) {
        try {
            val arr = JSONArray(currentProfile.certificationsJson)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                CertItem(obj.optString("name"), obj.optString("issuer"), obj.optString("date"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Parse links JSON
    val parsedLinks = remember(currentProfile.portfolioLinksJson) {
        try {
            val arr = JSONArray(currentProfile.portfolioLinksJson)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                LinkItem(obj.optString("title"), obj.optString("url"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Parse personal goals JSON
    val parsedGoals = remember(currentProfile.personalGoalsJson) {
        try {
            val arr = JSONArray(currentProfile.personalGoalsJson)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                GoalItem(obj.optString("goal"), obj.optBoolean("isCompleted"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Parse memories JSON
    val parsedMemories = remember(currentProfile.aiPersonalMemoriesJson) {
        try {
            val arr = JSONArray(currentProfile.aiPersonalMemoriesJson)
            MutableList(arr.length()) { i -> arr.getString(i) }
        } catch (e: Exception) {
            mutableListOf("Targeting Lead Role")
        }
    }

    // Parse active connected accounts state
    val parsedConnections = remember(currentProfile.connectedAccountsJson) {
        try {
            val obj = JSONObject(currentProfile.connectedAccountsJson)
            ConnectedAccounts(
                google = obj.optBoolean("google", true),
                github = obj.optBoolean("github", true),
                linkedin = obj.optBoolean("linkedin", false),
                stackoverflow = obj.optBoolean("stackoverflow", true)
            )
        } catch (e: Exception) {
            ConnectedAccounts()
        }
    }

    // Cover Gradients Map
    val gradients = mapOf(
        "gradient_cosmic" to Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFFEC4899))),
        "gradient_solar" to Brush.horizontalGradient(listOf(Color(0xFFF97316), Color(0xFFEC4899), Color(0xFFE11D48))),
        "gradient_aurora" to Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF6366F1))),
        "gradient_emerald" to Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)))
    )

    val currentCoverGradient = gradients[currentProfile.coverGradient] ?: gradients["gradient_cosmic"]!!

    // Avatars Mapping
    val avatarsColors = mapOf(
        "avatar_1" to Color(0xFF6366F1),
        "avatar_2" to Color(0xFF10B981),
        "avatar_3" to Color(0xFFF97316),
        "avatar_4" to Color(0xFFEC4899)
    )
    val avatarsLabels = mapOf(
        "avatar_1" to "🚀",
        "avatar_2" to "💻",
        "avatar_3" to "⭐",
        "avatar_4" to "🔥"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Hub & Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("profile_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showExportDialog = true
                        },
                        modifier = Modifier.testTag("export_profile_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export Profile Data")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // COVER HEADER COMPONENT
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(currentCoverGradient)
            ) {
                // Change Cover Button
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            val gradientKeys = gradients.keys.toList()
                            val nextIndex = (gradientKeys.indexOf(currentProfile.coverGradient) + 1) % gradientKeys.size
                            viewModel.saveUserProfile(currentProfile.copy(coverGradient = gradientKeys[nextIndex]))
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, "Theme Cover", tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cover Theme", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // PROFILE AVATAR OVERLAY & DISPLAYS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-45).dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Circular profile avatar
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentProfile.profilePictureUri.isNullOrEmpty()) {
                                avatarsColors[currentProfile.avatarId] ?: Color(0xFF6366F1)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .clickable {
                            showPhotoOptionDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentProfile.profilePictureUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = currentProfile.profilePictureUri,
                            contentDescription = "Custom Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = avatarsLabels[currentProfile.avatarId] ?: "🚀",
                            fontSize = 32.sp
                        )
                    }
                    
                    // Small visual indicator overlay to show editable camera icon
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Usernames block
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(bottom = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentProfile.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Identity",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (!currentProfile.fullName.isNullOrBlank()) {
                        Text(
                            text = currentProfile.fullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                    Text(
                        text = "@${currentProfile.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Edit Identity Quick Action
                IconButton(
                    onClick = { showEditIdentityDialog = true },
                    modifier = Modifier
                        .testTag("edit_identity_btn")
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .size(36.dp)
                ) {
                    Icon(Icons.Default.Edit, "Edit Name", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            // NESTED TAB DISPATCHER SYSTEM
            TabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.padding(horizontal = 4.dp),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Briefing", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Learning", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("AI Companion", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("companion_tab")
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = { Text("Advanced", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("advanced_tab")
                )
            }

            // Tab Content Frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> BriefingTab(
                        profile = currentProfile,
                        eduList = parsedEdu,
                        workList = parsedWork,
                        certsList = parsedCerts,
                        linksList = parsedLinks,
                        completedSessionsCount = sessions.filter { it.completed }.size,
                        onUpdateEdu = { showEditEduDialog = true },
                        onUpdateWork = { showEditWorkDialog = true },
                        onUpdateCerts = { showEditCertDialog = true },
                        onUpdateLinks = { showEditLinkDialog = true }
                    )
                    1 -> PrepGoalsTab(
                        viewModel = viewModel,
                        profile = currentProfile,
                        skillsList = parsedSkills,
                        goalsList = parsedGoals,
                        onAddGoal = { showAddGoalDialog = true }
                    )
                    2 -> AICompanionTab(
                        viewModel = viewModel,
                        profile = currentProfile,
                        memoriesList = parsedMemories,
                        onAddMemory = { showAddMemoryDialog = true }
                    )
                    3 -> AdvancedSettingsTab(
                        viewModel = viewModel,
                        profile = currentProfile,
                        connections = parsedConnections,
                        onWipeData = { showDeleteAccountWarning = true }
                    )
                }
            }
        }
    }

    // --- POPUP DIALOGS SHEET ENGINE ---

    // 1. Edit Identity Dialog
    if (showEditIdentityDialog) {
        var tempFullName by remember { mutableStateOf(currentProfile.fullName.ifBlank { currentProfile.displayName }) }
        var tempName by remember { mutableStateOf(currentProfile.displayName) }
        var tempUsername by remember { mutableStateOf(currentProfile.username) }
        var tempBio by remember { mutableStateOf(currentProfile.bio) }

        AlertDialog(
            onDismissRequest = { showEditIdentityDialog = false },
            title = { Text("Edit Identity Block") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tempFullName,
                        onValueChange = { tempFullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_full_name_input")
                    )
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Display/Preferred Name") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_display_name_input")
                    )
                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { tempUsername = it },
                        label = { Text("Username Handle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Personal Bio") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveUserProfile(
                            currentProfile.copy(
                                fullName = tempFullName.trim(),
                                displayName = tempName.trim(),
                                username = tempUsername.trim().lowercase(),
                                bio = tempBio.trim()
                            )
                        )
                        showEditIdentityDialog = false
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditIdentityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Photo Source Picker Dialog
    if (showPhotoOptionDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionDialog = false },
            title = { Text("Profile Photo Source") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select how you would like to set or upload your profile photo:", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            cameraLauncher.launch()
                            showPhotoOptionDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture Photo (Camera)")
                    }
                    Button(
                        onClick = {
                            GalleryLauncher.launch("image/*")
                            showPhotoOptionDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Collections, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Photo (Gallery/Files)")
                    }
                    if (!currentProfile.profilePictureUri.isNullOrEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.saveUserProfile(currentProfile.copy(profilePictureUri = null))
                                showPhotoOptionDialog = false
                                Toast.makeText(context, "Custom profile picture removed.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove Custom Picture")
                        }
                    }
                    TextButton(
                        onClick = { showPhotoOptionDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // 2. Edit Education Dialog
    if (showEditEduDialog) {
        var schoolName by remember { mutableStateOf("") }
        var degreeName by remember { mutableStateOf("") }
        var yearPassed by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditEduDialog = false },
            title = { Text("Add Education Milestone") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = schoolName, onValueChange = { schoolName = it }, label = { Text("Colleges / University") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = degreeName, onValueChange = { degreeName = it }, label = { Text("Degree / Majors") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = yearPassed,
                        onValueChange = { yearPassed = it },
                        label = { Text("Graduation Year") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (schoolName.isNotBlank() && degreeName.isNotBlank()) {
                            val entryObj = JSONObject().apply {
                                put("school", schoolName.trim())
                                put("degree", degreeName.trim())
                                put("year", yearPassed.trim())
                            }
                            val updatedArr = JSONArray(currentProfile.educationJson).apply {
                                put(entryObj)
                            }
                            viewModel.saveUserProfile(currentProfile.copy(educationJson = updatedArr.toString()))
                            showEditEduDialog = false
                        }
                    }
                ) {
                    Text("Add Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditEduDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Edit Work Experience Dialog
    if (showEditWorkDialog) {
        var companyName by remember { mutableStateOf("") }
        var roleTitle by remember { mutableStateOf("") }
        var durationRange by remember { mutableStateOf("") }
        var highlightBullet by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditWorkDialog = false },
            title = { Text("Add Career Work Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = roleTitle, onValueChange = { roleTitle = it }, label = { Text("Role / Job Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = durationRange, onValueChange = { durationRange = it }, label = { Text("Duration (e.g. 2022 - 2024)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = highlightBullet,
                        onValueChange = { highlightBullet = it },
                        label = { Text("Key Achievement / Description") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (companyName.isNotBlank() && roleTitle.isNotBlank()) {
                            val entryObj = JSONObject().apply {
                                put("company", companyName.trim())
                                put("role", roleTitle.trim())
                                put("duration", durationRange.trim())
                                put("highlights", highlightBullet.trim())
                            }
                            val updatedArr = JSONArray(currentProfile.workExperienceJson).apply {
                                put(entryObj)
                            }
                            viewModel.saveUserProfile(currentProfile.copy(workExperienceJson = updatedArr.toString()))
                            showEditWorkDialog = false
                        }
                    }
                ) {
                    Text("Add Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWorkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Edit Certifications Dialog
    if (showEditCertDialog) {
        var certName by remember { mutableStateOf("") }
        var certIssuer by remember { mutableStateOf("") }
        var certDate by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditCertDialog = false },
            title = { Text("Add Accredited Certification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = certName, onValueChange = { certName = it }, label = { Text("Certification Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = certIssuer, onValueChange = { certIssuer = it }, label = { Text("Issuing Organization") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = certDate, onValueChange = { certDate = it }, label = { Text("date Obtained") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (certName.isNotBlank()) {
                            val entryObj = JSONObject().apply {
                                put("name", certName.trim())
                                put("issuer", certIssuer.trim())
                                put("date", certDate.trim())
                            }
                            val updatedArr = JSONArray(currentProfile.certificationsJson).apply {
                                put(entryObj)
                            }
                            viewModel.saveUserProfile(currentProfile.copy(certificationsJson = updatedArr.toString()))
                            showEditCertDialog = false
                        }
                    }
                ) {
                    Text("Add Certification")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCertDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Edit Portfolio Links Dialog
    if (showEditLinkDialog) {
        var linkTitle by remember { mutableStateOf("") }
        var linkUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditLinkDialog = false },
            title = { Text("Add Portfolio Anchor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = linkTitle, onValueChange = { linkTitle = it }, label = { Text("Link Label (e.g. Portfolio)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = linkUrl, onValueChange = { linkUrl = it }, label = { Text("Destination URL") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (linkTitle.isNotBlank() && linkUrl.isNotBlank()) {
                            val entryObj = JSONObject().apply {
                                put("title", linkTitle.trim())
                                put("url", linkUrl.trim())
                            }
                            val updatedArr = JSONArray(currentProfile.portfolioLinksJson).apply {
                                put(entryObj)
                            }
                            viewModel.saveUserProfile(currentProfile.copy(portfolioLinksJson = updatedArr.toString()))
                            showEditLinkDialog = false
                        }
                    }
                ) {
                    Text("Add Hub Anchor")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditLinkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. Add Personal Goal Dialog
    if (showAddGoalDialog) {
        var goalText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Add Personal Milestone Tracker") },
            text = {
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    label = { Text("Actionable Target Goal") },
                    placeholder = { Text("e.g. Master 3 sliding window algorithms") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (goalText.isNotBlank()) {
                            val entryObj = JSONObject().apply {
                                put("goal", goalText.trim())
                                put("isCompleted", false)
                            }
                            val updatedArr = JSONArray(currentProfile.personalGoalsJson).apply {
                                put(entryObj)
                            }
                            viewModel.saveUserProfile(currentProfile.copy(personalGoalsJson = updatedArr.toString()))
                            showAddGoalDialog = false
                        }
                    }
                ) {
                    Text("Add Target")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 7. Add Personal Memory Facts Dialog
    if (showAddMemoryDialog) {
        var memoryText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddMemoryDialog = false },
            title = { Text("Insert AI Assistant Factoid") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Inform your AI companion of specific habits, targets, or weak areas. These are dynamically injected into active companion conversations:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = memoryText,
                        onValueChange = { memoryText = it },
                        label = { Text("What should the companion remember?") },
                        placeholder = { Text("e.g. Struggles with O(log N) partitioning") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (memoryText.isNotBlank()) {
                            val arr = JSONArray(currentProfile.aiPersonalMemoriesJson).put(memoryText.trim())
                            viewModel.saveUserProfile(currentProfile.copy(aiPersonalMemoriesJson = arr.toString()))
                            showAddMemoryDialog = false
                        }
                    }
                ) {
                    Text("Inject Fact")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 8. Profile raw exporter sheet dialog
    if (showExportDialog) {
        val jsonPayload = remember(currentProfile) {
            JSONObject().apply {
                put("displayName", currentProfile.displayName)
                put("username", currentProfile.username)
                put("bio", currentProfile.bio)
                put("skills", JSONArray(currentProfile.skills))
                put("education", JSONArray(currentProfile.educationJson))
                put("workExperience", JSONArray(currentProfile.workExperienceJson))
                put("certifications", JSONArray(currentProfile.certificationsJson))
                put("portfolioLinks", JSONArray(currentProfile.portfolioLinksJson))
                put("personalGoals", JSONArray(currentProfile.personalGoalsJson))
                put("aiPersonalMemories", JSONArray(currentProfile.aiPersonalMemoriesJson))
                put("configurationPreferences", JSONObject().apply {
                    put("language", currentProfile.languagePref)
                    put("theme", currentProfile.themePref)
                    put("personality", currentProfile.aiPersonality)
                    put("level", currentProfile.learningLayoutLevel)
                })
            }.toString(4)
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Profile JSON Core")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your career properties compiled dynamically into standard compliant JSON layout formats. Useful for resumes adapters or model feeds:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF1E1E24), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF2E2E38), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = jsonPayload,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFFA9B1D6)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(jsonPayload))
                        Toast.makeText(context, "Profile parsed core copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    }
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // 9. Wipe account dialog warning
    if (showDeleteAccountWarning) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountWarning = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dangerous, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Absolute Secure Erase Warning", color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Text(
                    text = "Are you absolutely sure you want to trigger account erasure? Under secure GDPR standards guidelines, completing this action will irreversibly destroy:\n" +
                            "• Your entire portfolio resume profile\n" +
                            "• All complete mock practice sessions & scoring arrays\n" +
                            "• Historic activity metrics\n" +
                            "• Locked and unlocked achievement metrics\n\n" +
                            "This procedure cannot be undone in any format.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteAccountData {
                            Toast.makeText(context, "Memory registry successfully wiped", Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        }
                        showDeleteAccountWarning = false
                    }
                ) {
                    Text("Irreversibly Erase Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =================== TAB REVIEWS ===================

// TAB 0: BRIEFING (Portfolio, work record, resume ATS feed)
@Composable
fun BriefingTab(
    profile: UserProfileEntity,
    eduList: List<EduItem>,
    workList: List<WorkItem>,
    certsList: List<CertItem>,
    linksList: List<LinkItem>,
    completedSessionsCount: Int,
    onUpdateEdu: () -> Unit,
    onUpdateWork: () -> Unit,
    onUpdateCerts: () -> Unit,
    onUpdateLinks: () -> Unit
) {
    val scrollState = rememberScrollState()

    val skillsCount = remember(profile.skills) {
        try {
            JSONArray(profile.skills).length()
        } catch (e: Exception) {
            0
        }
    }

    val goalsStats = remember(profile.personalGoalsJson) {
        try {
            val arr = JSONArray(profile.personalGoalsJson)
            var completed = 0
            for (i in 0 until arr.length()) {
                if (arr.getJSONObject(i).optBoolean("isCompleted")) completed++
            }
            Pair(completed, arr.length())
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    val isBioCompleted = remember(profile.bio) {
        profile.bio.isNotBlank() && profile.bio != "Click edit above to write your bio blueprint..."
    }
    val isSkillsCompleted = remember(skillsCount) {
        skillsCount > 0
    }
    val isExperienceCompleted = remember(workList) {
        workList.isNotEmpty()
    }
    val isPhotoCompleted = remember(profile.profilePictureUri) {
        !profile.profilePictureUri.isNullOrEmpty()
    }

    val completedFieldsCount = remember(isBioCompleted, isSkillsCompleted, isExperienceCompleted, isPhotoCompleted) {
        listOf(isBioCompleted, isSkillsCompleted, isExperienceCompleted, isPhotoCompleted).count { it }
    }

    val profileProgressFraction = remember(completedFieldsCount) {
        completedFieldsCount / 4f
    }

    val progressFraction = remember(goalsStats, completedSessionsCount) {
        val totalTasks = goalsStats.second + 5 // baseline target of 5 practices
        val completedTasks = goalsStats.first + completedSessionsCount
        if (totalTasks == 0) 0f else (completedTasks.toFloat() / totalTasks.toFloat()).coerceIn(0f, 1f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CAREER PREPARATION PROGRESS HEAD CARD
        Card(
            modifier = Modifier.fillMaxWidth().testTag("profile_progress_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Profile Preparedness Score",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${(profileProgressFraction * 100).toInt()}% Set",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { profileProgressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Completion Checklist
                Text(
                    text = "Profile Completion Checklist",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ChecklistItemRow(
                        title = "Professional Bio Blueprint",
                        subtext = "Tell the AI about your goals & style",
                        isCompleted = isBioCompleted,
                        testTag = "checklist_item_bio"
                    )
                    ChecklistItemRow(
                        title = "Skills Inventory declaration",
                        subtext = "Define top skills and tools below",
                        isCompleted = isSkillsCompleted,
                        testTag = "checklist_item_skills"
                    )
                    ChecklistItemRow(
                        title = "Professional Employment History",
                        subtext = "Add experience to train AI tailored practice",
                        isCompleted = isExperienceCompleted,
                        testTag = "checklist_item_experience"
                    )
                    ChecklistItemRow(
                        title = "Custom Profile Picture Upload",
                        subtext = "Upload using camera or choose a file",
                        isCompleted = isPhotoCompleted,
                        testTag = "checklist_item_photo"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))

                // Activity Metrics sub-row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("practices", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$completedSessionsCount Sessions", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)).align(Alignment.CenterVertically))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Milestones", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${goalsStats.first}/${goalsStats.second} Goals", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)).align(Alignment.CenterVertically))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Acquired", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$skillsCount Skills", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bio Blueprint", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = profile.bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Education details block
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Education Chronology", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onUpdateEdu, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.AddCircleOutline, "Add Education", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (eduList.isEmpty()) {
                Text("No Education details provided yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                eduList.forEach { edu ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(edu.school, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${edu.degree}  •  ${edu.year}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Working records block
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Work Chronology", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onUpdateWork, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.AddCircleOutline, "Add Work", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (workList.isEmpty()) {
                Text("No professional employment history declared.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                workList.forEach { work ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.BusinessCenter, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(work.company, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${work.role} (${work.duration})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (work.highlights.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• ${work.highlights}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Certifications block
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Professional Credentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onUpdateCerts, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.AddCircleOutline, "Add Cert", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (certsList.isEmpty()) {
                Text("No technical certifications added yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                certsList.forEach { cert ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardMembership, null, tint = Color(0xFFE0AF68), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(cert.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Issued by ${cert.issuer}  •  ${cert.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Anchors and Links
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Digital Handles & Portfolios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onUpdateLinks, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.AddCircleOutline, "Add Link", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Static Git and LinkedIn Handles
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("GitHub Handle", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(profile.githubUsername, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Card(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("LinkedIn Hub", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(profile.linkedinHandle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                linksList.forEach { link ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.OpenInNew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(link.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(link.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// TAB 1: PREPARATION & GOALS (Skills addition, Goals checklist, Learning configs)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrepGoalsTab(
    viewModel: InterviewViewModel,
    profile: UserProfileEntity,
    skillsList: MutableList<String>,
    goalsList: List<GoalItem>,
    onAddGoal: () -> Unit
) {
    var newSkillName by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Custom Skills Tags Manager Block
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Competency Core Portfolio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Manage candidate taxonomy skills fed directly to Gemini prompts during adaptive interview generation:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    skillsList.forEach { skill ->
                        InputChip(
                            selected = true,
                            onClick = {
                                val replacement = skillsList.toMutableList().apply { remove(skill) }
                                viewModel.saveUserProfile(profile.copy(skills = JSONArray(replacement).toString()))
                            },
                            label = { Text(skill, fontSize = 11.sp) },
                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSkillName,
                        onValueChange = { newSkillName = it },
                        placeholder = { Text("Add specialized skill (e.g., Redis)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newSkillName.isNotBlank() && !skillsList.contains(newSkillName.trim())) {
                                val replacement = skillsList.toMutableList().apply { add(newSkillName.trim()) }
                                viewModel.saveUserProfile(profile.copy(skills = JSONArray(replacement).toString()))
                                newSkillName = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, null)
                    }
                }
            }
        }

        // Custom Targets Checklist Goals Block
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Interactive Goal Blueprint", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onAddGoal,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Target", fontSize = 11.sp)
                    }
                }
            }

            if (goalsList.isEmpty()) {
                Text("No preparation targets declared.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        goalsList.forEachIndexed { index, item ->
                            val strike = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            val color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val arr = JSONArray(profile.personalGoalsJson)
                                        val entry = arr.getJSONObject(index)
                                        entry.put("isCompleted", !item.isCompleted)
                                        viewModel.saveUserProfile(profile.copy(personalGoalsJson = arr.toString()))
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = item.isCompleted,
                                        onCheckedChange = {
                                            val arr = JSONArray(profile.personalGoalsJson)
                                            val entry = arr.getJSONObject(index)
                                            entry.put("isCompleted", it)
                                            viewModel.saveUserProfile(profile.copy(personalGoalsJson = arr.toString()))
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item.goal, textDecoration = strike, color = color, style = MaterialTheme.typography.bodyMedium)
                                }

                                IconButton(
                                    onClick = {
                                        val arr = JSONArray(profile.personalGoalsJson).apply {
                                            remove(index)
                                        }
                                        viewModel.saveUserProfile(profile.copy(personalGoalsJson = arr.toString()))
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Delete Goal", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Learning & Profile Preferences configuration
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Learning Adaptability Schemes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Layout level picker
                Column {
                    Text("HIERARCHY LAYOUT PRESET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Intern", "Junior", "Senior", "Staff").forEach { lvl ->
                            FilterChip(
                                selected = profile.learningLayoutLevel == lvl,
                                onClick = { viewModel.saveUserProfile(profile.copy(learningLayoutLevel = lvl)) },
                                label = { Text(lvl, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Target pace picker
                Column {
                    Text("TARGET PACING INTENSITY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Relaxed Pacing", "Intensive Pacing").forEach { pace ->
                            FilterChip(
                                selected = profile.targetPace == pace,
                                onClick = { viewModel.saveUserProfile(profile.copy(targetPace = pace)) },
                                label = { Text(pace, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// TAB 2: AI COMPANION (Live chat area, personality picker, memories management)
@Composable
fun AICompanionTab(
    viewModel: InterviewViewModel,
    profile: UserProfileEntity,
    memoriesList: MutableList<String>,
    onAddMemory: () -> Unit
) {
    val chat by viewModel.companionChat.collectAsState()
    val isTyping by viewModel.isCompanionTyping.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Slide bottom when companion replies
    LaunchedEffect(chat.size, isTyping) {
        if (chat.isNotEmpty()) {
            listState.animateScrollToItem(chat.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        
        // Dynamic Personality Header Selection Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SupportAgent, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Active Companion Persona", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(profile.aiPersonality, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearCompanionChat() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Reset Chat", fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable chip row of personality presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Encouraging Coach", "Stern Interviewer", "Pragmatic Architect", "Socratic Philosopher").forEach { persona ->
                        FilterChip(
                            selected = profile.aiPersonality == persona,
                            onClick = {
                                viewModel.saveUserProfile(profile.copy(aiPersonality = persona))
                                viewModel.addDiagnosticLogDirect("Companion persona changed: $persona")
                            },
                            label = { Text(persona, fontSize = 9.sp) }
                        )
                    }
                }
            }
        }

        // Personal AI Memory Management Segment (Inline collapse)
        var memExpanded by remember { mutableStateOf(false) }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { memExpanded = !memExpanded }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Personal AI Memory Sync (${memoriesList.size} Facts)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Icon(
                        imageVector = if (memExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(visible = memExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("These facts are selectively embedded into the Companion context ensuring customized training:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        memoriesList.forEachIndexed { idx, entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.secondary, CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(entry, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(
                                    onClick = {
                                        val replacement = memoriesList.toMutableList().apply { removeAt(idx) }
                                        viewModel.saveUserProfile(profile.copy(aiPersonalMemoriesJson = JSONArray(replacement).toString()))
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Clear, "Forget", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        Button(
                            onClick = onAddMemory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Inject Factoid Into Coach Memory", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Live chatbot messages window
        Card(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = chat, key = { it.id }) { msg ->
                        val align = if (msg.isUser) Alignment.End else Alignment.Start
                        val background = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val textCol = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = align
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (msg.isUser) 16.dp else 0.dp,
                                            bottomEnd = if (msg.isUser) 0.dp else 16.dp
                                        )
                                    )
                                    .background(background)
                                    .padding(12.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = textCol,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    if (isTyping) {
                        item {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Tutor formulating insight...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Prompt helper chips row
        Text("LAUNCH STRATEGY ASSISTANCE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Verify my STAR structures", "Suggest technical skill highlights", "Analyze my background gaps").forEach { prompt ->
                SuggestionChip(
                    onClick = {
                        viewModel.sendMessageToCompanion(prompt)
                    },
                    label = { Text(prompt, fontSize = 9.sp) }
                )
            }
        }

        // Text send bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Ask Coach support query...", fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1.0f)
                    .testTag("companion_chat_input"),
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    if (messageText.isNotBlank()) {
                        IconButton(onClick = { messageText = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessageToCompanion(messageText.trim())
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("companion_send_btn"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", modifier = Modifier.size(18.dp))
            }
        }
    }
}

// TAB 3: ADVANCED PREFERENCES (Toggles, Security simulation, Account compliance, GDPR Wipe)
@Composable
fun AdvancedSettingsTab(
    viewModel: InterviewViewModel,
    profile: UserProfileEntity,
    connections: ConnectedAccounts,
    onWipeData: () -> Unit
) {
    val scrollState = rememberScrollState()

    val parsedLogins = remember(profile.loginHistoryJson) {
        try {
            val arr = JSONArray(profile.loginHistoryJson)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                LoginRecord(
                    device = obj.optString("device"),
                    location = obj.optString("location"),
                    timestamp = obj.optString("timestamp")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Multi-Preferences selectors
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("App Preferences Configs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Select Language preference
                Column {
                    Text("PLATFORM SYSTEM LANGUAGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("English", "Spanish", "French", "Japanese", "Hindi").forEach { lang ->
                            FilterChip(
                                selected = profile.languagePref == lang,
                                onClick = { viewModel.saveUserProfile(profile.copy(languagePref = lang)) },
                                label = { Text(lang, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Select Theme preference
                Column {
                    Text("ACTIVE COGNITIVE THEME PALETTE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Cosmic Slate", "Midnight Indigo", "Solar Blaze", "Emerald Premium").forEach { thm ->
                            FilterChip(
                                selected = profile.themePref == thm,
                                onClick = { viewModel.saveUserProfile(profile.copy(themePref = thm)) },
                                label = { Text(thm, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Privacy configuration toggles
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Privacy & GDPR Compliance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Profile Public Visibility", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Generate indexable anchors representing profile state", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = profile.profileVisible, onCheckedChange = { viewModel.saveUserProfile(profile.copy(profileVisible = it)) })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Companion Brain Fact Retention", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Allows Gemini assistant algorithms to cache habits and bio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = profile.aiMemoryRetention, onCheckedChange = { viewModel.saveUserProfile(profile.copy(aiMemoryRetention = it)) })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Diagnostics Telemetry sync", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Permits logging active queries metrics safely", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = profile.crashDiagnosticsSync, onCheckedChange = { viewModel.saveUserProfile(profile.copy(crashDiagnosticsSync = it)) })
                }
            }
        }

        // Security locks biometric hardware simulator
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Security Credentials Lock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enforce Biometric PIN Authentication", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Prompts locks during platform relaunch simulation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = profile.securityPinEnabled, onCheckedChange = { viewModel.saveUserProfile(profile.copy(securityPinEnabled = it)) })
                }
            }
        }

        // Notification routing toggles
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Notification Delivery Routing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Daily Career Push Reminders", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Trigger localized alarms matching schedule", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = profile.pushRemindersEnabled, onCheckedChange = { viewModel.saveUserProfile(profile.copy(pushRemindersEnabled = it)) })
                }

                if (profile.pushRemindersEnabled) {
                    val reminderHour by viewModel.reminderHour.collectAsState()
                    val reminderMinute by viewModel.reminderMinute.collectAsState()
                    val formattedTime = viewModel.getFormattedReminderTime(reminderHour, reminderMinute)

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Practice Reminder Time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Scheduled daily at: $formattedTime",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box {
                                    var showHourMenu by remember { mutableStateOf(false) }
                                    Button(
                                        onClick = { showHourMenu = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                                    ) {
                                        Text(text = "Hr: $reminderHour", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp))
                                    }
                                    DropdownMenu(expanded = showHourMenu, onDismissRequest = { showHourMenu = false }) {
                                        (0..23).forEach { h ->
                                            DropdownMenuItem(
                                                text = { Text("$h (${if (h >= 12) "PM" else "AM"})") },
                                                onClick = {
                                                    viewModel.updateReminderTime(h, reminderMinute)
                                                    showHourMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Box {
                                    var showMinuteMenu by remember { mutableStateOf(false) }
                                    Button(
                                        onClick = { showMinuteMenu = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                                    ) {
                                        Text(text = "Min: ${String.format("%02d", reminderMinute)}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp))
                                    }
                                    DropdownMenu(expanded = showMinuteMenu, onDismissRequest = { showMinuteMenu = false }) {
                                        listOf(0, 15, 30, 45).forEach { m ->
                                            DropdownMenuItem(
                                                text = { Text(String.format("%02d", m)) },
                                                onClick = {
                                                    viewModel.updateReminderTime(reminderHour, m)
                                                    showMinuteMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.triggerTestReminder() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .testTag("test_streak_reminder_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Send Instant Streak Reminder Notification", 
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Weekly Analytics Digests via Email", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Generates standard performance reports", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = profile.performanceEmailReports, onCheckedChange = { viewModel.saveUserProfile(profile.copy(performanceEmailReports = it)) })
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Badge Milestones Sound Alerts", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Triggers haptic pulses on completing session goals", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = profile.badgeAlertsEnabled, onCheckedChange = { viewModel.saveUserProfile(profile.copy(badgeAlertsEnabled = it)) })
                }
            }
        }

        // Account setups
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Account Configurations Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Email Registration:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.accountEmail, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Service Subscription Tier:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.accountTier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Current Country Hub:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.country, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Connected Accounts Management
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Connected Platform Integration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                listOf(
                    IntegrationRowItem("Google Identity", "eprem1737@gmail.com", connections.google) {
                        val updated = JSONObject(profile.connectedAccountsJson).apply { put("google", !connections.google) }.toString()
                        viewModel.saveUserProfile(profile.copy(connectedAccountsJson = updated))
                    },
                    IntegrationRowItem("GitHub Hub Sync", "@eprem-dev", connections.github) {
                        val updated = JSONObject(profile.connectedAccountsJson).apply { put("github", !connections.github) }.toString()
                        viewModel.saveUserProfile(profile.copy(connectedAccountsJson = updated))
                    },
                    IntegrationRowItem("StackOverflow Connect", "UserID: 829283", connections.stackoverflow) {
                        val updated = JSONObject(profile.connectedAccountsJson).apply { put("stackoverflow", !connections.stackoverflow) }.toString()
                        viewModel.saveUserProfile(profile.copy(connectedAccountsJson = updated))
                    },
                    IntegrationRowItem("LinkedIn Profile Sync", "In/candidate-pro", connections.linkedin) {
                        val updated = JSONObject(profile.connectedAccountsJson).apply { put("linkedin", !connections.linkedin) }.toString()
                        viewModel.saveUserProfile(profile.copy(connectedAccountsJson = updated))
                    }
                ).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(item.subtext, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = item.onToggle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (item.isConnected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(if (item.isConnected) "Disconnect" else "Connect Link", fontSize = 10.sp, color = if (item.isConnected) MaterialTheme.colorScheme.onSecondaryContainer else Color.White)
                        }
                    }
                }
            }
        }

        // MOCKED LOGIN SEGMENT HISTORIC RECORDS LIST
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Login Access Historic Audits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                if (parsedLogins.isEmpty()) {
                    Text("No logins recorded", style = MaterialTheme.typography.bodySmall)
                } else {
                    parsedLogins.forEach { obj ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Devices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(obj.device, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(obj.location, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(obj.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // DANGER ZONE (Delete account, wipe schemas GDPR compliant)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Advanced Core Danger Zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("Permanently and securely shred all credentials, portfolio files, achievements, and statistics matching local databases. Privacy-first compliance routine:", style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = onWipeData,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("delete_account_btn")
                ) {
                    Text("Secure Destructive Account Deletion", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Data holder subclasses
data class EduItem(val school: String, val degree: String, val year: String)
data class WorkItem(val company: String, val role: String, val duration: String, val highlights: String)
data class CertItem(val name: String, val issuer: String, val date: String)
data class LinkItem(val title: String, val url: String)
data class GoalItem(val goal: String, val isCompleted: Boolean)
data class ConnectedAccounts(val google: Boolean = true, val github: Boolean = true, val linkedin: Boolean = false, val stackoverflow: Boolean = true)
data class IntegrationRowItem(val name: String, val subtext: String, val isConnected: Boolean, val onToggle: () -> Unit)
data class LoginRecord(val device: String, val location: String, val timestamp: String)

fun saveUriToLocalFile(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = java.io.File(context.filesDir, "custom_profile_picture.jpg")
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveBitmapToLocalFile(context: android.content.Context, bitmap: Bitmap): String? {
    return try {
        val file = java.io.File(context.filesDir, "custom_profile_picture.jpg")
        val outputStream = java.io.FileOutputStream(file)
        outputStream.use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun ChecklistItemRow(
    title: String,
    subtext: String,
    isCompleted: Boolean,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isCompleted) "Completed" else "Pending",
            tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null
                ),
                fontWeight = FontWeight.SemiBold,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

