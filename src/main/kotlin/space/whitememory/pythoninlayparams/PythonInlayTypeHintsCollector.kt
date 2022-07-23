package space.whitememory.pythoninlayparams

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.types.TypeEvalContext

@Suppress("UnstableApiUsage")
class PythonInlayTypeHintsCollector(editor: Editor, val settings: PythonInlayTypeHintsProvider.Settings) :
    FactoryInlayHintsCollector(editor) {

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        if (!element.isValid || element.project.isDefault) {
            return false
        }

        if (element !is PyTargetExpression) {
            return true
        }

        val typeEvalContext = TypeEvalContext.codeCompletion(editor.project!!, element.containingFile)
        val typeAnnotation = typeEvalContext.getType(element)

        if (HintResolver.resolve(element, typeAnnotation, typeEvalContext)) {
            return true
        }

        val hintName = HintGenerator.generateTypeHintText(typeAnnotation, typeEvalContext)

        sink.addInlineElement(
            element.endOffset,
            false,
            factory.roundWithBackground(factory.smallText(": ${ellipsis(hintName)}")),
            false
        )

        return true
    }

    // TODO: Implement ellipsis on long strings
    private fun ellipsis(text: String, length: Int = 28): String {
        return text
    }
}
