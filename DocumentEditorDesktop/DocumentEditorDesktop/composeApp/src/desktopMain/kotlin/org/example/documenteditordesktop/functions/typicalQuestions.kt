package org.example.documenteditordesktop.functions

import org.example.documenteditordesktop.ClassesViewModels.DocumentCase
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.Manager
import org.example.documenteditordesktop.ClassesViewModels.TemplateAnswers
import org.example.documenteditordesktop.ClassesViewModels.TypicalQuestion

fun normalizeQuestion(text: String): String =
    text.lowercase()
        .replace(Regex("[\\p{Punct}«»„“”]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

fun matchTypicalAnswers(
    template: DocumentTemplate,
    typicalQuestions: List<TypicalQuestion>
): Map<String, String> {
    val result = mutableMapOf<String, String>()
    for (field in template.fields) {
        val fieldNorm = normalizeQuestion(field.label)
        if (fieldNorm.isEmpty()) continue
        val match = typicalQuestions.firstOrNull { typical ->
            typical.answer.isNotBlank() &&
                typical.questions.any { normalizeQuestion(it) == fieldNorm }
        }
        if (match != null) {
            result[field.key] = match.answer
        }
    }
    return result
}

fun countMatchedFields(
    templates: List<DocumentTemplate>,
    typicalQuestions: List<TypicalQuestion>
): Int = templates.sumOf { matchTypicalAnswers(it, typicalQuestions).size }

fun storedAnswers(
    filledTemplates: List<TemplateAnswers>,
    templateId: Int
): Map<String, String> =
    filledTemplates.firstOrNull { it.templateId == templateId }?.values.orEmpty()

fun mergedAnswers(
    template: DocumentTemplate,
    typicalQuestions: List<TypicalQuestion>,
    filledTemplates: List<TemplateAnswers>
): Map<String, String> =
    matchTypicalAnswers(template, typicalQuestions) + storedAnswers(filledTemplates, template.id)

fun isTemplateFullyFilled(template: DocumentTemplate, answers: Map<String, String>): Boolean {
    if (template.fields.isEmpty()) return true
    return template.fields.all { field -> answers[field.key]?.isNotBlank() == true }
}

fun saveAnswersToCase(caseId: Int, templateId: Int, values: Map<String, String>) {
    val manager = Manager(DocumentCase::class.java)
    val documentCase = manager.documents.firstOrNull { it.id == caseId } ?: return
    val existing = documentCase.filledTemplates.firstOrNull { it.templateId == templateId }
    if (existing != null) {
        existing.values.clear()
        existing.values.putAll(values)
    } else {
        documentCase.filledTemplates.add(
            TemplateAnswers(
                templateId = templateId,
                values = values.toMutableMap()
            )
        )
    }
    manager.updateById(caseId, documentCase)
}
