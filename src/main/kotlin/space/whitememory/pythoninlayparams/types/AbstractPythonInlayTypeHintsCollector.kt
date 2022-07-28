package space.whitememory.pythoninlayparams.types

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.types.TypeEvalContext
import space.whitememory.pythoninlayparams.types.hints.HintGenerator
import space.whitememory.pythoninlayparams.types.hints.HintResolver

@Suppress("UnstableApiUsage")
abstract class AbstractPythonInlayTypeHintsCollector(editor: Editor, open val settings: Any): FactoryInlayHintsCollector(editor) {
    abstract fun validateExpression(element: PsiElement): Boolean

    abstract val textBeforeTypeHint: String

    protected fun getTypeEvalContext(editor: Editor, element: PsiElement): TypeEvalContext {
        return TypeEvalContext.codeCompletion(editor.project!!, element.containingFile)
    }

    protected fun renderTypeHint(element: PyElement, typeEvalContext: TypeEvalContext, sink: InlayHintsSink) {
        val typeAnnotation = HintResolver.getExpressionAnnotationType(element, typeEvalContext)
        val hintName = HintGenerator.generateTypeHintText(typeAnnotation, typeEvalContext)

        displayTypeHint(element, sink, hintName)
    }

    abstract fun displayTypeHint(element: PyElement, sink: InlayHintsSink, hintName: String)

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        if (!element.isValid || element.project.isDefault) {
            return false
        }

        return true
    }
}