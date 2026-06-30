# revolutionary_bank
🏦 Revolutionary Bank - Version 4.0
Revolutionary Bank is a robust, professional-grade financial platform built with JavaFX and MySQL. Featuring an architecture that scales from local serialization to a fully persistent, secure relational database, this application offers a high-contrast responsive banking experience tailored to the "Executive" brand profile.

🚀 Key Version 4.0 Updates
Since its previous release, the platform has integrated an asynchronous background task layer to support automated credit evaluations, added comprehensive visual overrides for monetary ledgers, and streamlined real-time data syncs across sub-views.

🏛 The Automated Loan Center & Background Processing
Version 4.0 introduces a state-of-the-art Automated Loan Request Center that models commercial banking lifecycles. Rather than locking up the application thread during complex database evaluations, the lending center is driven by an independent, multi-threaded background processing engine.

🔄 End-to-End Lending Workflow
1. Application Submission: Within the Loan Center sub-view, users specify their requested principal amount and input their verified monthly income using stylized custom inputs.

2. State Persistence: When submitted, the application is assigned a unique tracking ID and written to the ACID-compliant MySQL database with a structural status of PENDING.

3. Asynchronous Processing: The background service checks for pending applications, processes them against internal credit scoring logic, and instantly updates the status.

4. Instant View Synchronization: Once processed, internal event hooks tell the dashboard to update balances and transaction history logs automatically.

⚙️ Under the Hood: The Background Architecture
The lending backend is explicitly decoupled from the user interface to ensure the primary GUI thread remains stutter-free:

Isolated Thread Execution (LoanProcessor): Built using a ScheduledExecutorService pool (Executors.newSingleThreadScheduledExecutor()), the processor boots up concurrently during application initialization (App.java). It runs on an independent worker thread entirely insulated from the main JavaFX Application Thread.

Automated Risk Assessment Matrix: Every 60 seconds, the engine awakens and polls the MySQL database (UserStore.fetchAllPendingLoans()). It evaluates outstanding profiles against a rigid debt-to-income metric:

Approval Condition → Principal Amount ≤ (Monthly Income x 10)

ACID-Compliant Finalization: If an application passes the evaluation, it is flagged as APPROVED, and the principal is atomically injected directly into the user's Checking account balance. If it fails the safety rule, it is securely updated to DENIED.

Resource Lifecycle Guardrails: To prevent background zombie threads, a termination hook is bound to the primary stage layout close request (primaryStage.setOnCloseRequest()). Closing the application intercepts the thread pool, gives active processes up to 5 seconds to commit running updates safely, and cuts hanging tasks.

🔐 Advanced Security & Data Maintenance
MySQL Integration: All interactions operate over a persistent relational database model, guaranteeing complete transaction safety and cross-session integrity.

SHA-256 Cryptographic Hashing: Sensitive customer records—including passwords and secondary security questions—are irreversibly one-way hashed using SHA-256 protocols before persistence.

Persistent "Remember Me": Caches login metadata inside protected local environments to streamline subsequent user authentication sessions.

👤 Profile Management Dashboard
An intuitive Profile Command Center inside the sidebar allows users to manage their administrative identity securely:

Real-Time Identity Updates: Modify your registered name with immediate reflection across dashboard greetings and active session wrappers.

Security Rotation: Dynamically alter your security questions and answers using automated backend validation.

Credentials Verification: Force confirmation matching against current records prior to authorizing a password change.

Permanent Account Termination: Features an enterprise-grade confirmation workflow that performs safe cascading database cleanups upon account deletion.

🎨 UI/UX & Centralized CSS Styling
The look-and-feel uses a centralized styling architecture (style.css) configured to a Midnight Navy (#001f3f) and Institutional Silver (#c0c0c0) executive layout scheme:

Table View Enhancements: Table headers leverage gradient overlays to look like metallic silver plaques. Rows feature subtle alternate zebra striping (#00152b) and shift color properties dynamically on selection or mouseover events.

Anti-Ghosting Render Defenses: Custom TableCell factories clear out cached CSS classes dynamically during fast scroll passes to prevent deposit/withdrawal highlight bleeding.

Customized UI Controls: Dropdown selectors (ComboBox) match form inputs perfectly by custom-positioning arrow nodes, mapping arrow shapes to accent marks, and tinting popover contextual menu panels.

Smart Feedback Displays: Data validation forms use color-coded status elements (Sea Green for success indicators, Crimson Red for application warnings) that clear out automatically as soon as the user starts typing.

✨ Core Features
Modern Navigation Matrix: Responsive sidebar navigation where buttons dynamically light up (nav-button-active) to represent the active loaded sub-page layout.

Peer-to-Peer Transfers: Transfer funds between system accounts with real-time balance verification, input cleansing, and automatic currency formatting.

Intelligent Ledger Highlight Engine: Integrates smart color-tracking rules that highlight recent activity in bold colors (such as Emerald Green or Orange-Red) without interfering with controller cell-factory properties.

🛠 Built With
JavaFX: Core UI framework for generating responsive layout containers.

MySQL: Relational database management engine for robust data persistence.

SceneBuilder: Graphical tool utilized to construct semantic .fxml structural layouts.

CSS3: Advanced vector formatting scripts for theme identity and layout components.

🏁 Getting Started
Clone the repository:

Bash
git clone https://github.com/Gjzayas/revolutionary_bank.git

Database Setup: Run and import the provided .sql database schemas directly into your local running MySQL instance.

Open in IDE: Import the project directory inside NetBeans, IntelliJ IDEA, or Eclipse.

Clean and Build: Verify that the MySQL Connector/J driver dependency is linked properly inside your project libraries.

Run: Execute the primary system thread by launching App.java.

🧪 Testing & Quality Assurance
Revolutionary Bank utilizes an automated testing suite to validate system atomicity, isolation constraints, and the reliability of financial updates across the Java runtime environments and the relational database layer.

📋 Test Suites
Transfer Integration Test (TransferIntegrationTest): Validates the end-to-end P2P fund transfer lifecycle. It confirms that the sender's balance is debited and the recipient's balance is credited concurrently, verifies audit log entries, and invokes automated schema sweeps to wipe test values post-execution.

User Deletion Integrity Test (UserDeletionTest): Verifies database execution across account closure requests. It tests that removing a master account record triggers a CASCADE delete to clean all dependent tracking records and eliminate orphaned logs.

Relational Integrity Test: Targets foreign key constraint definitions between the tables directly at the engine level to verify structural integrity.

🛠 Prerequisites for Testing
Ensure your development environment contains the following testing libraries:

JUnit 4 & JUnit 5 (Jupiter): Core assertion wrappers and execution runners.

Hamcrest: Matcher components for evaluation log parsing.

MySQL Connector/J: Driver dependency to establish direct data connections to the test schemas.

🚦 Running Tests
Verify that your local MySQL server instance is active and the revolutionary_bank schema has been properly initialized.

Right-click the dedicated test package structure in your preferred IDE.

Select Run Test or Test File.

Monitor execution panels to confirm all structural verification passes return successful checks.



Short Video Demonstrations:

Note: If any of these short video demonstrations have trouble playing, please try refreshing your browser. Thank you, and enjoy!

🏛 New Loan Center Demonstrations:

Automatic Loan Approval upon Applying for a loan under $50,000.00:


https://github.com/user-attachments/assets/cc9715a7-dc8c-4c37-91d0-41810bb37213


Make a payment and paying off the Approved Loan:


https://github.com/user-attachments/assets/a33d8660-8567-44c1-9d07-f542c1fd5035


Automatic Loan Denial upon Applying for a loan under $50,000.00:


https://github.com/user-attachments/assets/2e74b624-5051-438d-97c1-73c16683369c


Pending high value Loan, and awating Approval:


https://github.com/user-attachments/assets/401016e5-964a-428e-8972-2252e311bc00


Approved high value Loan and making a payment:


https://github.com/user-attachments/assets/948781f2-776f-44b2-aa4b-48148db70ef7


Paying off the high value Approved Loan:


https://github.com/user-attachments/assets/f9ea7aaf-00a8-4e85-98c2-5186adadb511


Pending high value Loan, awating Denial:


https://github.com/user-attachments/assets/dfbdf45d-b46f-4252-a3ff-d5db418dcd13


https://github.com/user-attachments/assets/1d16dc9b-12c1-43f4-9860-f3d1cf73e63c


Registration Signup:


https://github.com/user-attachments/assets/8c2909a7-0510-48f0-81fd-03fdbbfc6357


https://github.com/user-attachments/assets/879c54db-db28-4b1d-a3bf-788a26d9d6ae


Login after Signup:


https://github.com/user-attachments/assets/fd13a607-6378-4942-9836-c3e9affbc7fb


Profile Update - Updating User Fullname:


https://github.com/user-attachments/assets/7170bf61-616f-44c4-b5f4-6cda585c143b


Updating User Security Question and Answer:


https://github.com/user-attachments/assets/05403ba6-fefc-4c06-910a-7c55f135f23a


Updating User Password:


https://github.com/user-attachments/assets/68790800-8df4-4357-be7c-228084b55116


https://github.com/user-attachments/assets/acdabebe-d18d-4f8f-83fc-ac95c2a906a2


Transfering Funds to another Account:


https://github.com/user-attachments/assets/84048186-5c11-4fec-8eeb-8ec8090d2303


Transaction History and Saving the Bank Statement:


https://github.com/user-attachments/assets/ef1672b6-fae1-403f-a55a-714a4818a9be


User Password Reset:


https://github.com/user-attachments/assets/5fc6aab9-bed9-41d6-9b22-2e9d3b7ac745


https://github.com/user-attachments/assets/40429520-b3ed-41e8-9c18-f2a74fe6494a


Account Dashboard and History of Receiving Account from the New User Bank Transfer:


https://github.com/user-attachments/assets/79f82b74-2f8b-4447-bc6d-ccf0e7933e17


Saving User Bank Statement:


https://github.com/user-attachments/assets/715d6e5c-165c-476e-9379-9a5261e0646c


Transfer Recipient Bank Statement:


https://github.com/user-attachments/assets/484125db-fd8c-4723-8d30-10526759a6bf


Delete User Account:


https://github.com/user-attachments/assets/08109f11-424c-4617-bfe3-db7592043dd6

