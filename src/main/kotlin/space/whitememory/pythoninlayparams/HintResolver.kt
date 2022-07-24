package space.whitememory.pythoninlayparams

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyNames
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
            if (element.name == PyNames.UNDERSCORE) {
                return false
            }

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

    CLASS_ATTRIBUTE_HINT() {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext
        ): Boolean {
            if (PyUtil.isClassAttribute(element)) {
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
            val assignedValue = unfoldParens(element.findAssignedValue())

            if (
                typeAnnotation is PyClassType
                && assignedValue is PyCallExpression
                && PyCallExpressionHelper.resolveCalleeClass(assignedValue) != null
            ) {
                if (typeAnnotation.isBuiltin || assignedValue.callee?.reference?.resolve() is PyFunction) {
                    return values()
                        .filter { it != CLASS_HINT }
                        .all { it.shouldShowTypeHint(element, typeAnnotation, typeEvalContext) }
                }

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
            val assignmentValue = unfoldParens(element.findAssignedValue())

            if (assignmentValue is PyConditionalExpression) {
                return resolveExpression(assignmentValue.truePart, assignmentValue.falsePart, typeEvalContext)
            }

            if (assignmentValue is PyBinaryExpression) {
                return resolveExpression(assignmentValue.leftExpression, assignmentValue.rightExpression, typeEvalContext)
            }

            return true
        }

        private fun resolveExpression(
            firstElement: PyExpression,
            secondElement: PyExpression?,
            typeEvalContext: TypeEvalContext
        ): Boolean {
            if (isLiteralExpression(firstElement) && isLiteralExpression(secondElement)) {
                return false
            }

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
            val assignmentValue = unfoldParens(element.findAssignedValue())

            if (assignmentValue is PyComprehensionElement ) {
                if (typeAnnotation is PyCollectionType) {
                    return typeAnnotation.elementTypes.filterNotNull().isNotEmpty()
                }

                if (assignmentValue is PySetCompExpression || assignmentValue is PyDictCompExpression) {
                    return false
                }
            }

            return true
        }
    },

    SET_HINT() {
        private val collectionNames = setOf("frozenset", "set")

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext
        ): Boolean {
            val assignmentValue = unfoldParens(element.findAssignedValue())

            if (assignmentValue !is PyCallExpression) {
                return true
            }

            val resolvedClass = PyCallExpressionHelper.resolveCalleeClass(assignmentValue) ?: return true

            if (!collectionNames.contains(resolvedClass.name)) {
                // TODO: Make it as option and think how to implement classmethod check for this
                if (resolvedClass.name == typeAnnotation?.name) {
                    return false
                }

                return true
            }

            return typeAnnotation?.isBuiltin == true
                    && (typeAnnotation as PyCollectionType).elementTypes.filterNotNull().isNotEmpty()
        }
    },

    LITERAL_EXPRESSION() {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext
        ): Boolean {
            val assignedValue = unfoldParens(element.findAssignedValue())

            if (isLiteralExpression(assignedValue)) {
                return try {
                    !(assignedValue as PySequenceExpression).isEmpty
                } catch (e: Exception) {
                    false
                }
            }

            return true
        }
    },

    TUPLE_TYPE() {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext
        ): Boolean {
            if (typeAnnotation !is PyTupleType) {
                return true
            }

            if (typeAnnotation.elementTypes.filterNotNull().isEmpty()) {
                return false
            }

            val assignedValue = unfoldParens(element.findAssignedValue())

            if (assignedValue !is PyTupleExpression) {
                return true
            }

            return assignedValue.elements.any { !isLiteralExpression(it) }
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

        private fun isLiteralExpression(element: PyExpression?): Boolean {
            return element is PySequenceExpression || element is PyLiteralExpression || element is PySetCompExpression
        }

        private fun unfoldParens(element: PyExpression?): PyExpression? {
            if (element is PyParenthesizedExpression) {
                return element.containedExpression
            }

            return element
        }
    }
}