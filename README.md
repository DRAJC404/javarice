# bAI — AI Assistant Business

**bAI (AI Assistant Business)** is a Java Swing desktop application designed to help businesses manage their daily operations, including products, inventory, sales, reports, and AI-assisted business features.

---

## Requirements

Before running the project, make sure you have:

* **Java JDK 21**
* **Git**

> **Maven does NOT need to be installed.**
>
> This project uses **Maven Wrapper**, which is included in the repository.

Check Java:

```cmd
java -version
```

Check Git:

```cmd
git --version
```

---

# Getting Started

## 1. Clone the Repository

Open **Command Prompt** or **PowerShell**:

```cmd
git clone <repository-url>
```

Enter the project folder:

```cmd
cd bAI
```

---

## 2. Compile the Project

Run:

```cmd
mvnw.cmd clean compile
```

Wait until you see:

```text
BUILD SUCCESS
```

If you get `BUILD FAILURE`, **do not run the application yet**. Fix the compilation errors first.

---

## 3. Run the Application

After a successful compilation:

```cmd
mvnw.cmd exec:java
```

The **bAI — AI Assistant Business** application should start.

---

# Quick Start

After cloning:

```cmd
git clone <repository-url>
cd bAI
mvnw.cmd clean compile
mvnw.cmd exec:java
```

---

# After Making Changes

Whenever you modify, add, or delete Java files:

### Step 1 — Compile

```cmd
mvnw.cmd clean compile
```

### Step 2 — Make sure it says:

```text
BUILD SUCCESS
```

### Step 3 — Run

```cmd
mvnw.cmd exec:java
```

### In short:

```cmd
mvnw.cmd clean compile
mvnw.cmd exec:java
```

---

# Getting the Latest Changes

Before starting your work, always get the latest changes:

```cmd
git pull
```

Then compile and run:

```cmd
mvnw.cmd clean compile
mvnw.cmd exec:java
```

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
│   ├── DashboardFrame.java
│   └── Loginframe.java
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

---

# Folder Responsibilities

## `Views/`

Contains the graphical user interface of the application.

Examples:

* Login
* Dashboard
* Products
* Inventory
* Sales
* Reports
* AI Assistant

---

## `Services/`

Contains the application's business logic and operations.

Examples:

* Product management
* Inventory operations
* Sales processing
* Database operations
* AI API communication

---

## `Models/`

Contains classes that represent the application's data.

Examples:

```text
User
Product
Inventory
Sale
```

---

## `Utils/`

Contains reusable helper functions.

Examples:

* Input validation
* Formatting
* Common utility functions

---

## `database/`

Contains SQL files used for creating and setting up the database.

---

## `src/`

Contains the main entry point of the application.

```text
Main.java
```

---

# Development Workflow

Each team member should follow this workflow:

```text
        git pull
           ↓
     Make your changes
           ↓
  mvnw.cmd clean compile
           ↓
      BUILD SUCCESS?
        ↙       ↘
      NO         YES
      ↓           ↓
  Fix errors   Run application
                  ↓
          mvnw.cmd exec:java
                  ↓
             Test your work
                  ↓
            git add .
                  ↓
          git commit -m "..."
                  ↓
              git push
```

---

# Git Workflow

Before starting:

```cmd
git pull
```

After making changes:

```cmd
git add .
git commit -m "Describe your changes"
git push
```

Example:

```cmd
git add .
git commit -m "Add product management UI"
git push
```

---

# Important Rules

### 1. Always compile before pushing

```cmd
mvnw.cmd clean compile
```

Make sure you get:

```text
BUILD SUCCESS
```

### 2. Do not commit generated files

Do **not** commit:

```text
target/
*.class
```

These are already included in `.gitignore`.

### 3. Do not commit `.env`

Never push API keys, passwords, or other secrets to Git.

```text
.env
```

should remain in `.gitignore`.

### 4. If compilation fails

Do not push broken code.

Fix the errors first:

```cmd
mvnw.cmd clean compile
```

Then run:

```cmd
mvnw.cmd exec:java
```

---

# Common Commands

### Compile

```cmd
mvnw.cmd clean compile
```

### Run

```cmd
mvnw.cmd exec:java
```

### Get latest changes

```cmd
git pull
```

### Check Git status

```cmd
git status
```

### Commit changes

```cmd
git add .
git commit -m "Your message"
```

### Push changes

```cmd
git push
```

---

# Team Setup

Every team member only needs:

```text
Git
JDK 21
```

Maven is already handled by:

```text
mvnw.cmd
```

Therefore, there is **no need to install Maven manually**.

---

## bAI

**AI Assistant Business**

A simple desktop business management system with AI-assisted features.
