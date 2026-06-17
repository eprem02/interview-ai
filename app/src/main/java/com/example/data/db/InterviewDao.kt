package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InterviewDao {

    // --- Resume ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: ResumeEntity)

    @Query("SELECT * FROM resumes WHERE id = 1 LIMIT 1")
    fun getResumeFlow(): Flow<ResumeEntity?>

    @Query("SELECT * FROM resumes WHERE id = 1 LIMIT 1")
    suspend fun getResumeOnce(): ResumeEntity?

    @Query("DELETE FROM resumes WHERE id = 1")
    suspend fun deleteResume()

    // --- Sessions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: InterviewSessionEntity): Long

    @Update
    suspend fun updateSession(session: InterviewSessionEntity)

    @Query("SELECT * FROM interview_sessions ORDER BY dateLong DESC")
    fun getAllSessionsFlow(): Flow<List<InterviewSessionEntity>>

    @Query("SELECT * FROM interview_sessions WHERE completed = 1 ORDER BY dateLong DESC")
    fun getCompletedSessionsFlow(): Flow<List<InterviewSessionEntity>>

    @Query("SELECT * FROM interview_sessions WHERE id = :id LIMIT 1")
    fun getSessionFlowById(id: Long): Flow<InterviewSessionEntity?>

    @Query("SELECT * FROM interview_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionByIdOnce(id: Long): InterviewSessionEntity?

    @Query("DELETE FROM interview_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    // --- Questions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<InterviewQuestionAnswerEntity>)

    @Update
    suspend fun updateQuestion(question: InterviewQuestionAnswerEntity)

    @Query("SELECT * FROM interview_questions WHERE sessionId = :sessionId ORDER BY questionNumber ASC")
    fun getQuestionsForSessionFlow(sessionId: Long): Flow<List<InterviewQuestionAnswerEntity>>

    @Query("SELECT * FROM interview_questions WHERE sessionId = :sessionId ORDER BY questionNumber ASC")
    suspend fun getQuestionsForSessionOnce(sessionId: Long): List<InterviewQuestionAnswerEntity>

    // --- User Profile ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Query("DELETE FROM user_profiles WHERE id = 1")
    suspend fun deleteUserProfile()

    @Query("DELETE FROM resumes WHERE id = 1")
    suspend fun clearAllResumes()

    @Query("DELETE FROM interview_sessions")
    suspend fun clearAllSessions()

    @Query("DELETE FROM interview_questions")
    suspend fun clearAllQuestions()

    // --- Knowledge Base Documents ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgeDocument(doc: KnowledgeDocumentEntity): Long

    @Update
    suspend fun updateKnowledgeDocument(doc: KnowledgeDocumentEntity)

    @Query("SELECT * FROM knowledge_documents ORDER BY dateAdded DESC")
    fun getAllKnowledgeDocumentsFlow(): Flow<List<KnowledgeDocumentEntity>>

    @Query("SELECT * FROM knowledge_documents WHERE id = :id LIMIT 1")
    suspend fun getKnowledgeDocumentByIdOnce(id: Long): KnowledgeDocumentEntity?

    @Query("DELETE FROM knowledge_documents WHERE id = :id")
    suspend fun deleteKnowledgeDocumentById(id: Long)

    @Query("DELETE FROM knowledge_documents")
    suspend fun clearAllKnowledgeDocuments()

    // --- Flashcards ---
    @Query("SELECT * FROM flashcards WHERE documentId = :documentId")
    fun getFlashcardsForDocumentFlow(documentId: Long): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Query("DELETE FROM flashcards WHERE documentId = :documentId")
    suspend fun deleteFlashcardsForDocument(documentId: Long)

    // --- Quizzes ---
    @Query("SELECT * FROM quiz_questions WHERE documentId = :documentId")
    fun getQuizQuestionsForDocumentFlow(documentId: Long): Flow<List<QuizQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(quizQuestions: List<QuizQuestionEntity>)

    @Query("DELETE FROM quiz_questions WHERE documentId = :documentId")
    suspend fun deleteQuizQuestionsForDocument(documentId: Long)

    // --- Document Chat Logs ---
    @Query("SELECT * FROM document_chats ORDER BY timestamp ASC")
    fun getAllDocumentChatsFlow(): Flow<List<DocumentChatEntity>>

    @Query("SELECT * FROM document_chats WHERE documentId = :documentId ORDER BY timestamp ASC")
    fun getDocumentChatsFlow(documentId: Long): Flow<List<DocumentChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentChat(chat: DocumentChatEntity): Long

    @Query("DELETE FROM document_chats WHERE documentId = :documentId")
    suspend fun deleteChatsForDocument(documentId: Long)

    @Query("DELETE FROM document_chats")
    suspend fun clearAllDocumentChats()
}
