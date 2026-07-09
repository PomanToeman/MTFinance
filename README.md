# MTFinance

First solo project: a simple Java-powered Android app called MT Finance. A finance-tracking app that lets you enter transactions with predefined budgets and categories to easily manage personal finances and goals.

## Key Features (will update as features are added)

### Categories
The main feature of the tracking app! Allows you to put transactions in specific categories to track particular spending (such as groceries, gaming, utilities, etc.). Can also put sub-categories into categories for more organised actions. The parent category keeps track of all general expenses, while a sub-category can track something specific. Can have up to 5 subs.

### Default Categories (these are automatically in place)
- **General Category** - A general Expense style category. All expense categories are sub-categories of this.
- **Income Category** - Separate category to track income. Mainly meant to exclude them from expense style categories.
- **Account Transfers** - Separate category for organisation purposes.

### Transactions
The actual transactions you get from banking apps (via CSV import), or manual entry. Will be able to store and track all necessary details like date, type (expense or income), and more.

# User Guide
None for now as no front-end has been developed.

## Tech Stack (More will be added as project develops)
- **Language**: Java (As I have good proficiency from previous work)
- **Local Database**: Room database (Due to simple implementation and native Android support)
- **Queries and DAO**: CRUD (with SQLite)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **UI**: Jetpack Compose
- **Testing**: JUnit
- **Other**: ViewModel, LiveData, Gradle.



