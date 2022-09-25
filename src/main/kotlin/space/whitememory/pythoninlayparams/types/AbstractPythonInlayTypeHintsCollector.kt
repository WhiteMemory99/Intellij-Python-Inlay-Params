package space.whitememory.pythoninlayparams.types

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.types.TypeEvalContext
import space.whitememory.pythoninlayparams.types.hints.HintGenerator
import space.whitememory.pythoninlayparams.types.hints.HintResolver
import space.whitememory.pythoninlayparams.types.hints.InlayInfoDetails
import space.whitememory.pythoninlayparams.types.hints.PsiInlayInfoDetail

@Suppress("UnstableApiUsage")
abstract class AbstractPythonInlayTypeHintsCollector(editor: Editor, open val settings: Any) :
    FactoryInlayHintsCollector(editor) {
    abstract fun validateExpression(element: PsiElement): Boolean

    abstract val textBeforeTypeHint: String

    protected fun getTypeEvalContext(editor: Editor, element: PsiElement): TypeEvalContext {
        return TypeEvalContext.codeCompletion(editor.project!!, element.containingFile)
    }

    protected fun renderTypeHint(element: PyElement, typeEvalContext: TypeEvalContext, sink: InlayHintsSink) {
        val typeAnnotation = HintResolver.getExpressionAnnotationType(element, typeEvalContext)
        val hintName = HintGenerator.generateTypeHintText(element, typeAnnotation, typeEvalContext)

        if (hintName.isEmpty()) {
            return
        }

        val resolvedHintName = resolveInlayPresentation(hintName)

        displayTypeHint(element, sink, resolvedHintName)
    }

    private fun resolveInlayPresentation(
        infoDetails: List<InlayInfoDetails>,
        separator: String = " | ",
        limit: Int? = 3
    ): InlayPresentation {
        val convertedInlayInfoDetails = infoDetails.map { getInlayPresentationForInlayInfoDetails(it) }

        return factory.seq(*separatePresentation(convertedInlayInfoDetails, separator, limit).toTypedArray())
    }

    private fun getInlayPresentationForInlayInfoDetails(infoDetail: InlayInfoDetails): InlayPresentation {
        if (infoDetail.rootInlayInfo == null) {
            return resolveInlayPresentation(infoDetail.details, separator = infoDetail.separator, limit = infoDetail.limit)
        }

        val textPresentation = factory.smallText(infoDetail.rootInlayInfo.text)
        val navigationElementProvider: (() -> PsiElement?)? = when(infoDetail.rootInlayInfo) {
            is PsiInlayInfoDetail -> {{ infoDetail.rootInlayInfo.element }}
            else -> null
        }

        val basePresentation = navigationElementProvider?.let {
            factory.psiSingleReference(textPresentation, it)
        } ?: textPresentation

        if (infoDetail.details.isEmpty()) {
            return basePresentation
        }

        val childDetails = infoDetail.details.map { getInlayPresentationForInlayInfoDetails(it) }

        return factory.seq(
            basePresentation,
            factory.smallText("["),
            *separatePresentation(
                childDetails,
                separator = infoDetail.separator,
                limit = infoDetail.limit
            ).toTypedArray(),
            factory.smallText("]")
        )
    }

    private fun separatePresentation(
        presentations: List<InlayPresentation>,
        separator: String,
        limit: Int?
    ): List<InlayPresentation> {
        if (presentations.size <= 1) {
            return presentations
        }

        val separatedInlayPresentation = mutableListOf<InlayPresentation>()

        val iterator = presentations.iterator()
        var count = 0

        while (iterator.hasNext()) {
            if (limit != null && count == limit) {
                if (iterator.hasNext()) {
                    separatedInlayPresentation.add(factory.smallText("..."))
                }
                break
            }

            separatedInlayPresentation.add(iterator.next())

            count = count.inc()

            if (iterator.hasNext() && (limit != null && count != limit)) {
                separatedInlayPresentation.add(factory.smallText(separator))
            }
        }

        return separatedInlayPresentation
    }

    abstract fun displayTypeHint(element: PyElement, sink: InlayHintsSink, hintName: InlayPresentation)

    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        if (!element.isValid || element.project.isDefault) {
            return false
        }

        return true
    }
}