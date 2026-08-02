package com.direader.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.direader.playback.PlaybackState
import com.direader.playback.PlaybackStatus
import com.direader.ui.theme.*

@Composable
fun PlayerScreen(
    state: PlaybackState,
    sentences: List<String>,
    onPlayPause: () -> Unit,
    onNextSentence: () -> Unit,
    onPrevSentence: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVoiceChange: (String) -> Unit,
    onChapterSelect: () -> Unit,
    onAddBookmark: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(state.sentenceIndex) {
        if (state.sentenceIndex in sentences.indices) {
            listState.animateScrollToItem(state.sentenceIndex)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Left Panel (60%)
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            Text(
                text = state.chapterTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(sentences) { index, sentence ->
                    val isCurrent = index == state.sentenceIndex
                    val isPast = index < state.sentenceIndex
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCurrent) IceBlue.copy(alpha = 0.2f) else Color.Transparent)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = sentence,
                            fontSize = 20.sp,
                            color = when {
                                isCurrent -> Color.White
                                isPast -> TextSecondary
                                else -> TextPrimary
                            }
                        )
                    }
                }
            }
        }
        
        Divider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = DarkSurfaceVariant
        )
        
        // Right Panel (40%)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.bookTitle,
                fontSize = 20.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "章节进度: ${state.chapterIndex + 1}/${state.totalChapters}",
                fontSize = 16.sp,
                color = TextSecondary
            )
            Text(
                text = "句子进度: ${state.sentenceIndex + 1}/${state.totalSentences}",
                fontSize = 16.sp,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            val isPlaying = state.status == PlaybackStatus.PLAYING
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isPlaying) IceBlue else Color.Transparent,
                    contentColor = if (isPlaying) Color.White else IceBlue
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = onPrevSentence, modifier = Modifier.size(72.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一句", tint = TextPrimary, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = onNextSentence, modifier = Modifier.size(72.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一句", tint = TextPrimary, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = onChapterSelect, modifier = Modifier.size(72.dp)) {
                    Icon(Icons.Default.List, contentDescription = "章节列表", tint = TextPrimary, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = onAddBookmark, modifier = Modifier.size(72.dp)) {
                    Icon(Icons.Default.Star, contentDescription = "添加书签", tint = TextPrimary, modifier = Modifier.size(36.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Speed Control
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("语速: ${String.format("%.1f", state.speed)}x", color = TextPrimary, fontSize = 16.sp)
                Slider(
                    value = state.speed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = IceBlue,
                        activeTrackColor = IceBlue,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Voice Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("zf_xiaobei" to "小北女声", "zm_yunjian" to "云间男声").forEach { (id, name) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.heightIn(min = 72.dp)
                    ) {
                        RadioButton(
                            selected = state.voiceId == id,
                            onClick = { onVoiceChange(id) },
                            modifier = Modifier.size(48.dp),
                            colors = RadioButtonDefaults.colors(
                                selectedColor = IceBlue,
                                unselectedColor = TextSecondary
                            )
                        )
                        Text(text = name, color = TextPrimary, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("返回书架", fontSize = 18.sp)
            }
        }
    }
}
