package org.example.documenteditordesktop.ClassesViewModels

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import org.example.documenteditordesktop.functions.createFolder
import java.io.File

@Serializable
class RecentDocument(
    var path: String,
    var dict: Map<String, String>,
    var name: String,
    var templateId: Int,
    var nameForDev: String
) {
    constructor() : this(
        path = "",
        dict = emptyMap(),
        name = "",
        templateId = 0,
        nameForDev = ""
    )
}

// Класс описывающий поле
@Serializable
data class DocumentField(
    var label: String, // Название поля
    var key: String = ""
) {
    constructor() : this(
        label = "",
        key = ""
    )
}

// Класс описывающий шаблон документа
@Serializable
data class DocumentTemplate(
    var id: Int, // Номер шаблона
    var nameForUser: String, // Имя шаблона
    var nameForDevelop: String,
    var fields: List<DocumentField>, // Поля шаблона
    var createdAt: Long = 0L,
    var lastUsedAt: Long = 0L
) {
    constructor() : this(
        id = 0,
        nameForUser = "",
        nameForDevelop = "",
        fields = emptyList()
    )
}

@Serializable
data class TypicalQuestion(
    var questions: MutableList<String> = mutableListOf(),
    var answer: String = ""
) {
    constructor() : this(mutableListOf(), "")
}

@Serializable
data class TemplateAnswers(
    var templateId: Int = 0,
    var values: MutableMap<String, String> = mutableMapOf()
) {
    constructor() : this(0, mutableMapOf())
}

@Serializable
data class DocumentCase(
    var id: Int = 0,
    var name: String = "",
    var templateIds: MutableList<Int> = mutableListOf(),
    var typicalQuestions: MutableList<TypicalQuestion> = mutableListOf(),
    var filledTemplates: MutableList<TemplateAnswers> = mutableListOf(),
    var createdAt: Long = 0L,
    var lastUsedAt: Long = 0L
) {
    constructor() : this(0, "", mutableListOf(), mutableListOf(), mutableListOf())
}

class Manager<T: Any>(
    val documentType: Class<T>
){
    private val documentDir = File(System.getProperty("user.home"), "DocumentEditor/DataBase")
    private val documentFile = when (documentType) {
        DocumentTemplate::class.java -> File(documentDir,"Templates.json")
        RecentDocument::class.java -> File(documentDir, "RecentDocs.json")
        DocumentCase::class.java -> File(documentDir, "Cases.json")
        else -> throw IllegalArgumentException("Unsupported document type")
    }
    var documents: MutableList<T> = loadJson()

    fun loadJson(): MutableList<T>{
        return if (documentFile.exists()) {
            val json = documentFile.readText()
            val type = TypeToken.getParameterized(MutableList::class.java, documentType).type
            Gson().fromJson<MutableList<T>>(json, type) ?: mutableListOf()
        } else {
            createDocFile()
        }
    }

    private fun ensureDir() {
        if (!documentDir.exists()) {
            documentDir.mkdirs()
        }
    }

    private fun createDocFile(): MutableList<T> {
        ensureDir()
        documentFile.writeText("[]")
        return mutableListOf()
    }

    fun saveJson(){
        ensureDir()
        val json = Gson().toJson(documents)
        documentFile.writeText(json)
    }

    fun addDocument(document: T){
        if (documentType == RecentDocument::class.java) {
            documents.add(0, document)
        } else {
            documents.add(document)
        }
        saveJson()
    }

    fun deleteDocument(name: String = "", id: Int = 0){
        when(documentType){
            RecentDocument::class.java -> {
                val list = documents as MutableList<RecentDocument>
                val index = list.indexOfFirst { it.path == name }
                if (index >= 0) {
                    list.removeAt(index)
                }
                saveJson()
            }
            DocumentTemplate::class.java ->{
                val list = documents as MutableList<DocumentTemplate>
                val index = list.indexOfFirst { it.id == id }
                if (index >= 0) {
                    list.removeAt(index)
                }
                saveJson()
            }
            DocumentCase::class.java -> {
                val list = documents as MutableList<DocumentCase>
                val index = list.indexOfFirst { it.id == id }
                if (index >= 0) {
                    list.removeAt(index)
                }
                saveJson()
            }
        }
    }

    fun updateById(id: Int, document: T) {
        val index = documents.indexOfFirst { item ->
            when (item) {
                is DocumentCase -> item.id == id
                is DocumentTemplate -> item.id == id
                else -> false
            }
        }
        if (index >= 0) {
            documents[index] = document
        } else {
            documents.add(document)
        }
        saveJson()
    }

    fun clear(){
        if (documentType == RecentDocument::class.java) {
            documents = mutableListOf()
            saveJson()
        }
    }
}