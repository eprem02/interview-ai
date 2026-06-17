package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.InterviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeScreen(
    viewModel: InterviewViewModel,
    onNavigateBack: () -> Unit
) {
    val resume by viewModel.resumeState.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingResume.collectAsState()
    val analysisError by viewModel.analysisError.collectAsState()

    var resumeText by remember { mutableStateOf("") }
    var targetJobTitle by remember { mutableStateOf("") }
    
    // Simulating visual upload scanner
    var isSimulatingUpload by remember { mutableStateOf(false) }

    // Synchronize initial form value
    LaunchedEffect(resume) {
        resume?.let {
            resumeText = it.textContent
            targetJobTitle = it.jobTitle
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI ATS Career Suite", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. File Upload section (PDF / DOCX Simulator)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "RESUME DOCUMENT UPLOADER",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                            .clickable {
                                isSimulatingUpload = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSimulatingUpload) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Reading PDF/DOCX stream metadata...", style = MaterialTheme.typography.labelSmall)
                            }
                            
                            // Simulate fast OCR parsing after 1.5 seconds
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(1500)
                                isSimulatingUpload = false
                                targetJobTitle = "Senior Software Architect"
                                resumeText = """
                                  John Doe - Senior Software Engineer
                                  Email: john.doe@example.com | GitHub: github.com/johndoe
                                  
                                  SUMMARY:
                                  Highly collaborative Engineer with 6+ years experience in reactive client applications, multi-tier microservice API deployment, and system architecture.
                                  
                                  TECHNICAL SKILLS:
                                  - Languages: Kotlin, Java, Python, SQL, HTML
                                  - Frameworks: Jetpack Compose, Spring Boot, Ktor, OkHttp, Retrofit
                                  - Tools: Git, Docker, AWS (EC2/S3), Room Database, SQLite
                                  
                                  EXPERIENCE:
                                  Senior Developer @ TechCorp (2022 - Present)
                                  - Designed scalable microservices resulting in a 40% reduction in API overhead.
                                  - Championed migration from complex XML layouts to Jetpack Compose, improving UI speed by 18%.
                                """.trimIndent()
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Upload icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Click to import PDF / DOCX file", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Accepts standard resume file structures", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 2. Resume Text Form Inputs
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "VERIFY PROFILE DATA",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    OutlinedTextField(
                        value = targetJobTitle,
                        onValueChange = { targetJobTitle = it },
                        label = { Text("Target Position Title") },
                        placeholder = { Text("e.g. Senior Software Architect") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("resume_job_title_input")
                    )

                    OutlinedTextField(
                        value = resumeText,
                        onValueChange = { resumeText = it },
                        label = { Text("Resume details / Work background text") },
                        placeholder = { Text("Copy and paste details here...") },
                        minLines = 6,
                        maxLines = 10,
                        leadingIcon = { Icon(Icons.Default.TextFormat, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("resume_content_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (resume != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.deleteResume()
                                    resumeText = ""
                                    targetJobTitle = ""
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }

                        Button(
                            onClick = {
                                if (resumeText.isNotBlank() && targetJobTitle.isNotBlank()) {
                                    viewModel.analyzeUserResume(resumeText, targetJobTitle)
                                }
                            },
                            modifier = Modifier.weight(2f).height(50.dp).testTag("save_resume_btn"),
                            enabled = resumeText.isNotBlank() && targetJobTitle.isNotBlank() && !isAnalyzing
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auditing with Gemini...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Audit & Fit Resume")
                            }
                        }
                    }

                    if (analysisError != null) {
                        Text(
                            text = analysisError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 3. ATS Analysis & Advanced career feedback (Visible if audited)
            val activeResume = resume
            if (activeResume != null && activeResume.atsScore > 0) {
                // Score Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Circular score preview
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${activeResume.atsScore}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "ATS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            val evaluationLabel = when {
                                activeResume.atsScore >= 85 -> "Highly Optimized"
                                activeResume.atsScore >= 70 -> "Well Formatted"
                                else -> "Requires Keyword Mapping"
                            }
                            Text(
                                text = "ATS PREPAREDNESS: $evaluationLabel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Your profile was validated against real candidate filters with Gemini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // ATS feedback Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, "ATS Score", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ATS Feedback Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        
                        Text(
                            text = activeResume.atsFeedback,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Skill Gaps Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, "Skill Gap Indicator", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Identified Skill Gaps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "To match 100% of recruiters' tech criteria for '${activeResume.jobTitle}', strengthen these areas:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeResume.skillGapAnalysis,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Personalized learning path roadmap
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MenuBook, "Roadmap", tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Personalized Learning Roadmap",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Text(
                            text = activeResume.learningRoadmap,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else if (activeResume != null) {
                // Profile Saved but not audited yet
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.CloudDone, "Done", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Profile Saved Locally!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "To access customized ATS scores, comprehensive skill gap alignment lists, and learning roadmaps, click 'Audit & Fit Resume' above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
