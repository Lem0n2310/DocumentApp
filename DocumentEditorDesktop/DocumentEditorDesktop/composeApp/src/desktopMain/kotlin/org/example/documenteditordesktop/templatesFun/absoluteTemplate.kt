package org.example.documenteditordesktop.templatesFun

import com.example.documenteditor.functions.tableChange
import com.example.documenteditor.functions.textChange
import org.apache.poi.ss.formula.functions.Replace
import org.apache.poi.xwpf.usermodel.XWPFDocument

fun absoluteTemplate(
    document: XWPFDocument,
    dict: Map<String, String>
): XWPFDocument {
    var replace: MutableMap<String, String> = HashMap()

    dict.keys.forEach { key ->
        dict[key]?.let { replace.put(key, it) }
    }

    tableChange(document, replace)

    textChange(document, replace)

    return document
}