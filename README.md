# MTFinance

A finance-tracking Android app for managing personal finances, tracking budgets, and analysing spending across custom categories and accounts.

## Disclaimer

MTFinance is a personal portfolio project. It is provided "as is", without
warranty of any kind. It is not financial, tax, or accounting advice, and I
make no guarantee that calculations, budget figures, or imported data are
accurate or complete. Use it at your own risk and keep your own backups.
This project is not affiliated with or endorsed by any bank or financial
institution.

## Key Features

### Categories & Tracking Engine
- **Hierarchical Categories**: Organise expenses and income across multi-level category trees (up to 5 levels of depth) with parent category spending rollups and monthly budget limits.
- **Default System Categories**: Pre-configured root categories for General Expenses, Groceries & Utilities, Income, Account Transfers, and Other fallback transactions.

### Transactions & CSV Import
- **Transaction Management**: Manual entry and editing of transactions with multi-category tagging, amounts, dates, notes, and transaction types (Expense, Income, Account Transfer).
- **CSV Transaction Import**: Import transactions from bank export CSV files with live data preview tables, column count validation, and alias auto-detection.

### Settings & System Preferences
- **App Data Management**: Secure application data reset options (wipe all data / transactions) with confirmation dialogs.
- **Configurable Page Size**: Adjustable list pagination size (default 10 items, range 1–100) persisted in `SystemPreferences`.

### Forms, Dashboards & UI Polish
- **Interactive Forms**: Streamlined category and transaction forms equipped with global focus management, IME action navigation (`Next` / `Done`), keyboard dismissal on tap, and hybrid calendar date pickers.
- **Dashboards & Visualisations**: Category and transaction dashboards featuring custom canvas spending trend charts and summary metrics.

## Tech Stack & Architecture

- **Language**: Java 11 (Core logic, domain models, services, repositories) + Kotlin (Jetpack Compose UI, forms, state management, navigation)
- **Architecture**: **MVVM + CQRS (Command Query Responsibility Segregation)**
  - **Read Services**: Public read-only services communicating directly with Room DAOs for fast query execution.
  - **Write Repositories**: Encapsulated write repositories enforcing tracking business rules and budget validation.
- **Local Persistence**: Room Database (SQLite) with custom Converters, CrossRef DAOs, and automated root category initialization.
- **Dependency Injection**: Hilt (`@HiltViewModel`, `DatabaseModule` bindings)
- **UI Framework**: Jetpack Compose + Material 3, Navigation Compose, Hilt Navigation Compose
- **Testing Infrastructure**:
  - **JVM Unit Tests**: **JUnit 5 Jupiter** (`assertAll`, `assertThrows`, `@BeforeEach`)
  - **Android Instrumented Integration Tests**: **JUnit 4** (`AndroidJUnit4`, `Room.inMemoryDatabaseBuilder`)
- **Other Core Components**: LiveData, ViewModel, Commons CSV, Gradle Kotlin DSL, KSP
