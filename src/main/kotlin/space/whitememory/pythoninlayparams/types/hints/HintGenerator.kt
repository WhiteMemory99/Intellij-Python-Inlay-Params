package space.whitememory.pythoninlayparams.types.hints

import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.types.*

enum class HintGenerator {
    UNION_TYPE() {
        override fun handleType(element: PyElement, type: PyType?, typeEvalContext: TypeEvalContext): String? {
            if (type !is PyUnionType) {
                return null
            }

            val generatedValues = type.members
                .filterNotNull()
                .map { generateTypeHintText(element, it, typeEvalContext) }
                .distinct()

            if (PyNames.NONE in generatedValues) {
                return generatedValues.joinToString(separator = " | ", limit = 3)
            }

            return generatedValues.joinToString(separator = " | ", limit = 2)
        }
    },

    ASYNC_TYPE() {
        override fun handleType(element: PyElement, type: PyType?, typeEvalContext: TypeEvalContext): String? {
            if (type == null || element !is PyFunction) {
                return null
            }

            if (type is PyCollectionType && type.classQName == PyTypingTypeProvider.COROUTINE && element.isAsync) {
                return generateTypeHintText(
                    element, PyTypingTypeProvider.coroutineOrGeneratorElementType(type)?.get(), typeEvalContext
                )
            }

            return null
        }
    },

    COLLECTION_TYPE() {
        override fun handleType(element: PyElement, type: PyType?, typeEvalContext: TypeEvalContext): String? {
            if (
                type is PyCollectionType
                && type.name != null
                && type !is PyTupleType
                && type.elementTypes.isNotEmpty()
            ) {
                val collectionName = when (type) {
                    is PyTypedDictType -> "dict"
                    else -> type.name
                }

                if (type.elementTypes.all { it == null }) {
                    return collectionName
                }

                val elements = type.elementTypes.mapNotNull { generateTypeHintText(element, it, typeEvalContext) }

                if (elements.isEmpty()) {
                    return collectionName
                }

                return elements.joinToString(separator = ", ", limit = 3, prefix = "$collectionName[", postfix = "]")
            }

            return null
        }
    },

    TUPLE_TYPE() {
        override fun handleType(element: PyElement, type: PyType?, typeEvalContext: TypeEvalContext): String? {
            if (type !is PyTupleType) {
                return null
            }

            if (type.elementCount == 0 || type.elementTypes.filterNotNull().isEmpty()) {
                return PyNames.TUPLE
            }

            if (type.elementCount > 2) {
                val firstElement = generateTypeHintText(element, type.elementTypes[0], typeEvalContext)
                val secondElement = generateTypeHintText(element, type.elementTypes[1], typeEvalContext)
                
                return "${PyNames.TUPLE}[$firstElement, $secondElement, ...]"
            }

            return type.elementTypes
                .mapNotNull { generateTypeHintText(element, it, typeEvalContext) }
                .joinToString(separator = ", ", prefix = "${PyNames.TUPLE}[", postfix = "]")
        }
    },

    CLASS_TYPE() {
        override fun handleType(element: PyElement, type: PyType?, typeEvalContext: TypeEvalContext): String? {
            if (type is PyClassType && type.isDefinition) {
                return "${PyNames.TYPE.replaceFirstChar { it.titlecaseChar() }}[${type.declarationElement?.name}]"
            }

            return null
        }
    },

    FUNCTION_TYPE() {
        override fun handleType(element: PyElement, type: PyType?, typeEvalContext: TypeEvalContext): String? {
            if (type !is PyFunctionType) {
                return null
            }

            val parametersText = when (type.callable) {
                is PyLambdaExpression -> type.callable.parameterList.getPresentableText(false, typeEvalContext)
                else -> "(${
                    type.callable.parameterList.parameters
                        .filter { !it.isSelf }
                        .joinToString(separator = ", ") { "${HintUtil.getParameterAnnotationValue(it)}" }
                })"
            }

            val callableReturnType = typeEvalContext.getReturnType(type.callable)

            return "$parametersText -> (${generateTypeHintText(element, callableReturnType, typeEvalContext)})"
        }
    },

    ANY_TYPE() {
        override fun handleType(element: PyElement, type: PyType?, typeEvalContext: TypeEvalContext): String {
            return type?.name ?: PyNames.UNKNOWN_TYPE
        }
    };

    abstract fun handleType(element: PyElement, type: PyType?, typeEvalContext: TypeEvalContext): String?

    companion object {
        fun generateTypeHintText(element: PyElement, type: PyType?, typeEvalContext: TypeEvalContext): String =
            values().firstNotNullOf { it.handleType(element, type, typeEvalContext) }
    }
}