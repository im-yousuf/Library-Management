# Smart Library Management System (SLMS)

A fully **offline** JavaFX + SQLite desktop application for managing books, members, and borrowing records in schools, colleges, and institutes.  
No internet connection, no server, no installation wizard required — just unzip and run.

---

## 📦 Download & Quick Start (Recommended)

> **No Java installation needed.** The portable bundle ships with a bundled JRE.

1. **Download** `SLMS-Portable-Windows.zip` from the [Releases](../../releases) page (or from the root of this repository).
2. **Extract** the ZIP to any folder (e.g., `C:\SLMS\`).
3. **Run** the application using one of:
   - Double-click **`Smart Library Management System.exe`** inside the extracted folder, **or**
   - Double-click **`run.bat`** (opens a console window alongside the app).
4. **Login** with the default administrator credentials:
   | Field    | Value     |
   |----------|-----------|
   | Username | `admin`   |
   | Password | `admin123`|

> ⚠️ Change the default password immediately via **Settings → User Management** before going into production.

On first launch, a `data/` folder is automatically created next to the application to store the SQLite database (`slms.db`). All your library data lives in that single file — back it up anytime via **Settings → Backup & Restore**.

---

## ☕ Running the Fat JAR (Requires Java 17+)

If you prefer to run the raw JAR file directly (e.g., on a machine that already has Java 17+ installed):

### Prerequisites

| Tool | Version |
|------|---------|
| JDK  | 17 or newer |

Verify your Java installation:
```bash
java -version
```

### Steps

1. **Download** `SmartLibraryManagementSystem-1.0.0.jar` from the [Releases](../../releases) page.
2. **Open a terminal** (Command Prompt or PowerShell) in the folder where you saved the JAR.
3. **Run** the JAR:
   ```bash
   java -jar SmartLibraryManagementSystem-1.0.0.jar
   ```
4. The application will launch. A `data/` folder with `slms.db` is created in the **same directory** as the JAR on first run.
5. **Login** with the default credentials above.

> **Note:** The fat JAR bundles all dependencies (including JavaFX native libraries). You do **not** need to add `--module-path` or any JavaFX flags.

---

## ✨ Features

| Category | What You Can Do |
|----------|-----------------|
| **Authentication** | Role-based access (ADMIN / LIBRARIAN) with BCrypt password hashing |
| **Book Management** | Add, edit, delete, search books; track available vs. total copies |
| **Member Management** | Manage member profiles and borrowing privileges |
| **Issue / Return** | Track active loans, auto-calculate due dates, handle returns |
| **Fine Management** | Auto-calculate late fees (configurable grace days & daily rate); settle or waive fines with reasoning |
| **Library Layout** | Hierarchical Rack → Shelf organization for easy physical location tracking |
| **Global Search** | Instant search across books, members, and shelves from any screen |
| **Bulk Import / Export** | Import books from Excel (.xlsx); export reports to Excel or PDF |
| **Reports** | Inventory, Issued, Returns, Overdue, Fines, Lost Books |
| **Settings & Backup** | Configure library rules; safely backup/restore the SQLite database offline |

---

## 🛠️ Building from Source

### Prerequisites

| Tool | Version |
|------|---------|
| JDK  | 17 or newer |
| Maven | 3.8 or newer |

```bash
java -version
mvn -version
```

### Run in Development Mode

```bash
cd LibraryManagementSystem
mvn clean javafx:run
```

### Build the Fat JAR

```bash
mvn clean package
```

Output: `target/SmartLibraryManagementSystem-1.0.0.jar`

You can run it immediately with:
```bash
java -jar target/SmartLibraryManagementSystem-1.0.0.jar
```

### Run Unit Tests

Tests run against an in-memory SQLite database and never touch your live `data/slms.db`.

```bash
mvn test
```

### Re-package a New JAR into the Portable Bundle

After making code changes, use the provided no-Maven build script to recompile and hot-swap the JAR inside the portable bundle:

```bat
build.bat
```

This script:
1. Compiles all Java sources using your system JDK (Java 17+).
2. Copies fresh FXML, CSS, and icon resources.
3. Creates a backup of the existing JAR (`*.jar.bak`).
4. Repacks the updated classes into `Smart Library Management System\app\SmartLibraryManagementSystem-1.0.0.jar`.

After a successful build, run the app via `run.bat` or `Smart Library Management System.exe`.

### Create a Native Windows Installer (Optional)

Requires **WiX Toolset v3.11+** and JDK 14+.

**MSI Installer:**
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

**Portable App Image (no WiX required):**
```powershell
jpackage --type app-image `
  --name "Smart Library Management System" `
  --input target/ `
  --main-jar SmartLibraryManagementSystem-1.0.0.jar `
  --main-class com.slms.Main
```

> Add `--icon src/main/resources/icons/app_icon.ico` to any command above to bundle a custom icon.

---

## 🏗️ Project Architecture

```
LibraryManagementSystem/
├── pom.xml
├── build.bat                  # No-Maven hot-rebuild script
├── data/                      # Created at runtime (SQLite database)
├── Smart Library Management System/   # Portable bundle (run directly)
│   ├── Smart Library Management System.exe
│   ├── run.bat
│   ├── app/
│   │   └── SmartLibraryManagementSystem-1.0.0.jar
│   └── runtime/               # Bundled JRE
└── src/
    ├── main/java/com/slms/
    │   ├── Main.java              # JavaFX Entry Point
    │   ├── controllers/           # JavaFX UI Controllers
    │   ├── models/                # Data models (Book, Member, Fine, …)
    │   ├── database/              # DatabaseManager & schema initialization
    │   ├── services/              # Business logic (TransactionService, …)
    │   ├── utils/                 # SceneManager, session state, helpers
    │   └── reports/               # Report generation queries
    ├── main/resources/
    │   ├── views/                 # FXML layouts for all screens
    │   ├── css/                   # Global styles & dark mode themes
    │   └── icons/                 # Application icons and branding
    └── test/java/com/slms/        # JUnit 5 service tests
```

---

## 🔑 Default Credentials

| Role      | Username | Password   |
|-----------|----------|------------|
| Admin     | `admin`  | `admin123` |

*Change these immediately after first login.*

---

## 📄 License

This project is released for educational and institutional use.
