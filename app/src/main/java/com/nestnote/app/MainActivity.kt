package com.nestnote.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Violet = Color(0xFF5B50F6)
private val Ink = Color(0xFF24263B)
private val Muted = Color(0xFF858BA1)
private val Paper = Color(0xFFF7F8FC)
private val accents = listOf(Color(0xFF7567F8), Color(0xFF45A9E8), Color(0xFF58B990), Color(0xFFF2B341), Color(0xFFFF8D7D), Color(0xFFED6C9B))
private val tints = listOf(Color(0xFFF0EFFF), Color(0xFFE9F6FF), Color(0xFFEAF9F1), Color(0xFFFFF5E4), Color(0xFFFFF0ED), Color(0xFFFFF0F5))
private val categoryNames = listOf("Work", "Personal", "Ideas", "Health", "Finance", "Education", "Travel", "Other")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = NoteStore(this)
        setContent { NestNoteApp(store) }
    }
}

@Composable
private fun NestNoteApp(store: NoteStore) {
    var notes by remember { mutableStateOf(store.load()) }
    var dark by remember { mutableStateOf(false) }
    var selectedView by remember { mutableStateOf("Home") }
    var search by remember { mutableStateOf("") }
    var editor by remember { mutableStateOf<NestNote?>(null) }
    var creating by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var quickText by remember { mutableStateOf("") }
    var snackbar by remember { mutableStateOf("") }
    val context = LocalContext.current
    val bg = if (dark) Color(0xFF111421) else Paper
    val surface = if (dark) Color(0xFF191E2F) else Color.White
    val text = if (dark) Color(0xFFEEF0FB) else Ink
    val line = if (dark) Color(0xFF2A3045) else Color(0xFFECEEF5)
    val visibleNotes = notes.filter { n ->
        (selectedView == "Home" || selectedView == "All notes" || n.category == selectedView || (selectedView == "Reminders" && n.reminder.isNotBlank())) &&
            (search.isBlank() || (n.title + " " + n.body + " " + n.category).contains(search, true))
    }.sortedWith(compareByDescending<NestNote> { it.pinned }.thenByDescending { it.updated })
    fun persist(next: List<NestNote>) { notes = next; store.save(next) }

    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFFAAA3FF), background = bg, surface = surface) else lightColorScheme(primary = Violet, background = bg, surface = surface)) {
        Scaffold(
            containerColor = bg,
            topBar = {
                Surface(color = surface, shadowElevation = 0.dp) {
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("✦ NESTNOTE", color = Violet, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                            Text("YOUR PERSONAL THINKING SPACE", color = Muted, fontSize = 9.sp, letterSpacing = 1.3.sp)
                        }
                        IconButton(onClick = { showSearch = !showSearch; if (!showSearch) search = "" }) { Icon(Icons.Default.Search, "Search", tint = text) }
                        IconButton(onClick = { dark = !dark }) { Text(if (dark) "☀" else "☾", color = text, fontSize = 20.sp) }
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { editor = null; creating = true },
                    containerColor = Violet, contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New note") }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = surface) {
                    listOf("Home", "All notes", "Categories", "Reminders").forEach { item ->
                        NavigationBarItem(
                            selected = selectedView == item,
                            onClick = { selectedView = item },
                            icon = { Icon(when(item) { "Home" -> Icons.Default.Home; "Categories" -> Icons.Default.Menu; "Reminders" -> Icons.Default.WbSunny; else -> Icons.Default.Edit }, contentDescription = item) },
                            label = { Text(item, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Violet, selectedTextColor = Violet, indicatorColor = Violet.copy(alpha = .12f))
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).background(bg)) {
                if (showSearch) {
                    OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), placeholder = { Text("Search your notes…") }, singleLine = true)
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 18.dp, bottom = 12.dp)) {
                    Text("A LITTLE SPACE FOR BIG IDEAS", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                    Text(when(selectedView) { "Home" -> "Good day, Alex 👋"; "All notes" -> "Your thoughts, collected."; "Categories" -> "Find your focus."; "Reminders" -> "Things to remember." ; else -> selectedView }, color = text, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Make space for what matters.", color = Muted, fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    StatCard("Total notes", notes.size.toString(), Modifier.weight(1f), surface, line, text)
                    StatCard("Categories", categoryNames.size.toString(), Modifier.weight(1f), surface, line, text)
                    StatCard("Reminders", notes.count { it.reminder.isNotBlank() }.toString(), Modifier.weight(1f), surface, line, text)
                }
                if (selectedView == "Categories") {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categoryNames.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { category ->
                                    Card(Modifier.weight(1f).clickable { selectedView = category }, colors = CardDefaults.cardColors(containerColor = surface), border = androidx.compose.foundation.BorderStroke(1.dp, line)) {
                                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                            Text("✦", color = accents[categoryNames.indexOf(category) % accents.size], fontSize = 22.sp)
                                            Text(category, color = text, fontWeight = FontWeight.SemiBold)
                                            Text("${notes.count { it.category == category }} notes", color = Muted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (selectedView == "Reminders") "Reminders" else "Recent notes", color = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Pick up where your mind left off.", color = Muted, fontSize = 11.sp)
                        }
                        Text("${visibleNotes.size} notes", color = Muted, fontSize = 10.sp)
                    }
                    if (visibleNotes.isEmpty()) {
                        Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✎", color = Violet, fontSize = 36.sp)
                            Text("A fresh page awaits", color = text, fontWeight = FontWeight.Bold)
                            Text("Create a note and let ideas flow.", color = Muted, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(visibleNotes, key = { it.id }) { note ->
                                NoteCard(note, surface, line, text, onOpen = { editor = note; creating = true }, onPin = {
                                    persist(notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned, updated = System.currentTimeMillis()) else it })
                                }, onDelete = {
                                    persist(notes.filterNot { it.id == note.id })
                                })
                            }
                        }
                    }
                }
            }
        }
        if (creating) {
            NoteEditor(
                original = editor,
                categories = categoryNames,
                onDismiss = { creating = false },
                onSave = { updated ->
                    val next = if (editor == null) notes + updated else notes.map { if (it.id == editor!!.id) updated.copy(id = it.id) else it }
                    persist(next); creating = false; snackbar = "Note saved"
                },
                onDelete = if (editor != null) ({ persist(notes.filterNot { it.id == editor!!.id }); creating = false }) else null
            )
        }
        if (snackbar.isNotBlank()) {
            LaunchedEffect(snackbar) { kotlinx.coroutines.delay(1800); snackbar = "" }
            SnackbarHost(hostState = remember { SnackbarHostState() }, modifier = Modifier)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier, surface: Color, line: Color, text: Color) {
    Card(modifier, shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = surface), border = androidx.compose.foundation.BorderStroke(1.dp, line)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 13.dp)) {
            Text(label, color = Muted, fontSize = 10.sp, maxLines = 1)
            Text(value, color = text, fontWeight = FontWeight.Bold, fontSize = 23.sp)
        }
    }
}

@Composable
private fun NoteCard(note: NestNote, surface: Color, line: Color, text: Color, onOpen: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val accent = accents[note.color.coerceIn(0, accents.lastIndex)]
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, line)
    ) {
        Row {
            Box(Modifier.width(4.dp).fillMaxHeight().defaultMinSize(minHeight = 112.dp).background(accent))
            Column(Modifier.weight(1f).padding(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = tints[note.color.coerceIn(0, tints.lastIndex)], shape = RoundedCornerShape(6.dp)) {
                        Text(note.category, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.weight(1f))
                    Box {
                        IconButton(onClick = { menu = true }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.MoreVert, "Note actions", tint = Muted) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(if (note.pinned) "Unpin note" else "Pin note") }, onClick = { menu = false; onPin() }, leadingIcon = { Icon(Icons.Default.PushPin, null) })
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onOpen() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                        }
                    }
                }
                Text(note.title, color = text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
                Text(note.body, color = Muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp, bottom = 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (note.reminder.isNotBlank()) "◷ Reminder set" else SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(note.updated)), color = Muted, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    if (note.pinned) Icon(Icons.Default.PushPin, "Pinned", tint = accent, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditor(original: NestNote?, categories: List<String>, onDismiss: () -> Unit, onSave: (NestNote) -> Unit, onDelete: (() -> Unit)?) {
    var title by remember(original?.id) { mutableStateOf(original?.title.orEmpty()) }
    var body by remember(original?.id) { mutableStateOf(original?.body.orEmpty()) }
    var category by remember(original?.id) { mutableStateOf(original?.category ?: "Ideas") }
    var color by remember(original?.id) { mutableIntStateOf(original?.color ?: 0) }
    var reminder by remember(original?.id) { mutableStateOf(original?.reminder.orEmpty()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (original == null) "Create a note" else "Edit note", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Note title") }, singleLine = true)
                OutlinedTextField(body, { body = it }, label = { Text("Your thoughts") }, minLines = 4)
                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                    OutlinedTextField(value = category, onValueChange = {}, readOnly = true, label = { Text("Category") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) }, modifier = Modifier.menuAnchor())
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categories.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; categoryExpanded = false }) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Color", color = Muted, fontSize = 12.sp)
                    accents.forEachIndexed { i, c ->
                        Box(Modifier.size(25.dp).background(c, CircleShape).clickable { color = i }, contentAlignment = Alignment.Center) {
                            if (color == i) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                OutlinedButton(onClick = {
                    val now = java.util.Calendar.getInstance()
                    DatePickerDialog(context, { _, y, m, d ->
                        TimePickerDialog(context, { _, h, min -> reminder = String.format(Locale.getDefault(), "%04d-%02d-%02d %02d:%02d", y, m + 1, d, h, min) }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), true).show()
                    }, now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH), now.get(java.util.Calendar.DAY_OF_MONTH)).show()
                }, modifier = Modifier.fillMaxWidth()) { Text(if (reminder.isBlank()) "＋ Add reminder date & time" else "◷ $reminder") }
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank() && body.isNotBlank()) onSave(NestNote(original?.id ?: System.currentTimeMillis(), title.trim(), body.trim(), category, color, original?.pinned ?: false, reminder, System.currentTimeMillis())) }, enabled = title.isNotBlank() && body.isNotBlank()) { Text("Save note") } },
        dismissButton = { Row { if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = Color(0xFFD64B55)) }; TextButton(onClick = onDismiss) { Text("Cancel") } } }
    )
}
