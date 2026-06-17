package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewHistoryScreen(
    viewModel: InterviewViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSessionDetails: (Long) -> Unit
) {
    val sessions by viewModel.allSessionsState.collectAsState()

    // Filter, search & sorting UI State
    var searchQuery by remember { mutableStateOf("") }
    var selectedModeFilter by remember { mutableStateOf("All") } // "All", "Behavioral", "Coding"
    var selectedCompanyFilter by remember { mutableStateOf("All") } // "All", "Google", "Amazon", "Microsoft", "TCS", "Infosys"
    var sortBy by remember { mutableStateOf("Date Newest") } // "Date Newest", "Date Oldest", "Score Highest", "Score Lowest"

    var showDeleteConfirmId by remember { mutableStateOf<Long?>(null) }

    // Helpers to compute statistics
    val completedSessions = sessions.filter { it.completed }
    val totalFinished = completedSessions.size
    val averageScore = if (totalFinished > 0) {
        completedSessions.map { it.overallScore }.average().toInt()
    } else 0

    val highestScore = if (totalFinished > 0) {
        completedSessions.maxOf { it.overallScore }
    } else 0

    val activeCount = sessions.count { !it.completed }

    // List of dynamic preset companies present in the session history or general list
    val companyPresets = listOf("All", "Google", "Amazon", "Microsoft", "TCS", "Infosys")

    // Filtered and Sorted Sessions
    val filteredSessions = remember(sessions, searchQuery, selectedModeFilter, selectedCompanyFilter, sortBy) {
        var base = sessions.filter { session ->
            val matchQuery = session.jobTitle.contains(searchQuery, ignoreCase = true) ||
                    session.company.contains(searchQuery, ignoreCase = true) ||
                    session.overallFeedback.contains(searchQuery, ignoreCase = true)
            
            val matchMode = selectedModeFilter == "All" || session.mode.equals(selectedModeFilter, ignoreCase = true)
            val matchCompany = selectedCompanyFilter == "All" || session.company.equals(selectedCompanyFilter, ignoreCase = true)

            matchQuery && matchMode && matchCompany
        }

        base = when (sortBy) {
            "Date Oldest" -> base.sortedBy { it.dateLong }
            "Score Highest" -> base.sortedWith(compareByDescending<InterviewSessionEntity> { it.completed }.thenByDescending { it.overallScore })
            "Score Lowest" -> base.sortedWith(compareByDescending<InterviewSessionEntity> { it.completed }.thenBy { it.overallScore })
            else -> base.sortedByDescending { it.dateLong } // "Date Newest"
        }
        base
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Interview History Archive",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Revisit past performances & metrics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("history_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Aggregate Executive Metrics Header Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EXECUTIVE METRICS",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Stat 1: Total Mock Rounds
                        WidgetStatItem(
                            label = "Total Rounds",
                            value = "${sessions.size}",
                            subText = "$totalFinished Finished",
                            icon = Icons.Default.School,
                            modifier = Modifier.weight(1f)
                        )
                        // Vertical dividing line
                        Box(modifier = Modifier.width(1.dp).height(48.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
                        // Stat 2: Avg Score
                        WidgetStatItem(
                            label = "Average Score",
                            value = if (totalFinished > 0) "$averageScore%" else "--",
                            subText = if (totalFinished > 0) "Completed" else "No Data Yet",
                            icon = Icons.Default.Assessment,
                            color = getScoreColor(averageScore),
                            modifier = Modifier.weight(1f)
                        )
                        // Vertical dividing line
                        Box(modifier = Modifier.width(1.dp).height(48.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)).align(Alignment.CenterVertically))
                        // Stat 3: Highest Run
                        WidgetStatItem(
                            label = "Highest Rating",
                            value = if (totalFinished > 0) "$highestScore" else "--",
                            subText = if (activeCount > 0) "$activeCount In Progress" else "Archive Safe",
                            icon = Icons.Default.Star,
                            color = getScoreColor(highestScore),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Search Bar & Filters Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Search Input
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search title, specs, or diagnostic logs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search query")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("search_history_input"),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sorting Selectors Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter and Sort Sessions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Sort Button Trigger
                    var expandedSort by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .clickable { expandedSort = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort direction indicator", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(sortBy, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = expandedSort,
                            onDismissRequest = { expandedSort = false }
                        ) {
                            listOf("Date Newest", "Date Oldest", "Score Highest", "Score Lowest").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        sortBy = option
                                        expandedSort = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Company preset scroll row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Company:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Google", "Amazon", "Microsoft").forEach { comp ->
                            val isSelected = selectedCompanyFilter == comp
                            InputChip(
                                selected = isSelected,
                                onClick = { selectedCompanyFilter = comp },
                                label = { Text(comp, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Mode presets row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Format:   ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Behavioral", "Coding").forEach { mode ->
                            val isSelected = selectedModeFilter == mode
                            InputChip(
                                selected = isSelected,
                                onClick = { selectedModeFilter = mode },
                                label = { Text(mode, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Archive List
            if (filteredSessions.isEmpty()) {
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
                            imageVector = Icons.Default.History,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (sessions.isEmpty()) "Your Mock Archive is Empty" else "No matching sessions found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (sessions.isEmpty()) "Prepare your profile skills or resume first, then start mock sessions to generate comprehensive feedback streams."
                            else "Adjust your filter criteria or search keyword to retrieve previous documents from history log.",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp)
                ) {
                    items(filteredSessions, key = { it.id }) { session ->
                        HistoryArchiveCard(
                            session = session,
                            onClickRevisit = { onNavigateToSessionDetails(session.id) },
                            onDelete = { showDeleteConfirmId = session.id }
                        )
                    }
                }
            }
        }
    }

    // Delete session confirmation dialog
    if (showDeleteConfirmId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmId = null },
            title = { Text("Erase Performance Record") },
            text = { Text("Are you sure you want to permanently erase this session evaluation from your archives, including ratings, comments, and simulated analytics reports?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmId?.let { id ->
                            viewModel.deleteSession(id)
                        }
                        showDeleteConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WidgetStatItem(
    label: String,
    value: String,
    subText: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = subText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

@Composable
fun HistoryArchiveCard(
    session: InterviewSessionEntity,
    onClickRevisit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault()) }
    val formattedDate = formatter.format(Date(session.dateLong))

    // Accent line on the left side indicating ranking levels
    val stripeColor = when {
        !session.completed -> MaterialTheme.colorScheme.primary // Blue / In progress
        session.overallScore >= 85 -> Color(0xFF4CAF50) // Premium green
        session.overallScore >= 70 -> Color(0xFFFF9800) // Orange Warning
        else -> Color(0xFFF44336) // Red support
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_archive_session_card")
            .clickable { onClickRevisit() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, stripes(stripeColor)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Highlighting side bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically)
                    .background(stripeColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Formatting block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = session.company.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = session.mode.uppercase() + if (session.mode.equals("Coding", ignoreCase = true)) " (${session.codingLanguage})" else "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Delete button icon
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp).testTag("delete_archive_session_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Session",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Job Position title name
                Text(
                    text = session.jobTitle,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Date conducted
                Text(
                    text = "Conducted: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Short Excerpt of feedback
                if (session.completed) {
                    Text(
                        text = if (session.overallFeedback.isNotBlank()) session.overallFeedback else "No custom feedback comments was saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                            .padding(8.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Pending, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Interactive session currently active or interrupted. Tap Revisit to complete.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Divider and Revisit CTA link bottom row
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rating score if finished
                    if (session.completed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Overall Score: ",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${session.overallScore}/100",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = stripeColor
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("IN PROGRESS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Interactive link button CTA
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onClickRevisit() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (session.completed) "Revisit Feedback & Metrics" else "Resume Session",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Navigate detail feedback link button",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helpers for left stripe accent card border representation 
fun stripes(stripeColor: Color): Brush {
    return Brush.horizontalGradient(listOf(stripeColor, stripeColor))
}

private fun getScoreColor(score: Int): Color {
    return when {
        score >= 85 -> Color(0xFF4CAF50)
        score >= 70 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}
