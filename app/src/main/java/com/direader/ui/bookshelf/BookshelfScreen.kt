package com.direader.ui.bookshelf

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.direader.ui.theme.*

data class BookUiModel(
    val id: String,
    val title: String,
    val author: String,
    val coverPath: String?,
    val progress: Float,
    val lastReadAt: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookshelfScreen(
    books: List<BookUiModel>,
    onBookClick: (String) -> Unit,
    onImportClick: () -> Unit,
    onDeleteBook: (String) -> Unit
) {
    var bookToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的书架", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                actions = {
                    IconButton(
                        onClick = onImportClick,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "导入书籍",
                            tint = IceBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (books.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "无书籍，请导入EPUB小说",
                        color = TextSecondary,
                        fontSize = 20.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(books) { book ->
                    BookCard(
                        book = book,
                        onClick = { onBookClick(book.id) },
                        onLongClick = { bookToDelete = book.id }
                    )
                }
            }
        }
    }

    bookToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("删除书籍", color = TextPrimary) },
            text = { Text("确定要删除这本书吗？", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBook(id)
                        bookToDelete = null
                    },
                    modifier = Modifier.heightIn(min = 72.dp)
                ) {
                    Text("删除", color = AccentRed, fontSize = 18.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { bookToDelete = null },
                    modifier = Modifier.heightIn(min = 72.dp)
                ) {
                    Text("取消", color = TextPrimary, fontSize = 18.sp)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: BookUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(DarkSurfaceVariant, DarkBackground)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.title.take(1),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary.copy(alpha = 0.5f)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = book.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = IceBlue,
                trackColor = DarkSurfaceVariant
            )
        }
    }
}
