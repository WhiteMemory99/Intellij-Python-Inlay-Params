package space.whitememory.pythoninlayparams

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyCallExpressionHelper
import com.jetbrains.python.psi.types.*

enum class HintResolver() {

    GENERAL_HINT() {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext
        ): Boolean {
            val hasTypeAnnotation = PsiTreeUtil.getNextSiblingOfType(element, PyAnnotation::class.java) != null

            if (
                typeAnnotation == null
                || hasTypeAnnotation
                || element.isQualified
                || PyTypeChecker.isUnknown(typeAnnotation, false, typeEvalContext)
            ) {
                return false
            }

            return true
        }
    },

    UNION_HINT() {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext
        ): Boolean {
            if (typeAnnotation is PyUnionType) {
                return typeAnnotation.members.any {
                    val type = it

                    return values().any {
                        shouldShowTypeHint(element, type, typeEvalContext)
                    }
                }
            }

            return true
        }
    },

    CLASS_HINT() {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext
        ): Boolean {
            val assignedValue = element.findAssignedValue()

            if (
                typeAnnotation is PyClassType
                && assignedValue is PyCallExpression
                && PyCallExpressionHelper.resolveCalleeClass(assignedValue) != null
            ) {
                return false
            }

            return true
        }
    },

    CONDITIONAL_HINT() {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext
        ): Boolean {
            if (element is PyConditionalExpression) {
                return resolveExpression(element.truePart, element.falsePart)
            }

            if (element is PyBinaryExpression) {
                return resolveExpression(element.leftExpression, element.rightExpression)
            }

            return true
        }

        private fun resolveExpression(firstElement: PyExpression, secondElement: PyExpression?): Boolean {
            val falsePartExpression = firstElement is PyCallExpression
            val truePartExpression = secondElement is PyCallExpression

            if (!falsePartExpression || !truePartExpression) {
                return true
            }

            val isFalsePartClass = PyCallExpressionHelper.resolveCalleeClass(firstElement as PyCallExpression) != null
            val isTruePartClass = PyCallExpressionHelper.resolveCalleeClass(secondElement as PyCallExpression) != null

            if (isFalsePartClass && isTruePartClass) {
                return false
            }

            return true
        }
    },

    COMPREHENSION_HINT() {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext
        ): Boolean {
            if (element is PyComprehensionElement) {
                if (typeAnnotation is PyCollectionType) {
                    return typeAnnotation.elementTypes.filterNotNull().isNotEmpty()
                }
            }

            return true
        }
    };

    abstract fun shouldShowTypeHint(
        element: PyTargetExpression,
        typeAnnotation: PyType?,
        typeEvalContext: TypeEvalContext
    ): Boolean

    companion object {
        fun resolve(element: PyTargetExpression, typeAnnotation: PyType?, typeEvalContext: TypeEvalContext): Boolean =
            values().any { !it.shouldShowTypeHint(element, typeAnnotation, typeEvalContext) }
    }
}