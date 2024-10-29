package com.example.mynotesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mynotesapp.data.Note
import com.example.mynotesapp.data.NoteDao
import com.example.mynotesapp.data.NoteDatabase
import com.example.mynotesapp.ui.theme.MyNotesAppTheme
import com.example.mynotesapp.ui.theme.cursuive
import com.example.mynotesapp.ui.theme.viewmodel.NoteViewModel
import com.example.mynotesapp.ui.theme.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    private lateinit var database: NoteDatabase
    private lateinit var noteDao: NoteDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = NoteDatabase.getDatabase(this)
        noteDao = database.noteDao()
        setContent {
            MyNotesAppTheme {
                NotesApp(noteDao = noteDao)
            }
        }
    }
}

@Composable
fun NotesApp(noteDao: NoteDao) {
    // Create an instance of NoteViewModel using ViewModelProvider.Factory
    val viewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory(noteDao)
    )

    val navController = rememberNavController()
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    val notes by viewModel.notes.collectAsState(initial = emptyList()) // Get notes from ViewModel
    val coroutineScope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            MainScreen(
                notes = notes,
                onNotesClicked = { navController.navigate("create_note") },
                onEditClicked = { note ->
                    noteToEdit = note
                    navController.navigate("create_note")
                },
                onDeleteClicked = { note ->
                    coroutineScope.launch {
                        viewModel.delete(note) // Use ViewModel to delete note
                    }
                },
                onViewClicked = { note ->
                    viewModel.setNote(note)
                    navController.navigate("view_note")
                }
            )
        }
        composable("create_note") {
            CreateNoteScreen(
                initialNote = noteToEdit,
                onBackClicked = {
                    noteToEdit = null
                    navController.popBackStack()
                },
                onNoteSaved = { newNote ->
                    coroutineScope.launch {
                        if (noteToEdit != null) {
                            viewModel.update(newNote) // Use ViewModel to update
                        } else {
                            viewModel.insert(newNote) // Use ViewModel to insert new note
                        }
                        noteToEdit = null
                        navController.popBackStack()
                    }
                }
            )
        }
        composable("view_note") {

                ViewNoteScreen(note = viewModel.getNote(), onBackClicked = { navController.popBackStack() })
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoteScreen(
    initialNote: Note? = null,
    onBackClicked: () -> Unit,
    onNoteSaved: (Note) -> Unit // Assuming this handles both create and update
) {
    var heading by remember { mutableStateOf(initialNote?.heading ?: "") }
    var noteText by remember { mutableStateOf(initialNote?.text ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (initialNote != null) "Edit Note" else "Create Note") },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Check if heading is a single word before saving
                        if (heading.trim().split("\\s+".toRegex()).size == 1 && noteText.isNotBlank()) {
                            // If editing, pass the same note object with updated values
                            val noteToSave = if (initialNote != null) {
                                initialNote.copy(heading = heading, text = noteText)
                            } else {
                                Note(heading = heading, text = noteText)
                            }
                            onNoteSaved(noteToSave)
                            errorMessage = "" // Clear error message if saved successfully
                        } else {
                            errorMessage = "Heading must be a single word."
                        }
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(dimensionResource(id = R.dimen.padding_small)),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                TextField(
                    value = heading,
                    onValueChange = { newHeading ->
                        // Allow only a single word in heading
                        if (newHeading.trim().isNotBlank() && newHeading.split("\\s+".toRegex()).size <= 1) {
                            heading = newHeading
                        }
                    },
                    label = { Text("Heading (Single Word Only)", fontSize = 20.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = noteText,
                    onValueChange = { noteText = it }, // Allow any number of words
                    label = { Text("Note Text", fontSize = 20.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                // Display error message if any
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            Button(
                onClick = {
                    // Check if heading is a single word before saving
                    if (heading.trim().split("\\s+".toRegex()).size == 1 && noteText.isNotBlank()) {
                        // If editing, pass the same note object with updated values
                        val noteToSave = if (initialNote != null) {
                            initialNote.copy(heading = heading, text = noteText)
                        } else {
                            Note(heading = heading, text = noteText)
                        }
                        onNoteSaved(noteToSave)
                        errorMessage = "" // Clear error message if saved successfully
                    } else {
                        errorMessage = "Heading must be a single word."
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save")
            }
        }
    }
}


// New ViewNoteScreen Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewNoteScreen(note: Note, onBackClicked: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(
                    text = note.heading,
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )},
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(dimensionResource(id = R.dimen.padding_small))
        ) {
            Text(
                text = note.text,
                style = TextStyle(fontSize = 18.sp),
                modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_small))
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    notes: List<Note>,
    onNotesClicked: () -> Unit,
    onViewClicked: (Note) -> Unit,
    onEditClicked: (Note) -> Unit,
    onDeleteClicked: (Note) -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            MyNotesAppBar(
                modifier = Modifier.padding(top = 16.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNotesClicked) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Display notes in a LazyColumn
            LazyColumn(modifier = Modifier.padding()) {
                items(notes) { note ->
                    NoteCard(note = note,
                        onViewClicked = onViewClicked,
                        onEditClicked = onEditClicked,
                        onDeleteClicked = onDeleteClicked
                    )
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onViewClicked: (Note) -> Unit,
    onEditClicked: (Note) -> Unit,
    onDeleteClicked: (Note) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.padding_small))
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_small)),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = note.heading,
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = note.text,
                style = TextStyle(fontSize = 18.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
                )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onViewClicked(note) }) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = "View")
                }
                IconButton(onClick = { onEditClicked(note) }) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { onDeleteClicked(note) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyNotesAppBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.image_size))
                        .padding(dimensionResource(id = R.dimen.padding_small)),
                    painter = painterResource(R.drawable.ic_notes_icon),

                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 40.sp,
                    fontFamily = cursuive,
                    fontWeight = FontWeight.Bold

                )
            }
        },
        modifier = modifier
    )
}