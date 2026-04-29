# FinFlow - Expense Tracker

## Group Project POE

### Overview
FinFlow is an Android expense tracking app that helps users manage their finances by recording expenses, categorizing them, setting monthly goals, and attaching receipt photos.

### Features
- User registration and login with session management
- Create, read, update, delete expense categories
- Add expense entries with amount, date, description, category, and optional receipt photo
- Set minimum and maximum monthly spending goals
- View expense list with date filtering
- View total spent per category with date filtering
- Attach photos from camera or gallery to expenses

### Tech Stack
- Kotlin
- Room Database
- GitHub Actions CI/CD
- Material Design

### Team Members & Responsibilities
| Person | Responsibility |
|--------|----------------|
| Person 1 | Project setup, GitHub Actions, Room DB entities & DAOs, Unit tests |
| Person 2 | User authentication (registration/login, password validation, session management), UI/UX |
| Person 3 | Core functionality (Categories CRUD, Add Expense, Attach Photo) |
| Person 4 | Monthly goals (min/max), Expense list with date filter, View receipt photo, Total per category report |
| Person 5 | UI polish, integration, bug fixing, APK build, demo video |

### Setup Instructions
1. Clone the repository
2. Open in Android Studio (Ladybug or later)
3. Sync Gradle
4. Run on emulator or physical device (min SDK 24)

### GitHub Actions
This project uses GitHub Actions for automated builds. Every push to main/master triggers a build and test run.

### License
Academic project – no commercial use.