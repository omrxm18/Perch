import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation(libs.eclipse.angus)
    implementation(libs.jakarta.mail)
    implementation(libs.google.oauth.client)
    implementation(libs.google.api.client)
    implementation(libs.jsoup)
    implementation(libs.dorkbox)
    implementation(libs.slf4j)
    implementation(libs.jcefmaven)
}

compose.desktop {
    application {
        mainClass = "com.omrx.perch.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "perch"
            packageVersion = "1.0.0"
        }
    }
}