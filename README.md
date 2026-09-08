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

# Project Technology

```text
Language:
Java 21

GUI:
JavaFX

UI Markup:
FXML

Styling:
JavaFX CSS

Build Tool:
Maven

Database:
[Add database here]

AI:
[Add AI API/service here]

Version Control:
Git / GitHub
```

---

# bAI

**AI Business Assistant**

A JavaFX desktop business management application with AI-assisted features designed to help businesses manage products, inventory, sales, reports, and business operations.
