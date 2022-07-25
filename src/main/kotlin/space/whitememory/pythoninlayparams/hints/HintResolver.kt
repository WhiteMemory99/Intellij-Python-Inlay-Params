package space.whitememory.pythoninlayparams.hints

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyCallExpressionHelper
import com.jetbrains.python.psi.types.*
import space.whitememory.pythoninlayparams.variables.PythonVariablesInlayTypeHintsProvider

enum class HintResolver {

    UNDERSCORE_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean = element.name != PyNames.UNDERSCORE
    },

    QUALIFIED_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean = !element.isQualified
    },

    GENERAL_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            return shouldShowTypeHint(element, typeEvalContext)
        }
    },

    TYPING_MODULE_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            if (typeAnnotation is PyClassType && isElementInsideTypingModule(typeAnnotation.pyClass)) {
                return false
            }

            val assignedValue = element.findAssignedValue()

            if (assignedValue is PySubscriptionExpression) {
                assignedValue.rootOperand.reference?.resolve().let {
                    if (isElementInsideTypingModule(it as PyElement)) {
                        return false
                    }
                }
            }

            if (assignedValue is PyReferenceExpression && isElementInsideTypingModule(assignedValue.reference.resolve() as PyElement)) {
                return false
            }

            return true
        }
    },

    GENERIC_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            val assignedValue = element.findAssignedValue()

            assignedValue?.let {
                typeEvalContext.getType(element.findAssignedValue() as PyTypedElement)?.let {
                    return it !is PyGenericType
                }
            }

            return true
        }
    },

    EXCEPTION_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean = PsiTreeUtil.getParentOfType(element, PyExceptPart::class.java) == null
    },

    CLASS_ATTRIBUTE_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings): Boolean = settings.showClassAttributeHints

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean = !PyUtil.isClassAttribute(element)
    },

    UNION_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            if (typeAnnotation is PyUnionType) {
                return typeAnnotation.members.any { type ->
                    resolveEnabled(settings).all {
                        shouldShowTypeHint(element, type, typeEvalContext, settings)
                    }
                }
            }

            return true
        }
    },

    CLASS_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())

            if (typeAnnotation !is PyClassType) {
                return true
            }

            val resolvedClasses = typeAnnotation.pyClass.getSuperClassTypes(typeEvalContext)

            if (resolvedClasses.isNotEmpty()) {
                return resolvedClasses.all {
                    shouldShowTypeHint(element, it, typeEvalContext, settings)
                }
            }

            if (
                assignedValue is PyCallExpression
                && PyCallExpressionHelper.resolveCalleeClass(assignedValue) != null
            ) {
                // Handle case like User().get_name() and list()
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

    CONDITIONAL_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            val assignmentValue = PyUtil.peelArgument(element.findAssignedValue())

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

            if (firstElement is PyCallExpression && secondElement is PyCallExpression) {
                val isFalsePartClass = PyCallExpressionHelper.resolveCalleeClass(firstElement) != null
                val isTruePartClass = PyCallExpressionHelper.resolveCalleeClass(secondElement) != null

                if (isFalsePartClass && isTruePartClass) {
                    return false
                }
            }

            return true
        }
    },

    COMPREHENSION_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            val assignmentValue = PyUtil.peelArgument(element.findAssignedValue())

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

    SET_HINT {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        private val collectionNames = setOf("frozenset", PyNames.SET)

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            val assignmentValue = PyUtil.peelArgument(element.findAssignedValue())

            if (assignmentValue !is PyCallExpression) {
                return true
            }

            val resolvedClass = PyCallExpressionHelper.resolveCalleeClass(assignmentValue) ?: return true

            if (!collectionNames.contains(resolvedClass.name)) {
                if (resolvedClass.name == typeAnnotation?.name) {
                    return false
                }

                return true
            }

            return typeAnnotation?.isBuiltin == true
                    && (typeAnnotation as PyCollectionType).elementTypes.filterNotNull().isNotEmpty()
        }
    },

    LITERAL_EXPRESSION {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())

            if (isLiteralExpression(assignedValue)) {
                if (typeAnnotation is PyTypedDictType && assignedValue is PySequenceExpression) {
                    // Handle case when dict contains all literal expressions
                    return assignedValue.elements.none { it is PyKeyValueExpression && isLiteralExpression(it.value) }
                }

                return try {
                    !(assignedValue as PySequenceExpression).isEmpty
                } catch (e: Exception) {
                    false
                }
            }

            return true
        }
    },

    TUPLE_TYPE {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

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

            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())

            if (assignedValue !is PyTupleExpression) {
                return true
            }

            return assignedValue.elements.any { !isLiteralExpression(it) }
        }
    },

    ENUM_TYPE {
        override fun isApplicable(settings: PythonVariablesInlayTypeHintsProvider.Settings) = true

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
            settings: PythonVariablesInlayTypeHintsProvider.Settings
        ): Boolean {
            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())

            if (assignedValue !is PyReferenceExpression) {
                return true
            }

            val resolvedExpression = assignedValue.reference.resolve() ?: return true

            if (resolvedExpression is PyTargetExpression) {
                return resolveEnabled(settings)
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
            val typeAnnotation = getExpressionAnnotationType(element, typeEvalContext)

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

        private fun isElementInsideTypingModule(element: PyElement): Boolean {
            PyUtil.getContainingPyFile(element)?.let {
                return it.name == "${PyTypingTypeProvider.TYPING}${PyNames.DOT_PYI}"
                        || it.name == "typing_extensions${PyNames.DOT_PY}"
            }

            return false
        }

        fun getExpressionAnnotationType(element: PyElement, typeEvalContext: TypeEvalContext): PyType? {
            if (element is PyFunction) {
                return typeEvalContext.getReturnType(element)
            }

            if (element is PyTargetExpression) {
                return typeEvalContext.getType(element)
            }

            return null
        }

        fun shouldShowTypeHint(element: PyElement, typeEvalContext: TypeEvalContext): Boolean {
            val typeAnnotation = getExpressionAnnotationType(element, typeEvalContext)

            if (
                typeAnnotation == null
                || (element is PyFunction && typeAnnotation is PyNoneType)
                || (element is PyAnnotationOwner && element.annotation != null)
                || PyTypeChecker.isUnknown(typeAnnotation, false, typeEvalContext)
            ) {
                return false
            }

            return true
        }
    }
}