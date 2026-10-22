# QueueLess — Digital Queue & Appointment Management System

QueueLess is a native Android application built with **Java** and **XML** using **Firebase Authentication** and **Cloud Firestore** for Mobile Application Development Lab (MADL) practical examination and academic demonstration.

---

## 📌 Project Architecture

- **Platform**: Native Android (Android Studio Giraffe / Ladybug or newer)
- **Programming Language**: Java (JDK 17)
- **User Interface**: XML layouts with Google Material Design Components & ViewBinding
- **Backend / Cloud**: Firebase Authentication (Email/Password) & Cloud Firestore (Real-time NoSQL)
- **Concurrency Protection**: Firestore Atomic Transactions preventing duplicate queue tokens

---

## 🚀 Setup & Execution Guide

### 1. Prerequisites
- **Android Studio** (2023.x or 2024.x)
- **JDK 17** (configured as Gradle JDK in Android Studio Settings -> Build, Execution, Deployment -> Build Tools -> Gradle)
- An active Google / Firebase account

### 2. Firebase Project Configuration
1. Go to the [Firebase Console](https://console.firebase.google.com/) and click **Add project** -> Name it `QueueLess`.
2. Register an **Android App** inside your project:
   - **Android package name**: `com.queueless.app`
3. Download the generated `google-services.json` file.
4. Copy and replace `d:\QueueLess\app\google-services.json` with your downloaded file.
5. In Firebase Console:
   - Go to **Build > Authentication** -> Click **Get Started** -> Enable **Email/Password**.
   - Go to **Build > Firestore Database** -> Click **Create database** -> Choose location -> Start in **test mode** (or deploy the provided rules).
   - Go to **Firestore > Rules** tab -> Paste the contents of `firestore.rules` located in the root of this project -> Click **Publish**.

### 3. Provisioning the Office Administrator
Public registration in the app only registers **Customers** to prevent privilege escalation.
To create an authorized Admin:
1. In Firebase Console -> **Authentication** -> **Add user**:
   - Email: `admin@queueless.com`
   - Password: `adminpassword`
   - Copy the newly created **User UID**.
2. In Firebase Console -> **Firestore Database** -> Collection: `users`:
   - Document ID: `<Paste the Admin User UID>`
   - Fields:
     - `uid` (string): `<Admin User UID>`
     - `name` (string): `Prof. Sharma (Admin)`
     - `email` (string): `admin@queueless.com`
     - `role` (string): `ADMIN`
     - `officeId` (string): `OFFICE_COL101`
     - `createdAt` (number): `1729240000000`

### 4. Running the Application in Android Studio
1. Open Android Studio -> Click **Open** -> Select `d:\QueueLess`.
2. Wait for Gradle Sync to complete.
3. Select an Emulator (or connect a physical Android phone via USB with USB Debugging enabled).
4. Click **Run ▶** (`Shift + F10`).

---

## 🧪 Testing Checklist & Demonstration Procedure

### Workflow A: Customer Registration & Office Lookup
1. Launch app -> On Splash Screen, it navigates to **Sign In**.
2. Tap **"Register"** -> Fill in Name, Email, Password, and Confirm Password -> Tap **Create Account**.
3. You land on the **Customer Dashboard**.
4. Tap **"Connect to Office"**.
5. Tap the helper button **"⚡ Initialize Demo Offices into Firebase"** (instantly seeds `COL101` and `HOSP202` with active services into your database!).
6. Tap the quick chip **COL101** -> Office information and 3 services appear!

### Workflow B: Concurrency-Safe Queue Token Generation
1. Select service **Fee Clearance & No-Dues**.
2. Select **"⚡ Instant Queue Token"** -> Tap **Generate Instant Token**.
3. You immediately land on **My Queue Token** screen showing:
   - Token Number: `#F001`
   - Status: `WAITING` (orange badge)
   - People Ahead: `0`
   - Est. Wait Time: `~5 mins`
4. Re-enter as another customer and take a token: it allocates `#F002` with People Ahead = `1` and Wait Time = `~10 mins`.

### Workflow C: Admin Live Desk (Call Next, Complete, Skip)
1. Sign out and log in with `admin@queueless.com` (or use a second device/emulator).
2. The **Admin Dashboard** shows:
   - Waiting: `2`
   - Total Today: `2`
   - Serving: `None`
3. Tap **"Queue Management"** -> Tokens `#F001` and `#F002` are shown in order in the waiting list.
4. Tap **"▶ Call Next Customer"**:
   - Token `#F001` moves to **NOW SERVING AT DESK**.
   - Check the customer's phone: the status badge instantly turns **GREEN: `SERVING`** without refreshing!
5. Tap **"✓ Completed"** -> Desk is cleared, customer's badge updates to `COMPLETED`.
6. Tap **"▶ Call Next Customer"** -> Token `#F002` is called.
7. Tap **"✕ Skip Absent"** -> Token `#F002` is marked `SKIPPED`.

### Workflow D: Scheduled Appointment & Check-In
1. As a customer, select service **Transcript & Grade Verification**.
2. Select **"📅 Scheduled Appointment"** -> Pick today's date -> Select Time Slot (`10:15 AM`) -> Confirm.
3. On the Token Screen, status displays `BOOKED`.
4. Tap **"Check In to Join Live Queue"** -> Converts scheduled booking into an active live queue sequence!

### Workflow E: Booking History & Status Filters
1. From Customer Dashboard, tap **"Booking History"**.
2. View all past tokens and appointments.
3. Test filter chips: **All Bookings**, **Active & Waiting**, **Completed**, **Cancelled**.

---

## 📂 Source Code Tree

```
d:/QueueLess/
├── app/
│   ├── src/main/java/com/queueless/app/
│   │   ├── activities/
│   │   │   ├── SplashActivity.java
│   │   │   ├── LoginActivity.java
│   │   │   ├── RegisterActivity.java
│   │   │   ├── CustomerDashboardActivity.java
│   │   │   ├── AdminDashboardActivity.java
│   │   │   ├── OfficeConnectActivity.java
│   │   │   ├── BookingActivity.java
│   │   │   ├── MyTokenActivity.java
│   │   │   ├── BookingHistoryActivity.java
│   │   │   ├── AdminQueueActivity.java
│   │   │   └── ManageSlotsActivity.java
│   │   ├── adapters/
│   │   │   ├── ServiceAdapter.java
│   │   │   ├── BookingHistoryAdapter.java
│   │   │   └── AdminQueueAdapter.java
│   │   ├── models/
│   │   │   ├── User.java
│   │   │   ├── Office.java
│   │   │   ├── OfficeService.java
│   │   │   ├── Appointment.java
│   │   │   └── QueueCounter.java
│   │   └── utils/
│   │       ├── Constants.java
│   │       └── DateTimeUtils.java
│   └── src/main/res/
│       ├── layout/
│       ├── values/
│       └── drawable/
├── firestore.rules
├── build.gradle
├── settings.gradle
└── README.md
```
