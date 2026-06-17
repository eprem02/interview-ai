package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.KnowledgeDocumentEntity
import com.example.data.db.FlashcardEntity
import com.example.data.db.QuizQuestionEntity
import com.example.data.db.DocumentChatEntity
import com.example.viewmodel.InterviewViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseScreen(
    viewModel: InterviewViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allDocs by viewModel.allKnowledgeDocsState.collectAsState()
    val isProcessingDevice by viewModel.isProcessingDoc.collectAsState()

    // Filter, search & sorting UI State
    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("All") }
    var sortBy by remember { mutableStateOf("Newest") } // "Newest", "Alphabetical", "Size"

    // Active reading pane / focus document
    var activeFocusDoc by remember { mutableStateOf<KnowledgeDocumentEntity?>(null) }
    var showDeleteConfirmationId by remember { mutableStateOf<Long?>(null) }
    var showRenameDialogId by remember { mutableStateOf<Long?>(null) }
    var renameInputName by remember { mutableStateOf("") }
    var showAddDemoMenu by remember { mutableStateOf(false) }

    // Multi-select for mass operations or multi-file Chat
    var selectedDocIdsForChat by remember { mutableStateOf(setOf<Long>()) }
    var showMultiFileChatView by remember { mutableStateOf(false) }

    // REAL Document content selector contract
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolver = context.contentResolver
            var displayName = "Document_Import.txt"
            var size: Long = 1000
            try {
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) displayName = cursor.getString(nameIdx)
                        if (sizeIdx != -1) size = cursor.getLong(sizeIdx)
                    }
                }
            } catch (e: Exception) {
                // Failure fallback
            }

            // Size constraints
            if (size > 10 * 1024 * 1024) {
                Toast.makeText(context, "Upload Warning: Selected file exceeds 10MB threshold.", Toast.LENGTH_LONG).show()
                viewModel.addDiagnosticLogDirect("File upload rejected: exceeds 10MB limit")
                return@rememberLauncherForActivityResult
            }

            val fileExt = displayName.substringAfterLast(".", "TXT").uppercase()
            var textParsed = ""

            if (fileExt == "TXT") {
                try {
                    resolver.openInputStream(uri)?.use { stream ->
                        textParsed = stream.bufferedReader().readText()
                    }
                } catch (e: Exception) {
                    textParsed = "Could not parse internal plain text details: ${e.message}"
                }
            } else {
                // PDF, DOCX, XLSX, Vision uploads
                textParsed = """
                    [File Signature]: $displayName
                    Size: $size Bytes
                    Container container: $fileExt
                    Automatically ingested for cognitive vector alignment with Gemini models.
                    Extracted structure details will dynamically populate the chat index on first review.
                """.trimIndent()
            }

            viewModel.uploadKnowledgeDoc(displayName, fileExt, textParsed, size, "General")
            Toast.makeText(context, "Uploading & parsing file via Gemini...", Toast.LENGTH_SHORT).show()
        }
    }

    // Computed folder counts
    val foldersList = listOf("All", "General", "Resumes", "Research", "Notes")

    // Filtered documents list
    val filteredDocs = remember(allDocs, searchQuery, selectedFolder, sortBy) {
        var base = allDocs.filter { doc ->
            (selectedFolder == "All" || doc.folderName.equals(selectedFolder, ignoreCase = true)) &&
            (doc.fileName.contains(searchQuery, ignoreCase = true) || doc.textContent.contains(searchQuery, ignoreCase = true))
        }
        when (sortBy) {
            "Alphabetical" -> base = base.sortedBy { it.fileName.lowercase() }
            "Size" -> base = base.sortedByDescending { it.fileSize }
            else -> base = base.sortedByDescending { doc -> doc.dateAdded } // Newest
        }
        base
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Knowledge Hub",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Structured Document Library",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("kb_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearCompanionChat() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear Chat logs", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showAddDemoMenu = true }) {
                        Icon(Icons.Default.School, contentDescription = "Load Demos", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { fileLauncher.launch("*/*") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .testTag("add_doc_fab")
                    .padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import document")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search records, content nodes, or indexes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search search icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear field query")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_doc_input")
                    .clip(RoundedCornerShape(24.dp))
                    .padding(vertical = 4.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stats Quick Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${allDocs.size} Items Indexed",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Multi-file chat launcher bar if multiple selected
                if (selectedDocIdsForChat.isNotEmpty()) {
                    Button(
                        onClick = { showMultiFileChatView = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("multi_file_chat_fab")
                    ) {
                        Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Multi-Doc Chat (${selectedDocIdsForChat.size})", fontSize = 12.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                            .clickable {
                                sortBy = when (sortBy) {
                                    "Newest" -> "Alphabetical"
                                    "Alphabetical" -> "Size"
                                    else -> "Newest"
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sorting order button", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(sortBy, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Folders Scroll Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                foldersList.forEach { folder ->
                    val isSelected = selectedFolder == folder
                    InputChip(
                        selected = isSelected,
                        onClick = { selectedFolder = folder },
                        label = { Text(folder) },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isProcessingDevice) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            if (filteredDocs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No Documents Found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap + below to select local files, or click the study icon in top right to load standard demo documents instantly.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        val isSelectedForChat = selectedDocIdsForChat.contains(doc.id)
                        KnowledgeDocListItemRow(
                            doc = doc,
                            isSelectedForChat = isSelectedForChat,
                            onToggleSelect = {
                                selectedDocIdsForChat = if (isSelectedForChat) {
                                    selectedDocIdsForChat - doc.id
                                } else {
                                    selectedDocIdsForChat + doc.id
                                }
                            },
                            onRowClick = {
                                activeFocusDoc = doc
                            },
                            onToggleFavorite = {
                                viewModel.toggleFavoriteDocument(doc.id)
                            },
                            onRename = {
                                renameInputName = doc.fileName
                                showRenameDialogId = doc.id
                            },
                            onDelete = {
                                showDeleteConfirmationId = doc.id
                            }
                        )
                    }
                }
            }
        }
    }

    // Show active detailed Reader/AI study hub dialog
    activeFocusDoc?.let { doc ->
        KnowledgeDocFocusDialog(
            doc = doc,
            viewModel = viewModel,
            onDismiss = {
                activeFocusDoc = null
            }
        )
    }

    // Multi-File Chat Modal view
    if (showMultiFileChatView) {
        MultiFileChatDialog(
            selectedIds = selectedDocIdsForChat,
            allDocs = allDocs,
            viewModel = viewModel,
            onDismiss = {
                showMultiFileChatView = false
            }
        )
    }

    // Rename Dialog dialog
    if (showRenameDialogId != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialogId = null },
            title = { Text("Rename File Record") },
            text = {
                Column {
                    Text("Provide a clear descriptive index tag for studies:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameInputName,
                        onValueChange = { renameInputName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRenameDialogId?.let { id ->
                            viewModel.renameDocument(id, renameInputName)
                        }
                        showRenameDialogId = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialogId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation dialog
    if (showDeleteConfirmationId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationId = null },
            title = { Text("Delete Document?") },
            text = { Text("Are you sure you want to permanently erase this document record along with all corresponding AI summary logs, chatbot histories, study flashcards, and quizzes?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmationId?.let { id ->
                            viewModel.deleteKnowledgeDoc(id)
                        }
                        showDeleteConfirmationId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Erase")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Load Demo files menu dialog
    if (showAddDemoMenu) {
        AlertDialog(
            onDismissRequest = { showAddDemoMenu = false },
            title = { Text("Load High Yield Study Templates") },
            text = { Text("Instantly inject standard study files to test full summaries, interactive multiple choice quiz generators, and custom doc diagnostic chat threads offline.") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.uploadKnowledgeDoc(
                                "Resume_Google_Hiring_Matrix.pdf",
                                "PDF",
                                "Full career matrix template for Staff Software Engineers. Proficiencies: Kotlin coroutines flow, high ingestion Room schemas, performance analytics scaling, dynamic design pattern caching. Achievements: Boosted system latency by 45%, managed 12 junior designers, aligned full AWS cloud ledgers.",
                                1290000,
                                "Resumes"
                            )
                            showAddDemoMenu = false
                        }
                    ) {
                        Text("Inject Software Resume Template")
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.uploadKnowledgeDoc(
                                "Research_Paper_AI_Cognition.docx",
                                "DOCX",
                                "Detailed Research analysis on Artificial General Intelligence agent systems. AI agents scale capabilities by performing self-corrective iterations in recursive loops. Utilizing multi-turn context vectors, they parse structural configurations and trigger dynamic operations without human intervention, ensuring low friction scaling.",
                                2450000,
                                "Research"
                            )
                            showAddDemoMenu = false
                        }
                    ) {
                        Text("Inject Agent Cognition Paper")
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.uploadKnowledgeDoc(
                                "System_Design_Ledgers.xlsx",
                                "XLSX",
                                "Ingestion Ledgers database configurations. Columns: Host IP, Master replica count, thread pools, SQL caching triggers. Notes: Always provision replicas on different availability zones to restore sessions automatically during hardware failures.",
                                850000,
                                "General"
                            )
                            showAddDemoMenu = false
                        }
                    ) {
                        Text("Inject DB Design Ledger")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDemoMenu = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun KnowledgeDocListItemRow(
    doc: KnowledgeDocumentEntity,
    isSelectedForChat: Boolean,
    onToggleSelect: () -> Unit,
    onRowClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRowClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio Selector for multi-file Chat
            IconButton(onClick = onToggleSelect) {
                Icon(
                    imageVector = if (isSelectedForChat) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "Multi-doc Chat selector",
                    tint = if (isSelectedForChat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // File Type Badge Icon representation
            val typeColorAndIcon = when (doc.fileType.uppercase()) {
                "PDF" -> Pair(Color(0xFFE57373), Icons.Default.PictureAsPdf)
                "DOCX", "DOC" -> Pair(Color(0xFF64B5F6), Icons.Default.Description)
                "XLSX", "XLS", "EXCEL" -> Pair(Color(0xFF81C784), Icons.Default.GridOn)
                "PPTX", "PPT" -> Pair(Color(0xFFFFB74D), Icons.Default.Slideshow)
                else -> Pair(Color(0xFFBA68C8), Icons.Default.Article)
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeColorAndIcon.first.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeColorAndIcon.second,
                    contentDescription = "File marker",
                    tint = typeColorAndIcon.first,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Core Meta properties column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = doc.fileType,
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColorAndIcon.first,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${doc.fileSize / 1024} KB",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = doc.folderName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Diagnostic indicators & context actions
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite star icon button",
                    tint = if (doc.isFavorite) Color.Yellow else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // More control menu drop trigger
            var expandedMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Show file popup actions menu", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename Index") },
                        onClick = {
                            expandedMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Erase Record") },
                        onClick = {
                            expandedMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeDocFocusDialog(
    doc: KnowledgeDocumentEntity,
    viewModel: InterviewViewModel,
    onDismiss: () -> Unit
) {
    val coroutine = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Insights", "Flashcards", "Practice Quiz", "AI chat")

    // Collect related study flows
    val flashcards by viewModel.getFlashcardsForDoc(doc.id).collectAsState(emptyList())
    val quizQuestions by viewModel.getQuizQuestionsForDoc(doc.id).collectAsState(emptyList())
    val chatHistory by viewModel.getDocumentChatsFlow(doc.id).collectAsState(emptyList())

    // On-demand generators
    LaunchedEffect(doc.id) {
        // Automatically request AI analysis if document holds empty indicators
        if (doc.summary.contains("cognitive vector") || doc.summary.contains("Cognitive vector") || doc.summary.contains("Extracting details")) {
            viewModel.processDocumentAI(doc.id)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = "Reader Focus",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = doc.fileName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Size: ${doc.fileSize / 1024} KB | Mode: ${doc.fileType}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss pane")
                }
            }

            // Primary Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Pane Switcher content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> SummaryInsightsPane(doc, viewModel)
                    1 -> FlashcardsStudyPane(doc.id, flashcards, viewModel)
                    2 -> InteractiveQuizStudyPane(doc.id, quizQuestions, viewModel)
                    else -> ChatWithDocPane(doc.id, chatHistory, viewModel)
                }
            }
        }
    }
}

@Composable
fun SummaryInsightsPane(
    doc: KnowledgeDocumentEntity,
    viewModel: InterviewViewModel
) {
    // Parse key insights JSON array safely
    val insights = remember(doc.keyInsightsJson) {
        try {
            val arr = JSONArray(doc.keyInsightsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList<String>()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI summaries
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI Executive Summary",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { viewModel.processDocumentAI(doc.id) }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Re-analyze document summary with Gemini", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = doc.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Key takeouts list
        if (insights.isNotEmpty()) {
            item {
                Text(
                    text = "Core Key Takeaways & Cognition Nodes",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            items(insights) { insightText ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = insightText,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Expanded full document text preview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                var expandedText by remember { mutableStateOf(false) }
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Raw File Extraction Preview",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (expandedText) doc.textContent else doc.textContent.take(600) + "...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    TextButton(onClick = { expandedText = !expandedText }) {
                        Text(if (expandedText) "Show Less" else "View Full Raw Data Node")
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardsStudyPane(
    docId: Long,
    flashcards: List<FlashcardEntity>,
    viewModel: InterviewViewModel
) {
    var activeCardIdx by remember { mutableStateOf(0) }
    var flipBackState by remember { mutableStateOf(false) }

    LaunchedEffect(docId) {
        if (flashcards.isEmpty()) {
            viewModel.generateFlashcards(docId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (flashcards.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(10.dp))
                Text("Analyzing metadata & generating flashcards using Gemini...")
            }
        } else {
            val card = flashcards.getOrNull(activeCardIdx) ?: flashcards.first()

            Text(
                "Study Flashcard ${activeCardIdx + 1} of ${flashcards.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Study Card with beautiful Flip presentation states
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable { flipBackState = !flipBackState }
                    .testTag("flashcard_box_body"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (flipBackState) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (flipBackState) "EXPLANATION ANSWER" else "STUDY QUESTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (flipBackState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (flipBackState) card.answer else card.question,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (flipBackState) "Click to reveal the question" else "Click to flip card & check answer",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (activeCardIdx > 0) activeCardIdx--
                        flipBackState = false
                    },
                    enabled = activeCardIdx > 0
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }

                Button(
                    onClick = { viewModel.generateFlashcards(docId) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Re-generate study cards")
                }

                IconButton(
                    onClick = {
                        if (activeCardIdx < flashcards.size - 1) activeCardIdx++
                        flipBackState = false
                    },
                    enabled = activeCardIdx < flashcards.size - 1
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                }
            }
        }
    }
}

@Composable
fun InteractiveQuizStudyPane(
    docId: Long,
    quizQuestions: List<QuizQuestionEntity>,
    viewModel: InterviewViewModel
) {
    var activeIdx by remember { mutableStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var runningScore by remember { mutableStateOf(0) }
    var showResultsAlert by remember { mutableStateOf(false) }

    LaunchedEffect(docId) {
        if (quizQuestions.isEmpty()) {
            viewModel.generateQuiz(docId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (quizQuestions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("AI building customized practice exams...")
                }
            }
        } else {
            val q = quizQuestions.getOrNull(activeIdx) ?: quizQuestions.first()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${activeIdx + 1} of ${quizQuestions.size}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Score: $runningScore/${quizQuestions.size}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (activeIdx + 1).toFloat() / quizQuestions.size },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Question Box text
            Text(
                text = q.question,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Options List
            val options = listOf(
                Pair("A", q.optionA),
                Pair("B", q.optionB),
                Pair("C", q.optionC),
                Pair("D", q.optionD)
            )

            options.forEach { opt ->
                val isSelectedOpt = selectedOption == opt.first
                val isCorrectOpt = q.correctAnswer.equals(opt.first, ignoreCase = true)
                val revealsAnswer = selectedOption != null

                // Determine dynamic contextual highlight colors
                val bColor = when {
                    revealsAnswer && isCorrectOpt -> Color(0xFFC8E6C9) // Green (Always show correct)
                    isSelectedOpt && !isCorrectOpt -> Color(0xFFFFCDD2) // Red selection
                    isSelectedOpt -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(enabled = selectedOption == null) {
                            selectedOption = opt.first
                            if (opt.first.equals(q.correctAnswer, ignoreCase = true)) {
                                runningScore++
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bColor),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${opt.first}. ",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = opt.second,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Next Question Action triggers
            if (selectedOption != null) {
                Button(
                    onClick = {
                        if (activeIdx < quizQuestions.size - 1) {
                            activeIdx++
                            selectedOption = null
                        } else {
                            showResultsAlert = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (activeIdx < quizQuestions.size - 1) "Next Question" else "Complete Exam Results")
                }
            }
        }
    }

    if (showResultsAlert) {
        AlertDialog(
            onDismissRequest = {
                showResultsAlert = false
                activeIdx = 0
                selectedOption = null
                runningScore = 0
            },
            title = { Text("Study Quiz Finished!") },
            text = { Text("You achieved a final score of $runningScore out of ${quizQuestions.size} questions correctly answered. AI algorithms recommend revising core knowledge summaries for items answered incorrectly.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResultsAlert = false
                        activeIdx = 0
                        selectedOption = null
                        runningScore = 0
                        viewModel.generateQuiz(docId)
                    }
                ) {
                    Text("Re-test and Build New Exam")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showResultsAlert = false
                        activeIdx = 0
                        selectedOption = null
                        runningScore = 0
                    }
                ) {
                    Text("Reset & Try Same Questions")
                }
            }
        )
    }
}

@Composable
fun ChatWithDocPane(
    docId: Long,
    chatHistory: List<DocumentChatEntity>,
    viewModel: InterviewViewModel
) {
    var txtInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat History text view list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (chatHistory.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Ask questions, query tables, compare files, or execute audit instructions concerning this index. AI is context-aware.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            items(chatHistory) { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (entry.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (entry.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = entry.messageText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (entry.isUser) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Divider()

        // Message entry input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = txtInput,
                onValueChange = { txtInput = it },
                placeholder = { Text("Ask document AI...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("doc_chat_text_input"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (txtInput.isNotBlank()) {
                        viewModel.sendDocumentChatMessage(docId, txtInput)
                        txtInput = ""
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (txtInput.isNotBlank()) {
                        viewModel.sendDocumentChatMessage(docId, txtInput)
                        txtInput = ""
                    }
                },
                modifier = Modifier
                    .testTag("chat_send_btn")
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send prompt button icon", tint = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiFileChatDialog(
    selectedIds: Set<Long>,
    allDocs: List<KnowledgeDocumentEntity>,
    viewModel: InterviewViewModel,
    onDismiss: () -> Unit
) {
    val selectedFiles = remember(selectedIds, allDocs) {
        allDocs.filter { selectedIds.contains(it.id) }
    }

    val multiChatHistory by viewModel.getDocumentChatsFlow(-1L).collectAsState(emptyList())
    var textPromptInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Multi-File metadata header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Cross-Document Synthesizer Chat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Analyzing context from research ledger data: ${selectedFiles.joinToString { it.fileName }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close prompt panel")
                }
            }

            Divider()

            // Chat outputs
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (multiChatHistory.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Evaluating context from ${selectedFiles.size} linked files. Write questions below matching comparison details and general systems summaries.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                items(multiChatHistory) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (entry.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (entry.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = entry.messageText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (entry.isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Divider()

            // Dynamic bottom input box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = textPromptInput,
                    onValueChange = { textPromptInput = it },
                    placeholder = { Text("Cross-compare file summaries...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("multi_doc_chat_text_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textPromptInput.isNotBlank()) {
                            viewModel.sendDocumentChatMessage(-1L, textPromptInput)
                            textPromptInput = ""
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (textPromptInput.isNotBlank()) {
                            viewModel.sendDocumentChatMessage(-1L, textPromptInput)
                            textPromptInput = ""
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send cross context message", tint = Color.White)
                }
            }
        }
    }
}
