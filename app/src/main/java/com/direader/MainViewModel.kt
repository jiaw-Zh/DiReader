package com.direader

import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.direader.data.AppDatabase
import com.direader.epub.EpubParser
import com.direader.model.Book
import com.direader.model.Chapter
import com.direader.playback.PlaybackController
import com.direader.playback.PlaybackState
import com.direader.playback.PlaybackStatus
import com.direader.tts.*
import com.direader.ui.bookshelf.BookUiModel
import com.direader.ui.chapters.ChapterUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * 主 ViewModel，桥接 UI 层和业务逻辑层。
 * 管理书架状态、EPUB 导入、播放控制。
 */
class MainViewModel : ViewModel() {

    private val database: AppDatabase = DiReaderApp.instance.database
    private val epubParser = EpubParser()

    // 真实 Sherpa-ONNX C++ 本地离线引擎
    private val ttsEngine: TtsEngine = SherpaOnnxTtsEngine()
    private val audioPlayer = AudioPlayer()
    private val playbackController = PlaybackController(ttsEngine, audioPlayer, database)

    // 文件导入回调（由 MainActivity 设置）
    var onRequestFileImport: (() -> Unit)? = null

    // 文件管理权限请求回调（由 MainActivity 设置）
    var onRequestAllFilesPermission: (() -> Unit)? = null

    fun requestFileImport() {
        onRequestFileImport?.invoke()
    }

    fun requestAllFilesPermission() {
        onRequestAllFilesPermission?.invoke()
    }

    // ---- 书架状态 ----

    val books: StateFlow<List<BookUiModel>> = database.bookDao().getAllBooks()
        .map { list ->
            list.map { book ->
                val progress = database.progressDao().getProgress(book.id)
                val chapterProgress = if (book.totalChapters > 0 && progress != null) {
                    progress.chapterIndex.toFloat() / book.totalChapters
                } else 0f
                BookUiModel(
                    id = book.id,
                    title = book.title,
                    author = book.author,
                    coverPath = book.coverPath,
                    progress = chapterProgress,
                    lastReadAt = formatTimestamp(book.lastReadAt)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- 播放状态 ----

    val playbackState: StateFlow<PlaybackState> = playbackController.state

    // 当前章节的所有句子（供 UI 展示）
    private val _sentences = MutableStateFlow<List<String>>(emptyList())
    val sentences: StateFlow<List<String>> = _sentences.asStateFlow()

    // 当前书籍的章节列表
    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters: StateFlow<List<Chapter>> = _chapters.asStateFlow()

    val chapterUiModels: StateFlow<List<ChapterUiModel>> = _chapters.map { list ->
        list.map { ChapterUiModel(it.index, it.title) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 是否显示章节列表对话框
    private val _showChapterDialog = MutableStateFlow(false)
    val showChapterDialog: StateFlow<Boolean> = _showChapterDialog.asStateFlow()

    // 导入状态
    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    // 模型缺失状态
    private val _isModelMissing = MutableStateFlow(false)
    val isModelMissing: StateFlow<Boolean> = _isModelMissing.asStateFlow()

    init {
        checkModelExists()
        viewModelScope.launch {
            // 初始化 TTS 引擎
            val modelDir = File(
                Environment.getExternalStorageDirectory(),
                "DiReader/models"
            ).absolutePath
            try {
                ttsEngine.initialize(modelDir)
            } catch (e: Exception) {
                // Stub 引擎不需要真实模型目录
            }
            audioPlayer.initialize()
        }

        // 监听播放状态变化，同步句子列表
        viewModelScope.launch {
            playbackController.state.collect { state ->
                if (state.status != PlaybackStatus.IDLE && _chapters.value.isNotEmpty()) {
                    val chapterIndex = state.chapterIndex.coerceIn(0, _chapters.value.size - 1)
                    val chapter = _chapters.value[chapterIndex]
                    val split = com.direader.tts.SentenceSplitter.split(chapter.text)
                    _sentences.value = split
                }
            }
        }
    }

    // ---- 书架操作 ----

    /**
     * 导入 EPUB 文件。
     * @param uri 文件 URI（来自 SAF 文件选择器）
     */
    fun importBook(uri: Uri) {
        viewModelScope.launch {
            _importStatus.value = "正在导入..."
            try {
                withContext(Dispatchers.IO) {
                    // 将文件拷贝到应用内部存储
                    val context = DiReaderApp.instance
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("无法读取文件")
                    val booksDir = File(context.filesDir, "books")
                    booksDir.mkdirs()

                    // 生成唯一文件名
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    val fileHash = sha256(bytes)
                    val destFile = File(booksDir, "$fileHash.epub")
                    destFile.writeBytes(bytes)

                    // 解析 EPUB
                    val parsed = epubParser.parse(destFile)

                    // 保存封面图片
                    var coverPath: String? = null
                    if (parsed.coverData != null) {
                        val coversDir = File(context.filesDir, "covers")
                        coversDir.mkdirs()
                        val coverFile = File(coversDir, "$fileHash.jpg")
                        coverFile.writeBytes(parsed.coverData)
                        coverPath = coverFile.absolutePath
                    }

                    // 插入数据库
                    val book = Book(
                        id = fileHash,
                        title = parsed.title,
                        author = parsed.author,
                        filePath = destFile.absolutePath,
                        coverPath = coverPath,
                        totalChapters = parsed.chapters.size
                    )
                    database.bookDao().insertBook(book)
                }
                _importStatus.value = "导入成功"
            } catch (e: Exception) {
                _importStatus.value = "导入失败: ${e.message}"
            }
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val book = database.bookDao().getBook(bookId) ?: return@launch
            // 删除文件
            File(book.filePath).delete()
            book.coverPath?.let { File(it).delete() }
            // 删除数据库记录
            database.bookDao().deleteBook(book)
        }
    }

    fun clearImportStatus() {
        _importStatus.value = null
    }

    // ---- 播放操作 ----

    /**
     * 打开一本书并准备播放。
     */
    fun openBook(bookId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val book = database.bookDao().getBook(bookId) ?: return@launch
            database.bookDao().updateLastReadAt(bookId, System.currentTimeMillis())

            // 重新解析 EPUB 获取章节内容
            val parsed = epubParser.parse(File(book.filePath))
            val chapters = parsed.chapters.map { pc ->
                Chapter(index = pc.index, title = pc.title, text = pc.text)
            }
            _chapters.value = chapters

            // 加载到播放控制器
            playbackController.loadBook(bookId, book.title, chapters)
        }
    }

    fun playPause() {
        val state = playbackState.value
        when (state.status) {
            PlaybackStatus.PLAYING -> playbackController.pause()
            PlaybackStatus.PAUSED -> playbackController.play()
            PlaybackStatus.IDLE -> {
                if (_chapters.value.isNotEmpty()) {
                    playbackController.play()
                }
            }
            else -> { /* LOADING / ERROR: ignore */ }
        }
    }

    fun nextSentence() = playbackController.nextSentence()
    fun previousSentence() = playbackController.previousSentence()
    fun jumpToChapter(index: Int) = playbackController.jumpToChapter(index)

    fun setSpeed(speed: Float) = playbackController.setSpeed(speed)
    fun setVoice(voiceId: String) = playbackController.setVoice(voiceId)

    fun showChapterList() { _showChapterDialog.value = true }
    fun hideChapterList() { _showChapterDialog.value = false }

    fun addBookmark() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = playbackState.value
            val bookId = state.bookId ?: return@launch
            val bookmark = com.direader.model.Bookmark(
                bookId = bookId,
                chapterIndex = state.chapterIndex,
                sentenceIndex = state.sentenceIndex
            )
            database.progressDao().addBookmark(bookmark)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackController.release()
        audioPlayer.release()
        ttsEngine.release()
    }

    // ---- 工具方法 ----

    fun checkModelExists() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = DiReaderApp.instance
            val publicDir = File(Environment.getExternalStorageDirectory(), "DiReader/models")
            val privateDir = File(context.getExternalFilesDir(null), "models")

            val hasPublicModel = publicDir.exists() && hasOnnx(publicDir)
            val hasPrivateModel = privateDir != null && privateDir.exists() && hasOnnx(privateDir)

            _isModelMissing.value = !(hasPublicModel || hasPrivateModel)
        }
    }

    private fun hasOnnx(dir: File): Boolean {
        return try {
            dir.walkTopDown().maxDepth(5).any { it.isFile && it.extension.equals("onnx", ignoreCase = true) }
        } catch (e: Exception) {
            false
        }
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
