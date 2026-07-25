package fr.leretourdelabete.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

data class AudioRouteState(
    val label: String = "Haut-parleur du téléphone",
    val external: Boolean = false,
)

class AudioRouteMonitor(
    context: Context,
    private val onChanged: (AudioRouteState) -> Unit,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var registered = false

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            onChanged(current())
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            onChanged(current())
        }
    }

    fun start() {
        if (registered) return
        audioManager.registerAudioDeviceCallback(callback, null)
        registered = true
        onChanged(current())
    }

    fun stop() {
        if (!registered) return
        audioManager.unregisterAudioDeviceCallback(callback)
        registered = false
    }

    fun current(): AudioRouteState {
        val outputs = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }.getOrDefault(emptyList())

        val bleSpeaker = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER }
        } else {
            null
        }
        val preferred = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP }
            ?: bleSpeaker
            ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_DEVICE }
            ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_USB_HEADSET }
            ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES }
            ?: outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET }

        return if (preferred != null) {
            val name = runCatching { preferred.productName.toString() }
                .getOrDefault("Sortie audio externe")
            AudioRouteState(label = name, external = true)
        } else {
            AudioRouteState()
        }
    }
}
