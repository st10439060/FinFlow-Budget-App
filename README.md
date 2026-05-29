# FinFlow - Budget Tracker Android App

A fully functional budget tracking Android application built with Kotlin, using Firebase Firestore for online data storage and RoomDB for local offline access.

## Own Features (Part 3 - Required Documentation)

### Own Feature 1: Achievements System
FinFlow includes a gamified achievements system that rewards users for building good financial habits.

**How it works:**
- When the user opens the Achievements tab, the app automatically checks their activity
- Achievements are awarded for milestones such as logging a first expense, setting budget goals, creating categories, and staying under budget
- Each achievement awards points that accumulate on a running total
- Achievements are stored in RoomDB locally AND mirrored to Firebase Firestore so they persist online

**Achievements available:**
| Achievement | Trigger | Points |
|---|---|---|
| First Expense | Log your first expense | 10 |
| Budget Setter | Set your first monthly budget goal | 15 |
| Category Creator | Create 3 or more categories | 20 |
| Under Budget | Stay under your max goal for the month | 50 |

**Where to find it:** Tap the star icon in the bottom navigation bar.

---

### Own Feature 2: Photo Receipt Attachment
FinFlow allows users to attach a photo of their receipt to any expense entry.

**How it works:**
- When adding an expense, tap "Add Photo" to launch the device camera
- The photo is captured and previewed in the form before saving
- The photo file path is saved with the expense record
- In the Reports screen, tapping on any expense entry that has a photo will display the receipt image in a full-screen dialog
- Uses Android FileProvider for secure photo storage

**Where to find it:** When adding an expense, tap the "Add Photo" button. View photos by tapping expense entries in the Reports screen.

---

## Features Implemented

### Core Requirements (Part 2 - 100% Complete)

#### 1. User Authentication
- Local username/password authentication using RoomDB
- Password hashing with SHA-256 for security
- Login and registration functionality
- Session persistence using SharedPreferences

#### 2. Category Management
- Create custom expense categories with emoji icons
- Visual category identification with emojis and colors
- Delete categories with confirmation dialog

#### 3. Expense Entry
- **Required fields implemented:**
  - Date selection (DatePicker)
  - Start time and end time (TimePicker)
  - Description (EditText)
  - Amount (NumberFormat for currency)
  - Category selection (Dropdown)
  - Optional notes (EditText)
  - Optional photo receipt (camera)

#### 4. Photo Attachment (Own Feature 2)
- Camera integration for expense receipts
- Photo capture and preview
- FileProvider for secure photo handling
- View photos from Reports screen

#### 5. Budget Goals
- Set minimum monthly spending goal per category
- Set maximum monthly spending goal per category
- SeekBar widgets for intuitive goal setting
- Real-time value display with NumberFormat currency formatting (ZAR)
- Goals mirrored to Firebase Firestore

#### 6. Expense Reports with Bar Chart (Part 3)
- View expenses by user-selectable date range
- **Bar chart** showing spending per category (MPAndroidChart)
- **Min/max goal limit lines** on the bar chart (green = min, red = max)
- Category-wise spending breakdown text list
- Tap expense to view attached photo receipt

#### 7. Goal Compliance Visual (Part 3)
- Dashboard shows a "Goal Compliance" card for the current month
- Each category with goals shows:
  - Amount spent vs min/max range
  - Status badge: **ON TRACK** (green) / **UNDER MIN** (orange) / **OVER BUDGET** (red)
- Instant visual feedback without needing to open Reports

#### 8. Online Database - Firebase Firestore (Part 3)
- All expenses, categories, budgets, and achievements are saved to **Firebase Firestore**
- Data is stored under `users/{userId}/expenses`, `users/{userId}/categories`, etc.
- RoomDB continues to serve as a local cache for fast offline access
- Both read and write operations demonstrated

#### 9. Achievements System (Own Feature 1 - Part 3)
- Automatic achievement detection and awarding
- Points system with running total
- Achievements stored online in Firestore
- Dedicated Achievements tab in bottom navigation

---

## Technical Implementation

### Learning Units Covered

✅ **Layouts**: Multiple XML layouts for all screens  
✅ **EditText**: Username, password, amount, description, notes  
✅ **NumberFormat**: Currency formatting (South African Rand)  
✅ **SeekBar**: Three SeekBars for min goal, max goal, budget amount  
✅ **Event Handling**: Click listeners, date/time pickers, camera, category selection  
✅ **Activities**: LoginActivity (launcher), MainActivity  
✅ **Intents**: Navigation from LoginActivity to MainActivity  
✅ **RoomDB**: Local database with 6 entities  
✅ **Firebase Firestore**: Online read/write for expenses, categories, budgets, achievements  
✅ **Charts**: MPAndroidChart BarChart with LimitLines for min/max goals  

### Architecture

#### Data Layer
- **RoomDB** (local): Fast offline queries via DAOs
- **Firebase Firestore** (online): `FirebaseRepository` mirrors all writes
- **Dual-write pattern**: Every save goes to Room first, then Firestore

#### Firestore Structure
```
users/
  {userId}/
    expenses/
      {expenseId}: { amount, description, categoryId, date, ... }
    categories/
      {categoryId}: { name, emoji, color, description, ... }
    budgets/
      {budgetId}: { categoryId, monthYear, budgetAmount, minGoal, maxGoal, ... }
    achievements/
      {achievementId}: { title, description, pointsAwarded, unlockedAt, ... }
```

#### Database Entities
1. **User** - Username/password authentication
2. **Category** - Expense categories with emoji/color
3. **Expense** - Expense records with start/end times, photo, notes
4. **Budget** - Monthly budgets with min/max goals
5. **Achievement** - Gamification achievements
6. **UserProgress** - User activity tracking

#### Key Components
- **FirebaseRepository** - All Firestore read/write operations
- **ViewModels**: AuthViewModel, DashboardViewModel, ExpenseViewModel
- **Fragments**: Dashboard, AddExpense, Goals, Reports, Profile, Achievements, ManageCategories
- **Adapters**: ExpenseAdapter, CategoryProgressAdapter, AchievementAdapter
- **Utilities**: DateUtils

### Project Structure
```
android/
├── data/
│   ├── firebase/
│   │   └── FirebaseRepository.kt       ← Firestore read/write
│   ├── local/
│   │   ├── dao/          # Room DAOs
│   │   ├── database/     # RoomDB
│   │   └── entities/     # Data models
│   └── repository/
│       └── FinFlowRepository.kt        ← Local data operations
├── ui/
│   ├── adapters/         # RecyclerView adapters
│   ├── fragments/        # All app fragments
│   └── viewmodels/       # ViewModels (MVVM)
├── utils/                # DateUtils
└── res/
    ├── layout/           # XML layouts
    ├── navigation/       # Nav graph
    └── values/           # Colors, strings, themes
```

---

## Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Kotlin 1.9+
- Google Services JSON configured (firebase)

### Building
1. Clone the repository
2. Open project in Android Studio
3. Sync Gradle dependencies
4. Run on physical device (required for final submission)

### Dependencies
```gradle
// Room (local database)
androidx.room:room-runtime:2.6.1

// Firebase
firebase-bom:32.7.4
firebase-firestore-ktx
firebase-auth-ktx
firebase-analytics

// Charts
com.github.PhilJay:MPAndroidChart:v3.1.0

// Coroutines
kotlinx-coroutines-android:1.7.3

// Material Design
com.google.android.material:material:1.11.0

// Navigation
androidx.navigation:navigation-fragment-ktx:2.7.6
```

---

## GitHub Actions (CI/CD)

The `.github/workflows/build.yml` file automatically:
- Runs unit tests on every push/PR to main
- Builds the debug APK
- Uploads test results and APK as artifacts

---

## User Guide

### First Time Setup
1. Launch the app → tap "Sign Up" → create username and password
2. Login with credentials

### Adding Expenses
1. Tap the **+** floating button
2. Fill in amount, description, category, date, start/end time
3. Optionally tap **Add Photo** to attach a receipt photo
4. Tap **Save**

### Setting Budget Goals
1. Go to **Goals** tab
2. Select a category from the dropdown
3. Use SeekBars to set minimum goal, maximum goal, target budget
4. Tap **Save Budget Goal**

### Viewing Reports & Chart
1. Go to **Reports** tab
2. Select date range
3. Tap **Load Expenses**
4. The bar chart shows spending per category with green (min) and red (max) goal lines
5. Tap any expense to view its receipt photo

### Dashboard Goal Compliance
- The dashboard automatically shows a **Goal Compliance** card
- Each category with goals shows whether you are ON TRACK, UNDER MIN, or OVER BUDGET
- Updated every time you visit the dashboard

### Checking Achievements
1. Tap the **Achievements** star tab in the bottom nav
2. Achievements unlock automatically based on your activity
3. Accumulate points by reaching financial milestones

---


