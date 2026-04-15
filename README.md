# revolutionary_bank
🏦 Revolutionary Bank - Version 3.0
Revolutionary Bank is a robust, professional-grade financial platform built with JavaFX and MySQL. Transitioning from local serialization to a relational database, this application now offers a fully persistent, secure, and responsive banking experience.

🚀 Key Version 3.0 Updates
Since the initial release, the platform has undergone a complete architectural overhaul to improve security, scalability, and user experience.

🔐 Advanced Security & Data Maintenance
MySQL Integration: Replaced local object serialization with a robust MySQL backend, ensuring data integrity across sessions.

SHA-256 Cryptographic Hashing: All sensitive data, including passwords and security answers, are one-way hashed using SHA-256 before being persisted to the database.

Persistent "Remember Me": The Login screen now features a "Remember Me" utility that securely caches credentials for a streamlined return experience.

👤 Profile Management Dashboard
An entirely new Profile Command Center has been added to the dashboard, allowing users to:

Real-time Identity Updates: Modify your Full Name with immediate reflection in the Dashboard greeting.

Security Overhaul: Update your unique Security Question and Answer (fully hashed) at any time.

Secure Password Rotation: Change your password with built-in verification of your current credentials.

Account Termination: A permanent "Delete Account" feature with a professional confirmation workflow.

🎨 UI/UX & Responsive Design
Password Strength Indicators: Integrated dynamic ProgressIndicator logic across the Signup, Profile, and Reset Password screens.

Password Visibility Toggles: All password fields now feature "Eye" icons to toggle between masked and plain-text for better accessibility.

Responsive Signup & Reset Screens: Redesigned layouts to ensure all components scale gracefully across different window sizes.

Smart Feedback Labels: All input screens now utilize color-coded status labels (Sea Green for success, Crimson for errors) that clear automatically when the user starts typing.

✨ Core Features
Modern UI/UX: A consistent Midnight Navy and Institutional Silver theme applied across all screens using a centralized CSS architecture.

Dynamic Dashboard: Sidebar navigation where buttons light up to indicate the active view.

Peer-to-Peer Transfers: Transfer funds between platform accounts with real-time balance validation and currency formatting.

Intelligent Financial Ledger: A styled TableView utilizing custom CSS for a high-end financial ledger look, featuring alternate row striping.

🛠 Built With
JavaFX: Core framework for the graphical user interface.

MySQL: Relational database management for persistent data storage.

SceneBuilder: Used for designing responsive .fxml layouts.

CSS3: Custom styling for brand identity and high-fidelity UI components.

🏁 Getting Started
Clone the repo:

Bash

git clone https://github.com/Gjzayas/revolutionary_bank.git

Database Setup: Import the provided .sql schema into your MySQL instance.

Open in IDE: Import the project into NetBeans, IntelliJ, or Eclipse.

Clean and Build: Ensure the MySQL Connector/J driver is included in your libraries.

Run: Launch the application via App.java.


🧪 Testing & Quality Assurance
Revolutionary Bank utilizes an automated testing suite to ensure data integrity and the reliability of financial transactions. These tests validate the Atomicity and Durability of the system, specifically focusing on the interactions between the Java application and the MySQL database.

📋 Test Suites
Transfer Integration Test (TransferIntegrationTest):

Validates the end-to-end P2P fund transfer lifecycle.

Ensures that when a transfer occurs, the sender’s balance is debited and the recipient’s balance is credited simultaneously.

Verifies that audit logs (Transactions) are generated for both parties and persisted correctly.

Includes an automated cleanup routine that purges test data after completion.

User Deletion & Integrity Test (UserDeletionTest):

Verifies that account deletion works across the entire database.

Confirms that removing a user correctly triggers a CASCADE delete, wiping all associated transaction history to prevent orphaned data.

Relational Integrity Test:

Specifically targets the foreign key relationships between the users and transactions tables to ensure database constraints are functioning as expected at the engine level.

🛠 Prerequisites for Testing
To run the automated tests within your IDE (NetBeans, IntelliJ, or Eclipse), ensure your project environment includes the following dependencies:

JUnit 4 & JUnit 5 (Jupiter): The core testing frameworks used for test runners and assertions.

Hamcrest: Required for advanced matchers and descriptive error messages during assertions.

MySQL Connector/J: Necessary for the tests to establish a live connection to the test schema.

🚦 Running Tests
Ensure your local MySQL server is running and the revolutionary_bank schema is initialized.

Right-click the test package in your IDE.

Select "Run Test" or "Test File."

View the results in the Test Results window to confirm all green checks.



Short Video Demonstrations:

Registration Signup Demonstration:


https://github.com/user-attachments/assets/8c2909a7-0510-48f0-81fd-03fdbbfc6357


https://github.com/user-attachments/assets/879c54db-db28-4b1d-a3bf-788a26d9d6ae


Login after Signup Demonstration:


https://github.com/user-attachments/assets/fd13a607-6378-4942-9836-c3e9affbc7fb


Profile Update Demonstration:
Updating User Fullname:


https://github.com/user-attachments/assets/7170bf61-616f-44c4-b5f4-6cda585c143b


Updating User Security Question and Answer:


https://github.com/user-attachments/assets/05403ba6-fefc-4c06-910a-7c55f135f23a


Updating User Password:


https://github.com/user-attachments/assets/68790800-8df4-4357-be7c-228084b55116


https://github.com/user-attachments/assets/acdabebe-d18d-4f8f-83fc-ac95c2a906a2


Transfering Funds to another Account Demonstration:


https://github.com/user-attachments/assets/84048186-5c11-4fec-8eeb-8ec8090d2303


Transaction History and Saving Bank Statement Demonstration:


https://github.com/user-attachments/assets/ef1672b6-fae1-403f-a55a-714a4818a9be


User Password Reset Demonstration:


https://github.com/user-attachments/assets/40429520-b3ed-41e8-9c18-f2a74fe6494a


Account Dashboard and History of Receiving Account from New User Bank Transfer Demonstration:


https://github.com/user-attachments/assets/79f82b74-2f8b-4447-bc6d-ccf0e7933e17


Saving User Bank Statement Demonstration:


https://github.com/user-attachments/assets/715d6e5c-165c-476e-9379-9a5261e0646c


Transfer Recipient Bank Statement Demonstration:


https://github.com/user-attachments/assets/484125db-fd8c-4723-8d30-10526759a6bf


Delete User Account Demonstration:


https://github.com/user-attachments/assets/08109f11-424c-4617-bfe3-db7592043dd6

