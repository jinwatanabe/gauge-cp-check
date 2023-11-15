package com.github.jinwatanabe.intellijpluginsandbox

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;

class ConceptChecker : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        Messages.showMessageDialog("Hello World!", "Information", Messages.getInformationIcon());
    }
}