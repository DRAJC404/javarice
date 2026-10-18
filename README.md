# bAI — AI Business Assistant

**bAI (AI Business Assistant)** is a JavaFX desktop application designed to help businesses manage their daily operations, including products, inventory, sales, reports, and AI-assisted business features.

The application uses **JavaFX** for the graphical user interface and **Maven** for project and dependency management.

---

# Requirements

Before running the project, make sure you have:

* **Java JDK 21**
* **Git**

Maven does **not** need to be installed manually.

This project uses the **Maven Wrapper**, which is included in the repository.

JavaFX is also managed through Maven, so there is **no need to manually install the JavaFX SDK**.

## Check Java

### Windows

```cmd
java -version
```

### Linux / macOS

```bash
java -version
```

You should have Java 21 installed.

## Check Git

```bash
git --version
```

---

# Getting Started

## 1. Clone the Repository

Open **Command Prompt**, **PowerShell**, or a terminal.

```bash
git clone <repository-url>
```

Enter the project folder:

```bash
cd bAI
```

---

# 2. Compile the Project

The Maven Wrapper handles Maven for you.

### Windows

```cmd
mvnw.cmd clean compile
```

### Linux / macOS

```bash
./mvnw clean compile
```

Wait until you see:

```text
BUILD SUCCESS
```

If you get `BUILD FAILURE`, fix the compilation errors before running the application.

---

# 3. Run the JavaFX Application

After a successful compilation:

### Windows

```cmd
mvnw.cmd javafx:run
```

### Linux / macOS

```bash
./mvnw javafx:run
```

The **bAI — AI Business Assistant** JavaFX application should start.

---

# Quick Start

## Windows

```cmd
git clone <repository-url>
cd bAI
mvnw.cmd clean compile
mvnw.cmd javafx:run
```

## Linux / macOS

```bash
git clone <repository-url>
cd bAI
./mvnw clean compile
./mvnw javafx:run
```

---

# JavaFX

This project uses:

* **Java 21**
* **JavaFX 21**
* **JavaFX Controls**
* **JavaFX FXML**
* **JavaFX CSS**
* **Maven**

JavaFX dependencies are automatically downloaded by Maven based on the project's `pom.xml`.

You do **not** need to manually download the JavaFX SDK.

---

# Project Structure

```text
bAI/
│
├── database/
│   └── database.sql
│
├── Models/
│   └── User.java
│
├── Services/
│   └── ProductService.java
│
├── Utils/
│   └── ValidationUtils.java
│
├── Views/
│   └── JavaFX views/controllers
│
├── src/
│   └── Main.java
│
├── .mvn/
│
├── mvnw
├── mvnw.cmd
├── pom.xml
├── README.md
└── .gitignore
```

As the JavaFX application develops, the `Views` folder may contain JavaFX controllers and related GUI classes, while FXML and CSS files can be stored under the project's resources directory.

---

# Folder Responsibilities

## `Views/`

Contains the graphical user interface of the application.

The application uses **JavaFX** instead of Java Swing.

Possible screens include:

* Login
* Dashboard
* Products
* Inventory
* Sales
* Reports
* AI Assistant

JavaFX components may include:

* `Stage`
* `Scene`
* `Button`
* `Label`
* `TextField`
* `TableView`
* `ComboBox`
* `VBox`
* `HBox`
* `BorderPane`
* `GridPane`

---

## `Services/`

Contains the application's business logic and operations.

Examples:

* Product management
* Inventory operations
* Sales processing
* Database operations
* AI API communication
* Business calculations

---

## `Models/`

Contains classes that represent the application's data.

Examples:

```text
User
Product
Inventory
Sale
Transaction
```

---

## `Utils/`

Contains reusable helper functions.

Examples:

* Input validation
* Formatting
* Common utility functions
* Application helpers

---

## `database/`

Contains SQL files used for creating and setting up the database.

Example:

```text
database/
└── database.sql
```

---

## `src/`

Contains the main entry point of the application.

```text
src/
└── Main.java
```

The main class starts the JavaFX application.

---

# JavaFX Architecture

The application separates the graphical interface from the business logic.

The general flow is:

```text
        JavaFX UI
            │
            ▼
       Controller
            │
            ▼
        Services
            │
            ▼
         Models
            │
            ▼
        Database
```

For example:

```text
Products.fxml
      │
      ▼
ProductController.java
      │
      ▼
ProductService.java
      │
      ▼
Product.java
      │
      ▼
Database
```

This separation makes the application easier to maintain and allows different team members to work on different parts of the project.

---

# FXML

FXML can be used to define JavaFX user interfaces separately from Java code.

Example:

```text
resources/
└── fxml/
    ├── Login.fxml
    ├── Dashboard.fxml
    ├── Products.fxml
    └── Reports.fxml
```

FXML defines the interface, while the Java controller handles its behavior.

Example:

```text
FXML
 │
 ▼
Controller
 │
 ▼
Service
```

---

# CSS

JavaFX supports CSS for styling the application's interface.

Example:

```text
resources/
└── css/
    └── style.css
```

CSS can be used for:

* Colors
* Fonts
* Buttons
* Backgrounds
* Borders
* Spacing
* UI themes

This allows the team to modify the application's appearance without changing the main application logic.

---

# Development Workflow

Each team member should follow this workflow:

```text
        git pull
           │
           ▼
    Make your changes
           │
           ▼
    Compile the project
           │
           ▼
     BUILD SUCCESS?
        /       \
      NO         YES
      │           │
      ▼           ▼
  Fix errors   Run JavaFX
                  │
                  ▼
             Test your work
                  │
                  ▼
              git add .
                  │
                  ▼
          git commit -m "..."
                  │
                  ▼
              git push
```

---

# Compile and Run

## Windows

Compile:

```cmd
mvnw.cmd clean compile
```

Run:

```cmd
mvnw.cmd javafx:run
```

## Linux / macOS

Compile:

```bash
./mvnw clean compile
```

Run:

```bash
./mvnw javafx:run
```

---

# After Making Changes

Whenever you modify, add, or delete Java files:

## Step 1 — Compile

### Windows

```cmd
mvnw.cmd clean compile
```

### Linux / macOS

```bash
./mvnw clean compile
```

## Step 2 — Make sure it says

```text
BUILD SUCCESS
```

## Step 3 — Run the application

### Windows

```cmd
mvnw.cmd javafx:run
```

### Linux / macOS

```bash
./mvnw javafx:run
```

## In short

### Windows

```cmd
mvnw.cmd clean compile
mvnw.cmd javafx:run
```

### Linux / macOS

```bash
./mvnw clean compile
./mvnw javafx:run
```

---

# Getting the Latest Changes

Before starting your work, always get the latest changes:

```bash
git pull
```

Then compile and run the application.

### Windows

```cmd
mvnw.cmd clean compile
mvnw.cmd javafx:run
```

### Linux / macOS

```bash
./mvnw clean compile
./mvnw javafx:run
```

---

# Git Workflow

## Before Starting Work

Always get the latest changes:

```bash
git pull
```

## After Making Changes

Check your changes:

```bash
git status
```

Add your changes:

```bash
git add .
```

Commit your changes:

```bash
git commit -m "Describe your changes"
```

Push your changes:

```bash
git push
```

Example:

```bash
git add .

git commit -m "Add product management UI"

git push
```

---

# Important Rules

## 1. Always Compile Before Pushing

Make sure the project compiles successfully before pushing your changes.

### Windows

```cmd
mvnw.cmd clean compile
```

### Linux / macOS

```bash
./mvnw clean compile
```

Make sure you get:

```text
BUILD SUCCESS
```

---

## 2. Do Not Commit Generated Files

Do **not** commit:

```text
target/
*.class
```

These files should remain in `.gitignore`.

---

## 3. Do Not Commit `.env`

Never push API keys, passwords, database credentials, or other secrets to Git.

Do not commit:

```text
.env
```

Make sure `.env` is included in `.gitignore`.

---

## 4. If Compilation Fails

Do not push broken code.

Fix the errors first.

### Windows

```cmd
mvnw.cmd clean compile
```

### Linux / macOS

```bash
./mvnw clean compile
```

After receiving:

```text
BUILD SUCCESS
```

run the application:

### Windows

```cmd
mvnw.cmd javafx:run
```

### Linux / macOS

```bash
./mvnw javafx:run
```

---

# Common Commands

## Compile

### Windows

```cmd
mvnw.cmd clean compile
```

### Linux / macOS

```bash
./mvnw clean compile
```

## Run JavaFX

### Windows

```cmd
mvnw.cmd javafx:run
```

### Linux / macOS

```bash
./mvnw javafx:run
```

## Get Latest Changes

```bash
git pull
```

## Check Git Status

```bash
git status
```

## Add Changes

```bash
git add .
```

## Commit Changes

```bash
git commit -m "Your message"
```

## Push Changes

```bash
git push
```

---

# Team Setup

Every team member needs:

```text
Git
JDK 21
```

Maven does **not** need to be installed manually.

The project includes the Maven Wrapper:

```text
mvnw
mvnw.cmd
```

JavaFX dependencies are automatically handled by Maven through `pom.xml`.

---

# Maven Wrapper

The Maven Wrapper allows the project to use Maven without requiring each team member to install Maven separately.

### Windows

```cmd
mvnw.cmd clean compile
```

### Linux / macOS

```bash
./mvnw clean compile
```

If Linux gives a permission error:

```bash
chmod +x mvnw
```

Then:

```bash
./mvnw clean compile
```

---

# Troubleshooting

## Java Version

Check your Java version:

```bash
java -version
```

The project requires:

```text
Java 21
```

---

## Maven Build Failure

Try:

```bash
./mvnw clean compile
```

If you are using Windows:

```cmd
mvnw.cmd clean compile
```

Read the compilation error and fix it before running the application.

---

## JavaFX Application Does Not Start

Make sure the project compiles successfully first:

```bash
./mvnw clean compile
```

Then run:

```bash
./mvnw javafx:run
```

---

# Project Technology & Architecture

```text
Language:        Java 21 (LTS)
GUI Framework:   JavaFX 21 (Pure Java programmatic UI, no FXML)
Styling:         Modern JavaFX CSS (theme variables, dark/light accents, SVGs)
Connection Pool: HikariCP
Database:        PostgreSQL (ACID transactions, pessimistic locking, Row-Level Security)
Security:        BCrypt (work factor 12), SHA-256 tokens, SecureRandom 32-char alphabet
Currency:        Philippine Peso (₱)
Build Tool:      Maven Wrapper (mvnw)
```

---

# Onboarding & Authentication Architecture

bAI uses a multi-tenant business structure designed for retail operations:

### 1. Business Owner Registration
* Only the **Business Owner** registers publicly via the **"Create a business account"** portal.
* Automatically creates the store's `organizations` record, the owner user (`OWNER` role, `APPROVED` status), and an immutable audit trail entry in a single atomic database transaction.
* Owners have access to the full management console (Dashboard, Products, Inventory, Analytics, AI Insights, Reports, and Team).

### 2. Staff Onboarding via 10-Minute Join Codes
Staff members do **not** register publicly and there are no invite links or approval queues.

1. **Generation (Owner Only)**:
   * The store owner navigates to the **Team** page and clicks **"Add Team Member"**.
   * Selects the role: `Store Associate (EMPLOYEE)` or `Inventory Manager (MANAGER)`. A business can never generate another `OWNER`.
   * An 8-character code is generated from a 32-character unambiguous alphabet (`ABCDEFGHJKLMNPQRSTUVWXYZ23456789`, excluding confusing characters `0`, `1`, `I`, `O`).
   * The code is displayed formatted as `XXXX-XXXX` **ONCE** with a live 10:00 countdown timer, a **Copy Code** button, and a **Cancel Code** button.
   * **Airtight Security**: The raw code is never stored in the database (only its SHA-256 hash is saved). Once the modal is closed, the raw code can never be retrieved again.
   * Organizations are capped at a maximum of 5 concurrent active join codes.
2. **Redemption (Staff Join)**:
   * Staff click **"Join with a code"** on the login screen.
   * Enter the 8-character invitation code, full name, work email, and create a password (minimum 8 characters with at least one letter and one number).
   * **Atomic Transaction**: Evaluates `expires_at > NOW()` inside SQL with pessimistic row locking (`SELECT ... FOR UPDATE`).
   * On redemption, the staff account is created in `APPROVED` status with the pre-assigned role, the join code is marked used, and an audit log entry is recorded.
   * Any invalid, expired, used, or cancelled code returns the uniform response: `"Invalid or expired code."`
   * Join attempts are rate-limited to 5 per minute per device with automated exponential backoff on repeated failures.
3. **Automated Hygiene**:
   * Expired join codes older than 1 day are automatically purged during code generation and maintenance jobs.

---

# AI Configuration & Security Architecture

bAI integrates AI-driven analytics, restocking recommendations, and the **Ask bAI** inventory copilot.

### 1. Environment Configuration (`.env`)
To configure AI services locally, copy `.env.example` to `.env` in the working directory:
```bash
cp .env.example .env
```
Populate the environment variables:
```properties
# AI Configuration (.env)
AI_API_KEY=your_gemini_or_groq_api_key_here
AI_MODEL=gemini-1.5-flash
AI_BASE_URL=
```
* **Resolution Order**: `EnvConfig` reads real system environment variables first (`System.getenv`), then falls back to `.env` in the working directory.
* **Safe Diagnostics**: The application never displays or logs the raw API key. All diagnostic outputs and logs mask keys (e.g., `gsk_...****` or `[NOT CONFIGURED]`).
* **Rule-Based Fallback**: If `AI_API_KEY` is omitted, bAI smoothly falls back to an offline rule-based heuristic engine with a small `"AI model not configured"` notice.

### 2. Architecture & Production Notice
> [!WARNING]
> **DEMO ONLY NOTICE**: Shipping an AI API key inside a client-side desktop application binary or `.env` file is strictly intended for local development and offline demonstrations. In a commercial or production environment, desktop clients must **never** hold raw LLM provider API credentials. All AI requests must be routed through an authenticated backend API service that securely holds credentials, enforces per-tenant quotas, and performs server-side prompt validation.

### 3. Tenant Privacy & Aggregation Safeguards
* **Zero PII**: Only aggregated, anonymous inventory totals (e.g., product counts, total units deducted, velocity numbers) for the authenticated tenant are transmitted to the LLM. No user names, emails, customer records, or internal database IDs are ever sent.
* **Tenant Isolation**: "Ask bAI" and analytics queries are strictly scoped to the authenticated organization's verified session via PostgreSQL Row-Level Security (`app.current_organization_id`).
* **Rate Limits & Caching**: LLM calls are limited to 20 calls per organization per day (tracked in `ai_usage_tracking`), and cached in `ai_insights` with a 1-hour expiration window.
* **7-Day History Guard**: Stores with fewer than 7 days of movement records automatically skip LLM calls and display rule-based insights.

---

# bAI — AI Business Assistant

AI-Powered Inventory & Retail Management Assistant built for Filipino retail store employees and business owners. Real-time stock tracking, automated par-level alerts, intelligent shift audits, and full-screen kiosk support.
