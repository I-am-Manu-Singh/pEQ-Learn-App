package com.creativeblisslabs.peqlearn.app.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import com.creativeblisslabs.peqlearn.app.ui.viewmodel.AppViewModel

class AndroidMidiHandler(
    private val context: Context,
    private val viewModel: AppViewModel
) {
    private val midiManager: MidiManager = context.getSystemService(Context.MIDI_SERVICE) as? MidiManager
        ?: throw IllegalStateException("MidiManager not available")
    
    private val handler = Handler(Looper.getMainLooper())
    private val openDevices = mutableMapOf<MidiDeviceInfo, MidiDevice>()
    private val openPorts = mutableListOf<MidiOutputPort>()

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo) {
            updateDeviceList()
            openDevice(device)
        }

        override fun onDeviceRemoved(device: MidiDeviceInfo) {
            openDevices.remove(device)?.close()
            updateDeviceList()
        }
    }

    fun start() {
        midiManager.registerDeviceCallback(deviceCallback, handler)
        updateDeviceList()
        midiManager.devices?.forEach { info ->
            if (info != null) {
                openDevice(info)
            }
        }
    }

    fun stop() {
        try {
            midiManager.unregisterDeviceCallback(deviceCallback)
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

    private fun updateDeviceList() {
        val devices = midiManager.devices ?: emptyArray()
        val deviceNames = devices.map { info ->
            val prop = info.properties
            val name = prop.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown MIDI Device"
            val manufacturer = prop.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER) ?: ""
            "$manufacturer $name".trim()
        }

        handler.post {
            viewModel.midiConnectedDevices.clear()
            viewModel.midiConnectedDevices.addAll(deviceNames)
        }
    }

    private fun openDevice(info: MidiDeviceInfo) {
        midiManager.openDevice(info, { device ->
            if (device != null) {
                openDevices[info] = device
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
                    when (command) {
                        0x80 -> { // Note Off
                            if (iterator + 2 < end) {
                                val note = data[iterator + 1].toInt() and 0x7F
                                viewModel.onMidiEventReceived(note, false)
                                iterator += 3
                            } else return
                        }
                        0x90 -> { // Note On
                            if (iterator + 2 < end) {
                                val note = data[iterator + 1].toInt() and 0x7F
                                val velocity = data[iterator + 2].toInt() and 0x7F
                                viewModel.onMidiEventReceived(note, velocity > 0)
                                iterator += 3
                            } else return
                        }
                        0xB0 -> { // Control Change (Sustain Pedal)
                            if (iterator + 2 < end) {
                                val ccNum = data[iterator + 1].toInt() and 0x7F
                                val ccVal = data[iterator + 2].toInt() and 0x7F
                                if (ccNum == 64) {
                                    viewModel.onMidiPedalEventReceived(ccVal >= 64)
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