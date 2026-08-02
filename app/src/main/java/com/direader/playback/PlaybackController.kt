package com.direader.playback

import com.direader.data.AppDatabase
import com.direader.model.Chapter
import com.direader.model.ReadingProgress
import com.direader.tts.AudioPlayer
import com.direader.tts.SentenceSplitter
import com.direader.tts.TtsEngine
import com.direader.tts.TtsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 播放控制器：统一调度 TTS 合成、AudioTrack 播放、双缓冲预合成以及进度持久化。
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

    // 双缓冲预合成状态
    private var preSynthesizeJob: Job? = null
    private var nextTtsResult: TtsResult? = null

    init {
        audioPlayer.onPlaybackComplete = {
            nextSentence()
        }
    }

    /**
     * 加载一本书及其章节并初始化状态。
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
        currentSentences = SentenceSplitter.split(chapter.text)

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

        nextTtsResult = null
        preSynthesizeJob?.cancel()
    }

    /**
     * 开始或恢复播放。
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
     * 暂停播放。
     */
    fun pause() {
        _state.update { it.copy(status = PlaybackStatus.PAUSED) }
        audioPlayer.pause()
        playbackJob?.cancel()
    }

    /**
     * 跳转到下一句。
     */
    fun nextSentence() {
        scope.launch {
            val currentState = _state.value
            val nextIndex = currentState.sentenceIndex + 1

            if (nextIndex < currentSentences.size) {
                // 当前章节下一句
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
                // 下一章节
                if (currentState.chapterIndex + 1 < currentChapters.size) {
                    jumpToChapter(currentState.chapterIndex + 1)
                } else {
                    // 读完整本书
                    _state.update { it.copy(status = PlaybackStatus.IDLE) }
                }
            }
        }
    }

    /**
     * 跳转到上一句。
     */
    fun previousSentence() {
        scope.launch {
            val currentState = _state.value
            val prevIndex = currentState.sentenceIndex - 1

            if (prevIndex >= 0) {
                // 当前章节上一句
                _state.update {
                    it.copy(
                        sentenceIndex = prevIndex,
                        currentText = currentSentences[prevIndex]
                    )
                }
                nextTtsResult = null
                saveProgress()
                if (currentState.status == PlaybackStatus.PLAYING) {
                    playCurrentSentence()
                }
            } else {
                // 上一章节
                if (currentState.chapterIndex > 0) {
                    val prevChapterIndex = currentState.chapterIndex - 1
                    val prevChapter = currentChapters[prevChapterIndex]
                    val prevSentences = SentenceSplitter.split(prevChapter.text)

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
     * 跳转到指定章节。
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
     * 切换音色。
     */
    fun setVoice(voiceId: String) {
        _state.update { it.copy(voiceId = voiceId) }
        nextTtsResult = null
        if (_state.value.status == PlaybackStatus.PLAYING) {
            scope.launch { playCurrentSentence() }
        }
    }

    /**
     * 调节语速。
     */
    fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        nextTtsResult = null
        if (_state.value.status == PlaybackStatus.PLAYING) {
            scope.launch { playCurrentSentence() }
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
            val audioToPlay = nextTtsResult ?: ttsEngine.synthesize(text, currentState.voiceId, currentState.speed)
            nextTtsResult = null

            if (audioToPlay != null && audioToPlay.samples.isNotEmpty()) {
                // 异步预合成下一句（双缓冲机制）
                preSynthesizeJob?.cancel()
                val nextIdx = currentState.sentenceIndex + 1
                if (nextIdx < currentSentences.size) {
                    preSynthesizeJob = scope.launch {
                        nextTtsResult = ttsEngine.synthesize(
                            currentSentences[nextIdx],
                            currentState.voiceId,
                            currentState.speed
                        )
                    }
                }

                // 播放当前句 PCM
                audioPlayer.play(audioToPlay.samples)
            } else {
                _state.update {
                    it.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = "合成失败"
                    )
                }
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    status = PlaybackStatus.ERROR,
                    errorMessage = e.message ?: "未知错误"
                )
            }
        }
    }

    private fun saveProgress() {
        val currentState = _state.value
        if (currentState.bookId.isEmpty()) return

        scope.launch {
            database.progressDao().saveProgress(
                ReadingProgress(
                    bookId = currentState.bookId,
                    chapterIndex = currentState.chapterIndex,
                    sentenceIndex = currentState.sentenceIndex,
                    voiceId = currentState.voiceId,
                    speed = currentState.speed,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * 释放所有资源。
     */
    fun release() {
        audioPlayer.release()
        playbackJob?.cancel()
        preSynthesizeJob?.cancel()
    }
}
