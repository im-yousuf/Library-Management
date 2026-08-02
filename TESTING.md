# Manual QA Checklist

## 1. Authentication & Session
- [ ] Launch application.
- [ ] Attempt to login with invalid credentials (e.g., `admin` / `wrongpassword`); verify error message.
- [ ] Login with valid LIBRARIAN credentials; verify UI restricts ADMIN-only features (e.g., Settings, User Management).
- [ ] Login with valid ADMIN credentials (`admin` / `admin123`); verify full access.

## 2. Book Management
- [ ] Navigate to **Books**.
- [ ] Search for a book; verify list filters as you type.
- [ ] Add a new book (fill all fields); verify success dialog and book appears in the list.
- [ ] Edit an existing book; change title/copies and save. Verify changes.
- [ ] Attempt to add a book with an existing ISBN; verify validation error.

## 3. Member Management
- [ ] Navigate to **Members**.
- [ ] Search for a member by name or phone.
- [ ] Add a new member; verify they appear in the list with `ACTIVE` status.
- [ ] Edit the member's details.
- [ ] View member details; verify empty history initially.

## 4. Issue/Return & Fines
- [ ] Navigate to **Issue Book**.
- [ ] Select the newly created member and book. Issue the book.
- [ ] Verify book's available copies decreased by 1.
- [ ] Navigate to **Return Book**.
- [ ] Return the book *on time* (adjust due date in DB if necessary or use code to simulate).
- [ ] Return a book *late* (simulate by modifying issue date in DB to past date).
- [ ] Verify a fine is generated for the late return.
- [ ] Mark the fine as **Paid** directly or via the **Fine Management** screen.

## 5. Library Layout & Global Search
- [ ] Navigate to **Library Layout**.
- [ ] Add a new Rack and a new Shelf.
- [ ] Assign a book to the new shelf.
- [ ] Use the Global Search bar (Top Bar) to search for the book; click result and verify navigation.

## 6. Reports & Fine Management
- [ ] Navigate to **Fine Management**.
- [ ] Filter fines by "Pending" and "Paid".
- [ ] Select a fine and "Waive" it with a reason. Verify audit log.
- [ ] Navigate to **Reports**.
- [ ] Generate "Inventory Report" and export to PDF.
- [ ] Generate "Overdue Report" and export to Excel. Verify exported files exist and open correctly.

## 7. Settings, User Management & Backup (Admin Only)
- [ ] Navigate to **Settings** (if implemented). Change library name or fine per day.
- [ ] Navigate to **User Management** (if implemented). Create a new LIBRARIAN user.
- [ ] Disable a user. Attempt to login with that user; verify rejection.
- [ ] Navigate to **Backup** (if implemented). Trigger a manual backup. Verify backup file (.db/.sql) is created.
- [ ] Restore from the backup file; verify application reconnects to the database.

## 8. Final Polish & Logout
- [ ] Verify tab order in all forms (e.g., Book Form, Member Form).
- [ ] Verify 'Enter' submits forms and 'Escape' closes modal dialogs.
- [ ] Verify empty states in all tables (e.g., "No books found" instead of empty grid).
- [ ] Click Logout. Verify return to Login screen and session is cleared.
