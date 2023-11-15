package com.github.jinwatanabe.intellijpluginsandbox

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.charset.StandardCharsets
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.ui.Messages
import java.awt.Color


class ConceptChecker : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()

        if (currentFile != null && currentFile.extension == "spec") {
            val specContent = String(currentFile.contentsToByteArray(), StandardCharsets.UTF_8)
            val specSteps = extractStepsFromSpec(specContent)
            val cptFiles = findAllCptFiles(project)
            val concepts = extractConceptsFromCptFiles(cptFiles)
            val usedSteps = findMatchingSteps(specSteps, concepts)

            if (usedSteps.isNotEmpty()) {
                showResults(project, usedSteps)
            }
        }

    }

    private fun findAllCptFiles(project: Project): List<VirtualFile> {
        val basePath = project.basePath ?: return emptyList()
        val projectRoot = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return emptyList()
        val cptFiles = mutableListOf<VirtualFile>()

        VfsUtil.iterateChildrenRecursively(projectRoot, null) { file ->
            if (!file.isDirectory && file.extension == "cpt") {
                cptFiles.add(file)
            }
            true
        }

        return cptFiles
    }
    private fun extractStepsFromSpec(specContent: String): List<String> {
        return specContent.lines()
            .filter { it.trim().startsWith("*") }
            .map { it.trim().substring(1).trim() }
    }

    private fun extractConceptsFromCptFiles(cptFiles: List<VirtualFile>): List<String> {
        val concepts = mutableListOf<String>()
        for (file in cptFiles) {
            val content = String(file.contentsToByteArray(), StandardCharsets.UTF_8)
            concepts.addAll(content.lines()
                .filter { it.trim().startsWith("#") }
                .map { it.trim().substring(1).trim() })
        }
        return concepts
    }

    private fun findMatchingSteps(specSteps: List<String>, concepts: List<String>): List<String> {
        return specSteps.filter { it in concepts }
    }

    private fun showResults(project: Project, usedSteps: List<String>) { // project を引数として追加
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val document = editor.document
        val markupModel = editor.markupModel

        val highlightAttributes = TextAttributes()
        highlightAttributes.backgroundColor = Color.GRAY

        for (step in usedSteps) {
            val lineNumber = findLineForStep(document, step)
            Messages.showMessageDialog(lineNumber.toString(), "Information", Messages.getInformationIcon());
            if (lineNumber != -1) {
                val startOffset = document.getLineStartOffset(lineNumber)
                val endOffset = document.getLineEndOffset(lineNumber)
                markupModel.addRangeHighlighter(startOffset, endOffset, HighlighterLayer.SELECTION, highlightAttributes, HighlighterTargetArea.LINES_IN_RANGE)
            }
        }
    }
    private fun findLineForStep(document: Document, step: String): Int {
        for (i in 0 until document.lineCount) {
            val lineStartOffset = document.getLineStartOffset(i)
            val lineEndOffset = document.getLineEndOffset(i)
            val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
            if (lineText.trim() == "* $step") {
                return i
            }
        }
        return -1
    }
}
