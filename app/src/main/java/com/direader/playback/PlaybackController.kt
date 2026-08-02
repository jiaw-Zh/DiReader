package com.direader.playback

import com.direader.data.AppDatabase
import com.direader.model.Chapter
import com.direader.model.ReadingProgress
import com.direader.tts.AudioPlayer
import com.direader.tts.SentenceSplitter
import com.direader.tts.TtsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Orchestrates playback, TTS synthesis, audio playing, and progress saving.
 */
class PlaybackController(
    private val ttsEngine: TtsEngine,
    private val audioPlayer: AudioPlayer,
    private val database: AppDatabase
) {
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var playbackJob: Job? = null
    
    private var currentChapters = listOf<Chapter>()
    private var currentSentences = listOf<String>()
    
    // Double buffering state
    private var preSynthesizeJob: Job? = null
    private var nextAudioFile: File? = null

    init {
        audioPlayer.setOnCompletionListener {
            nextSentence()
        }
    }

    /**
     * Load a book and its chapters to start playback.
     */
    fun loadBook(bookId: String, bookTitle: String, chapters: List<Chapter>) {
        if (chapters.isEmpty()) return
        
        currentChapters = chapters
        
        scope.launch {
            val progress = database.progressDao().getProgress(bookId)
            val startChapterIndex = progress?.chapterIndex ?: 0
            val startSentenceIndex = progress?.sentenceIndex ?: 0
            
            val validChapterIndex = if (startChapterIndex in chapters.indices) startChapterIndex else 0
            val chapter = chapters[validChapterIndex]
            
            _state.update { 
                it.copy(
                    status = PlaybackStatus.LOADING,
                    bookId = bookId,
                    bookTitle = bookTitle,
                    chapterIndex = validChapterIndex,
                    chapterTitle = chapter.title,
                    totalChapters = chapters.size
                )
            }
            
            loadChapter(validChapterIndex, startSentenceIndex)
        }
    }
    
    private suspend fun loadChapter(chapterIndex: Int, startSentenceIndex: Int = 0) {
        val chapter = currentChapters[chapterIndex]
        currentSentences = SentenceSplitter.split(chapter.content)
        
        val validSentenceIndex = if (startSentenceIndex in currentSentences.indices) startSentenceIndex else 0
        
        _state.update {
            it.copy(
                chapterIndex = chapterIndex,
                chapterTitle = chapter.title,
                sentenceIndex = validSentenceIndex,
                totalSentences = currentSentences.size,
                currentText = if (currentSentences.isNotEmpty()) currentSentences[validSentenceIndex] else "",
                status = PlaybackStatus.PAUSED
            )
        }
        
        nextAudioFile = null
        preSynthesizeJob?.cancel()
    }

    /**
     * Start or resume playing.
     */
    fun play() {
        if (_state.value.status == PlaybackStatus.PLAYING) return
        if (currentSentences.isEmpty()) return
        
        playbackJob?.cancel()
        playbackJob = scope.launch {
            _state.update { it.copy(status = PlaybackStatus.PLAYING) }
            playCurrentSentence()
        }
    }

    /**
     * Pause playing.
     */
    fun pause() {
        _state.update { it.copy(status = PlaybackStatus.PAUSED) }
        audioPlayer.pause()
        playbackJob?.cancel()
    }

    /**
     * Skip to the next sentence. Handles chapter boundaries.
     */
    fun nextSentence() {
        scope.launch {
            val currentState = _state.value
            val nextIndex = currentState.sentenceIndex + 1
            
            if (nextIndex < currentSentences.size) {
                // Next sentence in current chapter
                _state.update { 
                    it.copy(
                        sentenceIndex = nextIndex,
                        currentText = currentSentences[nextIndex]
                    )
                }
                saveProgress()
                if (currentState.status == PlaybackStatus.PLAYING) {
                    playCurrentSentence()
                }
            } else {
                // Next chapter
                if (currentState.chapterIndex + 1 < currentChapters.size) {
                    jumpToChapter(currentState.chapterIndex + 1)
                } else {
                    // End of book
                    _state.update { it.copy(status = PlaybackStatus.IDLE) }
                }
            }
        }
    }

    /**
     * Skip to the previous sentence. Handles chapter boundaries.
     */
    fun previousSentence() {
        scope.launch {
            val currentState = _state.value
            val prevIndex = currentState.sentenceIndex - 1
            
            if (prevIndex >= 0) {
                // Previous sentence in current chapter
                _state.update { 
                    it.copy(
                        sentenceIndex = prevIndex,
                        currentText = currentSentences[prevIndex]
                    )
                }
                nextAudioFile = null
                saveProgress()
                if (currentState.status == PlaybackStatus.PLAYING) {
                    playCurrentSentence()
                }
            } else {
                // Previous chapter
                if (currentState.chapterIndex > 0) {
                    val prevChapterIndex = currentState.chapterIndex - 1
                    val prevChapter = currentChapters[prevChapterIndex]
                    val prevSentences = SentenceSplitter.split(prevChapter.content)
                    
                    loadChapter(prevChapterIndex, (prevSentences.size - 1).coerceAtLeast(0))
                    saveProgress()
                    if (currentState.status == PlaybackStatus.PLAYING) {
                        playCurrentSentence()
                    }
                }
            }
        }
    }

    /**
     * Jump to a specific chapter index.
     */
    fun jumpToChapter(index: Int) {
        if (index !in currentChapters.indices) return
        
        val wasPlaying = _state.value.status == PlaybackStatus.PLAYING
        
        scope.launch {
            _state.update { it.copy(status = PlaybackStatus.LOADING) }
            loadChapter(index, 0)
            saveProgress()
            if (wasPlaying) {
                _state.update { it.copy(status = PlaybackStatus.PLAYING) }
                playCurrentSentence()
            }
        }
    }

    /**
     * Change the TTS voice.
     */
    fun setVoice(voiceId: String) {
        _state.update { it.copy(voiceId = voiceId) }
        nextAudioFile = null
        if (_state.value.status == PlaybackStatus.PLAYING) {
            playCurrentSentence()
        }
    }

    /**
     * Change the playback speed.
     */
    fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        nextAudioFile = null
        if (_state.value.status == PlaybackStatus.PLAYING) {
            playCurrentSentence()
        }
    }
    
    private suspend fun playCurrentSentence() {
        val currentState = _state.value
        val text = currentState.currentText
        if (text.isEmpty()) {
            nextSentence()
            return
        }
        
        try {
            val audioToPlay = nextAudioFile ?: ttsEngine.synthesize(text, currentState.voiceId, currentState.speed)
            
            if (audioToPlay != null) {
                audioPlayer.play(audioToPlay.absolutePath)
                
                // Pre-synthesize next sentence (Double buffer)
                preSynthesizeJob?.cancel()
                val nextIdx = currentState.sentenceIndex + 1
                if (nextIdx < currentSentences.size) {
                    preSynthesizeJob = scope.launch {
                        nextAudioFile = ttsEngine.synthesize(
                            currentSentences[nextIdx],
                            currentState.voiceId,
                            currentState.speed
                        )
                    }
                } else {
                    nextAudioFile = null
                }
            } else {
                _state.update { 
                    it.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = "Synthesis failed"
                    )
                }
            }
        } catch (e: Exception) {
            _state.update { 
                it.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = e.message ?: "Unknown error"
                )
            }
        }
    }
    
    private fun saveProgress() {
        val currentState = _state.value
        if (currentState.bookId.isEmpty()) return
        
        scope.launch {
            database.progressDao().insertProgress(
                ReadingProgress(
                    bookId = currentState.bookId,
                    chapterIndex = currentState.chapterIndex,
                    sentenceIndex = currentState.sentenceIndex,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Stop playback and release resources.
     */
    fun release() {
        audioPlayer.release()
        playbackJob?.cancel()
        preSynthesizeJob?.cancel()
    }
}
