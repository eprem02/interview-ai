package com.example.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.InterviewQuestionAnswerEntity
import com.example.data.db.InterviewSessionEntity
import com.example.data.db.ResumeEntity
import com.example.data.db.UserProfileEntity
import com.example.data.db.KnowledgeDocumentEntity
import com.example.data.db.FlashcardEntity
import com.example.data.db.QuizQuestionEntity
import com.example.data.db.DocumentChatEntity
import kotlinx.coroutines.flow.Flow
import com.example.data.repository.InterviewRepository
import android.content.Context
import com.example.PracticeReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class InterviewViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: InterviewRepository
    
    // --- Room Observables ---
    val resumeState: StateFlow<ResumeEntity?>
    val allSessionsState: StateFlow<List<InterviewSessionEntity>>
    val userProfileState: StateFlow<UserProfileEntity?>
    val allKnowledgeDocsState: StateFlow<List<KnowledgeDocumentEntity>>

    private val _isProcessingDoc = MutableStateFlow(false)
    val isProcessingDoc = _isProcessingDoc.asStateFlow()

    // --- Companion Chat States ---
    private val _companionChat = MutableStateFlow<List<CompanionChatMessage>>(
        listOf(
            CompanionChatMessage(
                text = "Hello! I am your AI Career Companion. Set your profile avatar, configure your skills, or upload your resume in the dashboard. What concept can we work on today?",
                isUser = false
            )
        )
    )
    val companionChat = _companionChat.asStateFlow()

    private val _isCompanionTyping = MutableStateFlow(false)
    val isCompanionTyping = _isCompanionTyping.asStateFlow()

    // --- State For Conducting Active Session ---
    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId = _activeSessionId.asStateFlow()

    private val _activeSession = MutableStateFlow<InterviewSessionEntity?>(null)
    val activeSession = _activeSession.asStateFlow()

    private val _activeQuestions = MutableStateFlow<List<InterviewQuestionAnswerEntity>>(emptyList())
    val activeQuestions = _activeQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex = _currentQuestionIndex.asStateFlow()

    // --- Loading & Analysis States ---
    private val _isGeneratingSession = MutableStateFlow(false)
    val isGeneratingSession = _isGeneratingSession.asStateFlow()

    private val _isEvaluatingAnswer = MutableStateFlow(false)
    val isEvaluatingAnswer = _isEvaluatingAnswer.asStateFlow()

    private val _isCompletingSession = MutableStateFlow(false)
    val isCompletingSession = _isCompletingSession.asStateFlow()

    private val _isAnalyzingResume = MutableStateFlow(false)
    val isAnalyzingResume = _isAnalyzingResume.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError = _analysisError.asStateFlow()

    // --- App Settings & Custom Streaks (Mocked metrics requested) ---
    private val _userStreakCount = MutableStateFlow(3) // starter baseline simulation
    val userStreakCount = _userStreakCount.asStateFlow()

    private val _reminderHour = MutableStateFlow(9)
    val reminderHour = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(0)
    val reminderMinute = _reminderMinute.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _reportDownloadStatus = MutableStateFlow<String?>(null)
    val reportDownloadStatus = _reportDownloadStatus.asStateFlow()

    private val _emailReportStatus = MutableStateFlow<String?>(null)
    val emailReportStatus = _emailReportStatus.asStateFlow()

    private val _diagnosticLogs = MutableStateFlow<List<String>>(
        listOf("Application Initialized", "Database Loaded", "Gemini 3.5 Ready", "Core MVVM Loop bound")
    )
    val diagnosticLogs = _diagnosticLogs.asStateFlow()

    // --- Speech & Voice States ---
    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText = _recognizedText.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError = _speechError.asStateFlow()

    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking = _isTtsSpeaking.asStateFlow()

    private val _ttsReady = MutableStateFlow(false)
    val ttsReady = _ttsReady.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = InterviewRepository(database.interviewDao())

        resumeState = repository.resumeFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        allSessionsState = repository.allSessionsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        userProfileState = repository.userProfileFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        allKnowledgeDocsState = repository.allKnowledgeDocsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Asynchronously secure or create profile singleton on database setup
        viewModelScope.launch {
            repository.getOrCreateUserProfile()
        }

        // Load saved reminder schedule configurations
        val reminderPrefs = application.getSharedPreferences("practice_reminder_prefs", Context.MODE_PRIVATE)
        _reminderHour.value = reminderPrefs.getInt("reminder_hour", 9)
        _reminderMinute.value = reminderPrefs.getInt("reminder_minute", 0)

        // Initialize Speech and Voice Components safely
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(application)
            textToSpeech = TextToSpeech(application, this)
        } catch (e: Exception) {
            Log.e("InterviewViewModel", "Failed to initialize speech or TTS engine", e)
        }
    }

    // --- Toggle Settings ---
    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
        addLog("Premium Mode Toggled: ${_isDarkMode.value}")
    }

    private fun addLog(text: String) {
        _diagnosticLogs.value = _diagnosticLogs.value + text
    }

    // --- Resume Actions with Real Gemini Analyzer ---
    fun analyzeUserResume(textContent: String, targetJobTitle: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isAnalyzingResume.value = true
            _analysisError.value = null
            addLog("Executing Real Gemini ATS Audit & Skill Gap Alignment for '$targetJobTitle'...")
            try {
                repository.analyzeResume(textContent, targetJobTitle)
                addLog("Gemini ATS Audit Completed: Custom Learning Roadmap generated successfully.")
                onSuccess()
            } catch (e: Exception) {
                _analysisError.value = "Analysis failed: ${e.message}"
                addLog("Resume analysis error: ${e.message}")
            } finally {
                _isAnalyzingResume.value = false
            }
        }
    }

    fun deleteResume() {
        viewModelScope.launch {
            repository.deleteResume()
            addLog("Resume Cleared from Database")
        }
    }

    // --- Session Setup & Launch with Params ---
    fun startNewInterviewSessionWithParam(
        jobTitle: String,
        company: String,
        mode: String,
        codingLanguage: String,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isGeneratingSession.value = true
            addLog("Launching Custom generation: Company=$company, Role=$jobTitle, Mode=$mode")
            try {
                val sessionId = repository.startNewSessionWithParams(
                    jobTitle = jobTitle,
                    company = company,
                    mode = mode,
                    codingLanguage = codingLanguage
                )
                loadActiveSession(sessionId)
                addLog("Session #$sessionId populated and live.")
                onSuccess(sessionId)
            } catch (e: Exception) {
                Log.e("InterviewViewModel", "Failed to start active session with parameters", e)
                addLog("Session generation failed: ${e.message}")
            } finally {
                _isGeneratingSession.value = false
            }
        }
    }

    fun startNewInterviewSession(jobTitle: String, onSuccess: (Long) -> Unit) {
        startNewInterviewSessionWithParam(jobTitle, "General", "Behavioral", "Kotlin", onSuccess)
    }

    fun loadActiveSession(sessionId: Long) {
        viewModelScope.launch {
            _activeSessionId.value = sessionId
            _currentQuestionIndex.value = 0
            _recognizedText.value = ""

            // Gather elements and observe
            repository.getSessionFlowById(sessionId).collect { session ->
                _activeSession.value = session
            }
        }

        viewModelScope.launch {
            repository.getQuestionsForSessionFlow(sessionId).collect { questions ->
                _activeQuestions.value = questions
            }
        }
    }

    fun resetSessionState() {
        stopSpeaking()
        stopListening()
        _activeSessionId.value = null
        _activeSession.value = null
        _activeQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _recognizedText.value = ""
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            addLog("Deleted Session #$sessionId")
        }
    }

    // --- Answer Lifecycle & Interactive Playground Typing ---
    fun setTypedAnswer(text: String) {
        _recognizedText.value = text
    }

    // --- Conduct Answer Evaluation ---
    fun submitAnswer(answer: String, onFinished: () -> Unit) {
        val session = _activeSession.value ?: return
        val questions = _activeQuestions.value
        val index = _currentQuestionIndex.value
        if (index < 0 || index >= questions.size) return

        val currentQuestion = questions[index]

        viewModelScope.launch {
            _isEvaluatingAnswer.value = true
            stopSpeaking()
            stopListening()
            addLog("Uploading response for Q${currentQuestion.questionNumber} to Gemini...")
            try {
                repository.evaluateAnswer(
                    question = currentQuestion,
                    answer = answer,
                    jobTitle = session.jobTitle
                )
                
                // Clear state for next question
                _recognizedText.value = ""

                // Advance question index
                if (index < questions.size - 1) {
                    _currentQuestionIndex.value = index + 1
                    // Voice prompt the next question
                    speakQuestion(questions[index + 1].question)
                } else {
                    // Complete Session
                    completeActiveSession {
                        onFinished()
                    }
                }
            } catch (e: Exception) {
                Log.e("InterviewViewModel", "Failed to submit and evaluate answer", e)
                addLog("Answer evaluation failed: ${e.message}")
            } finally {
                _isEvaluatingAnswer.value = false
            }
        }
    }

    private fun completeActiveSession(onFinished: () -> Unit) {
        val sessionId = _activeSessionId.value ?: return
        val session = _activeSession.value ?: return

        viewModelScope.launch {
            _isCompletingSession.value = true
            addLog("Executing final Aggregate Session Feedback Analyzer...")
            try {
                repository.completeSession(sessionId, session.jobTitle)
                _userStreakCount.value = _userStreakCount.value + 1
                addLog("Session Analyzed. Streak count increased to ${_userStreakCount.value}")
                onFinished()
            } catch (e: Exception) {
                Log.e("InterviewViewModel", "Failed to compile end session aggregate summary feedback", e)
                addLog("Session finalization error: ${e.message}")
            } finally {
                _isCompletingSession.value = false
            }
        }
    }

    // --- Premium Interactive Utilities: Simulated Reports & Exports ---
    fun downloadPdfReport(session: InterviewSessionEntity) {
        viewModelScope.launch {
            _reportDownloadStatus.value = "Generating high-fidelity career report..."
            kotlinx.coroutines.delay(1200)
            val updated = session.copy(reportDownloaded = true)
            repository.updateSessionEntity(updated)
            _activeSession.value = updated
            _reportDownloadStatus.value = "PDF Export Saved to Downloads successfully!"
            addLog("PDF Career Report saved: /sdcard/Downloads/InterviewReport_${session.id}.pdf")
        }
    }

    fun sendEmailReport(session: InterviewSessionEntity, targetEmail: String) {
        viewModelScope.launch {
            _emailReportStatus.value = "Connecting to SMTP secure relay..."
            kotlinx.coroutines.delay(1500)
            val updated = session.copy(emailReportSent = true)
            repository.updateSessionEntity(updated)
            _activeSession.value = updated
            _emailReportStatus.value = "Platform Report dispatched to $targetEmail successfully!"
            addLog("Career Report emailed to $targetEmail")
        }
    }

    fun clearDownloadStatus() {
        _reportDownloadStatus.value = null
    }

    fun clearEmailStatus() {
        _emailReportStatus.value = null
    }

    // --- Text-To-Speech (TTS) callbacks and actions ---
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    _ttsReady.value = true
                }
            }
        }
    }

    fun speakQuestion(text: String) {
        if (!_ttsReady.value) return
        textToSpeech?.let { tts ->
            _isTtsSpeaking.value = true
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "InterviewQuestionUtf")
            // Monitor speaking state
            tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isTtsSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isTtsSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isTtsSpeaking.value = false
                }
            })
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isTtsSpeaking.value = false
    }

    // --- Speech-To-Text (STT) SpeechRecognizer Actions ---
    fun startListening() {
        val context = getApplication<Application>()
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }

        stopSpeaking() // Turn off TTS reading while recording starts
        _speechError.value = null
        _isListening.value = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _speechError.value = getSpeechErrorMessage(error)
                Log.e("InterviewViewModel", "Speech Recognizer Error: $error")
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _recognizedText.value = matches[0]
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    _recognizedText.value = matches[0]
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    private fun getSpeechErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions required"
            SpeechRecognizer.ERROR_NETWORK -> "Network error occurred"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Speak clearly."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Mic service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Speech recognition failed"
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
    }

    // --- User Profile & Companion Functionality ---
    fun saveUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            addLog("Profile updated: @${profile.username}")
            
            // Alarm scheduling reaction
            if (profile.pushRemindersEnabled) {
                PracticeReminderScheduler.scheduleDailyReminder(
                    getApplication(),
                    _reminderHour.value,
                    _reminderMinute.value,
                    _userStreakCount.value
                )
                addLog("Daily practice reminders scheduled at ${getFormattedReminderTime(_reminderHour.value, _reminderMinute.value)}")
            } else {
                PracticeReminderScheduler.cancelDailyReminder(getApplication())
                addLog("Daily practice reminders disabled")
            }
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        _reminderHour.value = hour
        _reminderMinute.value = minute
        val prefs = getApplication<Application>().getSharedPreferences("practice_reminder_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("reminder_hour", hour).putInt("reminder_minute", minute).apply()
        
        val profile = userProfileState.value
        val isEnabled = profile?.pushRemindersEnabled != false
        
        if (isEnabled) {
            PracticeReminderScheduler.scheduleDailyReminder(
                getApplication(),
                hour,
                minute,
                _userStreakCount.value
            )
            addLog("Scheduled daily practice reminder at ${getFormattedReminderTime(hour, minute)}")
        }
    }

    fun triggerTestReminder() {
        PracticeReminderScheduler.triggerInstantTestReminder(getApplication(), _userStreakCount.value)
        addLog("Test streak practice reminder notification triggered")
    }
    
    fun getFormattedReminderTime(hour: Int, minute: Int): String {
        val ampm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val displayMinute = String.format("%02d", minute)
        return "$displayHour:$displayMinute $ampm"
    }

    fun deleteAccountData(onCompleted: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllUserData()
            addLog("Account erased completely")
            repository.getOrCreateUserProfile() // Reinitialize default singleton profile
            onCompleted()
        }
    }

    fun sendMessageToCompanion(message: String) {
        if (message.isBlank()) return
        
        val userMsg = CompanionChatMessage(text = message, isUser = true)
        val currentHistory = _companionChat.value.toMutableList()
        currentHistory.add(userMsg)
        _companionChat.value = currentHistory
        
        _isCompanionTyping.value = true
        addLog("Sending cue: $message")

        viewModelScope.launch {
            val profile = userProfileState.value ?: repository.getOrCreateUserProfile()
            val resume = resumeState.value
            val resumeText = resume?.textContent
            
            val historyPairs = _companionChat.value.map { it.text to it.isUser }.take(10)

            val response = repository.getCompanionResponse(
                userMessage = message,
                history = historyPairs,
                profile = profile,
                resumeText = resumeText
            )
            
            val aiMsg = CompanionChatMessage(text = response, isUser = false)
            val updatedHistory = _companionChat.value.toMutableList()
            updatedHistory.add(aiMsg)
            _companionChat.value = updatedHistory
            _isCompanionTyping.value = false
            addLog("AI Companion replied safely")
        }
    }

    fun clearCompanionChat() {
        val personality = userProfileState.value?.aiPersonality ?: "Encouraging Coach"
        _companionChat.value = listOf(
            CompanionChatMessage(
                text = "Chat history cleared. I'm ready for another customized session as your $personality!",
                isUser = false
            )
        )
        addLog("Companion chat history reset")
    }

    fun addDiagnosticLogDirect(log: String) {
        addLog(log)
    }

    // --- Advanced File Upload & Knowledge System ViewModel Bindings ---

    fun uploadKnowledgeDoc(
        fileName: String,
        fileType: String,
        textContent: String,
        fileSize: Long,
        folderName: String = "General"
    ) {
        viewModelScope.launch {
            _isProcessingDoc.value = true
            addLog("Importing document: '$fileName' of type '$fileType'")
            try {
                val doc = KnowledgeDocumentEntity(
                    fileName = fileName,
                    fileType = fileType,
                    textContent = textContent,
                    fileSize = fileSize,
                    folderName = folderName,
                    summary = "Extracting details and summarizing via Gemini...",
                    isLocalOnly = true
                )
                val docId = repository.insertKnowledgeDoc(doc)
                addLog("Document imported in local db with ID: $docId. Initiating Gemini summarizer...")
                // background processing
                processDocumentAI(docId)
            } catch (e: Exception) {
                Log.e("InterviewViewModel", "Failed to upload document", e)
                addLog("Error uploading document: ${e.message}")
            } finally {
                _isProcessingDoc.value = false
            }
        }
    }

    fun processDocumentAI(docId: Long) {
        viewModelScope.launch {
            _isProcessingDoc.value = true
            addLog("Executing AI summary and key takeaway generation with Gemini for document $docId...")
            try {
                repository.processDocumentAI(docId)
                addLog("Gemini analysis completed for document $docId. Summary and insights generated successfully.")
            } catch (e: Exception) {
                Log.e("InterviewViewModel", "Error processing document with AI", e)
            } finally {
                _isProcessingDoc.value = false
            }
        }
    }

    fun deleteKnowledgeDoc(docId: Long) {
        viewModelScope.launch {
            addLog("Deleting document $docId and all its study associations")
            repository.deleteKnowledgeDoc(docId)
        }
    }

    fun toggleFavoriteDocument(docId: Long) {
        viewModelScope.launch {
            repository.toggleFavoriteDocument(docId)
        }
    }

    fun renameDocument(docId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameDocument(docId, newName)
            addLog("Document renamed to: '$newName'")
        }
    }

    fun generateFlashcards(docId: Long) {
        viewModelScope.launch {
            _isProcessingDoc.value = true
            addLog("Generating study flashcards with Gemini for doc $docId...")
            try {
                repository.generateFlashcardsAI(docId)
                addLog("Flashcards generated successfully!")
            } catch (e: Exception) {
                Log.e("InterviewViewModel", "Error generating flashcards", e)
            } finally {
                _isProcessingDoc.value = false
            }
        }
    }

    fun generateQuiz(docId: Long) {
        viewModelScope.launch {
            _isProcessingDoc.value = true
            addLog("Generating study quiz with Gemini for doc $docId...")
            try {
                repository.generateQuizQuestionsAI(docId)
                addLog("Multiple choice quiz generated successfully!")
            } catch (e: Exception) {
                Log.e("InterviewViewModel", "Error generating quiz", e)
            } finally {
                _isProcessingDoc.value = false
            }
        }
    }

    fun getFlashcardsForDoc(docId: Long): Flow<List<FlashcardEntity>> {
        return repository.getFlashcardsFlow(docId)
    }

    fun getQuizQuestionsForDoc(docId: Long): Flow<List<QuizQuestionEntity>> {
        return repository.getQuizQuestionsFlow(docId)
    }

    fun getDocumentChatsFlow(docId: Long): Flow<List<DocumentChatEntity>> {
        return repository.getDocumentChatsFlow(docId)
    }

    fun getAllDocumentChatsFlow(): Flow<List<DocumentChatEntity>> {
        return repository.getAllDocumentChatsFlow()
    }

    fun sendDocumentChatMessage(docId: Long, messageText: String) {
        viewModelScope.launch {
            if (messageText.isBlank()) return@launch
            // 1. Insert user message in database
            val userChat = DocumentChatEntity(
                documentId = docId,
                isUser = true,
                messageText = messageText
            )
            repository.insertDocumentChat(userChat)

            _isCompanionTyping.value = true
            addLog("Sending chat prompt to document context flow...")

            try {
                // 2. Fetch conversational context and history inside UI/repo
                // Obtain previous history from DB (say, collect top 10 messages)
                // For simplicity, repository.getDocChatResponse handles systemPrompt context extraction
                val answer = repository.getDocChatResponse(docId, messageText, emptyList(), allKnowledgeDocsState.value)
                
                // 3. Insert AI response
                val aiChat = DocumentChatEntity(
                    documentId = docId,
                    isUser = false,
                    messageText = answer
                )
                repository.insertDocumentChat(aiChat)
                addLog("Doc chat response processed.")
            } catch (e: Exception) {
                Log.e("InterviewViewModel", "Doc chat response failed", e)
                val aiChat = DocumentChatEntity(
                    documentId = docId,
                    isUser = false,
                    messageText = "AI Response error: ${e.message}"
                )
                repository.insertDocumentChat(aiChat)
            } finally {
                _isCompanionTyping.value = false
            }
        }
    }

    fun clearAllDocKnowledge() {
        viewModelScope.launch {
            repository.clearAllKnowledgeDocs()
            addLog("All imported knowledge documents cleared successfully.")
        }
    }
}

data class CompanionChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
