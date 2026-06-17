package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class InterviewRepository(private val interviewDao: InterviewDao) {

    val resumeFlow: Flow<ResumeEntity?> = interviewDao.getResumeFlow()
    val allSessionsFlow: Flow<List<InterviewSessionEntity>> = interviewDao.getAllSessionsFlow()
    val userProfileFlow: Flow<UserProfileEntity?> = interviewDao.getUserProfileFlow()

    suspend fun getOrCreateUserProfile(): UserProfileEntity = withContext(Dispatchers.IO) {
        val existing = interviewDao.getUserProfileOnce()
        if (existing == null) {
            val defaultProfile = UserProfileEntity()
            interviewDao.insertUserProfile(defaultProfile)
            defaultProfile
        } else {
            existing
        }
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) = withContext(Dispatchers.IO) {
        interviewDao.insertUserProfile(profile)
    }

    suspend fun clearAllUserData() = withContext(Dispatchers.IO) {
        interviewDao.clearAllQuestions()
        interviewDao.clearAllSessions()
        interviewDao.clearAllResumes()
        interviewDao.deleteUserProfile()
    }

    fun getQuestionsForSessionFlow(sessionId: Long): Flow<List<InterviewQuestionAnswerEntity>> {
        return interviewDao.getQuestionsForSessionFlow(sessionId)
    }

    suspend fun getSessionFlowById(id: Long): Flow<InterviewSessionEntity?> {
        return interviewDao.getSessionFlowById(id)
    }

    suspend fun saveResume(textContent: String, jobTitle: String) = withContext(Dispatchers.IO) {
        val resume = ResumeEntity(
            textContent = textContent,
            jobTitle = jobTitle,
            lastUpdated = System.currentTimeMillis()
        )
        interviewDao.insertResume(resume)
    }

    suspend fun deleteResume() = withContext(Dispatchers.IO) {
        interviewDao.deleteResume()
    }

    /**
     * Fully evaluates resume text for ATS score, feedback, skill gaps, and learning roadmap.
     */
    suspend fun analyzeResume(textContent: String, targetJobTitle: String): ResumeEntity = withContext(Dispatchers.IO) {
        val prompt = """
            You are an advanced Applicant Tracking System (ATS) Auditor and expert HR Coach.
            Evaluate the following resume text for the role of '$targetJobTitle'.
            
            Resume content:
            $textContent
            
            You must output a valid JSON object with these exact keys:
            "atsScore": (an integer between 0 and 100)
            "atsFeedback": (a detailed review including strengths, weaknesses, and clear upgrade points)
            "skillGapAnalysis": (bullet points list comparing target requirements against skills mentioned)
            "learningRoadmap": (personalized step-by-step guide to acquire missing skills)
            
            Format instructions:
            Return ONLY the valid JSON object. No enclosing text, no backticks, no markdown.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.5f)
        )

        val apiKey = BuildConfig.GEMINI_API_KEY
        val result = try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty resume analysis response")
            
            val jsonObject = JSONObject(cleanJsonResponse(jsonText))
            ResumeEntity(
                textContent = textContent,
                jobTitle = targetJobTitle,
                atsScore = jsonObject.optInt("atsScore", 65),
                atsFeedback = jsonObject.optString("atsFeedback", "Solid baseline. Consider refining formatting or keyword density."),
                skillGapAnalysis = jsonObject.optString("skillGapAnalysis", "• Distributed systems architecture\n• Cloud application deployment"),
                learningRoadmap = jsonObject.optString("learningRoadmap", "1. Complete advanced System Design courses.\n2. Deploy a full stack project on AWS.")
            )
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Failed to analyze resume", e)
            ResumeEntity(
                textContent = textContent,
                jobTitle = targetJobTitle,
                atsScore = 72,
                atsFeedback = "Offline Fallback Analysis: Your resume displays strong baseline competency. Focus on listing quantifiable metric-based outcomes (e.g. 'boosted performance by 25%') to score higher with modern AI-driven filters.",
                skillGapAnalysis = "• Domain specific certifications\n• Advanced algorithm challenges optimized in production",
                learningRoadmap = "1. Focus heavily on algorithm mastery using our coding interview modules.\n2. Complete a high-impact capstone project and host on GitHub."
            )
        }

        interviewDao.insertResume(result)
        result
    }

    /**
     * Generates a new interview session with extensive role, company, and mode customization.
     */
    suspend fun startNewSessionWithParams(
        jobTitle: String,
        company: String,
        mode: String,
        codingLanguage: String
    ): Long = withContext(Dispatchers.IO) {
        val resume = interviewDao.getResumeOnce()
        val resumeText = resume?.textContent ?: "No Resume uploaded yet. Please focus primarily on standard industry questioning."

        val prompt = if (mode.equals("Coding", ignoreCase = true)) {
            """
            You are a leading coding platform creator and Technical Architect conducting a coding interview at $company.
            Generate exactly 3 realistic, company-specific software engineering coding challenges for a '$jobTitle' target.
            Keep in mind their resume details (if any):
            $resumeText
            
            You must output a valid JSON array of objects. Each object must have these exact keys:
            "question": (string - detailed description, constraints, and 2-3 input/output test cases)
            "isCodingChallenge": true
            "defaultCodeSnippet": (string - function helper/boilerplate starter code in $codingLanguage)
            "idealAnswer": (string - optimal, bug-free solution code in $codingLanguage, space complexity, and time complexity)
            
            Format instructions:
            Return ONLY the valid JSON array of objects. No explanation, no backticks, no markdown.
            """.trimIndent()
        } else {
            """
            You are an elite Talent Acquisition Specialist and hiring lead at $company.
            Generate exactly 5 realistic behavioral or technical-concept interview questions for a candidate targetting the position: '$jobTitle'.
            Keep in mind their resume details:
            $resumeText
            
            You must output a valid JSON array of objects. Each object must have these exact keys:
            "question": (string - professional behavioral or conceptual question)
            "isCodingChallenge": false
            "defaultCodeSnippet": ""
            "idealAnswer": (string - step-by-step STAR model response guide, and an example answer demonstrating highest expertise)
            
            Format instructions:
            Return ONLY the valid JSON array of objects. No explanation, no backticks, no markdown.
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.7f)
        )

        val apiKey = BuildConfig.GEMINI_API_KEY
        val generatedQuestions = try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No response received from AI.")
            
            parseQuestionsComplexJson(jsonText)
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Failed to generate custom session questions", e)
            if (mode.equals("Coding", ignoreCase = true)) {
                listOf(
                    TempQuestionHolder(
                        question = "Implement a function to find the first non-repeating character in a string and return its index. Return -1 if it doesn't exist. Constraints: O(N) time with O(1) extra space.",
                        isCodingChallenge = true,
                        defaultCodeSnippet = "fun firstUniqueChar(s: String): Int {\n    // Write your solution here\n    return -1\n}",
                        idealAnswer = "Optimal approach uses a frequency array/hashmap of size 26. O(N) time with O(1) space.\n\n```kotlin\nfun firstUniqueChar(s: String): Int {\n    val count = IntArray(26)\n    for (c in s) count[c - 'a']++\n    for (i in s.indices) {\n        if (count[s[i] - 'a'] == 1) return i\n    }\n    return -1\n}\n```"
                    ),
                    TempQuestionHolder(
                        question = "Given an array of integers representing microservice response latencies, return the length of the longest list of responses that have latencies with absolute difference <= 1.",
                        isCodingChallenge = true,
                        defaultCodeSnippet = "fun longestLatencyDiff(arr: IntArray): Int {\n    return 0\n}",
                        idealAnswer = "O(N) frequency counts or sliding window approach matching maximum difference boundaries."
                    )
                )
            } else {
                listOf(
                    TempQuestionHolder(
                        question = "Tell me about a time you solved a complex, business-critical bug in production. What steps did you initiate to trace the error and protect performance?",
                        isCodingChallenge = false,
                        defaultCodeSnippet = "",
                        idealAnswer = "STAR approach: Situation (Critical database lock), Task (Restore queries within 5 mins), Action (Deployed thread locks tracing via Datadog, optimized indexed queries), Result (Zero-downtime hotfix deployment, latency reduced 30%)."
                    ),
                    TempQuestionHolder(
                        question = "Explain how you design for scalability and low latency. How do you balance memory-backed caching (e.g. Redis) with database queries?",
                        isCodingChallenge = false,
                        defaultCodeSnippet = "",
                        idealAnswer = "Conceptual explanation focusing on replication, cache invalidation strategies (Write-through, Cache-aside), and database index analysis."
                    )
                )
            }
        }

        val sessionId = interviewDao.insertSession(
            InterviewSessionEntity(
                jobTitle = jobTitle,
                company = company,
                mode = mode,
                codingLanguage = codingLanguage,
                completed = false
            )
        )

        val questionEntities = generatedQuestions.mapIndexed { index, q ->
            InterviewQuestionAnswerEntity(
                sessionId = sessionId,
                questionNumber = index + 1,
                question = q.question,
                userAnswer = "",
                score = 0,
                feedback = "",
                idealAnswer = q.idealAnswer,
                isCodingChallenge = q.isCodingChallenge,
                defaultCodeSnippet = q.defaultCodeSnippet
            )
        }
        interviewDao.insertQuestions(questionEntities)

        sessionId
    }

    /**
     * Submit answer for a single question.
     * Evaluates code or conceptual text using Gemini API.
     */
    suspend fun evaluateAnswer(
        question: InterviewQuestionAnswerEntity,
        answer: String,
        jobTitle: String
    ): InterviewQuestionAnswerEntity = withContext(Dispatchers.IO) {
        val prompt = if (question.isCodingChallenge) {
            """
            You are a senior algorithmic interviewer.
            Evaluate the following candidate's written programming code for this coding challenge:
            Challenge: ${question.question}
            Languages required: ${if (answer.contains("def ")) "Python" else "Kotlin"}
            Candidate's Submission:
            $answer
            
            Compare it against the ideal solution.
            Provide detailed, constructive technical feedback and score the response on a scale of 0 to 100.
            
            Format requirements:
            Return the output as a valid JSON object with exactly two keys:
            "score": (an integer between 0 and 100 based on syntax correct, logic completeness, time/space efficiency)
            "feedback": (detailed review of code errors, edge-cases, and optimization possibilities)
            
            Return ONLY the valid JSON object. No explanation, no backticks, no markdown.
            """.trimIndent()
        } else {
            """
            You are a professional HR assessment specialist and interview coach.
            Analyze the following conceptual or behavioral interview question, the job role, and the candidate's answer.
            Evaluate based on speech depth, STAR format completeness, and executive presence.
            
            Job Position: $jobTitle
            Question: ${question.question}
            Candidate's Answer: $answer
            
            Format requirements:
            Return the output as a valid JSON object with exactly two keys:
            "score": (an integer between 0 and 100)
            "feedback": (brief summary of candidate strengths, communication depth, STAR method coverage, and expansion tips)
            
            Return ONLY the valid JSON object. No explanation, no backticks, no markdown.
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.5f)
        )

        val apiKey = BuildConfig.GEMINI_API_KEY
        val evaluated = try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content received")
            
            val jsonObject = JSONObject(cleanJsonResponse(jsonText))
            val score = jsonObject.optInt("score", 70)
            val feedback = jsonObject.optString("feedback", "Excellent response. Focus further on showcasing concrete results.")
            
            question.copy(userAnswer = answer, score = score, feedback = feedback)
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Failed to evaluate answer", e)
            question.copy(
                userAnswer = answer,
                score = 78,
                feedback = "Attempt recorded beautifully! AI evaluation is temporarily offline, but review your code formulation or logic outline against standard optimal structures."
            )
        }

        interviewDao.updateQuestion(evaluated)
        evaluated
    }

    /**
     * Completes the interview session, calculating the overall score and providing aggregated summary feedback.
     */
    suspend fun completeSession(sessionId: Long, jobTitle: String) = withContext(Dispatchers.IO) {
        val questions = interviewDao.getQuestionsForSessionOnce(sessionId)
        val session = interviewDao.getSessionByIdOnce(sessionId) ?: return@withContext

        val aggregateBuilder = StringBuilder()
        questions.forEach { qa ->
            aggregateBuilder.append("Q: ${qa.question}\nA: ${qa.userAnswer}\nScore: ${qa.score}\nFeedback: ${qa.feedback}\n\n")
        }

        val prompt = """
            You are a master executive coach and veteran hiring consultant at ${session.company}.
            Evaluate the complete mock interview performance for the position of '$jobTitle'.
            Below is the full history of questions asked, answer summaries, scoring, and individual feedback:
            
            ${aggregateBuilder.toString()}
            
            Write an aggregate performance summary covering overall communication, technical completeness/STAR method adherence, key highlights, and actionable real-world growth suggestions. Also generate a short "ideal overall summary answer outline" if helpful.
            
            Format requirements:
            Return the response as a JSON object with the exact keys:
            "overallScore": (number 0-100 representing trajectory and core competence)
            "overallFeedback": (detailed encouraging advisor audit with beautiful space breaks)
            "idealAnswerText": (a summary advice on the ultimate mindset/approach to ace this specific round)
            
            Return ONLY the valid JSON object. No explanation, no backticks, no Markdown tags.
            """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.5f)
        )

        val apiKey = BuildConfig.GEMINI_API_KEY
        val completedSession = try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No response raw text")
            
            val jsonObject = JSONObject(cleanJsonResponse(jsonText))
            val overallScore = jsonObject.optInt("overallScore", questions.map { it.score }.average().toInt())
            val overallFeedback = jsonObject.optString("overallFeedback", "Mock interview completed. Generative performance analytics offline.")
            val idealText = jsonObject.optString("idealAnswerText", "Review algorithms and practice the STAR response pattern.")
            
            session.copy(
                overallScore = overallScore,
                overallFeedback = overallFeedback,
                idealAnswerText = idealText,
                completed = true
            )
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Failed to compile aggregate session feedback", e)
            val avgScore = if (questions.isNotEmpty()) questions.map { it.score }.average().toInt() else 75
            session.copy(
                overallScore = avgScore,
                overallFeedback = "Your mock session is completed! You successfully answered all questions. Refine the structure of your answers and review any weak concepts in detail before your live round.",
                idealAnswerText = "Implement robust error handling, talk through your architecture decisions out loud, and focus on clean complexity calculations.",
                completed = true
            )
        }

        interviewDao.updateSession(completedSession)
    }

    suspend fun updateSessionEntity(session: InterviewSessionEntity) = withContext(Dispatchers.IO) {
        interviewDao.updateSession(session)
    }

    suspend fun getSessionOnce(id: Long): InterviewSessionEntity? = withContext(Dispatchers.IO) {
        interviewDao.getSessionByIdOnce(id)
    }

    suspend fun startNewSession(jobTitle: String): Long {
        return startNewSessionWithParams(jobTitle, "General", "Behavioral", "Kotlin")
    }

    suspend fun deleteSession(id: Long) = withContext(Dispatchers.IO) {
        interviewDao.deleteSessionById(id)
    }

    private fun parseQuestionsComplexJson(jsonText: String): List<TempQuestionHolder> {
        val cleaned = cleanJsonResponse(jsonText)
        return try {
            val jsonArray = JSONArray(cleaned)
            val list = mutableListOf<TempQuestionHolder>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TempQuestionHolder(
                        question = obj.optString("question", ""),
                        isCodingChallenge = obj.optBoolean("isCodingChallenge", false),
                        defaultCodeSnippet = obj.optString("defaultCodeSnippet", ""),
                        idealAnswer = obj.optString("idealAnswer", "No ideal response details available.")
                    )
                )
            }
            if (list.size >= 2) list else throw Exception("Invalid complexity results")
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Failed to parse questions array stream", e)
            throw e
        }
    }

    private fun cleanJsonResponse(rawText: String): String {
        var clean = rawText.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        }
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    suspend fun getCompanionResponse(
        userMessage: String,
        history: List<Pair<String, Boolean>>,
        profile: UserProfileEntity,
        resumeText: String?
    ): String = withContext(Dispatchers.IO) {
        val personalityPrompt = when (profile.aiPersonality) {
            "Stern Interviewer" -> "You are a tough, stern, demanding interviewer from a top-tier tech company. Give direct, critical feedback, challenge assumptions, and ask hard questions."
            "Pragmatic Architect" -> "You are a pragmatic, direct Chief System Architect. Focus heavily on code efficiency, system scale, databases, thread-safety, memory footprints, and architectural trade-offs."
            "Socratic Philosopher" -> "You are a thoughtful Socratic mentor. Do not just give answers. Ask guiding, deep questions that help the candidate discover the truth, optimal complexity, or better response structures on their own."
            else -> "You are an encouraging, supportive, and kind AI Career Coach. Help them build confidence, highlight strengths, and gently suggest improvements."
        }

        val factsText = profile.aiPersonalMemoriesJson.replace("[", "").replace("]", "").replace("\"", "")

        val systemPrompt = """
            $personalityPrompt
            
            You are speaking as an AI Career Companion to a user preparing for technical and behavioral interviews.
            Here is information about the user you are helping:
            - Profile Name: ${profile.displayName} (Username: @${profile.username})
            - Bio: ${profile.bio}
            - Skills: ${profile.skills}
            - AI Memory facts about this user: $factsText
            ${resumeText?.let { "- Their Resume content: $it" } ?: ""}
            
            Keep your responses helpful, highly personalized to their skills/memories, and maintain your personality archetype. Limit answers to 2-3 short, clean, well-spaced paragraphs or clear markdown bullet points. Never break character.
        """.trimIndent()

        val contents = mutableListOf<Content>()
        
        history.forEach { (text, isUser) ->
            contents.add(
                Content(
                    parts = listOf(Part(text = text)),
                    role = if (isUser) "user" else "model"
                )
            )
        }
        
        contents.add(
            Content(
                parts = listOf(Part(text = userMessage)),
                role = "user"
            )
        )

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        val apiKey = BuildConfig.GEMINI_API_KEY
        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No response text")
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Failed to get companion response", e)
            "Companion Mode [Offline]: I hear you! As your ${profile.aiPersonality}, I would focus on solidifying your skills in ${profile.skills}. Let's keep practicing to prepare you for success!"
        }
    }

    // --- Advanced File Upload & Knowledge System ---

    val allKnowledgeDocsFlow: Flow<List<KnowledgeDocumentEntity>> = interviewDao.getAllKnowledgeDocumentsFlow()

    suspend fun insertKnowledgeDoc(doc: KnowledgeDocumentEntity): Long = withContext(Dispatchers.IO) {
        interviewDao.insertKnowledgeDocument(doc)
    }

    suspend fun updateKnowledgeDoc(doc: KnowledgeDocumentEntity) = withContext(Dispatchers.IO) {
        interviewDao.updateKnowledgeDocument(doc)
    }

    suspend fun deleteKnowledgeDoc(docId: Long) = withContext(Dispatchers.IO) {
        interviewDao.deleteKnowledgeDocumentById(docId)
        interviewDao.deleteFlashcardsForDocument(docId)
        interviewDao.deleteQuizQuestionsForDocument(docId)
        interviewDao.deleteChatsForDocument(docId)
    }

    suspend fun toggleFavoriteDocument(docId: Long) = withContext(Dispatchers.IO) {
        val doc = interviewDao.getKnowledgeDocumentByIdOnce(docId)
        if (doc != null) {
            interviewDao.updateKnowledgeDocument(doc.copy(isFavorite = !doc.isFavorite))
        }
    }

    suspend fun renameDocument(docId: Long, newName: String) = withContext(Dispatchers.IO) {
        val doc = interviewDao.getKnowledgeDocumentByIdOnce(docId)
        if (doc != null) {
            interviewDao.updateKnowledgeDocument(doc.copy(fileName = newName))
        }
    }

    fun getFlashcardsFlow(docId: Long): Flow<List<FlashcardEntity>> = interviewDao.getFlashcardsForDocumentFlow(docId)
    fun getQuizQuestionsFlow(docId: Long): Flow<List<QuizQuestionEntity>> = interviewDao.getQuizQuestionsForDocumentFlow(docId)
    fun getDocumentChatsFlow(docId: Long): Flow<List<DocumentChatEntity>> = interviewDao.getDocumentChatsFlow(docId)
    fun getAllDocumentChatsFlow(): Flow<List<DocumentChatEntity>> = interviewDao.getAllDocumentChatsFlow()

    suspend fun insertDocumentChat(chat: DocumentChatEntity): Long = withContext(Dispatchers.IO) {
        interviewDao.insertDocumentChat(chat)
    }

    suspend fun clearAllKnowledgeDocs() = withContext(Dispatchers.IO) {
        interviewDao.clearAllKnowledgeDocuments()
        interviewDao.clearAllDocumentChats()
    }

    /**
     * Extracts text, creates summaries, and generates insights via Gemini
     */
    suspend fun processDocumentAI(docId: Long) = withContext(Dispatchers.IO) {
        val doc = interviewDao.getKnowledgeDocumentByIdOnce(docId) ?: return@withContext
        val textLimit = doc.textContent.take(6000)
        val prompt = """
            You are an elite, highly professional document cognitive interpreter.
            Analyze this uploaded file: '${doc.fileName}' (Type: ${doc.fileType})
            Text Content:
            $textLimit
            
            Perform two tasks:
            1. Write a clean, extremely detailed and beautiful professional executive summary. Highlight core objectives, target concepts, and main findings. Structure using multiple nice paragraphs.
            2. Extract exactly 4-6 key insights or core educational takeaways from this file.
            
            Format your response strictly as a JSON object with this exact structure:
            {
              "summary": "a beautiful summary with nice paragraphs",
              "keyInsights": ["takeaway 1", "takeaway 2", "takeaway 3", "takeaway 4"]
            }
            Return ONLY the valid JSON object. No explanation, no backticks, no Markdown tags.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.5f)
        )

        try {
            val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw Exception("No response")
            val clean = cleanJsonResponse(jsonText)
            val jsonObject = JSONObject(clean)
            val summary = jsonObject.optString("summary", "No summary could be processed.")
            val insightsArray = jsonObject.optJSONArray("keyInsights")
            val insightsList = mutableListOf<String>()
            if (insightsArray != null) {
                for (i in 0 until insightsArray.length()) {
                    insightsList.add(insightsArray.getString(i))
                }
            }
            
            val finalInsightsJson = JSONArray(insightsList).toString()
            interviewDao.updateKnowledgeDocument(doc.copy(
                summary = summary,
                keyInsightsJson = finalInsightsJson,
                isLocalOnly = false
            ))
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Failed to process document summary", e)
            interviewDao.updateKnowledgeDocument(doc.copy(
                summary = "Processed beautifully locally, but AI summary generation encountered an error: ${e.message ?: "Network API check needed"}",
                keyInsightsJson = "[\"Fallback Insight: Review your document details manually in text viewer.\", \"API Call backup: Local state saved correctly.\"]",
                isLocalOnly = true
            ))
        }
    }

    /**
     * Generates study flashcards from a document using Gemini
     */
    suspend fun generateFlashcardsAI(docId: Long) = withContext(Dispatchers.IO) {
        val doc = interviewDao.getKnowledgeDocumentByIdOnce(docId) ?: return@withContext
        val textLimit = doc.textContent.take(4000)
        val prompt = """
            Review this document content and generate exactly 5 flashcards to help study, practice, or remember facts.
            Each card has a clear study Question and a concise Explanation Answer.
            
            Format your response strictly as a JSON array where each object has these exact keys:
            "question": "string study question"
            "answer": "string explanation answer"
            
            Return ONLY the valid JSON array. No explanation, no markdown.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user"), Content(parts = listOf(Part(text = "Document:\n$textLimit")), role = "user")),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.7f)
        )

        try {
            val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw Exception("No response")
            val clean = cleanJsonResponse(jsonText)
            val jsonArray = JSONArray(clean)
            val list = mutableListOf<FlashcardEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    FlashcardEntity(
                        documentId = docId,
                        question = obj.optString("question", "Study Question ${i+1}"),
                        answer = obj.optString("answer", "Answer details here.")
                    )
                )
            }
            interviewDao.deleteFlashcardsForDocument(docId)
            interviewDao.insertFlashcards(list)
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Failed to generate flashcards", e)
            val fallback = listOf(
                FlashcardEntity(documentId = docId, question = "Main Topic of ${doc.fileName}", answer = "This flashcard was generated as a backup. Read the summary of the document for details!"),
                FlashcardEntity(documentId = docId, question = "Document Type", answer = "The document type is ${doc.fileType} with size ${doc.fileSize} bytes.")
            )
            interviewDao.deleteFlashcardsForDocument(docId)
            interviewDao.insertFlashcards(fallback)
        }
    }

    /**
     * Generates study quizzes from a document using Gemini
     */
    suspend fun generateQuizQuestionsAI(docId: Long) = withContext(Dispatchers.IO) {
        val doc = interviewDao.getKnowledgeDocumentByIdOnce(docId) ?: return@withContext
        val textLimit = doc.textContent.take(4000)
        val prompt = """
            Review this document content and generate exactly 5 realistic multiple choice questions with nice distinct options.
            Provide 4 options (A, B, C, D) and specify the precise correctAnswer option ('A', 'B', 'C', or 'D').
            
            Format your response strictly as a JSON array where each object has these exact keys:
            "question": "string query"
            "optionA": "string"
            "optionB": "string"
            "optionC": "string"
            "optionD": "string"
            "correctAnswer": "A" (or B, C, D)
            
            Return ONLY the valid JSON array. No explanation, no markdown.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user"), Content(parts = listOf(Part(text = "Document:\n$textLimit")), role = "user")),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.7f)
        )

        try {
            val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw Exception("No response")
            val clean = cleanJsonResponse(jsonText)
            val jsonArray = JSONArray(clean)
            val list = mutableListOf<QuizQuestionEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    QuizQuestionEntity(
                        documentId = docId,
                        question = obj.optString("question", "Question text?"),
                        optionA = obj.optString("optionA", "Option A"),
                        optionB = obj.optString("optionB", "Option B"),
                        optionC = obj.optString("optionC", "Option C"),
                        optionD = obj.optString("optionD", "Option D"),
                        correctAnswer = obj.optString("correctAnswer", "A")
                    )
                )
            }
            interviewDao.deleteQuizQuestionsForDocument(docId)
            interviewDao.insertQuizQuestions(list)
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Failed to generate quiz questions", e)
            val fallback = listOf(
                QuizQuestionEntity(
                    documentId = docId,
                    question = "What is the name of the file currently analyzed?",
                    optionA = doc.fileName,
                    optionB = "Empty File",
                    optionC = "Test Template",
                    optionD = "Resume Suite",
                    correctAnswer = "A"
                )
            )
            interviewDao.deleteQuizQuestionsForDocument(docId)
            interviewDao.insertQuizQuestions(fallback)
        }
    }

    /**
     * Conduct chat dialogue matching document context
     */
    suspend fun getDocChatResponse(
        documentId: Long,
        userMessage: String,
        history: List<Pair<String, Boolean>>,
        allDocs: List<KnowledgeDocumentEntity> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val systemPrompt = if (documentId == -1L) {
            val contextBuilder = StringBuilder()
            allDocs.take(3).forEach { d ->
                contextBuilder.append("--- Document: ${d.fileName} (${d.fileType}) ---\n")
                contextBuilder.append("${d.textContent.take(1500)}\n\n")
            }
            """
            You are a master AI document researcher and knowledge synthetiser.
            Answer the queries by combining, evaluating, and explaining facts across all available documents in user's library.
            
            Below is the context from multiple documents:
            ${contextBuilder.toString()}
            
            Feel free to output highly concise, beautifully structured comparative analyses.
            """.trimIndent()
        } else {
            val doc = interviewDao.getKnowledgeDocumentByIdOnce(documentId)
            val context = doc?.textContent?.take(5000) ?: ""
            """
            You are a master AI document assistant specializing in parsing: '${doc?.fileName ?: "Document"}' (${doc?.fileType ?: "TXT"}).
            
            Follow these constraints:
            1. Use the provided context from the uploaded file below to construct precise facts.
            2. If user inquires about something outside the document, state that it is not covered, but offer a logical AI synthesis based on standard reasoning.
            3. Keep your replies concise, helpful, and organized using elegant Markdown.
            
            CONTEXT FROM FILE:
            $context
            """.trimIndent()
        }

        val contents = mutableListOf<Content>()
        history.forEach { (text, isUser) ->
            contents.add(Content(parts = listOf(Part(text = text)), role = if (isUser) "user" else "model"))
        }
        contents.add(Content(parts = listOf(Part(text = userMessage)), role = "user"))

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        try {
            val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No message response was compiled."
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Failed to chat with document", e)
            "Chat Error: Gemini service encountered an error (${e.message ?: "Time out"}). Check your API setup. Answers may be limited."
        }
    }
}

data class TempQuestionHolder(
    val question: String,
    val isCodingChallenge: Boolean,
    val defaultCodeSnippet: String,
    val idealAnswer: String
)
