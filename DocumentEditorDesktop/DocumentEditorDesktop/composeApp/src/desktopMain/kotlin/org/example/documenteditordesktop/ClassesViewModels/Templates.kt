package com.example.documenteditor.ClassesViewModels

import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.Manager

val manager = Manager(DocumentTemplate::class.java)
val templates = manager.loadJson()