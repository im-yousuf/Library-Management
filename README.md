# Smart Library Management System (SLMS)

Offline JavaFX + SQLite desktop application for managing books, members, and borrowing records in schools/colleges/institutes. No internet connection or server required.

## Features

- **Authentication:** Role-based access (ADMIN vs LIBRARIAN) with BCrypt password hashing.
- **Book Management:** Add, edit, delete, and search books. Track available and total copies.
- **Member Management:** Manage member profiles and borrowing privileges.
- **Issue/Return System:** Track active loans, calculate due dates automatically, and handle returns.
- **Fine Management:** Automatically calculate late fees based on configurable rules (grace days, fine per day). Settle or waive fines with reasoning.
- **Library Layout:** Organize books hierarchically by Rack and Shelf for easy physical location tracking.
- **Global Search:** Instantly search across books, members, and shelves from any screen.
- **Reporting:** Generate comprehensive reports (Inventory, Issued, Returns, Overdue, Fines, Lost Books) and export to Excel (.xlsx) or PDF.
- **Settings & Backup:** Configure library rules and safely backup or restore the SQLite database offline.

## Prerequisites

- **JDK 17+** (JavaFX 21 requires Java 17 or newer)
- **Maven 3.8+**
- *(For Windows MSI installer generation only)* **WiX Toolset v3.11+**

Check your setup:
```bash
java -version
mvn -version
```

## Running the Application Locally

```bash
cd LibraryManagementSystem
mvn clean javafx:run
```

The first time the application runs, it will:
1. Create a `data/` folder next to the project to house `slms.db`.
2. Initialize all SQLite tables.
3. Seed a default admin account.

**Default Administrator Login:**
- Username: `admin`
- Password: `admin123`

*Make sure to change this password via Settings / User Management immediately in production.*

## Running Unit Tests

To run the full suite of unit tests on the service layer:
```bash
mvn test
```
Tests run using an in-memory SQLite database (`jdbc:sqlite::memory:`) and do not affect your local `data/slms.db`.

## Build & Packaging (Producing Installers)

### 1. Build the Runnable Fat JAR
First, package the application into a single runnable fat JAR containing all dependencies:
```bash
mvn clean package
```
This produces `target/SmartLibraryManagementSystem-1.0.0.jar`. You can run this JAR directly via `java -jar target/SmartLibraryManagementSystem-1.0.0.jar`.

### 2. Create Native Installers with `jpackage`

`jpackage` (included with JDK 14+) can wrap the JAR and bundle a private JRE so end-users do not need Java installed.

**Generate a Windows MSI Installer (Requires WiX Toolset):**
```powershell
jpackage --type msi `
  --name "Smart Library Management System" `
  --input target/ `
  --main-jar SmartLibraryManagementSystem-1.0.0.jar `
  --main-class com.slms.Main `
  --win-shortcut `
  --win-menu `
  --app-version "1.0.0"
```
This produces an MSI installer that puts the application in Program Files and adds Start Menu shortcuts.

**Generate a Windows EXE Installer (Requires WiX Toolset):**
```powershell
jpackage --type exe `
  --name "Smart Library Management System" `
  --input target/ `
  --main-jar SmartLibraryManagementSystem-1.0.0.jar `
  --main-class com.slms.Main `
  --win-shortcut `
  --win-menu `
  --app-version "1.0.0"
```

**Generate a Portable Windows Application (No WiX required):**
If you want an application directory that can be zipped and run directly without an installation wizard:
```powershell
jpackage --type app-image `
  --name "Smart Library Management System" `
  --input target/ `
  --main-jar SmartLibraryManagementSystem-1.0.0.jar `
  --main-class com.slms.Main
```
This produces a folder containing the `.exe` and bundled runtime. Zip it up and distribute.

*(Note: Add `--icon src/main/resources/icons/app_icon.ico` to any of the commands above to bundle a custom desktop icon).*

## Project Architecture

```
LibraryManagementSystem/
├── pom.xml
├── data/                      # Created at runtime (SQLite database)
└── src/
    ├── main/java/com/slms/
    │   ├── Main.java              # JavaFX Entry Point
    │   ├── controllers/           # JavaFX UI Controllers (Fines, Reports, etc.)
    │   ├── models/                # Data structures (Book, Member, Fine, etc.)
    │   ├── database/              # DatabaseManager & Schema Initialization
    │   ├── services/              # Business Logic (TransactionService, etc.)
    │   ├── utils/                 # SceneManager, Session State, Utilities
    │   └── reports/               # Report generation queries
    ├── main/resources/
    │   ├── views/                 # FXML layouts for all screens
    │   ├── css/                   # Global styles & dark mode themes
    │   └── icons/                 # Application icons and branding
    └── test/java/com/slms/        # JUnit 5 Service Tests
```
