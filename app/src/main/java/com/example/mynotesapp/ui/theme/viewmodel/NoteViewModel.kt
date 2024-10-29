package com.example.mynotesapp.ui.theme.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynotesapp.data.Note
import com.example.mynotesapp.data.NoteDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch



class NoteViewModel(private val noteDao: NoteDao) : ViewModel() {
    private var note = Note(heading = "",text = "")

    // Flow to observe all notes
    val notes: Flow<List<Note>> = noteDao.getAllNotes()

    // Insert a note into the database
    fun insert(note: Note) {
        viewModelScope.launch {
            noteDao.insert(note)
        }
    }

    // Update a note in the database
    fun update(note: Note) {
        viewModelScope.launch {
            noteDao.update(note)
        }
    }

    // Delete a note from the database
    fun delete(note: Note) {
        viewModelScope.launch {
            noteDao.delete(note)
        }
    }

    fun setNote(note : Note) {
        this.note = note
    }

    fun getNote() : Note {
        return note
    }
}