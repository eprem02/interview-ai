package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resumes")
data class ResumeEntity(
    @PrimaryKey val id: Int = 1,
    val textContent: String,
    val jobTitle: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val atsScore: Int = 0,
    val atsFeedback: String = "",
    val skillGapAnalysis: String = "",
    val learningRoadmap: String = ""
)

@Entity(tableName = "interview_sessions")
data class InterviewSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobTitle: String,
    val dateLong: Long = System.currentTimeMillis(),
    val overallScore: Int = 0,
    val completed: Boolean = false,
    val overallFeedback: String = "",
    val company: String = "General", // "Google", "Amazon", "Microsoft", "TCS", "Infosys", etc.
    val mode: String = "Behavioral",  // "Behavioral", "Coding"
    val codingLanguage: String = "Kotlin",
    val idealAnswerText: String = "",
    val emailReportSent: Boolean = false,
    val reportDownloaded: Boolean = false
)

@Entity(tableName = "interview_questions")
data class InterviewQuestionAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val questionNumber: Int,
    val question: String,
    val userAnswer: String = "",
    val score: Int = 0,
    val feedback: String = "",
    val idealAnswer: String = "", // AI-suggested code/answer text
    val isCodingChallenge: Boolean = false,
    val defaultCodeSnippet: String = "",
    val testCases: String = "" // Optional test state
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "candidate_ai",
    val displayName: String = "Candidate Pro",
    val fullName: String = "Candidate Pro",
    val profilePictureUri: String? = null,
    val avatarId: String = "avatar_1",
    val coverGradient: String = "gradient_cosmic",
    val bio: String = "Software Craftsman passionate about scalable services, Kotlin/Compose, and system architecture.",
    val skills: String = "[\"Kotlin\", \"Jetpack Compose\", \"Coroutines\", \"Room DB\", \"MVVM Architecture\"]",
    val educationJson: String = "[\n  {\"school\": \"Stanford University\", \"degree\": \"M.S. in Computer Science\", \"year\": \"2023\"},\n  {\"school\": \"IIT Madras\", \"degree\": \"B.Tech in Computer Science\", \"year\": \"2021\"}\n]",
    val workExperienceJson: String = "[\n  {\"company\": \"AITech Labs\", \"role\": \"Android Developer Specialist\", \"duration\": \"1.5 years\", \"highlights\": \"Engineered performance boosts of 40% with reactive flows.\"}, \n  {\"company\": \"ByteCloud Inc.\", \"role\": \"Software Engineering Intern\", \"duration\": \"6 months\", \"highlights\": \"Optimized database ingestion scaling to 1M events daily.\"}\n]",
    val certificationsJson: String = "[\n  {\"name\": \"Google Associate Android Developer\", \"issuer\": \"Google\", \"date\": \"June 2024\"},\n  {\"name\": \"AWS Certified Solutions Architect\", \"issuer\": \"Amazon\", \"date\": \"January 2025\"}\n]",
    val portfolioLinksJson: String = "[\n  {\"title\": \"Personal Website Portfolio\", \"url\": \"https://example.com\"},\n  {\"title\": \"Interactive Sandbox Playground\", \"url\": \"https://github.com/my-sandbox\"}\n]",
    val githubUsername: String = "eprem-dev",
    val linkedinHandle: String = "linkedin.com/in/candidate-pro",
    val personalGoalsJson: String = "[\n  {\"goal\": \"Incorporate 1 behavioral prep round every day\", \"isCompleted\": true},\n  {\"goal\": \"Design and implement Room migration protocols\", \"isCompleted\": false},\n  {\"goal\": \"Practice sliding window algorithmic challenges\", \"isCompleted\": false}\n]",
    val learningLayoutLevel: String = "Senior Engineer", // Intern, Junior, Senior, Staff
    val targetPace: String = "Intensive Pacing", // Relaxed vs Intensive
    val aiPersonality: String = "Encouraging Coach", // Stern Interviewer, Kind Coach, Pragmatic Dev, Socratic Philosopher
    val languagePref: String = "English", // English, Spanish, Spanish-ES, French, German, Japanese, Hindi
    val themePref: String = "Cosmic Slate",
    val profileVisible: Boolean = true,
    val aiMemoryRetention: Boolean = true,
    val crashDiagnosticsSync: Boolean = true,
    val accountEmail: String = "eprem1737@gmail.com",
    val accountTier: String = "AI Studio Prime Companion",
    val country: String = "United States",
    val pushRemindersEnabled: Boolean = true,
    val performanceEmailReports: Boolean = true,
    val badgeAlertsEnabled: Boolean = true,
    val aiPersonalMemoriesJson: String = "[\n  \"Currently targets high-complexity system design protocols\",\n  \"Requires clear, step-by-step STAR method explanations\",\n  \"Proficient in modern Kotlin streams and coroutine flows\"\n]",
    val securityPinEnabled: Boolean = false,
    val loginHistoryJson: String = "[\n  {\"device\": \"Streaming Web Emulator (Chromium 126)\", \"location\": \"San Francisco, CA\", \"timestamp\": \"Just now\"},\n  {\"device\": \"Android Mobile Client (Pixel 8 Pro)\", \"location\": \"Seattle, WA\", \"timestamp\": \"8 hours ago\"},\n  {\"device\": \"Desktop IDE Terminal Sync\", \"location\": \"Tokyo, Japan\", \"timestamp\": \"2 days ago\"}\n]",
    val connectedAccountsJson: String = "{\"google\": true, \"github\": true, \"linkedin\": false, \"stackoverflow\": true}"
)
