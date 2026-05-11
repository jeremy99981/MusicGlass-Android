package com.musicglass.app.youtubemusic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class MusicAIIntent(
    val type: String = "unknown",
    val artistName: String? = null,
    val trackTitle: String? = null,
    val albumTitle: String? = null,
    val playlistName: String? = null,
    val mood: String? = null,
    val language: String? = null,
    val confidence: Double = 0.0,
    val shouldOpenFullPlayer: Boolean = false,
    val requiresUserChoice: Boolean = false,
    val clarificationQuestion: String? = null
)

sealed class MusicAIResolution {
    data class PlayableTrack(val track: SongItem, val queue: List<SongItem>) : MusicAIResolution()
    data class PlayableAlbum(val album: com.musicglass.app.youtubemusic.AlbumInfo, val tracks: List<SongItem>) : MusicAIResolution()
    data class PlayablePlaylist(val playlist: SongItem, val tracks: List<SongItem>) : MusicAIResolution()
    data class PlayableRadio(val track: SongItem) : MusicAIResolution()
    data class AlbumList(val albums: List<SongItem>) : MusicAIResolution()
    data class OpenSearch(val query: String) : MusicAIResolution()
    data class NeedsClarification(val message: String) : MusicAIResolution()
    data class Failure(val message: String) : MusicAIResolution()
}

class DeepSeekMusicIntentService {
    private val apiKey = "sk-d7fa95872d1e4f3187f661e9459ac139"
    private val endpoint = "https://api.deepseek.com/chat/completions"
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun parseIntent(text: String): MusicAIIntent = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey.contains("PLACEHOLDER")) throw DeepSeekException("Clé API DeepSeek non configurée")
        if (text.isBlank()) throw DeepSeekException("Texte vide")

        val systemPrompt = """
Tu es un parseur d'intentions musicales pour MusicGlass.
Réponds UNIQUEMENT avec un objet JSON valide. Aucun texte avant ou après. Aucun ```json.

Tu dois DIFFÉRENCIER :
1. Demander une LISTE (ex: "album Gazo", "montre les albums de...") -> type: listAlbums, requiresUserChoice: true.
2. Demander une LECTURE directe (ex: "lance", "joue", "lis", "mets") -> type: playAlbum/playTrack, requiresUserChoice: false.
3. Demander une OUVERTURE (ex: "ouvre") -> type: openAlbum, requiresUserChoice: false.

Règles :
- "album [Artiste]" sans verbe d'action = listAlbums.
- "dernier album [Artiste]" sans verbe = openAlbum ou listAlbums (si ambigu).
- "lance le dernier album" = playLatestAlbum.

Champs JSON : type, artistName, trackTitle, albumTitle, playlistName, mood, language, confidence, shouldOpenFullPlayer, requiresUserChoice, clarificationQuestion.
Types : playTrack, playPopularTrack, playLatestTrack, playAlbum, playLatestAlbum, playPopularAlbum, openAlbum, listAlbums, listTracks, playArtistMix, playPlaylist, playLikedSongs, playHistory, playMood, searchOnly, unknown.
""".trimIndent()

        val body = JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", text)
                })
            })
            put("response_format", JSONObject().apply {
                put("type", "json_object")
            })
            put("temperature", 0.1)
        }

        val requestBody = body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Erreur ${response.code}"
            throw DeepSeekException("Erreur API DeepSeek: $errorBody")
        }

        val responseBody = response.body?.string() ?: throw DeepSeekException("Réponse vide")
        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        val content = choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
            ?: throw DeepSeekException("Pas de contenu dans la réponse")

        val cleanedJson = extractJSON(content)
        try {
            jsonParser.decodeFromString<MusicAIIntent>(cleanedJson)
        } catch (e: Exception) {
            println("🎙️ [DeepSeek] JSON Decode Error: ${e.message}")
            println("🎙️ [DeepSeek] Raw: $cleanedJson")
            throw DeepSeekException("Je n'ai pas pu comprendre la réponse de l'IA. Réessayez ou reformulez.")
        }
    }

    private fun extractJSON(text: String): String {
        var cleaned = text.trim()
        cleaned = cleaned.replace("```json", "").replace("```", "").trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        return if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
    }
}

class DeepSeekException(message: String) : Exception(message)
