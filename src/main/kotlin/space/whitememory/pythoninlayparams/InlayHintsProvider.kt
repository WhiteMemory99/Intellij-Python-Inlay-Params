package space.whitememory.pythoninlayparams

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import space.whitememory.pythoninlayparams.types.hints.HintGenerator
import space.whitememory.pythoninlayparams.types.hints.HintResolver
import space.whitememory.pythoninlayparams.types.hints.InlayElement

class InlayHintsProvider : InlayHintsProvider {
    companion object {
        const val PROVIDER_ID = "python.implicit.types"
        const val PROVIDER_NAME = "Implicit types"
    }

    override fun createCollector(file: PsiFile, editor: Editor): InlayHintsCollector {
        return Collector()
    }

    private class Collector() : SharedBypassCollector {

        override fun collectFromElement(element: PsiElement, sink: InlayTreeSink) {
            if (!element.isValid || element.project.isDefault || element !is PyTargetExpression) return

            val typeEvalContext = TypeEvalContext.codeCompletion(element.project, element.containingFile)

            if (HintResolver.resolve(element, typeEvalContext)) return

            val typeAnnotation = HintResolver.getExpressionAnnotationType(element, typeEvalContext)
            val inlayHint = HintGenerator.generateTypeHintText(
                element, typeAnnotation, typeEvalContext
            )

            if (inlayHint.isEmpty()) return

            sink.addPresentation(
                position = InlineInlayPosition(element.endOffset, true),
                hasBackground = true,
                builder = {
                    text(": ")

                    generateClickableInlayHint(this, inlayHint, createCollapsed = true)
                }
            )
        }

        private fun createRootInlayHint(builder: PresentationTreeBuilder, hintElement: InlayElement) {
            if (hintElement.element == null) return

            builder.text(
                hintElement.element.text, actionData = when (hintElement.element.psiElement) {
                    null -> null
                    else -> {
                        InlayActionData(
                            PsiPointerInlayActionPayload(
                                SmartPointerManager.createPointer(
                                    hintElement.element.psiElement
                                )
                            ),
                            handlerId = PsiPointerInlayActionNavigationHandler.HANDLER_ID
                        )
                    }
                }
            )
        }

        private fun generateClickableInlayHint(
            builder: PresentationTreeBuilder,
            hintElement: InlayElement,
            createCollapsed: Boolean = false
        ) {
            if (createCollapsed && hintElement.isTooLong()) {
                return builder.collapsibleList(
                    state = CollapseState.Collapsed,
                    expandedState = { toggleButton { generateClickableInlayHint(this, hintElement) } },
                    collapsedState = {
                        toggleButton {
                            createRootInlayHint(this, hintElement)
                            if (hintElement.isGenericType()) {
                                text("[...]")
                            } else {
                                text("${hintElement.inlayType.shortPrefix}[...]")
                            }
                        }
                    }
                )
            } else {
                createRootInlayHint(builder, hintElement)
            }

            if (hintElement.isGenericType()) builder.text("[")

            val childrenIterator = hintElement.children.iterator()

            while (childrenIterator.hasNext()) {
                generateClickableInlayHint(builder, childrenIterator.next())

                if (childrenIterator.hasNext()) {
                    builder.text(hintElement.separatorType.separator)
                }
            }

            if (hintElement.isGenericType()) builder.text("]")
        }
    }
}