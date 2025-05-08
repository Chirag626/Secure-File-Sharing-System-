
Secure File Sharing System

This is a web-based project built using Spring Boot that allows users to upload, share, and manage files securely. The system includes features like encryption, auto-deletion, trash management, email alerts, and history tracking.

----------------------------------

About the Project:

This project is designed to mimic the behavior of file-sharing tools like Google Drive, but with automatic clean-up and added security. It is a single-user prototype for secure document sharing.

----------------------------------

Features:

1. File Upload
   - User can upload any type of file from local device.

2. AES Encryption
   - Uploaded files are encrypted before storing to ensure data security.
   - Even if someone accesses the storage, the file is unreadable without a key.

3. Download Link Generation
   - A unique URL is created for every uploaded file so others can download it.

4. Auto Trash System
   - Files older than a specific time are automatically moved to trash.
   - This is handled by a scheduled task running in the background.

5. Restore from Trash
   - Deleted or auto-trashed files can be recovered manually.

6. Email Notification
   - A mail is sent whenever a file is uploaded or auto-deleted.
   - Works through SMTP setup in the application properties.

7. History Tracking
   - The system keeps records of all files, including deleted ones.
   - The history section allows users to view previous uploads.

8. Frontend UI
   - Simple and responsive HTML + CSS based user interface.
   - JavaScript is used for interactive operations like file selection and modal popups.

----------------------------------

Technology Stack:

Backend:
- Java 17
- Spring Boot (used for APIs and backend logic)
- Spring Data JPA (for database operations)
- Spring Scheduler (for background auto-delete task)
- Spring Mail (for sending emails)
- AES Encryption (custom utility class for file security)

Database:
- H2 In-Memory Database (can be replaced with MySQL or PostgreSQL if needed)

Frontend:
- HTML (for structure)
- CSS (for styling)
- JavaScript (for functionality like previews and upload actions)

Project Structure:

- /controller - All HTTP request handlers
- /model - Java classes representing data (like File)
- /repository - JPA interfaces for database
- /service - Business logic (email service, file handling)
- /utils - Contains AESUtil for encryption/decryption
- /scheduler - Runs the auto-trash functionality
- /static - Contains HTML, CSS, JS used in frontend

----------------------------------

How to Run:

1. Make sure Java 17 and Maven are installed.
2. Clone the project.
3. Open terminal and run:

   mvn clean install
   mvn spring-boot:run

4. Open browser and go to:
   http://localhost:8080/index.html

5. Make sure to configure email in:
   src/main/resources/application.properties

   spring.mail.username = your_email@gmail.com
   spring.mail.password = your_app_password

----------------------------------

Future Improvements:

- Add user login and registration
- Allow expiry time selection per file
- Drag and drop file uploads
- File preview feature before downloading
- Admin panel to monitor uploads and deletions

----------------------------------

Author: [Your Name]
Email: your.email@example.com
