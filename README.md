# MTFinance

First solo project: a simple Java-powered Android app called MT Finance. A finance-tracking app that lets you enter transactions with predefined budgets and categories to easily manage personal finances and goals.

## Key Features (will update as features are added)

### Categories
The main feature of the tracking app! Allows you to put transactions in specific categories to track particular spending (such as groceries, gaming, utilities, etc.). Can also put sub-categories into categories for more organised actions. The parent category keeps track of all general expenses, while a sub-category can track something specific. Can have up to 5 subs.

### Default Categories (these are automatically in place)
- **General Category** - A general Expense style category. All expense categories are sub-categories of this.
- **Groceries and Utilities** - Expense categories under General Categories.
- **Income Category** - Separate category to track income. Mainly meant to exclude them from expense style categories.
- **Account Transfers** - Separate category for organization purposes.

### Transactions
The actual transactions you get from banking apps (via CSV import), or manual entry. Will be able to store and track all necessary details like name, amount, date, type (i.e. expense), and more.

### Forms
These forms allow you to enter, save, edit, and delete data.
- **Category Form** - Allows you to create, edit, and delete your categories (cannot edit or delete root categories).
- **Transaction Form** - Allows you to create, edit, and delete transactions (including putting them under categories).
- **Import Form** - Allows you to import transactions from CSV files exported by banking apps.

### Lists and dashboards
 Lists all of a certain data, ability to search filter and bring up a dashboard when selecting one.
- **Category List** - A list of all categories, can see extra details like sub-categories and total spending upon selection.
- **Transaction List** - A list of all transactions, can see extra details like categories under upon selection.

# User Guide
None for now as no front-end has been developed.

## Tech Stack (More will be added as project develops)
- **Language**: Java (For core/backend) + Kotlin (for UI and activities)
- **Local Database**: Room database (Due to simple implementation and native Android support)
- **Queries and DAO**: CRUD (with SQLite)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **UI**: Jetpack Compose
- **Testing**: JUnit, Mockito
- **Other**: ViewModel, LiveData, Gradle.



