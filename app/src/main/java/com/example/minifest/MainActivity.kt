package com.example.minifest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object TriggerStore {
    var autoMuteOnCharge: Boolean = true
    var lastEvent: String = "Ожидание триггера..."
    var currentVolume: Int = -1
}

fun getCurrentMediaVolume(context: Context): Pair<Int, Int> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    return current to max
}

fun applyChargeRule(context: Context, connected: Boolean) {
    TriggerStore.lastEvent = if (connected) "Зарядка подключена" else "Зарядка отключена"
    if (!TriggerStore.autoMuteOnCharge) return

    try {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = AudioManager.STREAM_MUSIC
        val maxVol = audioManager.getStreamMaxVolume(stream)
        val targetVol = if (connected) 0 else (maxVol / 2).coerceAtLeast(1)

        audioManager.setStreamVolume(stream, targetVol, AudioManager.FLAG_SHOW_UI)
        TriggerStore.currentVolume = audioManager.getStreamVolume(stream)
    } catch (_: Exception) {
    }
}

class PowerTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> applyChargeRule(context, true)
            Intent.ACTION_POWER_DISCONNECTED -> applyChargeRule(context, false)
        }
    }
}

class MainActivity : ComponentActivity() {
    private val receiver = PowerTriggerReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }

        setContent {
            MaterialTheme {
                TriggerScreen(this)
            }
        }
    }override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}

@Composable
fun TriggerScreen(context: Context) {
    var isEnabled by remember { mutableStateOf(TriggerStore.autoMuteOnCharge) }
    var statusText by remember { mutableStateOf(TriggerStore.lastEvent) }
    var liveVol by remember { mutableStateOf(getCurrentMediaVolume(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            statusText = TriggerStore.lastEvent
            liveVol = getCurrentMediaVolume(context)
            delay(300)
        }
    }

    val bgColor = Color(0xFF10131A)
    val cardColor = Color(0xFF1B202C)
    val accentColor = Color(0xFF38BDF8)
    val textMain = Color(0xFFF8FAFC)
    val textMuted = Color(0xFF94A3B8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "TRIGGER ENGINE",
            color = accentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Автоматизатор событий",
            color = textMain,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Сценарий: состояние питания -> уровень громкости",
            color = textMuted,
            style = MaterialTheme.typography.bodyMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (isEnabled) Color(0xFF22C55E) else Color(0xFF64748B),
                                shape = CircleShape
                            )
                    )
                    Column {
                        Text(
                            text = if (isEnabled) "Правило включено" else "Правило на паузе",
                            color = textMain,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Переключатель триггера",
                            color = textMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        isEnabled = checked
                        TriggerStore.autoMuteOnCharge = checked
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accentColor
                    )
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "СОСТОЯНИЕ И ЛОГ",
                    color = accentColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Событие: $statusText",
                    color = textMain
                )
                Text(
                    text = "Текущая громкость медиа: ${liveVol.first} из ${liveVol.second}",
                    color = textMuted
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "РУЧНАЯ ПРОВЕРКА",
                    color = accentColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        applyChargeRule(context, true)
                        statusText = TriggerStore.lastEvent
                        liveVol = getCurrentMediaVolume(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color(0xFF081018)
                    )
                ) {
                    Text("Симулировать подключение зарядки", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        applyChargeRule(context, false)
                        statusText = TriggerStore.lastEvent
                        liveVol = getCurrentMediaVolume(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Симулировать отключение зарядки", color = textMain)
                }
            }
        }
    }
}