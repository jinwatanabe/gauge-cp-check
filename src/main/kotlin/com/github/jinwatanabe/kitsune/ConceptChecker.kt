package com.github.jinwatanabe.intellijpluginsandbox

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.charset.StandardCharsets

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
//
            if (usedSteps.isNotEmpty()) {
                showResults(usedSteps)
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

    private fun showResults(usedSteps: List<String>) {
        Messages.showInfoMessage(usedSteps.joinToString("\n"), "Matching Steps Found")
    }
}
