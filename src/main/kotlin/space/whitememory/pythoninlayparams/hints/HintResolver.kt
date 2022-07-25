package space.whitememory.pythoninlayparams.hints

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyNames
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyCallExpressionHelper
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.types.*
import space.whitememory.pythoninlayparams.variables.PythonVariablesInlayTypeHintsProvider

enum class HintResolver() {

    UNDERSCORE_HINT() {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            if (element.name == PyNames.UNDERSCORE) {
                return false
            }

            return true
        }
    },

    QUALIFIED_HINT() {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            if (element.isQualified) {
                return false
            }

            return true
        }
    },

    GENERAL_HINT() {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            return shouldShowTypeHint(element, typeEvalContext)
        }
    },

    CLASS_ATTRIBUTE_HINT() {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = settings.showClassAttributeHints

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            if (PyUtil.isClassAttribute(element)) {
                return false
            }

            return true
        }
    },

    UNION_HINT() {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            if (typeAnnotation is PyUnionType) {
                return typeAnnotation.members.any { type ->
                    return resolveEnabled(settings).any {
                        shouldShowTypeHint(element, type, typeEvalContext, settings)
                    }
                }
            }

            return true
        }
    },

    CLASS_HINT() {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            val assignedValue = unfoldParens(element.findAssignedValue())

            if (
                typeAnnotation is PyClassType
                && assignedValue is PyCallExpression
                && PyCallExpressionHelper.resolveCalleeClass(assignedValue) != null
            ) {
                if (typeAnnotation.isBuiltin || assignedValue.callee?.reference?.resolve() is PyFunction) {
                    return resolveEnabled(settings)
                        .filter { it != CLASS_HINT }
                        .all { it.shouldShowTypeHint(element, typeAnnotation, typeEvalContext, settings) }
                }

                return false
            }

            return true
        }
    },

    CONDITIONAL_HINT() {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            val assignmentValue = unfoldParens(element.findAssignedValue())

            if (assignmentValue is PyConditionalExpression) {
                return resolveExpression(assignmentValue.truePart, assignmentValue.falsePart)
            }

            if (assignmentValue is PyBinaryExpression) {
                return resolveExpression(
                    assignmentValue.leftExpression,
                    assignmentValue.rightExpression
                )
            }

            return true
        }

        private fun resolveExpression(
            firstElement: PyExpression,
            secondElement: PyExpression?
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
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            val assignmentValue = unfoldParens(element.findAssignedValue())

            if (assignmentValue is PyComprehensionElement) {
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
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = true

        private val collectionNames = setOf("frozenset", "set")

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
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
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
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
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
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
    },

    ENUM_TYPE() {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            val assignedValue = unfoldParens(element.findAssignedValue())

            if (assignedValue !is PyReferenceExpressionImpl) {
                return true
            }

            val resolvedExpression = assignedValue.reference.resolve() ?: return true

            if (resolvedExpression is PyTargetExpression) {
                return values()
                    .filter { it != ENUM_TYPE }
                    .all { it.shouldShowTypeHint(resolvedExpression, typeAnnotation, typeEvalContext, settings) }
            }

            return true
        }
    };

    abstract fun shouldShowTypeHint(
        element: PyTargetExpression,
        typeAnnotation: PyType?,
        typeEvalContext: TypeEvalContext,
        settings: PythonVariablesInlayTypeHintsProvider.Settings
    ): Boolean

    abstract fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean

    companion object {
        fun resolve(element: PyTargetExpression, typeEvalContext: TypeEvalContext, settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean {
            val typeAnnotation = getTypeAnnotation(element, typeEvalContext)

            return resolveEnabled(settings).any {
                !it.shouldShowTypeHint(element, typeAnnotation, typeEvalContext, settings)
            }
        }

        private fun resolveEnabled(
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): List<HintResolver> = values().filter { it.isApplicable(settings) }

        private fun isLiteralExpression(element: PyExpression?): Boolean {
            return element is PySequenceExpression || element is PyLiteralExpression || element is PySetCompExpression
        }

        private fun unfoldParens(element: PyExpression?): PyExpression? {
            if (element is PyParenthesizedExpression) {
                return element.containedExpression
            }

            return element
        }

        private fun hasTypeAnnotation(element: PyElement): Boolean {
            if (element is PyFunction) {
                return PsiTreeUtil.getChildOfType(element, PyAnnotation::class.java) != null
            }

            if (element is PyTargetExpression) {
                return PsiTreeUtil.getNextSiblingOfType(element, PyAnnotation::class.java) != null
            }

            return false;
        }

        fun getTypeAnnotation(element: PyElement, typeEvalContext: TypeEvalContext): PyType? {
            if (element is PyFunction) {
                return typeEvalContext.getReturnType(element)
            }

            if (element is PyTargetExpression) {
                return typeEvalContext.getType(element)
            }

            return null
        }

        fun shouldShowTypeHint(element: PyElement, typeEvalContext: TypeEvalContext): Boolean {
            val hasTypeAnnotation = hasTypeAnnotation(element)
            val typeAnnotation = getTypeAnnotation(element, typeEvalContext)

            if (
                typeAnnotation == null
                || (element is PyFunction && typeAnnotation is PyNoneType)
                || hasTypeAnnotation
                || PyTypeChecker.isUnknown(typeAnnotation, false, typeEvalContext)
            ) {
                return false
            }

            return true
        }
    }
}