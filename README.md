# AuthSense – Behavioral Continuous Authentication for Mobile Banking

AuthSense is an AI-powered mobile security system that enhances mobile banking protection through **behavior-based continuous authentication**. Instead of verifying users only during login, AuthSense continuously monitors behavioral patterns such as typing rhythm, touch gestures, and device motion to detect unauthorized access in real time.

The system uses lightweight deep learning models running directly on Android devices to provide secure, privacy-preserving, and energy-efficient authentication without interrupting the user experience.

---

## Problem Statement

Traditional authentication methods such as passwords, PINs, OTPs, and biometrics verify users only once during login. After authentication, sessions remain vulnerable to:

* Session hijacking
* Stolen credentials
* Unauthorized access
* Fraudulent banking activities

Existing systems fail to continuously validate user identity during active sessions.

AuthSense addresses this challenge using continuous behavioral monitoring and AI-based anomaly detection to identify suspicious behavior dynamically and trigger adaptive security responses.

---

# Features

## Continuous Authentication

* Monitors user behavior throughout the session
* Detects anomalies in real time
* Provides dynamic risk scoring

## Adaptive Decision Engine (Context-Aware Security)

The system dynamically adjusts its sensitivity based on the user's physical state and environment:

### 1. Traveling (Travel Mode)
Uses Variance Analysis on accelerometer data to identify the user's environment:
* **Detection:** Monitors motion variance over **10.0-second windows** (extreme stability to prevent false triggers from typing or grip adjustments).
* **Stationary:** No multiplier applied (1.0x). Adjusted threshold (**5.0**) to ignore natural hand tremors and device handling jitter.
* **Vehicle (1.8x Multiplier):** High tolerance for vibrations in cars/buses (Variance between **5.0 and 15.0**).
* **Walking (2.5x Multiplier):** Adjusts for rhythmic gait patterns (Variance above **15.0**).

### 2. Fractured / Disabled Users (Injury Mode)
Provides accessibility support for physical limitations:
* **Action:** Applies a **3.0x Multiplier** to all thresholds (Motion and Keystroke).
* **Benefit:** High tolerance for tremors, slower typing speeds, or shaky handling.
* **Trigger:** Listens for a secure broadcast (`com.authsense.bank.INJURY_MODE_TOGGLE`) after user identity confirmation.

## Behavioral Biometrics

The system analyzes:

* **Keystroke Dynamics:** Speed, rhythm, and touch pressure.
* **Motion Patterns:** Accelerometer and Gyroscope signatures.
* **Navigation Behavior:** How the user interacts with banking features.

## AI-Based Anomaly Detection

* Uses an **LSTM-CNN** model.
* Learns normal behavioral patterns.
* Detects deviations using reconstruction error (MSE).

## Adaptive Security Response

Depending on risk level:

* **Low Risk:** Silent monitoring.
* **Medium Risk:** Unusual activity notification and transaction restrictions.
* **High Risk:** Immediate account lock and password reset requirement.

## Lightweight Mobile Deployment

* ONNX optimized model (~37.9 KB).
* Designed for real-time inference with minimal battery impact.

---

# System Architecture

## 1. Behavioral Data Collection

The app continuously captures sensor data and touch events in the background, specifically tailored for banking environments.

## 2. Data Preprocessing

* Segmented into 300-sample windows.
* Downsampled to 10Hz.
* Normalized using user-specific scaler values stored in `lstm_cnn_meta.json`.

## 3. Model Training

An **LSTM-CNN** architecture is utilized to capture both sequential dependencies (LSTM) and local spatial patterns (CNN) in movement and typing.

## 4. Real-Time Inference

The model calculates a reconstruction Mean Squared Error (MSE). If the MSE (scaled by **Adaptive Multipliers**) exceeds the personalized threshold, the risk score increases.

---

# Tech Stack

## Mobile Application
* **Java**
* Android Studio
* Android Sensor API

## Machine Learning
* Python (TensorFlow/Keras)
* **ONNX Runtime** (for mobile inference)

---

# Machine Learning Workflow

## Training Strategy

### Phase 1 – Generalized Training
The model is pre-trained on large-scale behavioral datasets (HMOG, Touchalytics) to understand "human-like" interaction.

### Phase 2 – Personalization
The app collects user-specific data for **5 minutes** upon first use. This establishes a baseline for speed, pressure, and handling, creating a unique cryptographic-like behavioral profile.

---

# Model Details

## LSTM-CNN
The model is optimized to detect "Out-of-Distribution" behavior.

### Input Features
* Accelerometer X/Y/Z
* Gyroscope X/Y/Z

### Detection Logic
The base threshold is defined in `lstm_cnn_meta.json`:
`lstm_cnn_threshold`: 0.005

This is dynamically adjusted by the **Adaptive Decision Engine** to reduce false positives while walking (2.5x) or if Injury Mode is active (3.0x).

---

# Installation

## Clone the Repository
```bash
git clone https://github.com/UnisysUIP/2026-AuthSense-An-Intelligent-Adaptive-Continuous-Behavior-Based-Authentication-System-for-Secure
cd AuthSense
```

## Open in Android Studio
* Import the project.
* Sync Gradle dependencies.
* Deploy to a device running Android 8.0 or higher.

---

# Contributors
* Team AuthSense

---

# License
This project is developed for research and educational purposes.
