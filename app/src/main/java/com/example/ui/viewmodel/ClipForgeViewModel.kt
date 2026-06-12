package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Clip
import com.example.data.model.User
import com.example.data.model.VideoProject
import com.example.data.repository.ClipForgeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    LANDING, LOGIN, REGISTER, DASHBOARD, UPLOAD, LIBRARY, EDITOR, ANALYTICS, SETTINGS, BILLING, ADMIN
}

class ClipForgeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ClipForgeRepository(application)

    // --- Navigation ---
    private val _currentScreen = MutableStateFlow(Screen.LANDING)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // --- Authentication ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // --- Video Projects ---
    val allProjects: StateFlow<List<VideoProject>> = repository.allItemsStateFlow()
    
    private val _selectedProject = MutableStateFlow<VideoProject?>(null)
    val selectedProject: StateFlow<VideoProject?> = _selectedProject.asStateFlow()

    // --- Clips for Selected Project ---
    private val _projectClips = MutableStateFlow<List<Clip>>(emptyList())
    val projectClips: StateFlow<List<Clip>> = _projectClips.asStateFlow()

    // --- Active Editing Clip ---
    private val _selectedClip = MutableStateFlow<Clip?>(null)
    val selectedClip: StateFlow<Clip?> = _selectedClip.asStateFlow()

    // --- UI UX Flags ---
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- Export & Progress Queues ---
    private val _clipExportStates = MutableStateFlow<Map<Int, Int>>(emptyMap()) // clipId -> percentage
    val clipExportStates: StateFlow<Map<Int, Int>> = _clipExportStates.asStateFlow()

    init {
        // Automatically check/seed database on start
        viewModelScope.launch {
            repository.allProjects.collectLatest { list ->
                if (list.isEmpty()) {
                    seedSampleData()
                }
            }
        }

        viewModelScope.launch {
            _selectedProject.collectLatest { project ->
                if (project != null) {
                    repository.getClipsForProject(project.id).collectLatest { clips ->
                        _projectClips.value = clips
                        if (_selectedClip.value?.projectId != project.id) {
                            _selectedClip.value = clips.firstOrNull()
                        }
                    }
                } else {
                    _projectClips.value = emptyList()
                    _selectedClip.value = null
                }
            }
        }
    }

    private fun ClipForgeRepository.allItemsStateFlow(): StateFlow<List<VideoProject>> {
        return allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun navigateTo(screen: Screen) {
        _errorMessage.value = null
        _currentScreen.value = screen
    }

    // --- Authentication Actions ---
    fun login(email: String, name: String, onFinished: () -> Unit) = viewModelScope.launch {
        if (email.isBlank() || name.isBlank()) {
            _errorMessage.value = "Credentials cannot be blank!"
            return@launch
        }
        val existing = repository.getUserByEmail(email)
        if (existing != null) {
            _currentUser.value = existing
            navigateTo(Screen.DASHBOARD)
            onFinished()
        } else {
            val newUser = User(email = email, password = name, tier = "Free")
            val id = repository.insertUser(newUser)
            _currentUser.value = newUser.copy(id = id.toInt())
            navigateTo(Screen.DASHBOARD)
            onFinished()
        }
    }

    fun register(email: String, name: String, onFinished: () -> Unit) = viewModelScope.launch {
        if (email.isBlank() || name.isBlank()) {
            _errorMessage.value = "All fields required!"
            return@launch
        }
        val existing = repository.getUserByEmail(email)
        if (existing != null) {
            _errorMessage.value = "User already registered! Try logging in."
            return@launch
        }
        val newUser = User(email = email, password = name, tier = "Creator") // Sign up gifts Creator tier!
        val id = repository.insertUser(newUser)
        _currentUser.value = newUser.copy(id = id.toInt())
        navigateTo(Screen.DASHBOARD)
        onFinished()
    }

    fun logout() {
        _currentUser.value = null
        navigateTo(Screen.LANDING)
    }

    fun updateUserTier(tier: String) = viewModelScope.launch {
        val user = _currentUser.value ?: return@launch
        val updated = user.copy(tier = tier)
        repository.updateUser(updated)
        _currentUser.value = updated
    }

    fun toggleSocialConnection(social: String) = viewModelScope.launch {
        val user = _currentUser.value ?: return@launch
        val updated = when (social.uppercase()) {
            "YOUTUBE" -> user.copy(connectedYoutube = !user.connectedYoutube)
            "TIKTOK" -> user.copy(connectedTiktok = !user.connectedTiktok)
            "INSTAGRAM" -> user.copy(connectedInstagram = !user.connectedInstagram)
            else -> user
        }
        repository.updateUser(updated)
        _currentUser.value = updated
    }

    // --- Video Library Actions ---
    fun selectProject(project: VideoProject?) {
        _selectedProject.value = project
        if (project != null) {
            navigateTo(Screen.LIBRARY)
        }
    }

    fun deleteProject(project: VideoProject) = viewModelScope.launch {
        if (_selectedProject.value?.id == project.id) {
            _selectedProject.value = null
        }
        repository.deleteProject(project)
    }

    fun importVideo(title: String, url: String, sourceType: String, duration: String, desc: String) = viewModelScope.launch {
        if (title.isBlank() || url.isBlank()) {
            _errorMessage.value = "Video Title and Source URL are required!"
            return@launch
        }
        _isAnalyzing.value = true
        navigateTo(Screen.DASHBOARD)

        val suffixStr = if (url.contains("youtube.com") || url.contains("youtu.be")) "YOUTUBE" else sourceType
        val project = VideoProject(
            title = title,
            sourceUrl = url,
            sourceType = suffixStr,
            duration = duration,
            thumbnailIndex = (0..5).random(),
            metadata = desc
        )
        val projectId = repository.insertProject(project)
        val persistedProject = project.copy(id = projectId.toInt())

        // Run full AI extraction (and captions generation!) asynchronously
        viewModelScope.launch {
            try {
                repository.analyzeAndGenerateClips(persistedProject)
                _selectedProject.value = persistedProject
                navigateTo(Screen.LIBRARY)
            } catch (e: Exception) {
                _errorMessage.value = "AI analysis failed: ${e.message}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    // --- Editor Actions ---
    fun selectClip(clip: Clip) {
        _selectedClip.value = clip
        navigateTo(Screen.EDITOR)
    }

    fun saveClipEdits(updatedClip: Clip) = viewModelScope.launch {
        repository.updateClip(updatedClip)
        _selectedClip.value = updatedClip
        // Sync back within current cached clips list
        _projectClips.value = _projectClips.value.map {
            if (it.id == updatedClip.id) updatedClip else it
        }
    }

    fun triggerClipExport(clip: Clip) = viewModelScope.launch {
        val clipId = clip.id
        // Simulate background video compilation logic (MoviePy / FFmpeg simulation)
        viewModelScope.launch {
            _clipExportStates.value = _clipExportStates.value.toMutableMap().apply { put(clipId, 0) }
            for (progress in 10..100 step 15) {
                delay(350)
                _clipExportStates.value = _clipExportStates.value.toMutableMap().apply { put(clipId, minOf(progress, 100)) }
            }
            // Once completed, update the database state
            val completedClip = clip.copy(exportStatus = "EXPORTED", exportProgress = 100)
            repository.updateClip(completedClip)
            _selectedClip.value = completedClip
            _projectClips.value = _projectClips.value.map {
                if (it.id == completedClip.id) completedClip else it
            }
        }
    }

    fun scheduleOrPublishClip(clip: Clip, social: String, scheduleTime: Long = 0L) = viewModelScope.launch {
        val completedClip = clip.copy(
            exportStatus = if (scheduleTime > 0L) "SCHEDULED" else "PUBLISHED",
            scheduledTime = scheduleTime,
            publishSocial = social
        )
        repository.updateClip(completedClip)
        _selectedClip.value = completedClip
        _projectClips.value = _projectClips.value.map {
            if (it.id == completedClip.id) completedClip else it
        }
    }

    // --- Seeding Pre-made Data for first time launch high-fidelity ---
    private suspend fun seedSampleData() {
        val userMock = User(email = "demo@clipforge.com", password = "User", tier = "Creator")
        repository.insertUser(userMock)

        val project1 = VideoProject(
            title = "Ultimate AI SaaS Guide 2026",
            sourceUrl = "https://youtube.com/watch?v=saas_tutorial_2026",
            sourceType = "YOUTUBE",
            duration = "12:45",
            thumbnailIndex = 2,
            metadata = "Complete walk-through for startup builders detailing architectural pipelines and dynamic frontends.",
            transcription = "Achieving modular acceleration with robust SQLite engines compiles standard performance buffers. Today we will establish the modern premium layout models..."
        )
        val p1Id = repository.insertProject(project1).toInt()

        val project2 = VideoProject(
            title = "Creative Freedom & Aesthetic Focus",
            sourceUrl = "https://youtube.com/watch?v=mental_clarity",
            sourceType = "UPLOAD",
            duration = "08:15",
            thumbnailIndex = 4,
            metadata = "A calming visual narrative on aesthetic composition and how environment affects design choices.",
            transcription = "Noise represents the clutter inside average workspaces. Subtracting unrequested feature sets brings clarity. Let us discuss the concept of bold negative spacing..."
        )
        val p2Id = repository.insertProject(project2).toInt()

        // Seed clips for project 1
        val p1Clips = listOf(
            Clip(
                projectId = p1Id,
                title = "AI SaaS Hack in 2026 🤯",
                startSeconds = 15,
                endSeconds = 48,
                engagementRate = 97,
                transcription = "Achieving modular acceleration with robust SQLite engines compiles standard performance buffers.",
                subtitlesJson = """
                    [
                      {"word":"Achieving","start":15.0,"end":15.4},
                      {"word":"modular","start":15.4,"end":15.8},
                      {"word":"acceleration","start":15.8,"end":16.4},
                      {"word":"with","start":16.4,"end":16.6},
                      {"word":"robust","start":16.6,"end":17.0},
                      {"word":"SQLite","start":17.0,"end":17.5},
                      {"word":"engines","start":17.5,"end":18.0},
                      {"word":"compiles","start":18.2,"end":18.7},
                      {"word":"standard","start":18.7,"end":19.2},
                      {"word":"performance","start":19.2,"end":19.8},
                      {"word":"buffers.","start":19.8,"end":20.5}
                    ]
                """.trimIndent(),
                description = "Learn how SQLite databases compound storage performance for local offline caching solutions.",
                hashtags = "#SaaSArchitecture #AndroidDev #SQLiteTips #CodingLifestyle"
            ),
            Clip(
                projectId = p1Id,
                title = "Worst Coding Habits to Avoid ❌",
                startSeconds = 145,
                endSeconds = 192,
                engagementRate = 93,
                transcription = "Do not try to make multiple parallel edits to the same target content. It fractures the repository state.",
                subtitlesJson = """
                    [
                      {"word":"Do","start":145.2,"end":145.4},
                      {"word":"not","start":145.4,"end":145.6},
                      {"word":"try","start":145.6Trace,"end":145.9},
                      {"word":"to","start":145.9,"end":146.0},
                      {"word":"make","start":146.0,"end":146.3},
                      {"word":"multiple","start":146.3,"end":146.8},
                      {"word":"parallel","start":146.8,"end":147.3},
                      {"word":"edits.","start":147.3,"end":148.0}
                    ]
                """.trimIndent(),
                description = "Keep editing requests tidy and scoped rather than overcomplicating modular functions.",
                hashtags = "#LearnProgramming #JuniorDevTips #EngineeringTruths"
            )
        )
        p1Clips.forEach { repository.insertClip(it) }

        // Seed clips for project 2
        val p2Clips = listOf(
            Clip(
                projectId = p2Id,
                title = "Unlocking Creative Focus 🧘",
                startSeconds = 120,
                endSeconds = 175,
                engagementRate = 95,
                transcription = "Subtracting unrequested features brings clarity. Negativity is standard focus noise.",
                subtitlesJson = """
                    [
                      {"word":"Subtracting","start":120.1,"end":120.7},
                      {"word":"unrequested","start":120.7,"end":121.5},
                      {"word":"features","start":121.5,"end":122.0},
                      {"word":"brings","start":122.0,"end":122.4},
                      {"word":"clarity.","start":122.4,"end":123.1}
                    ]
                """.trimIndent(),
                description = "Eliminate unrequested scope clutter to recover core product focus and outstanding aesthetics.",
                hashtags = "#CreativeFlow #ZenOfCode #AestheticsMatter"
            )
        )
        p2Clips.forEach { repository.insertClip(it) }
    }
}
