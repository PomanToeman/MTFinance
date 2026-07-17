# Implementation Plan - Fix Jetpack Compose and Dependency Implementation

Resolve Gradle sync errors and correctly implement Jetpack Compose with essential dependencies using the latest verified versions.

## Proposed Changes

### [MTFinance Core]

#### [MODIFY] [libs.versions.toml](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/gradle/libs.versions.toml)
Update versions to match the latest stable ones verified by `version_lookup`:
- `kotlin = "2.4.10"`
- `composeBom = "2026.06.01"`
- `activityCompose = "1.13.0"`
- `navigationCompose = "2.9.8"`
- `hiltNavigationCompose = "1.4.0"`
- `lifecycleRuntimeCompose = "2.11.0"`

#### [MODIFY] [build.gradle.kts (root)](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/build.gradle.kts)
Fix the typo in the Kotlin plugin alias: change `kotlin.andriod` to `kotlin.android`.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/build.gradle.kts)
Ensure all aliases and library references are correct and consistent with `libs.versions.toml`.

## Verification Plan

### Automated Tests
- Run Gradle sync.
- Build the project: `./gradlew :app:assembleDebug`.

### Manual Verification
- Verify that the "kotlin" extension conflict is resolved.
