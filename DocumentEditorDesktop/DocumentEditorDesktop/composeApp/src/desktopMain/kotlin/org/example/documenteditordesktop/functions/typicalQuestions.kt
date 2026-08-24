package org.example.documenteditordesktop.functions

import org.example.documenteditordesktop.ClassesViewModels.DocumentCase
import org.example.documenteditordesktop.ClassesViewModels.DocumentField
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.Manager
import org.example.documenteditordesktop.ClassesViewModels.TemplateAnswers
import org.example.documenteditordesktop.ClassesViewModels.TypicalQuestion

fun normalizeQuestion(text: String): String =
    text.lowercase()
        .replace(Regex("[\\p{Punct}«»„“”]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

/** Gson may leave Kotlin non-null strings as null on old JSON without `key`. */
fun DocumentField.safeKey(): String = (key as String?).orEmpty().trim()

fun unwrapPlaceholder(raw: String): String =
    raw.trim().removePrefix("{{").removeSuffix("}}").trim()

fun wrapPlaceholder(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.startsWith("{{") && trimmed.endsWith("}}")) return trimmed
    val inner = trimmed.trim('{', '}').trim()
    return if (inner.isEmpty()) "" else "{{$inner}}"
}

fun placeholderKeyFromLabel(label: String): String {
    val inner = unwrapPlaceholder(label)
        .uppercase()
        .transliterateRussian()
        .replaceSpacesWithUnderscores()
        .replace(Regex("[^A-Z0-9_]+"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
    return if (inner.isEmpty()) "" else "{{$inner}}"
}

fun fieldLookupKeys(field: DocumentField): List<String> {
    val keys = linkedSetOf<String>()
    val raw = field.safeKey()
    if (raw.isNotEmpty()) {
        keys.add(raw)
        wrapPlaceholder(raw).takeIf { it.isNotEmpty() }?.let { keys.add(it) }
        unwrapPlaceholder(raw).takeIf { it.isNotEmpty() }?.let { keys.add(it) }
    }
    placeholderKeyFromLabel(field.label).takeIf { it.isNotEmpty() }?.let { keys.add(it) }
    return keys.toList()
}

fun fieldDisplayKey(field: DocumentField, index: Int = 0): String =
    fieldLookupKeys(field).firstOrNull().orEmpty().ifEmpty { "__field_$index" }

private fun fieldQuestionCandidates(field: DocumentField): Set<String> {
    val result = linkedSetOf<String>()
    fun addNorm(text: String) {
        val normalized = normalizeQuestion(text)
        if (normalized.isEmpty()) return
        result.add(normalized)
        val stripped = normalized.replace(Regex("^\\d+\\s+"), "")
        if (stripped.isNotEmpty()) result.add(stripped)
    }
    addNorm(field.label)
    addNorm(unwrapPlaceholder(field.safeKey()))
    return result
}

fun findTypicalAnswer(
    field: DocumentField,
    typicalQuestions: List<TypicalQuestion>
): String? {
    val candidates = fieldQuestionCandidates(field)
    if (candidates.isEmpty()) return null
    return typicalQuestions.firstOrNull { typical ->
        typical.answer.isNotBlank() &&
            typical.questions.any { question ->
                val normalized = normalizeQuestion(question)
                normalized.isNotEmpty() && normalized in candidates
            }
    }?.answer
}

private fun putFieldAnswer(
    target: MutableMap<String, String>,
    field: DocumentField,
    value: String
) {
    val keys = fieldLookupKeys(field)
    if (keys.isEmpty()) {
        val fallback = fieldDisplayKey(field)
        if (fallback.isNotEmpty()) target[fallback] = value
        return
    }
    keys.forEach { target[it] = value }
}

fun valueForField(field: DocumentField, answers: Map<String, String>): String? {
    for (key in fieldLookupKeys(field)) {
        val value = answers[key]
        if (!value.isNullOrBlank()) return value
    }
    val displayKey = fieldDisplayKey(field)
    answers[displayKey]?.takeIf { it.isNotBlank() }?.let { return it }
    val labelNorm = normalizeQuestion(field.label)
    if (labelNorm.isEmpty()) return null
    return answers.entries.firstOrNull { entry ->
        entry.value.isNotBlank() && normalizeQuestion(entry.key) == labelNorm
    }?.value
}

fun matchTypicalAnswers(
    template: DocumentTemplate,
    typicalQuestions: List<TypicalQuestion>
): Map<String, String> {
    val result = mutableMapOf<String, String>()
    for (field in template.fields) {
        val answer = findTypicalAnswer(field, typicalQuestions) ?: continue
        putFieldAnswer(result, field, answer)
    }
    return result
}

fun countMatchedFields(
    templates: List<DocumentTemplate>,
    typicalQuestions: List<TypicalQuestion>
): Int = templates.sumOf { template ->
    template.fields.count { findTypicalAnswer(it, typicalQuestions) != null }
}

fun storedAnswers(
    filledTemplates: List<TemplateAnswers>,
    templateId: Int
): Map<String, String> =
    filledTemplates.firstOrNull { it.templateId == templateId }?.values.orEmpty()

fun mergedAnswers(
    template: DocumentTemplate,
    typicalQuestions: List<TypicalQuestion>,
    filledTemplates: List<TemplateAnswers>
): Map<String, String> {
    val typical = matchTypicalAnswers(template, typicalQuestions)
    val stored = storedAnswers(filledTemplates, template.id)
    val result = mutableMapOf<String, String>()
    for (field in template.fields) {
        val value = valueForField(field, stored) ?: valueForField(field, typical) ?: continue
        putFieldAnswer(result, field, value)
    }
    return result
}

fun isTemplateFullyFilled(template: DocumentTemplate, answers: Map<String, String>): Boolean {
    if (template.fields.isEmpty()) return true
    return template.fields.all { field -> valueForField(field, answers)?.isNotBlank() == true }
}

fun saveAnswersToCase(caseId: Int, templateId: Int, values: Map<String, String>) {
    val manager = Manager(DocumentCase::class.java)
    val documentCase = manager.documents.firstOrNull { it.id == caseId } ?: return
    val cleaned = values.filterValues { it.isNotBlank() }.toMutableMap()
    val existing = documentCase.filledTemplates.firstOrNull { it.templateId == templateId }
    if (cleaned.isEmpty()) {
        if (existing != null) {
            documentCase.filledTemplates.remove(existing)
            manager.updateById(caseId, documentCase)
        }
        return
    }
    if (existing != null) {
        existing.values.clear()
        existing.values.putAll(cleaned)
    } else {
        documentCase.filledTemplates.add(
            TemplateAnswers(
                templateId = templateId,
                values = cleaned
            )
        )
    }
    manager.updateById(caseId, documentCase)
}

fun duplicateCase(source: DocumentCase, caseManager: Manager<DocumentCase>): DocumentCase {
    val newId = (caseManager.documents.maxOfOrNull { it.id } ?: -1) + 1
    val now = System.currentTimeMillis()
    val copy = DocumentCase(
        id = newId,
        name = uniqueCaseCopyName(source.name, caseManager.documents.map { it.name }),
        templateIds = source.templateIds.toMutableList(),
        typicalQuestions = source.typicalQuestions.map { typical ->
            TypicalQuestion(
                questions = typical.questions.toMutableList(),
                answer = ""
            )
        }.toMutableList(),
        filledTemplates = mutableListOf(),
        createdAt = now,
        lastUsedAt = now
    )
    caseManager.updateById(newId, copy)
    return copy
}

private fun uniqueCaseCopyName(original: String, existingNames: List<String>): String {
    val base = original
        .ifBlank { "Дело" }
        .replace(Regex(" \\(копия(?: \\d+)?\\)$"), "")
        .trim()
        .ifBlank { "Дело" }
    val names = existingNames.toHashSet()
    val first = "$base (копия)"
    if (first !in names) return first
    var index = 2
    while ("$base (копия $index)" in names) {
        index++
    }
    return "$base (копия $index)"
}
