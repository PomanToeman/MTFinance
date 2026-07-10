// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias { libs.plugins.ksp } apply false
    alias { libs.plugins.hilt } apply false



}

buildscript {
    dependencies {
        // For KGP
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:KGP_VERSION")

        // For KSP
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:KSP_VERSION")
    }
}


