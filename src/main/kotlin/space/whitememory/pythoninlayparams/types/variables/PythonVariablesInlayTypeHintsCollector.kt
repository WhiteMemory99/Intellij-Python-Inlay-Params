package space.whitememory.pythoninlayparams.types.variables

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyTargetExpression
import space.whitememory.pythoninlayparams.types.AbstractPythonInlayTypeHintsCollector
import space.whitememory.pythoninlayparams.types.hints.HintResolver

@Suppress("UnstableApiUsage")
class PythonVariablesInlayTypeHintsCollector(
    editor: Editor,
    override val settings: PythonVariablesInlayTypeHintsProvider.Settings
) :
    AbstractPythonInlayTypeHintsCollector(editor, settings) {

    override fun validateExpression(element: PsiElement): Boolean {
        return element is PyTargetExpression
    }

    override val textBeforeTypeHint = ":"

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        if (!super.collect(element, editor, sink)) {
            return false
        }

        if (!settings.showGeneralHints) {
            return false
        }

        if (!validateExpression(element)) {
            return true
        }

        val typeEvalContext = getTypeEvalContext(editor, element)

        if (HintResolver.resolve(element as PyTargetExpression, typeEvalContext, settings)) {
            return true
        }

        try {
            renderTypeHint(element, typeEvalContext, sink)
        } catch (e: Exception) {
            return true
        }

        return true
    }

    override fun displayTypeHint(element: PyElement, sink: InlayHintsSink, hintName: InlayPresentation) {
        sink.addInlineElement(
            element.endOffset,
            false,
            factory.roundWithBackground(factory.seq(factory.smallText("$textBeforeTypeHint "), hintName)),
            false
        )
    }
}
