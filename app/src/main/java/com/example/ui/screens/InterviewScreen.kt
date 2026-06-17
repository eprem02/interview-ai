package com.example.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.viewmodel.InterviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewScreen(
    viewModel: InterviewViewModel,
    jobTitle: String,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (Long) -> Unit
) {
    val isGenerating by viewModel.isGeneratingSession.collectAsState()
    val isEvaluating by viewModel.isEvaluatingAnswer.collectAsState()
    val isCompleting by viewModel.isCompletingSession.collectAsState()

    val activeSession by viewModel.activeSession.collectAsState()
    val questions by viewModel.activeQuestions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()

    val isListening by viewModel.isListening.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    val speechError by viewModel.speechError.collectAsState()
    val isSpeaking by viewModel.isTtsSpeaking.collectAsState()

    // Answer constructed state
    var typedAnswer by remember { mutableStateOf("") }

    // Native permission states
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        }
    }

    // Synchronize or speak questions
    LaunchedEffect(questions, currentIndex) {
        if (questions.isNotEmpty() && currentIndex < questions.size) {
            val q = questions[currentIndex]
            viewModel.speakQuestion(q.question)
            
            // Auto load boilerplates for Coding challenges
            if (q.isCodingChallenge && q.defaultCodeSnippet.isNotBlank()) {
                typedAnswer = q.defaultCodeSnippet
            } else {
                typedAnswer = ""
            }
        }
    }

    // Capture recognized text append safely
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotBlank()) {
            val q = questions.getOrNull(currentIndex)
            // Only auto-append speech input if it's behavioral or empty
            if (q == null || !q.isCodingChallenge) {
                typedAnswer = if (typedAnswer.endsWith(" ") || typedAnswer.isEmpty()) {
                    typedAnswer + recognizedText
                } else {
                    "$typedAnswer $recognizedText"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val session = activeSession
                    val companyName = session?.company ?: "General"
                    Text("$companyName Round Prep", fontWeight = FontWeight.ExtraBold) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.resetSessionState()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("exit_session_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isGenerating -> {
                    GeneratingQuestionsLoader(jobTitle = jobTitle)
                }
                questions.isEmpty() -> {
                    EmptyStateError(onNavigateBack)
                }
                else -> {
                    val currentQuestion = questions.getOrNull(currentIndex)
                    if (currentQuestion != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            
                            // Top progress tracker row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${currentIndex + 1}", 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Q. ${currentIndex + 1} of ${questions.size}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                val langLabel = if (currentQuestion.isCodingChallenge) "CODING (${activeSession?.codingLanguage ?: ""})" else "BEHAVIORAL"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = langLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { (currentIndex + 1).toFloat() / questions.size },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // Question Title Speech bubble
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = currentQuestion.question,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        lineHeight = 22.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // TTS audio pulse controller
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            if (isSpeaking) {
                                                viewModel.stopSpeaking()
                                            } else {
                                                viewModel.speakQuestion(currentQuestion.question)
                                            }
                                        }
                                    ) {
                                        AudioPulseAnimation(isSpeaking = isSpeaking)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isSpeaking) "Interviewer Speaking... Tap to Silence" else "Tap to Read Out Loud",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Render Custom Interactive UI depending on Coding Mode vs Concept mode
                            if (currentQuestion.isCodingChallenge) {
                                // 🌟 IDE-STYLED CODE PLAYGROUND EDITOR
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2E2E38))
                                ) {
                                    Column {
                                        // Playground Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF121214))
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFF5F56))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFFBD2E))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF27C93F))
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "active_workspace.${activeSession?.codingLanguage?.lowercase() ?: "kt"}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFA9B1D6)
                                                )
                                            }
                                            
                                            // Reset code command
                                            Row(
                                                modifier = Modifier.clickable {
                                                    typedAnswer = currentQuestion.defaultCodeSnippet
                                                },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Refresh, "reset snippet", tint = Color(0xFF7AA2F7), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Reset Boilerplate",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF7AA2F7)
                                                )
                                            }
                                        }

                                        // Playground Text input
                                        OutlinedTextField(
                                            value = typedAnswer,
                                            onValueChange = { typedAnswer = it },
                                            textStyle = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                color = Color(0xFFE0AF68)
                                            ),
                                            placeholder = { Text("// Write your optimized solution here...", color = Color(0xFF565F89)) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp)
                                                .testTag("answer_text_input"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.Transparent,
                                                unfocusedBorderColor = Color.Transparent,
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent
                                            )
                                        )

                                        // Smart Code Suggestion Booster
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF121214))
                                                .clickable {
                                                    // Quick code helper simulation matches requirements
                                                    typedAnswer = """
                                                      // Autocompleting optimal strategy skeleton
                                                      class Solution {
                                                          fun solve(input: String): Int {
                                                              val map = HashMap<Char, Int>()
                                                              for (item in input) {
                                                                  map[item] = map.getOrDefault(item, 0) + 1
                                                              }
                                                              return 0
                                                          }
                                                      }
                                                    """.trimIndent()
                                                }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.FlashOn, "Autocomplete skeleton", tint = Color(0xFFBB9AF7), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Autofill optimal strategy skeleton template",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFBB9AF7)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // 🌟 VOICE CONVERSATIONAL INTERFACES (Behavioral)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "TRANSCRIPT PANEL (PRECISE SPEECH-TO-TEXT)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedTextField(
                                            value = typedAnswer,
                                            onValueChange = { typedAnswer = it },
                                            placeholder = { Text("Speak or edit your answer transcript directly right here...") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .testTag("answer_text_input"),
                                            shape = RoundedCornerShape(16.dp)
                                        )

                                        if (speechError != null) {
                                            Text(
                                                text = speechError!!,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Mic recorder pulse
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            RecordVoiceButton(
                                                isListening = isListening,
                                                onClick = {
                                                    if (hasMicPermission) {
                                                        if (isListening) {
                                                            viewModel.stopListening()
                                                        } else {
                                                            viewModel.startListening()
                                                        }
                                                    } else {
                                                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = if (isListening) "Mic capturing audio..." else "Tap to dictate speech",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Submit Controllers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { typedAnswer = "" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("clear_answer_btn"),
                                    enabled = typedAnswer.isNotBlank()
                                ) {
                                    Text("Reset")
                                }

                                Button(
                                    onClick = {
                                        viewModel.submitAnswer(typedAnswer) {
                                            activeSession?.id?.let { onNavigateToResults(it) }
                                        }
                                        // Immediately reset typedAnswer state
                                        typedAnswer = ""
                                    },
                                    modifier = Modifier
                                        .weight(2f)
                                        .height(52.dp)
                                        .testTag("submit_answer_btn"),
                                    enabled = typedAnswer.isNotBlank()
                                ) {
                                    Text(if (currentIndex == questions.size - 1) "Finish Prep" else "Grade & Standard Key")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Evaluation Overlay Loader
            if (isEvaluating || isCompleting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (isEvaluating) "Gemini is evaluating code correctness & soft skills..." else "Compiling comprehensive aggregate career sheet...",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratingQuestionsLoader(jobTitle: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "generating")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "generating_rotate"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(44.dp)
                    .animateContentSize()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Assembling Customized Challenge Matrix...",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Gemini is building 3-5 high yield questions tailored around $jobTitle criteria.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyStateError(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "No questions found",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Empty active session detected",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please start a fresh session from your dashboard.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateBack) {
            Text("Back to Dashboard")
        }
    }
}

@Composable
fun AudioPulseAnimation(isSpeaking: Boolean) {
    val scale1 by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Box(contentAlignment = Alignment.Center) {
        if (isSpeaking) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f * scale1))
            )
        }
        Icon(
            imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
            contentDescription = "Read Aloud",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun RecordVoiceButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val scale by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(80.dp)
    ) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f * scale))
            )
        }

        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop Listening" else "Start Listening",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
