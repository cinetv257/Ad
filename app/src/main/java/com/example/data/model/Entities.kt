package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val password: String, // Mock-friendly password hashes
    val connectedYoutube: Boolean = false,
    val connectedTiktok: Boolean = false,
    val connectedInstagram: Boolean = false,
    val tier: String = "Free", // Free, Creator, Pro, Enterprise
    val usageCount: Int = 0
)

@Entity(tableName = "video_projects")
data class VideoProject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val sourceUrl: String,
    val sourceType: String, // YOUTUBE, VIMEO, TIKTOK, UPLOAD
    val duration: String,
    val thumbnailIndex: Int, // Represents gradient selection index for visual richness
    val metadata: String,
    val timestamp: Long = System.currentTimeMillis(),
    val transcription: String = ""
)

@Entity(tableName = "clips")
data class Clip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val title: String,
    val startSeconds: Int,
    val endSeconds: Int,
    val engagementRate: Int, // 0-100 score
    val transcription: String = "",
    val subtitlesJson: String = "", // JSON string containing captions list
    val captionsStyle: String = "Classic Karaoke", // Karaoke-style, Retro Neon, Cinematic, Word-by-Word
    val fontName: String = "Inter",
    val fontSize: Int = 24,
    val fontColorHex: String = "#FFDE4D", // Accent yellow
    val subtitlePosition: Float = 0.5f, // percentage from top 0 to 1
    val titleSuggestion: String = "",
    val description: String = "",
    val hashtags: String = "",
    val exportStatus: String = "DRAFT", // DRAFT, QUEUED, EXPORTED, PUBLISHED, SCHEDULED
    val exportProgress: Int = 0,
    val scheduledTime: Long = 0L,
    val watermarkText: String = "ClipForge AI",
    val watermarkPosition: String = "TOP_LEFT", // TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, NONE
    val musicTrack: String = "None", // Dynamic lofi, Upbeat Pop, cinematic
    val publishSocial: String = ""
)
