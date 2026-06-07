# Kids Dice ("Vibrant Dice") — Game Specification & Blueprint
This document outlines the design, architecture, physics, custom audio math, user experience, and CI/CD pipelines of the **Kids Dice** (metadata name: **Vibrant Dice**) application. It is constructed to allow any future engineering agent or developer to understand the codebase instantly or recreate it from scratch.

---

## 1. Core Vision & Purpose
**Vibrant Dice** is a single-screen, child-friendly, distraction-free Android application that transforms the utility of rolling virtual dice into an immersive sensory experience. Designed for toddlers and young children, it focuses on big visual cues, engaging physical response animations, tactile math-synthesized sound feedback, and multi-mode early learning representations (symbols, mascots, digits).

---

## 2. Technical System Constraints & Stack
- **OS Platform:** Android (minSdk 31, targetSdk 34)
- **UI Framework:** 100% Jetpack Compose (Kotlin declarative styling)
- **Design Paradigm:** Material 3 (M3) with adaptive edge-to-edge support (`enableEdgeToEdge`), deep negative space, and modern styling
- **Sound Generation:** In-app real-time Math-Synthesized PMC Audio with `AudioTrack` (0 asset latency, 100% zero-file load times)
- **Local Unit Tests:** JUnit + Robolectric (executes on local JVM)
- **State Architecture:** Single source of truth local Compose states with Coroutines side-effects ticking the physics loops.

---

## 3. High-Contrast Interactive Visual Scheme
The application dynamically updates its color profile based on the active face of the die. Every roll triggers an aesthetic background transition and custom element highlights:

| Die Value | Name Representation | Color Group (Hex Code) | Palette Identity | Mascot Emoji |
| :---: | :---: | :---: | :---: | :---: |
| **1** | `"One"` | `#FFFF2E93` | Vibrant Pink Magic | 🌸 (Blossom) |
| **2** | `"Two"` | `#FFFF6200` | Bright Comic Orange | 🦊 (Fox) |
| **3** | `"Three"`| `#FFFFD600` | Brilliant Sunshine Yellow | 🌟 (Star) |
| **4** | `"Four"` | `#FF00E676` | Neon Shamrock Green | 🌱 (Sprout) |
| **5** | `"Five"` | `#0xFF00B0FF` | Crystal Spray Cyan | 🐬 (Dolphin) |
| **6** | `"Six"`  | `#0xFF9D4EDD` | Cosmic Violet Purple | 🦄 (Unicorn) |

---

## 4. Multi-Mode Educational Mechanics
The app offers three viewing modes selectable through a fluid, glassmorphic horizontal pill layout:
- **Dots Mode:** Standard geometric dot alignments structured natively in standard Compose grids. Single dots dynamically auto-scale to be larger (56.dp) compared to clusters (28.dp) to draw focus.
- **Mascot Mode:** Large high-fidelity animal/nature emojis centered to teach word association.
- **Numbers Mode:** Bold typography representation showing raw numerical indices to bolster number recognition.

---

## 5. Synthesized Audio Engine (No Asset Overhead)
To guarantee zero-latency playback and avoid bulky `.mp3` assets, the sound engine calculates PCM buffers mathematically and streams them directly into an Android `AudioTrack` instance.

### A. The "Tick" Sound (Tumble Increment Feed)
- **Frequency Loop:** Decay from $1000\text{ Hz}$ down to $60\text{ Hz}$ in $25\text{ ms}$.
- **Envelope Formula:** Exponential decay, $e^{-8k}$ where $k$ is the sample progress.
- **Acoustic Impression:** Satisfying, dry woodblock "tick click" feel matching each flip step.

### B. The "Ding" Sound (Success Chime)
- **Interval Formula:** Ascending major triad chord combining four frequencies:
  - $E_5$ ($659.25\text{ Hz}$)
  - $G\#_5$ ($830.61\text{ Hz}$)
  - $B_5$ ($987.77\text{ Hz}$)
  - $E_6$ ($1318.51\text{ Hz}$)
- **Envelope Formula:** Fast linear attack ($5\%$ progress) followed by slow exponential dampening ($e^{-5(progress-0.05)}$). Includes a subtle $12\text{ Hz}$ internal wave vibrato.
- **Acoustic Impression:** Playful, sparkling arcade chime representing achievement.

---

## 6. Physics and Animations
- **Tumbling Matrix:** On-touch triggers multidimensional spring rotation:
  $$\text{rotationZ} = 1080^\circ \quad \text{rotationX} = 35^\circ \to 0^\circ \quad \text{rotationY} = -35^\circ \to 0^\circ$$
- **Spring Scale Burst:** The die card scales from $1.0\text{x} \to 1.35\text{x}$, then settles back using standard damped spring oscillations to simulate weight and organic physical presence.
- **Gravitational Burst Canvas:** A standalone `Canvas` is rendered as an overlay. Upon die resolution, 24 particles are spawned at the screen's center with random radial velocities and active spin speeds. A recurrent `LaunchedEffect` clocks at $16\text{ ms}$ (roughly $60\text{ fps}$) to simulate gravity:
  $$y = y_\text{prev} + v_y \quad v_y = v_{y,\text{prev}} + 0.5\text{px/frame}$$
  The items gracefully fade (decreasing $\alpha$ by $0.02$ per frame) until deleted.

---

## 7. App Architecture & File Map
The project is built inside a clean single-module directory layout:

```text
app/src/main/
├── AndroidManifest.xml (Minimalist entry point, edge-to-edge styling metadata)
├── java/com/example/
│   ├── MainActivity.kt (Handles game state, compose view elements, particles system, card spring)
│   ├── sound/
│   │   └── SoundEngine.kt (Contains pure-Kotlin PCM sound synthesis & AudioTrack interfaces)
│   └── ui/theme/
│       ├── Color.kt
│       ├── Theme.kt (Primary Material Theme configures customized system overlays)
│       └── Type.kt
└── res/
    ├── drawable/ic_launcher_foreground.xml (Custom-crafted dice vector vector path)
    └── values/strings.xml (Includes app resources, app_name definitions)
```

---

## 8. CI/CD Release Pipeline (GitHub Actions)
The repository includes a modern continuous integration and delivery pipeline defined in `.github/workflows/ci.yml`.

### Pipeline Stages
1. **Validation & Checks:** Codespace is pulled, JDK 17 configured, executable permissions verified, and `./gradlew lintDebug` is run to catch SDK boundary errors.
2. **Testing:** Runs automated Robolectric units locally on the GitHub Action runner (`./gradlew testDebugUnitTest`).
3. **Packaging:** Compiles unsigned Release and signed debug APKs.
4. **Automated SemVer Tagging:** Uses `github-tag-action` to calculate bumps dynamically on push to `main` branch.
5. **Asset Formatting & Output:** Translates raw output files to clean consumer artifacts:
   - `KidsDice-vX.Y.Z-debug.apk` (Pre-signed and optimized, ready for rapid side-loading)
   - `KidsDice-vX.Y.Z-release.apk` (Production binary)
6. **Release Execution:** Creates an immutable Release, sets up a complete, friendly sideloading instructions payload, and attaches the binary assets directly.
