# MTFinance

First solo project: a simple Java-powered Android app called MT Finance. A finance-tracking app that lets you enter transactions with predefined budgets and categories to easily manage personal finances and goals.

## Key Features

### Categories
The main feature of the tracking app! Allows you to put transactions in specific categories to track particular spending (such as groceries, gaming, utilities, etc.). Can also put sub-categories into categories for more organized tracking. The parent category keeps track of all general expenses, while sub-categories track specific spending (supports up to 5 levels of depth).

### Default System Categories
- **General Category** - The root Expense category. All expense categories are descendants of this root.
- **Groceries and Utilities** - Default expense categories under General Category.
- **Income Category** - Dedicated root category to track income transactions and exclude them from expense calculations.
- **Account Transfers** - Dedicated root category for transfer transactions between accounts.
- **Other Category** - Fallback category for logging erroneous or unclassified transactions.

### Transactions
Tracks banking transactions entered manually or imported via CSV files. Persists essential details including transaction name, amount, date, type (expense, income, account transfer), generated unique hash, and multi-category associations.

### Forms & Workflows
- **Category Form** - Enables creating, editing, re-parenting, and deleting categories with budget constraints and cycle prevention.
- **Transaction Form** - Enables creating, editing, tagging multiple categories, and deleting transactions.
- **Import Form** - Imports CSV files exported by banking apps with automated header mapping and auto-category assignment.

### Lists & Dashboards
- **Category List & Dashboard** - Displays category trees, total spending (including/excluding subcategories), remaining monthly budget, and child category breakdowns.
- **Transaction List** - Displays filtered transaction streams with real-time search and type filtering.

## Tech Stack & Architecture

- **Language**: Java 11 (Core logic, domain model, services, repositories) + Kotlin 2.4 (Jetpack Compose UI & Navigation)
- **Architecture**: **MVVM + CQRS (Command Query Responsibility Segregation)**
  - **Read Services**: Public read-only services (`CategoryService`, `TransactionService`, `TrackingService`) communicating directly with Room DAOs for fast, focused query execution.
  - **Write Repositories**: Encapsulated package-private write-only repositories (`CategoryRepository`, `TransactionRepository`, `TrackingRepository`) enforcing tracking business rules, budget validation, and transaction redirect policies.
- **Local Persistence**: Room Database (SQLite) with custom Converters, CrossRef DAOs, and automated root category initialization.
- **Dependency Injection**: Hilt (`@HiltViewModel`, `DatabaseModule` bindings)
- **UI Framework**: Jetpack Compose + Material 3, Navigation Compose, Hilt Navigation Compose
- **Testing Infrastructure**:
  - **JVM Unit Tests**: **JUnit 5 Jupiter** (`assertAll`, `assertThrows`, `@BeforeEach`, custom `InstantExecutorExtension` for LiveData testing)
  - **Android Instrumented Integration Tests**: **JUnit 4** (`AndroidJUnit4`, `Room.inMemoryDatabaseBuilder`)
- **Other Core Components**: LiveData, ViewModel, Commons CSV, Gradle Kotlin DSL, KSP, Jacoco Code Coverage
