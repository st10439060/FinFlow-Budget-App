# FinFlow - Budget Tracker App

The FinFlow app is a budget tracker for Android phones, created by a team of five students as our final project of POE. The basic concept of the app is to help its users track their expenses, achieve their financial objectives and follow their budget effectively instead of just recording expenditures.

To create a motivation-driven application, we included some elements such as achievements badges and goal compliance indicators on the main page of the application. Everything can be done even without an internet connection because FinFlow uses Room (local SQLite database). It saves all the data in the cloud (Firestore Firebase).

**Demonstration Video:** https://youtu.be/FcEfPudmoQg?si=peA7a2mJGtht6s5a

**GitHub Repo:** https://github.com/st10439060/FinFlow-Budget-App

---

## Purpose of the App

Managing finances is a problem that many people face due to lack of clarity regarding their expenditure pattern. The FinFlow application makes this process easier by allowing the user to categorize his expenditures, fix lower/upper limits of spending for each category and finally visualize his expenditure pattern through various graphs shown on the dashboards and report screens.

The application is designed for individuals requiring a solution which is simpler than spreadsheets but more personalized compared to existing platforms. Everything is linked to a user account and hence the same device can be used by multiple people with different sets of data.

---

## Own Features (Documented for Marking)

### Own Feature 1: Achievements System

A gamification component was integrated into our application as a way of rewarding the user for forming positive money-saving behaviors. As soon as you open the Achievements page, your activities will be analyzed, and badges will be unlocked accordingly.

The badges available are:

| Achievement | What unlocks it | Points |
|---|---|---|
| First Expense | Log your first expense | 10 |
| Budget Setter | Set your first monthly budget goal | 15 |
| Category Creator | Create 3 or more categories | 20 |
| Under Budget | Stay under your max goal for the month | 50 |

The earned points progress into a level progression system from Beginner to Legend (7 levels). Additionally, there is a daily streak tracker which counts how many consecutive days you have registered a spending. All data concerning achievements is stored on the local database RoomDB and synchronized to Firestore in case of reinstallation.

**Where to find it:** Tap the star icon in the bottom navigation bar.

---

### Own Feature 2: Photo Receipt Attachment

To add an expense, one could tap "Add Photo" button and use phone camera to take a picture of receipt. The picture will be shown in preview mode on top of the form before saving so one could re-take it if necessary. Picture location will be saved together with other expense attributes to the database.

From Report screen, user will see a picture view of the receipt when he taps the entry with picture attached to. User won't need to search for his receipt from Camera Roll as we have provided the functionality where user can easily access receipt by just tapping on its entry. Receipt attachment icon will be seen next to expense entries with pictures.

For photo attachment, Android's FileProvider API was used for security reasons – no exposed file paths.

**Where to find it:** "Add Photo" button on the Add Expense screen. View receipts by tapping expense entries in Reports.

---

## Features

### Login and Registration
- Toggle between Login and Sign Up on the same screen
- Password validation on sign up: minimum 8 characters, must include an uppercase letter, a digit, and a special character
- Passwords are hashed with SHA-256 before being stored — never saved in plain text
- Session stays active for 30 days of inactivity — after that it expires and shows "Session expired"
- Progress indicator shows while the app is authenticating
- Full back-stack is cleared on login success so the user can't navigate back to the login screen

### Dashboard
- Shows monthly budget total, amount spent, and remaining balance
- Progress bar showing what percentage of the budget has been used
- Per-category progress using CategoryProgressAdapter
- Goal Compliance cards dynamically generated per category: ON TRACK (green), UNDER MIN (orange), OVER BUDGET (red) — shows actual spending vs min/max range in ZAR
- Push notifications fire at 90% and 100% of a category's max goal, deduplicated per month

### Add Expense
- Fields: amount, description, notes, category dropdown (live, updates when new categories are created), date picker (defaults to today), start and end time pickers (24-hour format)
- Photo attachment via camera (Own Feature 2)
- Full input validation with Toast error messages
- Saves to Room and mirrors to Firebase Firestore

### Reports
- Date selector slider (by default the last 30 days)
- Bar graph for expenditure per category with dotted lines representing goals (green line for minimum, red for maximum)
- Pie/Donut graph illustrating the proportion of expenditure according to category
- Category selector drop-down list and live text box for filtering according to description and notes
- Tap an entry to see its receipt image in fullscreen mode
- Press and hold an entry to modify it in place (description, amount, notes)
- Export as CSV file stored in Downloads folder, columns: Date, Description, Category, Amount, Notes
- Totals list by category and total expenditure in ZAR displayed under graphs

### Goals
- Choose a category and set minimum goal, maximum cap and target budget using three SeekBars
- Range for all three SeekBars: R0 to R10,000
- Currency value under each SeekBar using NumberFormat (ZAR)
- Ensures minimum value isn’t larger than maximum value when saving
- Saved to Room and Firestore

### Manage Categories
- Create a category by providing name, an optional emoji and description
- User chooses one of 7 colours available (Red, Orange, Yellow, Green, Teal, Blue and Brown)
- Deleting categories via confirmation dialog
- List shown in RealTime Recycler View

### Achievements (Custom Feature 1)
- 4 badges with an emoji, title, description, point and unlock date
- 7-levels system: Beginner, Saver, Tracker, Planner, Expert, Master and Legend
- Progress bar with points required to unlock the next level
- Daily streak count based on number of consecutive days with recorded spending
- Saved to Room and Firestore

### Profile
- Displays username
- Lifetime stats: total expenses logged, category count, total amount spent
- Manage Categories shortcut button
- Logout clears session and returns to Login screen

---

## Learning Units Covered

| Learning Unit | Where it's used |
|---|---|
| Layouts | XML layouts for all screens |
| EditText | Username, password, amount, description, notes fields |
| NumberFormat | Currency formatting in ZAR throughout the app |
| SeekBar | Three SeekBars for min goal, max goal, and budget amount in Goals screen |
| Event Handling | Click listeners, date/time pickers, camera, category dropdown |
| Activities | LoginActivity (launcher), MainActivity |
| Intents | Navigation from LoginActivity to MainActivity |
| RoomDB | Local database with 6 entities |
| Firebase Firestore | Online read/write for expenses, categories, budgets, achievements |
| Charts | MPAndroidChart BarChart with LimitLines for min/max goals |

---

## Data and Backend

All information is saved in two places at once; this is known as the dual-write approach:

1. The **Room (SQLite)** database deals with offline storage so that the application operates fully offline. All reads occur from Room for efficiency.
2. **Firebase Firestore** serves as an online storage mechanism so that the data will be backed up online.

Whenever a user makes any changes (whether saving, modifying, or deleting something), it gets saved to the Room and from there to Firestore through coroutines with try-catch in order not to cause an application crash due to lack of Internet connection.

**Firestore structure:**
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

**Room entities:** User, Category, Expense, Budget, Achievement, UserProgress

---

## Architecture

The app follows MVVM (Model-View-ViewModel):

- **ViewModels** hold UI state and call the repository
- **Fragments** observe LiveData and update the UI
- **FinFlowRepository** handles all Room operations
- **FirebaseRepository** handles all Firestore read/write operations

### Key components
- `FirebaseRepository` — all Firestore operations
- `ViewModels`: AuthViewModel, DashboardViewModel, ExpenseViewModel
- `Fragments`: Dashboard, AddExpense, Goals, Reports, Profile, Achievements, ManageCategories
- `Adapters`: ExpenseAdapter, CategoryProgressAdapter, AchievementAdapter
- `Utilities`: DateUtils

### Project structure
```
app/src/main/java/.../
├── data/
│   ├── firebase/FirebaseRepository.kt
│   ├── local/
│   │   ├── dao/
│   │   ├── database/
│   │   └── entities/
│   └── repository/FinFlowRepository.kt
├── ui/
│   ├── adapters/
│   ├── fragments/
│   └── viewmodels/
└── utils/
```

---

## GitHub and GitHub Actions

### How we used GitHub

All development took place on a shared `master` branch via a sequential pull request strategy – each team member pulled the last commits, completed their assigned part, committed with a descriptive commit message and then pushed before the next one took over. This process simplified things by preventing merge issues.

There were no forced pushes into the master branch during the entire development cycle.

### GitHub Actions (CI/CD)

The `.github/workflows/build.yml` GitHub Actions workflow script is triggered automatically upon every push to the repository. The workflow performs two actions:

1. **Performs unit testing** to ensure that core functionality remains intact with each push
2. **Constructs the debug APK** to make sure the application builds cleanly on a fresh environment

The outputs of both these actions are saved as artifacts and can be accessed from the Actions tab on GitHub. This guarantees that any code present in the repository is able to be compiled and used.

We followed the setup guides at:
- https://github.com/marketplace/actions/automated-build-android-app-with-github-action
- https://github.com/IMAD5112/Github-actions/blob/main/.github/workflows/build.yml

---

## User Guide

### Setup for the first time
1. Start the application and click on **Sign Up**
2. Input a username and a password (must contain at least eight characters, include one uppercase letter, one number and one special symbol)
3. Click on **Login**

### Inputting Expenses
1. Click on the **+** button
2. Enter in amount, description, category, date, and start-end times
3. Click **Add Photo** to add a receipt picture (optional step)
4. Click **Save**

### Creating budget goals
1. Open the **Goals** tab
2. Choose a category from the drop-down menu
3. Utilize the Seek Bars to establish the lowest, highest and expected values
4. Click **Save Budget Goal**

### Exploring reports and graphs
1. Select the **Reports** tab
2. Select a date range and then click **Load Expenses**
3. The graph displays the spending by categories, with the lowest budget goal denoted by green lines and highest goal with red lines
4. Clicking an expense shows the receipt photo

### Monitoring progress towards goals on the dashboard
- The dashboard provides a Goal Compliance card that displays information about the current month
- Each category that contains goals will display either On Track, Under Minimum, or Over Budget status
- Updated automatically whenever you access the dashboard

### Reviewing accomplishments
1. Click on the **star** button from the bottom navigation panel
2. Achievements are unlocked automatically depending on your actions
3. Earn points to increase levels

---

## Setup Instructions

- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- Kotlin 1.9+
- Requires `google-services.json` (Firebase config file)

**To build and run:**
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on a physical Android device

**Key dependencies:**
```gradle
androidx.room:room-runtime:2.6.1
firebase-bom:32.7.4
firebase-firestore-ktx
firebase-auth-ktx
firebase-analytics
com.github.PhilJay:MPAndroidChart:v3.1.0
kotlinx-coroutines-android:1.7.3
com.google.android.material:material:1.11.0
androidx.navigation:navigation-fragment-ktx:2.7.6
```

---

## Submission Details

- **GitHub repo:** https://github.com/st10439060/FinFlow-Budget-App
- **Demo video:** https://youtu.be/FcEfPudmoQg?si=peA7a2mJGtht6s5a
- Built APK included in the repository
- Research and design documents from Part 1 included

## Compiled by Group 4 Members:
- Wade Rowe - ST10439060
- Thabang Kobe - ST10440553
- Shuaib Mohamed - ST10437501
- Darian Nair – ST10445414
- Muhammed Suliman – ST10433999
