package com.creativeblisslabs.peqlearn.app.midi

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.creativeblisslabs.peqlearn.app.ui.screens.getNoteNameFromMidi
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel
import kotlin.math.pow

class AndroidMidiHandler(
    private val context: Context,
    private val viewModel: AppViewModel
) {
    companion object {
        private const val ACTION_USB_MIDI_PERMISSION = "com.creativeblisslabs.peqlearn.USB_MIDI_PERMISSION"
    }

    private val midiManager: MidiManager? = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
    private val usbManager: UsbManager? = context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val handler = Handler(Looper.getMainLooper())
    private val openDevices = mutableMapOf<MidiDeviceInfo, MidiDevice>()
    private val openPorts = mutableListOf<MidiOutputPort>()

    private var usbReceiver: BroadcastReceiver? = null

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            val name = resolveDeviceName(device)
            handler.post {
                viewModel.connectedMidiDeviceName.value = name
                viewModel.showMidiPrompt(
                    title = "MIDI Keyboard Connected",
                    message = "Connected to \"$name\". Real-time note input, 3-pedal sensing (Sustain, Sostenuto, Soft), and velocity are active.",
                    isConnected = true,
                    deviceName = name
                )
            }
            updateDeviceList()
            openDevice(device)
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            val name = resolveDeviceName(device)
            openDevices.remove(device)?.let { runCatching { it.close() } }
            handler.post {
                // Release any stuck notes or pedals on disconnect
                viewModel.midiActiveNotes.clear()
                viewModel.midiPlayingNote.value = null
                viewModel.sustainPedalActive.value = false
                viewModel.sostenutoPedalActive.value = false
                viewModel.softPedalActive.value = false
                if (openDevices.isEmpty()) {
                    viewModel.connectedMidiDeviceName.value = null
                }
                viewModel.showMidiPrompt(
                    title = "MIDI Keyboard Disconnected",
                    message = "Disconnected from \"$name\". Reconnect your USB/MIDI cable to resume playing.",
                    isConnected = false,
                    deviceName = name
                )
            }
            updateDeviceList()
        }
    }

    fun start() {
        registerUsbReceiver()
        checkConnectedUsbMidiDevices()

        midiManager?.let { mm ->
            mm.registerDeviceCallback(deviceCallback, handler)
            updateDeviceList()
            mm.devices?.forEach { info ->
                if (info != null) {
                    openDevice(info)
                }
            }
        }
    }

    fun stop() {
        unregisterUsbReceiver()

        try {
            midiManager?.unregisterDeviceCallback(deviceCallback)
        } catch (_: Exception) {}

        openPorts.forEach { port ->
            runCatching { port.close() }
        }
        openPorts.clear()

        openDevices.values.forEach { device ->
            runCatching { device.close() }
        }
        openDevices.clear()
    }

    private fun requestUsbPermissionIfNeeded(device: UsbDevice) {
        val um = usbManager ?: return
        if (!um.hasPermission(device)) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pi = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_MIDI_PERMISSION), flags)
            runCatching { um.requestPermission(device, pi) }
        }
    }

    private fun registerUsbReceiver() {
        if (usbReceiver != null) return

        usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED, ACTION_USB_MIDI_PERMISSION -> {
                        val device = getUsbDeviceFromIntent(intent)
                        if (device != null && isMidiUsbDevice(device)) {
                            requestUsbPermissionIfNeeded(device)
                            val name = getUsbDeviceCleanName(device)
                            handler.post {
                                viewModel.connectedMidiDeviceName.value = name
                                if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                                    viewModel.showMidiPrompt(
                                        title = "USB Piano Attached",
                                        message = "Hardware USB Piano \"$name\" detected! Initializing MIDI data stream...",
                                        isConnected = true,
                                        deviceName = name
                                    )
                                }
                            }
                            // Poll briefly to allow Android MidiManager to enumerate the new USB interface
                            handler.postDelayed({
                                updateDeviceList()
                                midiManager?.devices?.forEach { info ->
                                    if (info != null && !openDevices.containsKey(info)) {
                                        openDevice(info)
                                    }
                                }
                            }, 250L)
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        val device = getUsbDeviceFromIntent(intent)
                        if (device != null && isMidiUsbDevice(device)) {
                            val name = getUsbDeviceCleanName(device)
                            handler.post {
                                viewModel.midiActiveNotes.clear()
                                viewModel.midiPlayingNote.value = null
                                viewModel.sustainPedalActive.value = false
                                viewModel.sostenutoPedalActive.value = false
                                viewModel.softPedalActive.value = false
                                if (openDevices.isEmpty()) {
                                    viewModel.connectedMidiDeviceName.value = null
                                }
                                viewModel.showMidiPrompt(
                                    title = "USB Piano Detached",
                                    message = "USB Piano \"$name\" unplugged.",
                                    isConnected = false,
                                    deviceName = name
                                )
                            }
                            updateDeviceList()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_MIDI_PERMISSION)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(usbReceiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterUsbReceiver() {
        usbReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
            usbReceiver = null
        }
    }

    private fun getUsbDeviceFromIntent(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    private fun isMidiUsbDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_AUDIO) return true
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_AUDIO) return true
        }
        return false
    }

    private fun getUsbDeviceCleanName(device: UsbDevice): String {
        val prod = runCatching { device.productName }.getOrNull()
        val mfg = runCatching { device.manufacturerName }.getOrNull()
        return when {
            !prod.isNullOrBlank() && !mfg.isNullOrBlank() -> {
                if (prod.startsWith(mfg, ignoreCase = true)) prod else "$mfg $prod"
            }
            !prod.isNullOrBlank() -> prod
            !mfg.isNullOrBlank() -> "$mfg MIDI Keyboard"
            else -> "USB MIDI Keyboard (${device.deviceName})"
        }
    }

    private fun checkConnectedUsbMidiDevices() {
        val deviceList = usbManager?.deviceList ?: return
        for (device in deviceList.values) {
            if (isMidiUsbDevice(device)) {
                requestUsbPermissionIfNeeded(device)
                val name = getUsbDeviceCleanName(device)
                handler.post {
                    if (viewModel.connectedMidiDeviceName.value == null) {
                        viewModel.connectedMidiDeviceName.value = name
                    }
                }
                break
            }
        }
    }

    private fun resolveDeviceName(info: MidiDeviceInfo): String {
        val prop = info.properties
        val name = prop.getString(MidiDeviceInfo.PROPERTY_NAME)
        val manufacturer = prop.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) ?: ""
        val prod = prop.getString(MidiDeviceInfo.PROPERTY_PRODUCT)

        val resolved = when {
            !name.isNullOrBlank() && !manufacturer.isNullOrBlank() -> {
                if (name.startsWith(manufacturer, ignoreCase = true)) name else "$manufacturer $name"
            }
            !name.isNullOrBlank() -> name
            !prod.isNullOrBlank() -> prod
            else -> null
        }

        if (resolved != null) return resolved

        usbManager?.deviceList?.values?.find { isMidiUsbDevice(it) }?.let { usbDev ->
            return getUsbDeviceCleanName(usbDev)
        }

        return "MIDI Keyboard / Piano"
    }

    private fun updateDeviceList() {
        val devices = midiManager?.devices ?: emptyArray()
        val deviceNames = devices.map { info -> resolveDeviceName(info) }

        handler.post {
            viewModel.midiConnectedDevices.clear()
            viewModel.midiConnectedDevices.addAll(deviceNames)
            if (deviceNames.isNotEmpty() && viewModel.connectedMidiDeviceName.value == null) {
                viewModel.connectedMidiDeviceName.value = deviceNames.first()
            } else if (deviceNames.isEmpty()) {
                checkConnectedUsbMidiDevices()
            }
        }
    }

    private fun openDevice(info: MidiDeviceInfo) {
        if (openDevices.containsKey(info)) return
        val resolvedName = resolveDeviceName(info)
        midiManager?.openDevice(info, { device ->
            if (device != null) {
                openDevices[info] = device
                handler.post {
                    viewModel.connectedMidiDeviceName.value = resolvedName
                }
                info.ports.forEach { port ->
                    // In Android MIDI API, a keyboard sends outgoing notes through its TYPE_OUTPUT port
                    if (port.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                        runCatching {
                            device.openOutputPort(port.portNumber)?.let { outputPort ->
                                openPorts.add(outputPort)
                                outputPort.connect(MidiReceiverProxy())
                            }
                        }
                    }
                }
            }
        }, handler)
    }

    /**
     * Stateful MIDI 1.0 Stream Parser supporting:
     * - Running Status (chords sent with a single leading 0x90/0x80/0xB0 status byte)
     * - Real-Time Messages (0xF8..0xFF Clock / Active Sensing) interleaved inside packets
     * - Sustain (CC 64), Sostenuto (CC 66), Soft/Una Corda (CC 67), Expression (CC 11), & All-Notes-Off (CC 120/123)
     * - Main-Thread UI & Play-Along Engine Dispatch
     */
    inner class MidiReceiverProxy : MidiReceiver() {
        private var runningStatus = 0
        private var inSysEx = false

        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            var i = offset
            val end = offset + count

            while (i < end) {
                val byteVal = data[i].toInt() and 0xFF

                // 1. System Real-Time Messages (0xF8..0xFF): 1 byte, do NOT alter runningStatus
                if (byteVal >= 0xF8) {
                    if (byteVal == 0xFF) {
                        // System Reset
                        runningStatus = 0
                        inSysEx = false
                        handler.post {
                            viewModel.midiActiveNotes.clear()
                            viewModel.midiPlayingNote.value = null
                        }
                    }
                    i++
                    continue
                }

                // 2. System Exclusive (SysEx 0xF0..0xF7)
                if (byteVal == 0xF0) {
                    inSysEx = true
                    runningStatus = 0
                    i++
                    continue
                }
                if (inSysEx) {
                    if (byteVal == 0xF7) {
                        inSysEx = false
                    } else if ((byteVal and 0x80) != 0) {
                        // Unterminated SysEx interrupted by a non-realtime status byte
                        inSysEx = false
                    } else {
                        i++
                        continue
                    }
                }

                // 3. System Common Messages (0xF1..0xF7): clear runningStatus & skip data bytes
                if (byteVal in 0xF1..0xF7) {
                    runningStatus = 0
                    i += when (byteVal) {
                        0xF1, 0xF3 -> 2
                        0xF2 -> 3
                        else -> 1
                    }
                    continue
                }

                // 4. Channel Voice Messages (0x80..0xEF) with Running Status support
                val status: Int
                if ((byteVal and 0x80) != 0) {
                    status = byteVal
                    runningStatus = status
                    i++
                } else {
                    if (runningStatus == 0) {
                        i++
                        continue
                    }
                    status = runningStatus
                }

                val command = status and 0xF0
                val channel = (status and 0x0F) + 1 // 1..16 (Channel 10 = Drums)

                when (command) {
                    0x80, 0x90 -> { // Note Off (0x80) or Note On (0x90)
                        if (i + 1 >= end) break
                        val note = data[i].toInt() and 0x7F
                        val velocity = data[i + 1].toInt() and 0x7F
                        i += 2

                        val isNoteOn = (command == 0x90 && velocity > 0)
                        dispatchNoteEventToMain(note, isNoteOn, velocity, channel)
                    }

                    0xA0 -> { // Polyphonic Key Pressure (Aftertouch): 2 data bytes
                        i += 2
                    }

                    0xB0 -> { // Control Change (Pedals, All Notes Off, Modulation)
                        if (i + 1 >= end) break
                        val ccNum = data[i].toInt() and 0x7F
                        val ccVal = data[i + 1].toInt() and 0x7F
                        i += 2

                        dispatchControlChangeToMain(ccNum, ccVal, channel)
                    }

                    0xC0, 0xD0 -> { // Program Change (0xC0) or Channel Pressure (0xD0): 1 data byte
                        i += 1
                    }

                    0xE0 -> { // Pitch Bend: 2 data bytes (LSB, MSB)
                        i += 2
                    }

                    else -> {
                        i++
                    }
                }
            }
        }

        private fun dispatchNoteEventToMain(note: Int, isNoteOn: Boolean, velocity: Int, channel: Int) {
            handler.post {
                // 1. Trigger core ViewModel MIDI handler (updates midiActiveNotes, midiPlayingNote, audioEngine, playAlongEngine)
                viewModel.onMidiEventReceived(note, isNoteOn, velocity, channel)

                // 2. Ensure UI state & learning tools (Sight-Reading, Tuner, Live Chord Detector) always update
                if (isNoteOn) {
                    viewModel.midiActiveNotes[note] = true
                    viewModel.midiPlayingNote.value = note
                    val freq = viewModel.tunerReferenceFrequency.value * 2.0.pow((note - 69) / 12.0).toFloat()
                    viewModel.detectedPitch.value = freq
                    viewModel.detectedNote.value = getNoteNameFromMidi(note)

                    // If incoming hardware MIDI is on Channel 10 (GM Drums), animate the 3D Drum Kit too!
                    if (channel == 10) {
                        val drumLabel = when (note) {
                            35, 36 -> "KICK"
                            38, 40, 37 -> "SNARE"
                            42, 44 -> "HH_CLOSED"
                            46 -> "HH_OPEN"
                            49, 55 -> "CRASH"
                            57, 52 -> "CRASH2"
                            51, 53, 59 -> "RIDE"
                            41, 43 -> "FLOOR"
                            45, 47 -> "TOM_LO"
                            48, 50 -> "TOM_HI"
                            39 -> "CLAP"
                            56 -> "PERC"
                            69, 70, 82 -> "SHAKER"
                            else -> null
                        }
                        if (drumLabel != null) {
                            viewModel.triggerThreeDDrumVisualizer(drumLabel)
                        }
                    }
                } else {
                    if (!viewModel.sustainPedalActive.value) {
                        viewModel.midiActiveNotes.remove(note)
                    }
                    if (viewModel.midiPlayingNote.value == note) {
                        viewModel.midiPlayingNote.value = viewModel.midiActiveNotes.entries.lastOrNull { it.value }?.key
                    }
                }
            }
        }

        private fun dispatchControlChangeToMain(ccNum: Int, ccVal: Int, channel: Int) {
            handler.post {
                when (ccNum) {
                    64 -> { // Sustain / Damper Pedal (CC 64)
                        val isPressed = ccVal >= 64
                        viewModel.sustainPedalActive.value = isPressed
                        viewModel.onMidiPedalEventReceived(64, isPressed)
                    }
                    66 -> { // Sostenuto Pedal (CC 66)
                        val isPressed = ccVal >= 64
                        viewModel.sostenutoPedalActive.value = isPressed
                        viewModel.onMidiPedalEventReceived(66, isPressed)
                    }
                    67 -> { // Soft / Una Corda Pedal (CC 67)
                        val isPressed = ccVal >= 64
                        viewModel.softPedalActive.value = isPressed
                        viewModel.onMidiPedalEventReceived(67, isPressed)
                    }
                    120, 123 -> { // All Sound Off (120) / All Notes Off (123)
                        viewModel.midiActiveNotes.clear()
                        viewModel.midiPlayingNote.value = null
                    }
                }
            }
        }
    }
}