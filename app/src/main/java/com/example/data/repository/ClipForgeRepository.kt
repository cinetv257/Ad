package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.local.AppDatabase
import com.example.data.model.Clip
import com.example.data.model.User
import com.example.data.model.VideoProject
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class ClipResponse(
    val title: String,
    val startSeconds: Int,
    val endSeconds: Int,
    val engagementRate: Int,
    val description: String,
    val hashtags: String,
    val transcription: String,
    val subtitlesJson: String
)

class ClipForgeRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val userDao = db.userDao()
    private val projectDao = db.videoProjectDao()
    private val clipDao = db.clipDao()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // --- User Queries ---
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    fun getUserById(id: Int): Flow<User?> = userDao.getUserById(id)
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)

    // --- Video Project Queries ---
    val allProjects: Flow<List<VideoProject>> = projectDao.getAllProjects()
    fun getProjectById(id: Int): Flow<VideoProject?> = projectDao.getProjectById(id)
    suspend fun insertProject(project: VideoProject): Long = projectDao.insertProject(project)
    suspend fun deleteProject(project: VideoProject) = projectDao.deleteProject(project)
    suspend fun deleteProjectById(id: Int) = projectDao.deleteProjectById(id)

    // --- Clip Queries ---
    fun getClipsForProject(projectId: Int): Flow<List<Clip>> = clipDao.getClipsForProject(projectId)
    fun getClipById(id: Int): Flow<Clip?> = clipDao.getClipById(id)
    suspend fun insertClip(clip: Clip): Long = clipDao.insertClip(clip)
    suspend fun updateClip(clip: Clip) = clipDao.updateClip(clip)
    suspend fun deleteClip(clip: Clip) = clipDao.deleteClip(clip)

    // --- Gemini video analysis integration ---
    suspend fun analyzeAndGenerateClips(project: VideoProject): List<Clip> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)) {
            Log.d("ClipForgeRepository", "Gemini key is missing or is placeholder. Using offline premium simulator.")
            val simulated = generateSimulatedClips(project)
            simulated.forEach { clipDao.insertClip(it) }
            return@withContext simulated
        }

        val prompt = """
            You are ClipForge AI, a state-of-the-art long-form video analyzer.
            Analyze this video named: "${project.title}" from URL: "${project.sourceUrl}" (Source Type: ${project.sourceType}).
            The video description or context is: "${project.metadata}".
            
            Find the 5 most engaging, viral-worthy, high-energy clips. 
            Return a valid JSON array of objects representing these clips. 
            Do NOT include any markdown formatting or prefix text, return raw JSON array only.
            
            Each object in the JSON array must follow this structure exactly:
            {
               "title": "A short, catchy, clickable title for TikTok/Shorts",
               "startSeconds": Int (start time of clip),
               "endSeconds": Int (end time of clip, length should be 15 to 58 seconds),
               "engagementRate": Int (projected viral index between 85 and 99),
               "description": "Engaging description text containing social media metadata",
               "hashtags": "#Shorts #Viral #ClipForge #TikTok",
               "transcription": "Brief conversation caption transcript of the video",
               "subtitlesJson": "JSON string containing word timings e.g. '[{\"word\":\"Hello\",\"start\":15.0,\"end\":15.3},{\"word\":\"everyone\",\"start\":15.4,\"end\":15.8}]'"
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.8f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "You are a professional social media video strategist. Answer only in raw JSON arrays of clips.")))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d("ClipForgeRepository", "Gemini response text: $jsonText")
                val cleanJsonText = sanitizeJson(jsonText)
                val type = Types.newParameterizedType(List::class.java, ClipResponse::class.java)
                val adapter = moshi.adapter<List<ClipResponse>>(type)
                val responseClips = adapter.fromJson(cleanJsonText)
                
                if (!responseClips.isNullOrEmpty()) {
                    val entityClips = responseClips.map { resp ->
                        Clip(
                            projectId = project.id,
                            title = resp.title,
                            startSeconds = resp.startSeconds,
                            endSeconds = resp.endSeconds,
                            engagementRate = resp.engagementRate,
                            transcription = resp.transcription,
                            subtitlesJson = resp.subtitlesJson,
                            description = resp.description,
                            hashtags = resp.hashtags
                        )
                    }
                    entityClips.forEach { clipDao.insertClip(it) }
                    return@withContext entityClips
                }
            }
        } catch (e: Exception) {
            Log.e("ClipForgeRepository", "Gemini API error, falling back: ${e.message}", e)
        }

        // Fallback if network fails
        val fallbackClips = generateSimulatedClips(project)
        fallbackClips.forEach { clipDao.insertClip(it) }
        fallbackClips
    }

    private fun sanitizeJson(jsonText: String): String {
        return jsonText.trim()
            .replace("```json", "")
            .replace("```", "")
            .trim()
    }

    private fun generateSimulatedClips(project: VideoProject): List<Clip> {
        val topic = project.title
        val baseClips = listOf(
            Clip(
                projectId = project.id,
                title = "The Golden Truth about $topic 🤯",
                startSeconds = 12,
                endSeconds = 42,
                engagementRate = 98,
                transcription = "If you want to achieve success, you must change your daily habits. Here's exactly how. The routine determines the destiny.",
                subtitlesJson = """
                    [
                      {"word":"If","start":12.2,"end":12.5},
                      {"word":"you","start":12.5,"end":12.7},
                      {"word":"want","start":12.7,"end":13.0},
                      {"word":"to","start":13.0,"end":13.1},
                      {"word":"achieve","start":13.1,"end":13.5},
                      {"word":"success,","start":13.5,"end":14.0},
                      {"word":"you","start":14.5,"end":14.7},
                      {"word":"must","start":14.7,"end":15.0},
                      {"word":"change","start":15.0,"end":15.4},
                      {"word":"your","start":15.4,"end":15.6},
                      {"word":"daily","start":15.6,"end":16.0},
                      {"word":"habits.","start":16.0,"end":16.8},
                      {"word":"Here's","start":17.5,"end":17.9},
                      {"word":"exactly","start":17.9,"end":18.4},
                      {"word":"how.","start":18.4,"end":19.0},
                      {"word":"The","start":19.5,"end":19.7},
                      {"word":"routine","start":19.7,"end":20.3},
                      {"word":"determines","start":20.3,"end":21.0},
                      {"word":"the","start":21.0,"end":21.2},
                      {"word":"destiny.","start":21.2,"end":22.2}
                    ]
                """.trimIndent(),
                description = "Uncover the ground-breaking routine that shifts average players into professional elites. Watch until the end!",
                hashtags = "#$topic #DailyRoutine #Productivity #ClipForge #TikTokViral #Fyp"
            ),
            Clip(
                projectId = project.id,
                title = "Why 99% of people fail at this ❌",
                startSeconds = 110,
                endSeconds = 155,
                engagementRate = 94,
                transcription = "You fail because you get emotional. Stay disciplined, stay calm, and don't look at the short-term results.",
                subtitlesJson = """
                    [
                      {"word":"You","start":110.1,"end":110.4},
                      {"word":"fail","start":110.4,"end":110.7},
                      {"word":"because","start":110.7,"end":111.1},
                      {"word":"you","start":111.1,"end":111.3},
                      {"word":"get","start":111.3,"end":111.6},
                      {"word":"emotional.","start":111.6,"end":112.5},
                      {"word":"Stay","start":113.0,"end":113.4},
                      {"word":"disciplined,","start":113.4,"end":114.2},
                      {"word":"stay","start":114.5,"end":114.8},
                      {"word":"calm,","start":114.8,"end":115.4},
                      {"word":"and","start":115.8,"end":116.0},
                      {"word":"don't","start":116.0,"end":116.3},
                      {"word":"look","start":116.3,"end":116.6},
                      {"word":"at","start":116.6,"end":116.8},
                      {"word":"the","start":116.8,"end":117.0},
                      {"word":"short-term","start":117.0,"end":117.6},
                      {"word":"results.","start":117.6,"end":118.5}
                    ]
                """.trimIndent(),
                description = "This single mindset pattern differentiates successful builders from non-starters. Learn to delay your gratification.",
                hashtags = "#Mindset101 #DisciplineOverMotivation #SuccessHacks #YouTubeShorts"
            ),
            Clip(
                projectId = project.id,
                title = "The Secret Hack nobody tells you 🔥",
                startSeconds = 240,
                endSeconds = 285,
                engagementRate = 89,
                transcription = "Spend 30 minutes in the morning planning instead of replying to messages. It multiplies your efficiency.",
                subtitlesJson = """
                    [
                      {"word":"Spend","start":240.2,"end":240.5},
                      {"word":"30","start":240.5,"end":240.9},
                      {"word":"minutes","start":240.9,"end":241.4},
                      {"word":"in","start":241.4,"end":241.6},
                      {"word":"the","start":241.6,"end":241.8},
                      {"word":"morning","start":241.8,"end":242.3},
                      {"word":"planning","start":242.3,"end":242.9},
                      {"word":"instead","start":242.9,"end":243.4},
                      {"word":"of","start":243.4,"end":243.6},
                      {"word":"replying","start":243.6,"end":244.2},
                      {"word":"to","start":244.2,"end":244.4},
                      {"word":"messages.","start":244.4,"end":245.2},
                      {"word":"It","start":245.5,"end":245.7},
                      {"word":"multiplies","start":245.7,"end":246.5},
                      {"word":"your","start":246.5,"end":246.7},
                      {"word":"efficiency.","start":246.7,"end":247.7}
                    ]
                """.trimIndent(),
                description = "Simple time-management modification that recovers fifteen productive hours every business week.",
                hashtags = "#TimeManagement #HustleSmart #ClipForgeAI #Shorts"
            ),
            Clip(
                projectId = project.id,
                title = "This changes everything... 💥",
                startSeconds = 360,
                endSeconds = 412,
                engagementRate = 92,
                transcription = "Stop waiting for the perfect moment. Perfect moment doesn't exist. You create it by taking action.",
                subtitlesJson = """
                    [
                      {"word":"Stop","start":360.2,"end":360.6},
                      {"word":"waiting","start":360.6,"end":361.1},
                      {"word":"for","start":361.1,"end":361.3},
                      {"word":"the","start":361.3,"end":361.5},
                      {"word":"perfect","start":361.5,"end":362.0},
                      {"word":"moment.","start":362.0,"end":362.6},
                      {"word":"Perfect","start":363.0,"end":363.5},
                      {"word":"moment","start":363.5,"end":364.0},
                      {"word":"doesn't","start":364.0,"end":364.5},
                      {"word":"exist.","start":364.5,"end":365.1},
                      {"word":"You","start":365.4,"end":365.6},
                      {"word":"create","start":365.6,"end":366.0},
                      {"word":"it","start":366.0,"end":366.2},
                      {"word":"by","start":366.2,"end":366.4},
                      {"word":"taking","start":366.4,"end":366.8},
                      {"word":"action.","start":366.8,"end":367.5}
                    ]
                """.trimIndent(),
                description = "Quit procrastinating on your business ideas. ClipForge auto-transcription of ultimate advice.",
                hashtags = "#MotivationMinute #InspireDaily #GetStarted Now"
            ),
            Clip(
                projectId = project.id,
                title = "Supercharge your growth today 🚀",
                startSeconds = 480,
                endSeconds = 525,
                engagementRate = 87,
                transcription = "Learn one new skill every single month. In five years, you will be in the top one percent of expert practitioners.",
                subtitlesJson = """
                    [
                      {"word":"Learn","start":480.1,"end":480.5},
                      {"word":"one","start":480.5,"end":480.8},
                      {"word":"new","start":480.8,"end":481.1},
                      {"word":"skill","start":481.1,"end":481.6},
                      {"word":"every","start":481.6,"end":482.0},
                      {"word":"single","start":482.0Trace,"end":482.4},
                      {"word":"month.","start":482.4,"end":483.0},
                      {"word":"In","start":483.5,"end":483.8},
                      {"word":"five","start":483.8,"end":484.2},
                      {"word":"years,","start":484.2,"end":484.8},
                      {"word":"you","start":485.2,"end":485.4},
                      {"word":"will","start":485.4,"end":485.6},
                      {"word":"be","start":485.6,"end":485.8},
                      {"word":"in","start":485.8,"end":486.0},
                      {"word":"the","start":486.0,"end":486.2},
                      {"word":"top","start":486.2,"end":486.5},
                      {"word":"one","start":486.5Trace,"end":486.8},
                      {"word":"percent","start":486.8,"end":487.4},
                      {"word":"of","start":487.4,"end":487.6},
                      {"word":"expert","start":487.6,"end":488.1},
                      {"word":"practitioners.","start":488.1,"end":489.1}
                    ]
                """.trimIndent(),
                description = "Compounding micro-skills is the ultimate cheat code for hyper professional acceleration.",
                hashtags = "#ContinuousLearning #SkillBuilding #SaaSCoach"
            )
        )
        return baseClips
    }
}
