package com.direader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.direader.MainViewModel
import com.direader.playback.PlaybackStatus
import com.direader.ui.bookshelf.BookshelfScreen
import com.direader.ui.chapters.ChapterListDialog
import com.direader.ui.player.PlayerScreen
import com.direader.ui.settings.SettingsScreen
import com.direader.ui.settings.VoiceOption

/**
 * 应用导航路由定义。
 */
object Routes {
    const val BOOKSHELF = "bookshelf"
    const val PLAYER = "player/{bookId}"
    const val SETTINGS = "settings"

    fun playerRoute(bookId: String) = "player/$bookId"
}

/**
 * 应用主导航。
 */
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.BOOKSHELF
    ) {
        // 书架页
        composable(Routes.BOOKSHELF) {
            val books by viewModel.books.collectAsState()
            val importStatus by viewModel.importStatus.collectAsState()
            val isModelMissing by viewModel.isModelMissing.collectAsState()

            BookshelfScreen(
                books = books,
                isModelMissing = isModelMissing,
                onBookClick = { bookId ->
                    viewModel.openBook(bookId)
                    navController.navigate(Routes.playerRoute(bookId))
                },
                onImportClick = {
                    viewModel.requestFileImport()
                },
                onDeleteBook = { bookId ->
                    viewModel.deleteBook(bookId)
                },
                onRefreshModelCheck = {
                    viewModel.checkModelExists()
                }
            )
        }

        // 播放页
        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) {
            val playbackState by viewModel.playbackState.collectAsState()
            val sentences by viewModel.sentences.collectAsState()
            val showChapterDialog by viewModel.showChapterDialog.collectAsState()
            val chapterModels by viewModel.chapterUiModels.collectAsState()

            PlayerScreen(
                state = playbackState,
                sentences = sentences,
                onPlayPause = { viewModel.playPause() },
                onNextSentence = { viewModel.nextSentence() },
                onPrevSentence = { viewModel.previousSentence() },
                onSpeedChange = { viewModel.setSpeed(it) },
                onVoiceChange = { viewModel.setVoice(it) },
                onChapterSelect = { viewModel.showChapterList() },
                onAddBookmark = { viewModel.addBookmark() },
                onBack = { navController.popBackStack() }
            )

            // 章节列表对话框
            if (showChapterDialog) {
                ChapterListDialog(
                    chapters = chapterModels,
                    currentChapterIndex = playbackState.chapterIndex,
                    onChapterSelect = { index ->
                        viewModel.jumpToChapter(index)
                        viewModel.hideChapterList()
                    },
                    onDismiss = { viewModel.hideChapterList() }
                )
            }
        }

        // 设置页
        composable(Routes.SETTINGS) {
            val playbackState by viewModel.playbackState.collectAsState()
            SettingsScreen(
                currentVoice = playbackState.voiceId,
                currentSpeed = playbackState.speed,
                voices = listOf(
                    VoiceOption("zf_xiaobei", "小北女声"),
                    VoiceOption("zm_yunjian", "云间男声")
                ),
                onVoiceChange = { viewModel.setVoice(it) },
                onSpeedChange = { viewModel.setSpeed(it) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
