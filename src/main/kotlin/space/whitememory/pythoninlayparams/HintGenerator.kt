package space.whitememory.pythoninlayparams

import com.jetbrains.python.PyNames
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.types.*

enum class HintGenerator {
    UNION_TYPE() {
        override fun handleType(type: PyType?, typeEvalContext: TypeEvalContext): String? {
            if (type !is PyUnionType) {
                return null
            }

            return type.members
                .filterNotNull()
                .joinToString(separator = " | ", limit = 2) { generateTypeHintText(it, typeEvalContext) }
        }
    },

    COLLECTION_TYPE() {
        override fun handleType(type: PyType?, typeEvalContext: TypeEvalContext): String? {
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

                val elements = type.elementTypes.mapNotNull { generateTypeHintText(it, typeEvalContext) }

                if (elements.isEmpty()) {
                    return collectionName
                }

                return elements.joinToString(separator = ", ", limit = 3, prefix = "$collectionName[", postfix = "]")
            }

            return null
        }
    },

    TUPLE_TYPE() {
        override fun handleType(type: PyType?, typeEvalContext: TypeEvalContext): String? {
            if (type !is PyTupleType) {
                return null
            }

            if (type.elementCount == 0 || type.elementTypes.filterNotNull().isEmpty()) {
                return PyNames.TUPLE
            }

            if (type.elementCount > 2) {
                val firstElement = generateTypeHintText(type.elementTypes[0], typeEvalContext)
                val secondElement = generateTypeHintText(type.elementTypes[1], typeEvalContext)
                
                return "${PyNames.TUPLE}[$firstElement, $secondElement, ...]"
            }
            
            return type.elementTypes
                .mapNotNull { generateTypeHintText(it, typeEvalContext) }
                .joinToString(separator = ", ", prefix = "${PyNames.TUPLE}[", postfix = "]")
        }
    },

    CLASS_TYPE() {
        override fun handleType(type: PyType?, typeEvalContext: TypeEvalContext): String? {
            if (type is PyClassType && type.isDefinition) {
                return "${PyNames.TYPE.replaceFirstChar { it.titlecaseChar() }}[${type.declarationElement?.name}]"
            }

            return null
        }
    },

    FUNCTION_TYPE() {
        override fun handleType(type: PyType?, typeEvalContext: TypeEvalContext): String? {
            if (type !is PyFunctionType) {
                return null
            }

            val parametersText = when (type.callable) {
                is PyLambdaExpression -> type.callable.parameterList.getPresentableText(false, typeEvalContext)
                else -> "(${type.callable.parameterList.parameters
                    .filter { !it.isSelf }
                    .joinToString(separator = ", ") { "${HintUtil.getParameterAnnotationValue(it)}" }})"
            }

            val callableReturnType = typeEvalContext.getReturnType(type.callable)

            return "$parametersText -> (${generateTypeHintText(callableReturnType, typeEvalContext)})"
        }
    },

    ANY_TYPE() {
        override fun handleType(type: PyType?, typeEvalContext: TypeEvalContext): String {
            return type?.name ?: PyNames.UNKNOWN_TYPE
        }
    };

    abstract fun handleType(type: PyType?, typeEvalContext: TypeEvalContext): String?

    companion object {
        fun generateTypeHintText(type: PyType?, typeEvalContext: TypeEvalContext): String =
            values().firstNotNullOf { it.handleType(type, typeEvalContext) }
    }
}