package space.whitememory.pythoninlayparams.types.hints

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyNames
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyCallExpressionHelper
import com.jetbrains.python.psi.types.*

enum class HintResolver {

    GLOBALS_HINT {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())
            if (assignedValue !is PyCallExpression) return true

            return assignedValue.callee?.name !in builtinMethods
        }
    },

    GENERAL_HINT {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())

            if (assignedValue is PyPrefixExpression) {
                if (assignedValue.operator == PyTokenTypes.AWAIT_KEYWORD) {
                    return shouldShowTypeHint(element, typeEvalContext)
                }

                return shouldShowTypeHint(assignedValue.operand as PyElement, typeEvalContext)
            }

            return shouldShowTypeHint(element, typeEvalContext)
        }
    },

    TYPING_MODULE_HINT {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())

            // Handle case `var = async_func()` without `await` keyword
            if (assignedValue is PyCallExpression) return true
            // Handle case 'var = await async_func()` which return `Coroutine` inside
            if (assignedValue is PyPrefixExpression && assignedValue.operator == PyTokenTypes.AWAIT_KEYWORD) return true

            if (assignedValue is PySubscriptionExpression) {
                assignedValue.rootOperand.reference?.resolve()?.let {
                    return !isElementInsideTypingModule(it as PyElement)
                }
            }

            if (assignedValue is PyReferenceExpression) {
                assignedValue.reference.resolve()?.let {
                    return !isElementInsideTypingModule(it as PyElement)
                }
            }

            if (typeAnnotation is PyClassType && isElementInsideTypingModule(typeAnnotation.pyClass)) return false

            return true
        }
    },

    GENERIC_HINT {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())

            assignedValue?.let {
                typeEvalContext.getType(element.findAssignedValue() as PyTypedElement)?.let {
                    return it !is PyTypeVarType
                }
            }

            return true
        }
    },

    EXCEPTION_HINT {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean = PsiTreeUtil.getParentOfType(element, PyExceptPart::class.java) == null
    },

//    CLASS_ATTRIBUTE_HINT {
//        override fun shouldShowTypeHint(
//            element: PyTargetExpression,
//            typeAnnotation: PyType?,
//            typeEvalContext: TypeEvalContext,
//        ): Boolean = !PyUtil.isClassAttribute(element)
//    },

    UNION_HINT {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            if (typeAnnotation is PyUnionType) {
                return typeAnnotation.members.any { type ->
                    values().all {
                        shouldShowTypeHint(element, type, typeEvalContext)
                    }
                }
            }

            return true
        }
    },

    CLASS_HINT {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())

            if (typeAnnotation !is PyClassType) return true

            val resolvedClasses = typeAnnotation.pyClass.getSuperClassTypes(typeEvalContext)

            if (resolvedClasses.isNotEmpty()) {
                return resolvedClasses.all {
                    shouldShowTypeHint(element, it, typeEvalContext)
                }
            }

            if (
                assignedValue is PyCallExpression
                && PyCallExpressionHelper.resolveCalleeClass(assignedValue) != null
            ) {
                // Handle case like User().get_name() and list()
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

    CONDITIONAL_HINT {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            val assignmentValue = PyUtil.peelArgument(element.findAssignedValue())

            if (assignmentValue is PyConditionalExpression) {
                return resolveExpression(ExpressionOperands.fromPyExpression(assignmentValue)!!)
            }

            if (assignmentValue is PyBinaryExpression) {
                return resolveExpression(ExpressionOperands.fromPyExpression(assignmentValue)!!)
            }

            return true
        }

        private fun resolveExpression(expressionOperands: ExpressionOperands): Boolean {
            if (isLiteralExpression(expressionOperands.leftOperand) && isLiteralExpression(expressionOperands.rightOperand)) {
                return false
            }

            if (expressionOperands.leftOperand is PyCallExpression && expressionOperands.rightOperand is PyCallExpression) {
                val isFalsePartClass = PyCallExpressionHelper.resolveCalleeClass(expressionOperands.leftOperand) != null
                val isTruePartClass = PyCallExpressionHelper.resolveCalleeClass(expressionOperands.rightOperand) != null

                if (isFalsePartClass && isTruePartClass) return false
            }

            return true
        }
    },

    COMPREHENSION_HINT {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            val assignmentValue = PyUtil.peelArgument(element.findAssignedValue())

            if (assignmentValue is PyComprehensionElement) {
                if (typeAnnotation is PyCollectionType) {
                    return typeAnnotation.elementTypes.filterNotNull().isNotEmpty()
                }

                if (assignmentValue is PySetCompExpression || assignmentValue is PyDictCompExpression) return false
            }

            return true
        }
    },

    SET_HINT {
        private val collectionNames = setOf("frozenset", PyNames.SET)

        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            val assignmentValue = PyUtil.peelArgument(element.findAssignedValue())

            if (assignmentValue !is PyCallExpression) return true

            if (typeAnnotation is PyNoneType) return true

            val resolvedClass = PyCallExpressionHelper.resolveCalleeClass(assignmentValue) ?: return true

            if (!collectionNames.contains(resolvedClass.name)) {
                return resolvedClass.name != typeAnnotation?.name
            }

            val collectionType = (typeAnnotation as? PyCollectionType) ?: return false
            return collectionType.isBuiltin && collectionType.elementTypes.filterNotNull().isNotEmpty()
        }
    },

    LITERAL_EXPRESSION {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())

            if (isLiteralExpression(assignedValue)) {
                if ((typeAnnotation is PyTypedDictType || (typeAnnotation is PyClassType && typeAnnotation.pyClass.qualifiedName == PyNames.DICT)) && assignedValue is PySequenceExpression) {
                    // Handle case when dict contains all literal expressions
                    if (assignedValue.elements.isNotEmpty() && assignedValue.elements.all {
                            it is PyKeyValueExpression && isLiteralExpression(it.value)
                        }) {
                        return false
                    }

                    if (typeAnnotation.name == "dict") return false

                    return !assignedValue.isEmpty
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
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            if (typeAnnotation !is PyTupleType) return true
            if (typeAnnotation.elementTypes.filterNotNull().isEmpty()) return false

            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())

            if (assignedValue !is PyTupleExpression) return true

            return assignedValue.elements.any { !isLiteralExpression(it) }
        }
    },

    ENUM_TYPE {
        override fun shouldShowTypeHint(
            element: PyTargetExpression,
            typeAnnotation: PyType?,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            val assignedValue = PyUtil.peelArgument(element.findAssignedValue())

            if (assignedValue !is PyReferenceExpression) return true

            val resolvedExpression = assignedValue.reference.resolve() ?: return true

            if (resolvedExpression is PyTargetExpression) {
                return values()
                    .filter { it != ENUM_TYPE }
                    .all { it.shouldShowTypeHint(resolvedExpression, typeAnnotation, typeEvalContext) }
            }

            return true
        }
    };

    abstract fun shouldShowTypeHint(
        element: PyTargetExpression,
        typeAnnotation: PyType?,
        typeEvalContext: TypeEvalContext,
    ): Boolean

    companion object {
        val builtinMethods = setOf("globals", "locals")

        fun resolve(
            element: PyTargetExpression,
            typeEvalContext: TypeEvalContext,
        ): Boolean {
            val typeAnnotation = getExpressionAnnotationType(element, typeEvalContext)

            return values().any {
                !it.shouldShowTypeHint(element, typeAnnotation, typeEvalContext)
            }
        }

        private fun isLiteralExpression(element: PyExpression?): Boolean {
            return element is PySequenceExpression || element is PyLiteralExpression || element is PySetCompExpression
        }

        private fun isElementInsideTypingModule(element: PyElement): Boolean {
            if (element is PyQualifiedNameOwner) {
                element.qualifiedName?.let {
                    return it.startsWith("${PyTypingTypeProvider.TYPING}.") || it.startsWith("typing_extensions.")
                }
            }

            return false
        }

        fun getExpressionAnnotationType(element: PyElement, typeEvalContext: TypeEvalContext): PyType? {
            if (element is PyFunction) {
                if (element.isAsync && !element.isGenerator) {
                    return element.getReturnStatementType(typeEvalContext)
                }

                return typeEvalContext.getReturnType(element)
            }
            if (element is PyTargetExpression) return typeEvalContext.getType(element)

            return null
        }

        fun shouldShowTypeHint(
            element: PyElement,
            typeEvalContext: TypeEvalContext
        ): Boolean {
            if (element.name == PyNames.UNDERSCORE) return false
            if (element is PyTargetExpression && element.isQualified) return false

            val typeAnnotation = getExpressionAnnotationType(element, typeEvalContext)

            if (
                typeAnnotation == null
                || (element is PyFunction && typeAnnotation is PyNoneType)
                || ((element is PyFunction || element is PyTargetExpression) && (element as PyTypeCommentOwner).typeCommentAnnotation != null)
                || (element is PyAnnotationOwner && element.annotation != null)
            ) {
                return false
            }

            if (typeAnnotation is PyUnionType) {
                return !typeAnnotation.members.all {
                    PyTypeChecker.isUnknown(it, false, typeEvalContext) || (it is PyNoneType || it == null)
                }
            }

            if (PyTypeChecker.isUnknown(typeAnnotation, false, typeEvalContext)) return false

            return true
        }
    }
}