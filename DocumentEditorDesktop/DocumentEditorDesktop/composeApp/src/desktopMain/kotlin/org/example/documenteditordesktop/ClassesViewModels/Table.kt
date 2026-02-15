package org.example.documenteditordesktop.ClassesViewModels

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import org.example.documenteditordesktop.functions.createFolder
import java.io.File

@Serializable
class RecentDocument(
    val path: String,
    val dict: Map<String, String>,
    val name: String,
    val templateId: Int,
    val nameForDev: String
)

// Класс описывающий поле
@Serializable
data class DocumentField (
    val label: String, // Название поля
    val key: String = ""
)

// Класс описывающий шаблон документа
@Serializable
data class DocumentTemplate(
    val id: Int, // Номер шаблона
    val nameForUser: String, // Имя шаблона
    val nameForDevelop: String,
    val fields: List<DocumentField> // Поля шаблона
)

class Manager<T: Any>(
    val documentType: Class<T>
){
    private val documentDir = File(System.getProperty("user.home"), "DocumentEditor/DataBase")
    private val documentFile = when (documentType) {
        DocumentTemplate::class.java -> File(documentDir,"Templates.json")
        RecentDocument::class.java -> File(documentDir, "RecentDocs.json")
        else -> throw IllegalArgumentException("Unsupported document type")
    }
    var documents: MutableList<T> = loadJson()

    fun loadJson(): MutableList<T>{
        return if(documentFile.exists()) {
            val json = documentFile.readText()
            val type = TypeToken.getParameterized(MutableList::class.java, documentType).type
            return Gson().fromJson(json, type) ?: mutableListOf()
        }else{
            createDocFile()
        }
    }

    private fun createDocFile(): MutableList<T> {
        documentFile.writeText("[]")
        return mutableListOf()
    }

    fun saveJson(){
        val json = Gson().toJson(documents)
        documentFile.writeText(json)
    }

    fun addDocument(document: T){
        if (documentType == RecentDocument::class.java) {
            documents.add(0, document)
        }else{
            documents.add(document)
        }
        saveJson()
    }

    fun deleteDocument(name: String = "", id: Int = 0){
        when(documentType){
            RecentDocument::class.java -> {
                (documents as MutableList<RecentDocument>).removeAll { it.path.contains(name) }
                saveJson()
            }
            DocumentTemplate::class.java ->{
                (documents as MutableList<DocumentTemplate>).removeAll { it.id == id }
                saveJson()
            }
        }
    }

    fun clear(){
        if (documentType == RecentDocument::class.java) {
            documents = mutableListOf()
            saveJson()
        }
    }
}