package org.example.documenteditordesktop.functions

import java.nio.file.Files
import java.nio.file.Paths

fun checkFolderExists(relativePath: String): Boolean {
    val userHome = System.getProperty("user.home")
    val fullPath = Paths.get(userHome, *relativePath.split("/").toTypedArray())

    return Files.exists(fullPath) && Files.isDirectory(fullPath)
}

fun createFolder(relativePath: String): Boolean {
    val userHome = System.getProperty("user.home")
    val fullPath = Paths.get(userHome, *relativePath.split("/").toTypedArray())

    return try {
        // Создаем все недостающие директории
        Files.createDirectories(fullPath)
        true
    } catch (e: SecurityException) {
        println("Ошибка безопасности: нет прав для создания папки - ${e.message}")
        false
    } catch (e: Exception) {
        println("Ошибка при создании папки: ${e.message}")
        false
    }
}
