package com.example.minifest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.widget.Toast

object TriggerStore {
    var autoMuteOnCharge: Boolean = true
    var lastEvent: String = "Ожидание триггера..."
}

class PowerTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                TriggerStore.lastEvent = "Зарядка подключена"
                if (TriggerStore.autoMuteOnCharge) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    Toast.makeText(context, "Триггер: беззвучный режим включён", Toast.LENGTH_SHORT).show()
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                TriggerStore.lastEvent = "Зарядка отключена"
                if (TriggerStore.autoMuteOnCharge) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    Toast.makeText(context, "Триггер: обычный режим возвращён", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}