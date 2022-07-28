package space.whitememory.pythoninlayparams.functions

import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyParameterList
import com.jetbrains.python.psi.PyStatementList
import space.whitememory.pythoninlayparams.AbstractPythonInlayTypeHintsCollector
import space.whitememory.pythoninlayparams.hints.HintGenerator
import space.whitememory.pythoninlayparams.hints.HintResolver

@Suppress("UnstableApiUsage")
class PythonFunctionInlayTypeHintsCollector(editor: Editor, settings: Any) :
    AbstractPythonInlayTypeHintsCollector(editor, settings) {

    override val textBeforeTypeHint = "->"

    override fun validateExpression(element: PsiElement): Boolean {
        return element is PyFunction
    }

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        if (!super.collect(element, editor, sink)) {
            return false
        }

        if (!validateExpression(element)) {
            return true
        }

        val typeEvalContext = getTypeEvalContext(editor, element)

        if (!HintResolver.shouldShowTypeHint(element as PyFunction, typeEvalContext)) {
            return true
        }

        try {
            renderTypeHint(element, typeEvalContext, sink)
        } catch (e: Exception) {
            return true
        }

        return true
    }

    override fun displayTypeHint(element: PyElement, sink: InlayHintsSink, hintName: String) {
        val statementList = PsiTreeUtil.getChildOfType(element, PyParameterList::class.java)

        statementList?.let {
            sink.addInlineElement(
                it.endOffset,
                false,
                factory.roundWithBackground(factory.smallText("$textBeforeTypeHint $hintName")),
                false
            )
        }
    }
}