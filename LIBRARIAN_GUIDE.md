# Smart Library Management System (SLMS) - User Guide for Librarians

Welcome to the Smart Library Management System! This guide will help you get started with the application. The system is designed to be fully offline, meaning you do not need an active internet connection to manage your library.

## Getting Started

### 1. Installation
1. Download the **SLMS Installer** (the `.exe` or `.msi` file) from our website.
2. Double-click the downloaded file and follow the installation wizard.
3. *Note: You do not need to install Java or any other prerequisites. Everything the app needs is already bundled!*

### 2. First Launch & Login
When you open the application for the first time, you will be presented with a login screen.
- **Default Username:** `admin`
- **Default Password:** `admin123`

*(Note: It is highly recommended that you change this password once you are logged in by going to the settings/user management section!)*

---

## Navigating the Application

The application features a sidebar on the left for easy navigation between different modules. 

### 📚 Book Management
- **View Books:** See a complete list of all books in the library. Use the search bar to quickly find books by Title, Author, ISBN, or Category.
- **Add a Book:** Click **"+ Add Book"** to enter a new book into the system. You'll need to provide details like title, author, quantity, and which shelf the book belongs to.
- **Edit/Delete:** Use the action buttons next to each book to update its details or remove it from the system. *(Note: You cannot delete a book if it is currently issued to a member).*

### 👥 Member Management
- **View Members:** See all registered library members (students, staff, etc.).
- **Add a Member:** Click **"+ Add Member"** to register someone new. You can fill in their Roll Number, Department, Phone, and other details.
- **Edit/Delete:** Update member details as needed. *(Note: You cannot delete a member if they have pending book returns).*

### 🔄 Issue & Return Books
- **Issue Book:**
  1. Go to the **Issue Book** section.
  2. Search for and select the **Member**.
  3. Search for and select the **Book**.
  4. Confirm the details and click **Issue Book**. The system will automatically calculate the due date.
- **Return Book:**
  1. Go to the **Return Book** section.
  2. Search for the member returning the book.
  3. The system will display all books currently issued to them. Click **Return** next to the correct book.
  4. If the book is late, the system will automatically generate a fine!

### 💰 Fine Management
- **View Fines:** See a list of all fines. You can filter by "Pending" or "Paid".
- **Settle Fines:** When a member pays their fine, click the action button to mark it as **Paid**.
- **Waive Fines:** If you need to forgive a fine, you can choose to **Waive** it and provide a reason. This action is recorded in the system audit log.

### 🏢 Library Layout
Use this section to map out the physical layout of your library.
- **Racks & Shelves:** Create visual representations of your physical library racks and shelves.
- **Assign Books:** Assign specific books to specific shelves so you always know exactly where a book is located.

### 📊 Reports
Generate detailed reports to keep track of your library's health. You can generate reports for:
- Current Inventory
- Overdue Books
- Fines Collected
- ...and more!
All reports can be exported directly to **Excel (.xlsx)** or **PDF** for easy printing and sharing.

---

## Troubleshooting & Tips

- **Search is Everywhere:** Use the Global Search bar at the top of the application to instantly find a book or a member from any screen.
- **Missing Data:** The application saves all data locally on your computer. Make sure you regularly back up the `data/slms.db` file (or use the built-in Backup tool if configured) to a USB drive or cloud storage to prevent data loss in case your computer breaks.
- **Need Help?** If you encounter any bugs or need assistance, please contact your IT administrator or the website where you downloaded this application.
