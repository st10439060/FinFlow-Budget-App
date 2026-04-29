# 💸 FinFlow – Expense Tracker

## Group Project POE | Android Development

---

## What is FinFlow?

FinFlow is an Android app we built to help people keep track of their spending. The idea is pretty simple – life gets busy and it's easy to lose track of where your money actually goes. With FinFlow, you can log your expenses, sort them into categories, set monthly spending goals for yourself, and even snap a photo of your receipt so you don't lose it.

It's nothing fancy, but it does the job and we're proud of what we put together as a group!

---

## What the App Can Do

Here's a quick rundown of all the features we managed to get working:

- **Sign up & Log in** – Create an account and stay logged in between sessions
- **Expense Categories** – Add, edit, view, and delete your own custom categories
- **Log an Expense** – Record how much you spent, when, what it was for, which category it falls under, and optionally attach a photo of the receipt
- **Monthly Goals** – Set a minimum and maximum amount you want to spend each month
- **Filter Your Expenses** – Browse your expense history and filter by date range
- **Category Totals** – See how much you've spent per category over any date range
- **Receipt Photos** – Attach a photo from your camera or gallery, and view it later

---

## Tech We Used

| Tool            | What it's for                                        |
|-----------------|------------------------------------------------------|
| Kotlin          | Main programming language for the app                |
| Room Database   | Storing all the app data locally on the device       |
| GitHub Actions  | Automatically builds and tests the app on every push |
| Material Design | Makes the UI look clean and consistent               |

---

## Our Team

| Name             | Student Number | What They Worked On                                                                |
|------------------|----------------|------------------------------------------------------------------------------------|
| Wade Rowe        | ST10439060     | Project setup, GitHub Actions CI/CD, Room DB entities & DAOs, Unit tests           |
| Thabang Kobe     | ST10437501     | User authentication (registration, login, password validation, session management) |
| Shuaib Mohamed   | ST10437501     | Core functionality – Categories CRUD, Add Expense screen, Attach Photo feature     |
| Darian Nair      | ST10445414     | Monthly goals (min/max), Expense list with date filter, Category totals report     |
| Muhammed Suliman | ST10433999     | UI polish, app integration, bug fixing, APK build, demo video                      |

---

## How to Run the App

Follow these steps to get it running on your machine:

1. **Clone the repo** – `git clone <repo-url>`
2. **Open in Android Studio** – Make sure you're on **Ladybug** or a newer version
3. **Sync Gradle** – Let Android Studio pull all the dependencies (just click *Sync Now* if it asks)
4. **Run it** – Use an emulator or plug in your phone (minimum Android SDK 24)

> ⚠️ If Gradle gives you grief, try *File > Invalidate Caches / Restart* and sync again.

---

## GitHub Actions (CI/CD)

We set up a basic CI/CD pipeline using GitHub Actions. Basically, every time someone pushes to `main` or `master`, it automatically triggers a build and runs the unit tests. This helped us catch broken code before it caused bigger problems for the rest of the group.

---

## License

This is an academic project submitted for our course. It's not intended for commercial use – please don't redistribute it.