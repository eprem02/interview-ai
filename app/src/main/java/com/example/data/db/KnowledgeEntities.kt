package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_documents")
data class KnowledgeDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val fileType: String, // "PDF", "DOCX", "TXT", "XLSX", "PPTX", "PNG", "JPG", "WEBP"
    val fileSize: Long,   // in bytes
    val textContent: String,
    val summary: String = "",
    val keyInsightsJson: String = "[]", // JSON Array of strings
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val folderName: String = "General",
    val version: Int = 1,
    val isLocalOnly: Boolean = true
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val question: String,
    val answer: String
)

@Entity(tableName = "quiz_questions")
data class QuizQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String // "A", "B", "C", or "D"
)

@Entity(tableName = "document_chats")
data class DocumentChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long, // -1 means global cross-document multi-file chat
    val isUser: Boolean,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis()
)
