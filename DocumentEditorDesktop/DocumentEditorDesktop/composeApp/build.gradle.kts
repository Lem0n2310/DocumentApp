import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)

}


kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            //implementation(libs.androidx.lifecycle.viewmodel)
            //implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.apache.poi:poi:5.4.1")
            implementation("org.apache.poi:poi-ooxml:5.4.1") {
                exclude(group = "org.apache.logging.log4j")
            }
            //implementation(libs.jvmDesktopSwing)
            implementation(libs.kotlinx.serialization.json)
            implementation("com.google.code.gson:gson:2.11.0")
            implementation("org.xhtmlrenderer:flying-saucer-pdf:9.1.22")
            //implementation("org.docx4j:docx4j-core:11.4.8")
            //implementation("org.docx4j:docx4j-export-fo:11.4.8")
            //implementation("org.jetbrains.compose.web:web-core:1.6.0")

            implementation("org.apache.poi:poi-scratchpad:5.4.1") {
                exclude(group = "org.apache.logging.log4j")
            }

            // Явно указываем совместимую версию Log4j
            implementation("org.apache.logging.log4j:log4j-api:2.23.1")
            implementation("org.apache.logging.log4j:log4j-core:2.23.1")

            // Для подавления предупреждений POI о логировании
            implementation("org.slf4j:slf4j-simple:2.0.9")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

configurations.all {
    resolutionStrategy {
        force(
            "org.apache.logging.log4j:log4j-api:2.23.1",
            "org.apache.logging.log4j:log4j-core:2.23.1",
            "org.apache.logging.log4j:log4j-slf4j-impl:2.23.1"
        )
    }
}


compose.desktop {
    application {
        mainClass = "org.example.documenteditordesktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Msi)
            packageName = "DocumentEditor"
            packageVersion = "1.0.2"

            windows{
                iconFile.set(project.file("icons/windows.ico"))
                dirChooser = false
                menu = true
                shortcut = true
            }
        }
    }
}
