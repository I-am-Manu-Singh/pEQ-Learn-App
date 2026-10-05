# pEQ Neural Workstation 🧠🎶

**pEQ** is a cross-platform neural media workstation built for high-fidelity playback, cinematic visualization, DSP experimentation, and intelligent media control across **Android and Desktop** environments.

Unlike traditional media players focused purely on consumption, pEQ blends studio-inspired tooling, immersive playback systems, AI-assisted processing, synchronized metadata, and advanced visualization into a unified workstation architecture. Because apparently humanity looked at ordinary music players and decided they needed neural telemetry overlays, VLC callback rendering pipelines, and FFT analyzers running at 2AM.

---

# ✨ Core Highlights

- 🎛 Professional DSP Workstation
- 🎥 Immersive Cinema Playback Engine
- 🧠 AI-Assisted Audio Processing
- 🌐 Intelligent Stream Resolution
- 🎨 Adaptive Neural UI System
- 🖥 Cross-Platform Android + Desktop Architecture

---

# 🚀 Features

## 🧠 Neural DSP & Audio Engineering

### 🎚 Parametric Equalization
- Multi-band DSP equalizer
- Real-time gain and Q adjustment
- Low-latency tonal shaping pipeline

### 🪄 AI Stem Separation
- Neural vocal/instrument isolation
- Independent stem balancing
- Experimental remix workflows

### 📊 Real-Time Audio Telemetry
- FFT spectrum visualization
- Stereo phase correlation
- Playback telemetry overlays
- Live analyzer dashboards

### 🔊 Advanced Processing Systems
- Vocal removal
- Deep bass enhancement
- Clarity processing
- Compressor & limiter controls
- Sub-harmonic generation
- Phase inversion utilities

---

# 🎥 Cinematic Playback Engine

## 🎬 Immersive Video System
- Gesture-driven playback controls
- Dynamic fullscreen transitions
- Hardware-accelerated rendering
- Brightness & volume swipe gestures
- Playback speed controls
- Adaptive scaling modes

## 🪟 Universal Picture-in-Picture
- Floating playback support
- Persistent multitasking playback
- Lifecycle-aware playback synchronization

## 🧭 Subtitle & Chapter Intelligence
- Embedded subtitle support
- External subtitle loading
- LRC / SRT / VTT parsing
- Synced lyrics engine
- Chapter timeline navigation

---

# 🖥 Desktop Workstation Engine

The Desktop edition is powered by a dedicated **VLCJ + FFmpeg rendering pipeline** using native VLC libraries (`libvlc` DLLs/shared binaries) for industrial-grade playback reliability and broad codec compatibility.

## ⚡ Desktop Playback Features
- VLC native decoding backend
- FFmpeg-powered codec compatibility
- Real-time callback video rendering
- Skia-backed Compose rendering pipeline
- Embedded media telemetry
- Hardware-accelerated playback
- Native stream playback support
- Advanced audio track handling
- External subtitle synchronization

## 🧩 Desktop Rendering Pipeline
- VLC callback video surfaces
- Compose Multiplatform UI rendering
- Skia bitmap frame conversion
- Dynamic frame synchronization
- Optimized memory lifecycle management

The desktop engine uses direct VLC frame callbacks and native rendering integration rather than embedding a traditional heavyweight media window. Which is considerably harder than slapping a browser view into an app and calling it “next-generation multimedia infrastructure.”

---

# ☁️ Cloud & Streaming Intelligence

## 🌐 Neural Stream Resolver
- `youtube-dl` / `yt-dlp` integration
- Remote stream extraction
- Adaptive URL resolution
- Network playback support

## 🔎 Integrated Media Discovery
- Lyrics search integration
- Subtitle repository support
- OpenSubtitles API integration
- Embedded browser workflows

---

# 🎨 Workstation UI System

## 🎨 Dynamic Neural Coloring
- Artwork-driven accent extraction
- Adaptive Material 3 themes
- Reactive color synchronization

## 🧩 Tactical Widget Ecosystem
- Android Glance widgets
- XML playback widgets
- Real-time workstation telemetry

## 🛡 Focus Interfaces
- Neural Focus mode
- Lock-screen optimized playback
- Minimal distraction overlays

---

# 🛠 Technology Stack

## Core Architecture
- **Kotlin Multiplatform (KMP)**
- **Compose Multiplatform**
- **Material 3**

## Android Engine
- **ExoPlayer / Media3**
- **Custom DSP wrappers**

## Desktop Engine
- **VLCJ**
- **LibVLC Native Libraries**
- **FFmpeg**
- **Skia Rendering Pipeline**

## AI & Media Intelligence
- **ONNX Runtime**
- **youtube-dl / yt-dlp**

## Networking & Persistence
- **Ktor**
- **Kotlinx Serialization**
- **SharedPreferences**
- **Desktop JSON Persistence**

---

# 🖥 Supported Platforms

| Platform | Status |
|---|---|
| Android | ✅ Stable |
| Desktop JVM | ✅ Stable |
| macOS/Linux Optimization | 🚧 In Progress |

---

# 📦 Installation

## 1. Clone Repository

```bash
git clone https://github.com/I-am-Manu-Singh/pEQ-Media-App.git
```

---

## 2. Open Project

Requirements:
- Android Studio Ladybug or newer
- Kotlin Multiplatform plugin
- JDK 17+

---

## 3. Desktop VLC Setup

The Desktop engine requires native VLC libraries installed on the host system.

### Windows
Install VLC Media Player and ensure VLC native libraries are accessible.

### Linux
Install VLC via package manager:

```bash
sudo apt install vlc
```

### macOS
Install VLC from:
https://www.videolan.org/vlc/

---

## 4. Run Project

### Android
Run the `composeApp` Android configuration.

### Desktop

```bash
./gradlew run
```

Because modern multimedia applications apparently require orchestrating Gradle, native VLC binaries, FFmpeg bindings, Compose rendering, coroutine synchronization, and enough abstraction layers to frighten small governments.

---

# 🧪 Experimental Systems

- Neural stem remixing
- DSP telemetry overlays
- Intelligent subtitle synchronization
- Dynamic workstation widgets
- Cross-platform playback abstraction
- VLC callback rendering pipeline

---

# 🏗 Architectural Goals

pEQ is engineered around:
- Cross-platform maintainability
- Modular KMP architecture
- Lifecycle-aware rendering
- Efficient media pipelines
- Professional workstation UX
- Native desktop media acceleration

---

# 👨‍💻 Developer

Developed by **Manu Singh**

GitHub Repository:
https://github.com/I-am-Manu-Singh/pEQ-Media-App

---

# 📌 Version

**pEQ Neural Workstation v1.0.0**
