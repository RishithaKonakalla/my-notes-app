package com.example.mynotesapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Notes")
data class Note (
    @PrimaryKey(autoGenerate = true)
    val id : Int = 0,
    val heading : String,
    val text : String
)