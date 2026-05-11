package com.musicglass.app.ui.features

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicglass.app.youtubemusic.DeepSeekException
import com.musicglass.app.youtubemusic.DeepSeekMusicIntentService
import com.musicglass.app.youtubemusic.InnerTubeClient
import com.musicglass.app.youtubemusic.InnerTubeJSONMapper
import com.musicglass.app.youtubemusic.MusicAIResolution
import com.musicglass.app.youtubemusic.MusicAIResolver
import com.musicglass.app.youtubemusic.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AIAssistantState {
    object Idle : AIAssistantState()
    object CheckingPermissions : AIAssistantState()
    object StartingAudio : AIAssistantState()
    object Listening : AIAssistantState()
    object Thinking : AIAssistantState()
    object Resolving : AIAssistantState()
    data class ShowingAlbumChoices(val question: String, val albums: List<SongItem>) : AIAssistantState()
    data class Playing(val title: String) : AIAssistantState()
    data class TextInput(val message: String?) : AIAssistantState()
    data class Error(val message: String) : AIAssistantState()
}

sealed class AIPlayAction {
    data class PlayTrack(val track: SongItem, val queue: List<SongItem>) : AIPlayAction()
    data class PlayRadio(val track: SongItem) : AIPlayAction()
}

class AIAssistantViewModel(
    private val innerTubeClient: InnerTubeClient,
    private val mapper: InnerTubeJSONMapper
) : ViewModel() {

    private val intentService = DeepSeekMusicIntentService()
    private val resolver = MusicAIResolver(innerTubeClient, mapper)

    private val _state = MutableStateFlow<AIAssistantState>(AIAssistantState.Idle)
    val state: StateFlow<AIAssistantState> = _state

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput

    private val _playAction = MutableStateFlow<AIPlayAction?>(null)
    val playAction: StateFlow<AIPlayAction?> = _playAction

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isStarting = false

    fun updateTextInput(text: String) {
        _textInput.value = text
    }

    fun consumePlayAction() {
        _playAction.value = null
    }

    fun setState(newState: AIAssistantState, reason: String = "") {
        println("🎙️ [AI STATE] ${_state.value} -> $newState | Reason: $reason")
        _state.value = newState
    }

    fun startAssistant(context: Context) {
        if (isStarting || isListening) return
        isStarting = true
        _transcript.value = ""

        viewModelScope.launch {
            setState(AIAssistantState.CheckingPermissions, "User tapped start")

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                setState(AIAssistantState.TextInput("Reconnaissance vocale indisponible. Vous pouvez écrire votre demande."),
                    "Speech recognition not available")
                isStarting = false
                return@launch
            }

            setState(AIAssistantState.StartingAudio, "Speech available")

            try {
                startSpeechRecognition(context)
                isListening = true
                isStarting = false
                setState(AIAssistantState.Listening, "Audio engine started")
            } catch (e: Exception) {
                isStarting = false
                val msg = e.message ?: "Erreur inconnue"
                if (msg.contains("indisponible", ignoreCase = true) || msg.contains("not available", ignoreCase = true)) {
                    setState(AIAssistantState.TextInput("Microphone indisponible. Vous pouvez écrire votre demande."),
                        "No microphone")
                } else {
                    setState(AIAssistantState.Error("Impossible de démarrer l'écoute. Vérifiez votre micro ou réessayez."),
                        "Start error: $msg")
                }
            }
        }
    }

    private fun startSpeechRecognition(context: Context) {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                setState(AIAssistantState.Listening, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Erreur audio"
                    SpeechRecognizer.ERROR_CLIENT -> "Erreur client"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions insuffisantes"
                    SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout réseau"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Aucune correspondance"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Occupé"
                    SpeechRecognizer.ERROR_SERVER -> "Erreur serveur"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Pas de parole détectée"
                    else -> "Erreur inconnue ($error)"
                }
                isListening = false
                setState(AIAssistantState.TextInput(errorMsg), "Speech error: $errorMsg")
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                _transcript.value = text
                if (text.isNotEmpty()) {
                    finishListening()
                } else {
                    setState(AIAssistantState.TextInput("Je n'ai rien entendu. Réessayez ou écrivez."),
                        "Empty results")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: return
                _transcript.value = text
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun finishListening() {
        if (!isListening) return
        isListening = false
        speechRecognizer?.stopListening()

        val text = _transcript.value
        if (text.isEmpty()) {
            setState(AIAssistantState.TextInput("Je n'ai rien entendu. Réessayez ou écrivez."),
                "Empty transcript")
        } else {
            processText(text)
        }
    }

    fun processText(text: String) {
        viewModelScope.launch {
            setState(AIAssistantState.Thinking, "Processing: $text")
            try {
                val intent = intentService.parseIntent(text)
                setState(AIAssistantState.Resolving, "Intent: ${intent.type}")
                val resolution = resolver.resolve(intent)
                handleResolution(resolution)
            } catch (e: DeepSeekException) {
                setState(AIAssistantState.Error(e.message ?: "Erreur IA"),
                    "DeepSeek error: ${e.message}")
            } catch (e: Exception) {
                setState(AIAssistantState.Error(
                    "Je n'ai pas pu analyser votre demande. Réessayez."),
                    "Error: ${e.message}")
            }
        }
    }

    private fun handleResolution(resolution: MusicAIResolution) {
        when (resolution) {
            is MusicAIResolution.PlayableTrack -> {
                _playAction.value = AIPlayAction.PlayTrack(resolution.track, resolution.queue)
                setState(AIAssistantState.Playing(resolution.track.title), "Playing track")
            }
            is MusicAIResolution.PlayableAlbum -> {
                val tracks = resolution.tracks
                if (tracks.isNotEmpty()) {
                    _playAction.value = AIPlayAction.PlayTrack(tracks.first(), tracks)
                    setState(AIAssistantState.Playing(tracks.first().title), "Playing album")
                } else {
                    setState(AIAssistantState.Error("L'album est vide."), "Empty album")
                }
            }
            is MusicAIResolution.PlayablePlaylist -> {
                val tracks = resolution.tracks
                if (tracks.isNotEmpty()) {
                    _playAction.value = AIPlayAction.PlayTrack(tracks.first(), tracks)
                    setState(AIAssistantState.Playing(tracks.first().title), "Playing playlist")
                } else {
                    setState(AIAssistantState.Error("La playlist est vide."), "Empty playlist")
                }
            }
            is MusicAIResolution.PlayableRadio -> {
                _playAction.value = AIPlayAction.PlayRadio(resolution.track)
                setState(AIAssistantState.Playing(resolution.track.title), "Radio")
            }
            is MusicAIResolution.AlbumList -> {
                setState(AIAssistantState.ShowingAlbumChoices(
                    "Quel album voulez-vous écouter ?", resolution.albums),
                    "Showing album choices")
            }
            is MusicAIResolution.OpenSearch -> {
                setState(AIAssistantState.Error("Recherche: ${resolution.query}"),
                    "Search resolution")
            }
            is MusicAIResolution.NeedsClarification -> {
                setState(AIAssistantState.TextInput(resolution.message),
                    "Needs clarification")
            }
            is MusicAIResolution.Failure -> {
                setState(AIAssistantState.Error(resolution.message),
                    "Resolution failure")
            }
        }
    }

    fun selectAlbum(album: SongItem) {
        viewModelScope.launch {
            setState(AIAssistantState.Resolving, "User selected: ${album.title}")
            try {
                val browseId = album.browseId
                if (browseId.isNullOrBlank()) {
                    setState(AIAssistantState.Error("Identifiant d'album manquant."),
                        "Missing browseId")
                    return@launch
                }
                val json = innerTubeClient.getPlaylistQueue(browseId)
                val tracks = mapper.mapSearchResults(json)
                if (tracks.isNotEmpty()) {
                    _playAction.value = AIPlayAction.PlayTrack(tracks.first(), tracks)
                    setState(AIAssistantState.Playing(tracks.first().title), "Playing selected album")
                } else {
                    setState(AIAssistantState.Error("Album vide."), "Empty selected album")
                }
            } catch (e: Exception) {
                setState(AIAssistantState.Error(
                    "Impossible d'ouvrir l'album : ${e.message}"),
                    "Album load error")
            }
        }
    }

    fun reset() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
        isStarting = false
        _transcript.value = ""
        _textInput.value = ""
        _playAction.value = null
        setState(AIAssistantState.Idle, "Reset")
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
