package com.direader.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.direader.ui.theme.*

data class VoiceOption(
    val id: String,
    val displayName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentVoice: String,
    currentSpeed: Float,
    voices: List<VoiceOption>,
    onVoiceChange: (String) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Voice Selection
            Column {
                Text("声音选择", fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    voices.forEach { voice ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.heightIn(min = 72.dp)
                        ) {
                            RadioButton(
                                selected = currentVoice == voice.id,
                                onClick = { onVoiceChange(voice.id) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = IceBlue,
                                    unselectedColor = TextSecondary
                                ),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = voice.displayName,
                                fontSize = 18.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = DarkSurfaceVariant)

            // Speed Control
            Column {
                Text("语速调节 (${String.format("%.1f", currentSpeed)}x)", fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = currentSpeed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = IceBlue,
                        activeTrackColor = IceBlue,
                        inactiveTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(72.dp)
                )
            }

            HorizontalDivider(color = DarkSurfaceVariant)

            // Model Path (Read-only)
            Column {
                Text("模型路径", fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "/sdcard/DiReader/models/piper/",
                    fontSize = 16.sp,
                    color = TextSecondary
                )
            }
            
            HorizontalDivider(color = DarkSurfaceVariant)
            
            // App Version
            Column {
                Text("应用版本", fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "v1.0.1",
                    fontSize = 16.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
