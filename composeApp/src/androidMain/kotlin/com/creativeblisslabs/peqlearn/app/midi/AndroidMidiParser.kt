package com.creativeblisslabs.peqlearn.app.midi

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
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel

class AndroidMidiHandler(
    private val context: Context,
    private val viewModel: AppViewModel
) {
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
                    message = "Connected to \"$name\". Real-time note input, 3-pedal sensing, and velocity are active.",
                    isConnected = true,
                    deviceName = name
                )
            }
            updateDeviceList()
            openDevice(device)
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            val name = resolveDeviceName(device)
            openDevices.remove(device)?.close()
            handler.post {
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

    private fun registerUsbReceiver() {
        if (usbReceiver != null) return

        usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        val device = getUsbDeviceFromIntent(intent)
                        if (device != null && isMidiUsbDevice(device)) {
                            val name = getUsbDeviceCleanName(device)
                            handler.post {
                                viewModel.connectedMidiDeviceName.value = name
                                viewModel.showMidiPrompt(
                                    title = "USB Piano Attached",
                                    message = "Hardware USB Piano \"$name\" detected! Initializing MIDI data stream...",
                                    isConnected = true,
                                    deviceName = name
                                )
                            }
                            // Trigger device scan
                            updateDeviceList()
                            midiManager?.devices?.forEach { info ->
                                if (info != null && !openDevices.containsKey(info)) {
                                    openDevice(info)
                                }
                            }
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        val device = getUsbDeviceFromIntent(intent)
                        if (device != null && isMidiUsbDevice(device)) {
                            val name = getUsbDeviceCleanName(device)
                            handler.post {
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
        }
        try {
            context.registerReceiver(usbReceiver, filter)
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
        // Direct Audio Class 1 with Subclass 3 is USB Audio/MIDI Streaming
        if (device.deviceClass == UsbConstants.USB_CLASS_AUDIO) {
            return true
        }
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            // USB Audio Class = 1, MIDI Streaming Subclass = 3
            if (iface.interfaceClass == UsbConstants.USB_CLASS_AUDIO && iface.interfaceSubclass == 3) {
                return true
            }
            if (iface.interfaceClass == UsbConstants.USB_CLASS_AUDIO) {
                return true
            }
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

        // Fallback to checking active USB device names
        usbManager?.deviceList?.values?.find { isMidiUsbDevice(it) }?.let { usbDev ->
            return getUsbDeviceCleanName(usbDev)
        }

        return "MIDI Keyboard / Piano"
    }

    private fun updateDeviceList() {
        val devices = midiManager?.devices ?: emptyArray()
        val deviceNames = devices.map { info ->
            resolveDeviceName(info)
        }

        handler.post {
            viewModel.midiConnectedDevices.clear()
            viewModel.midiConnectedDevices.addAll(deviceNames)
            if (deviceNames.isNotEmpty() && viewModel.connectedMidiDeviceName.value == null) {
                viewModel.connectedMidiDeviceName.value = deviceNames.first()
            } else if (deviceNames.isEmpty()) {
                // If no MIDI device recognized by MidiManager, check USB manager
                checkConnectedUsbMidiDevices()
            }
        }
    }

    private fun openDevice(info: MidiDeviceInfo) {
        val resolvedName = resolveDeviceName(info)
        midiManager?.openDevice(info, { device ->
            if (device != null) {
                openDevices[info] = device
                handler.post {
                    viewModel.connectedMidiDeviceName.value = resolvedName
                }
                info.ports.forEach { port ->
                    if (port.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT) {
                        device.openOutputPort(port.portNumber)?.let { outputPort ->
                            openPorts.add(outputPort)
                            outputPort.connect(MidiReceiverProxy())
                        }
                    }
                }
            }
        }, handler)
    }

    inner class MidiReceiverProxy : MidiReceiver() {
        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
            var iterator = offset
            val end = offset + count

            while (iterator < end) {
                val status = data[iterator].toInt() and 0xFF
                if (status >= 248) {
                    iterator++
                } else if (status < 240) {
                    val command = status and 0xF0
                    val channel = (status and 0x0F) + 1
                    when (command) {
                        0x80 -> { // Note Off
                            if (iterator + 2 < end) {
                                val note = data[iterator + 1].toInt() and 0x7F
                                val velocity = data[iterator + 2].toInt() and 0x7F
                                viewModel.onMidiEventReceived(note, false, velocity, channel)
                                iterator += 3
                            } else return
                        }
                        0x90 -> { // Note On
                            if (iterator + 2 < end) {
                                val note = data[iterator + 1].toInt() and 0x7F
                                val velocity = data[iterator + 2].toInt() and 0x7F
                                viewModel.onMidiEventReceived(note, velocity > 0, velocity, channel)
                                iterator += 3
                            } else return
                        }
                        0xB0 -> { // Control Change (Pedals & Controllers)
                            if (iterator + 2 < end) {
                                val ccNum = data[iterator + 1].toInt() and 0x7F
                                val ccVal = data[iterator + 2].toInt() and 0x7F
                                when (ccNum) {
                                    64 -> { // Sustain / Damper Pedal
                                        viewModel.onMidiPedalEventReceived(64, ccVal >= 64)
                                    }
                                    66 -> { // Sostenuto Pedal
                                        viewModel.onMidiPedalEventReceived(66, ccVal >= 64)
                                    }
                                    67 -> { // Soft Pedal / Una Corda
                                        viewModel.onMidiPedalEventReceived(67, ccVal >= 64)
                                    }
                                }
                                iterator += 3
                            } else return
                        }
                        else -> {
                            iterator++
                        }
                    }
                } else {
                    return
                }
            }
        }
    }
}