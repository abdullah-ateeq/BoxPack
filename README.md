# 📦 BoxPack - 3D Container Packing & Volume Calculator for Android

[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20Material3-4285F4.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![MinSDK](https://img.shields.io/badge/MinSDK-26%20%28Android%208.0%29-brightgreen.svg)](https://developer.android.com)
[![TargetSDK](https://img.shields.io/badge/TargetSDK-37-blue.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**BoxPack** is a powerful, modern Android application built with **Jetpack Compose Material 3** designed for logistics managers, warehouse operators, shipping planners, and e-commerce merchants. It provides a precise 3D physical rectangular packing engine to calculate exact box capacities inside shipping containers, cartons, trucks, or pallets—without relying on simple volume division.

---

## ✨ Features

### 🧮 1. Precision 3D Physical Packing Engine
- **Axis-Aligned Permutations:** Evaluates all **6 unique 3D spatial orientations** ($W \times D \times H$, $W \times H \times D$, $D \times W \times H$, $D \times H \times W$, $H \times W \times D$, $H \times D \times W$) to find the maximum possible box fit.
- **Exact Geometric Calculation:** Never uses naive volume division. Computes discrete spatial fitting ($across \times deep \times layers$).
- **Efficiency & Utilization Metrics:** Real-time feedback on Volume Utilization (%), Floor Surface Utilization (%), Vertical Height Utilization (%), and Theoretical Volume Ratio.

### 🔒 2. Advanced Orientation & Stacking Constraints
- **Orientation Modes:**
  - **Automatic (Optimal):** Tests all 6 rotations to maximize capacity.
  - **Upright Only:** Locks the box vertical axis ($Z$) while allowing horizontal $X/Y$ rotation (ideal for items marked "This Side Up").
  - **Fixed (Original):** Prevents box rotation entirely.
  - **Custom Orientation:** Test specific custom permutations.
- **Stacking Controls:** Unlimited stacking, Single layer only (Disabled stacking), or Limited to a specific maximum layer count.
- **Weight Limits:** Set individual box weight (kg) and container max load limit (kg). The calculator automatically caps capacity if weight limit is reached.

### 📐 3. Clearances, Wall Thickness & Spacing Gaps
- **Wall Thickness:** Deduct container wall thickness from inner usable space.
- **Outer Clearances:** Specify safety margins around container sides, depth, and top height.
- **Inter-Box Gaps:** Account for protective padding, bubble wrap, or corrugated spacing between individual boxes ($X, Y, Z$ gaps).

### 🎨 4. Interactive 3D & 2D Visualizations
- **3D Isometric Container Diagram:** Axonometric Compose Canvas view with clear dimension lines and scale labels.
- **2D Layer Packing Grid:** Interactive grid visualization showing top-down layout per layer, used/remaining dimensions, and floor footprint.

### 📦 5. Batch & Shipment Fulfillment Calculator
- Input required total order box quantity and calculate:
  - Total containers/cartons required (using ceiling division).
  - Unused space & remaining capacity in the last container.
  - Multiple container fleet capacity.

### 📊 6. History, Persistence & Multi-Format Exporting
- **Calculation History:** Save, load, duplicate, or delete past packing calculations.
- **Multi-Unit Support:** Seamlessly switch between **Millimeters (mm)**, **Centimeters (cm)**, **Meters (m)**, **Inches (in)**, and **Feet (ft)** with automatic live unit conversions.
- **Export Formats:** Export calculation summaries as formatted **TXT**, **CSV**, or structured **JSON**, or share directly via Android system share sheet.

---

## 🛠 Tech Stack & Architecture

- **Language:** 100% Kotlin
- **UI Framework:** Jetpack Compose with Material Design 3 (Material 3)
- **Architecture:** Clean MVVM (Model-View-ViewModel) with `StateFlow` and `State` updates
- **Navigation:** Jetpack Navigation Compose
- **Design:** Dynamic Color (Material You), Dark/Light theme support, custom isometric Compose Canvas rendering
- **Data Persistence:** SharedPreferences / Datastore with JSON serialization
- **Testing:** Comprehensive JUnit 4 unit test suite (`BoxPackCalculatorTest`) covering edge cases, orientation locks, weight constraints, and batch math.

---

## 📁 Project Structure

```text
com.example.boxpack
├── calculator/
│   └── BoxPackCalculator.kt      # Core 3D packing algorithm & discrete spatial engine
├── data/
│   ├── StorageRepository.kt       # Local storage for settings & history
│   └── ExportHelper.kt            # Export generators (TXT, CSV, JSON, Share)
├── model/
│   ├── PackingModels.kt          # Domain data models (Container, Box, Orientation, Results)
│   └── Units.kt                   # Length unit definitions & conversion logic
├── ui/
│   ├── BoxPackViewModel.kt       # UI State management & live calculation reactivity
│   ├── components/
│   │   ├── Box3DDiagram.kt       # Isometric 3D Compose Canvas diagram
│   │   ├── PackingViews.kt       # Interactive 2D packing layer grid
│   │   └── ResultCards.kt        # Metric cards & breakdown components
│   ├── navigation/
│   │   └── BoxPackNavHost.kt     # App navigation graph & bottom nav bar
│   ├── screens/
│   │   ├── HomeScreen.kt          # Main calculator screen
│   │   ├── PackingLayoutScreen.kt # 2D/3D layout detail screen
│   │   ├── BatchScreen.kt         # Batch order fulfillment screen
│   │   ├── HistoryScreen.kt       # Saved calculations history
│   │   ├── SettingsScreen.kt      # App preferences & defaults
│   │   └── AboutScreen.kt         # App information & developer notes
│   └── theme/                    # Material 3 Color scheme, Typography & Theme
└── MainActivity.kt                # Application entry point
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio:** Ladybug (2024.2.1) or newer recommended.
- **JDK:** Java 11 or Java 17.
- **Android SDK:** Compile SDK 37 (Min SDK 26 - Android 8.0 Oreo).

### Building & Running

1. **Clone the repository:**
   ```bash
   git clone https://github.com/abdullah-ateeq/BoxPack.git
   cd BoxPack
   ```

2. **Open in Android Studio:**
   - Select **File > Open** and select the root `BoxPack` directory.
   - Allow Gradle sync to complete.

3. **Run Unit Tests:**
   ```bash
   ./gradlew test
   ```

4. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
