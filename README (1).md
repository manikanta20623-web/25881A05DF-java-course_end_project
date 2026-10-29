# Quiz and Question Bank Management System

A Java Swing desktop application (single file, no external libraries) for building a
question bank, assembling quizzes from it, and taking timed, auto-graded quizzes.
Structured like the Employee Payroll Management System repo: data models + `StorageService`
persistence, a controller class with `main`, and one class per screen.

## Run

Requires JDK 11 or newer.

    javac QuizSystemApp.java
    java QuizSystemApp

Data is saved automatically to `quiz_data.dat` in the working directory. Ten sample
questions and one sample quiz are created on first run.

## Demo logins

| Level   | Username | Password   |
|---------|----------|------------|
| Admin   | admin    | admin123   |
| Student | student  | student123 |

## Features

**Admin**
- Add, edit, delete questions (subject, topic, difficulty, 4 options, correct answer, marks)
- Question bank with live search plus subject and difficulty filters
- CSV import / export of the question bank
- Create quizzes by hand-picking questions or picking N random ones; set the time limit
- Manage (delete) quizzes
- View all results, delete a result, export results to CSV
- Preview a quiz by taking it

**Student**
- Take any quiz: countdown timer, next/previous navigation, auto-submit when time ends
- Instant score, percentage, grade and a full answer review
- "My Results" history with average and best score

## CSV format for import

    subject,topic,difficulty,question,optionA,optionB,optionC,optionD,correct(A-D),marks

A header row is optional. Difficulty must be Easy, Medium or Hard (anything else becomes Medium).

## Class map

| Section | Classes |
|---|---|
| Data models | `User`, `Question`, `Quiz`, `QuizResult`, `StorageService` |
| Controller | `QuizSystemApp` (main, shared data, sample data) |
| Screens | `LoginFrame`, `MainMenuFrame`, `QuestionDialog`, `QuestionBankFrame`, `CreateQuizFrame`, `QuizListFrame`, `QuizPlayerFrame`, `ResultsFrame`, `ExitDialog` |

## Ideas to extend

- Student self-registration and password hashing (accounts are hard-coded in `main`)
- More question types (true/false, multiple answer, short answer)
- Swap `StorageService` for JDBC / MySQL
