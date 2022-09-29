@file:Suppress("UnstableApiUsage")

package space.whitememory.pythoninlayparams.types.hints

import com.intellij.psi.PsiElement
import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.types.*

enum class HintGenerator {
    UNION_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): List<InlayInfoDetails>? {
            if (type !is PyUnionType) return null

            val generatedValues = type.members
                .filterNotNull()
                .map { generateTypeHintText(element, it, typeEvalContext) }
                .distinct()
                .flatten()

            val isNoneExists = generatedValues.firstOrNull {
                it.rootInlayInfo is TextInlayInfoDetail && it.rootInlayInfo.text == PyNames.NONE
            }

            if (isNoneExists != null) {
                return listOf(
                    InlayInfoDetails(
                        null,
                        generatedValues
                    )
                )
            }

            return listOf(
                InlayInfoDetails(
                    null,
                    generatedValues,
                    limit = 2
                )
            )
        }
    },

    ASYNC_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): List<InlayInfoDetails>? {
            if (type == null || element !is PyFunction) return null

            if (type is PyCollectionType && type.classQName == PyTypingTypeProvider.COROUTINE && element.isAsync) {
                return generateTypeHintText(
                    element, PyTypingTypeProvider.coroutineOrGeneratorElementType(type)?.get(), typeEvalContext
                )
            }

            return null
        }
    },

    COLLECTION_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): List<InlayInfoDetails>? {
            if (
                type is PyCollectionType
                && type.name != null
                && type !is PyTupleType
                && type.elementTypes.isNotEmpty()
            ) {
                val collectionName = when (type) {
                    is PyTypedDictType -> "dict"
                    else -> type.name
                } ?: return null

                val baseInfoDetails = resolvePsiReference(type, collectionName)

                if (type.elementTypes.all { it == null }) {
                    return listOf(
                        InlayInfoDetails(
                            baseInfoDetails
                        )
                    )
                }

                val elements =
                    type.elementTypes.mapNotNull { generateTypeHintText(element, it, typeEvalContext) }.flatten()

                if (elements.isEmpty()) {
                    return listOf(
                        InlayInfoDetails(
                            baseInfoDetails
                        )
                    )
                }

                return listOf(
                    InlayInfoDetails(
                        baseInfoDetails,
                        elements,
                        separator = ", "
                    )
                )
            }

            return null
        }
    },

    TUPLE_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): List<InlayInfoDetails>? {
            if (type !is PyTupleType) return null

            val baseInlayDetail = resolvePsiReference(type, PyNames.TUPLE)

            if (type.elementCount == 0 || type.elementTypes.filterNotNull().isEmpty()) {
                return listOf(InlayInfoDetails(baseInlayDetail))
            }

            if (type.elementCount > 2) {
                val firstElement = generateTypeHintText(element, type.elementTypes[0], typeEvalContext)
                val secondElement = generateTypeHintText(element, type.elementTypes[1], typeEvalContext)

                return listOf(
                    InlayInfoDetails(
                        baseInlayDetail,
                        firstElement + secondElement + listOf(
                            InlayInfoDetails(
                                TextInlayInfoDetail("...")
                            )
                        ),
                        separator = ", "
                    )
                )
            }

            return listOf(
                InlayInfoDetails(
                    baseInlayDetail,
                    type.elementTypes
                        .mapNotNull { generateTypeHintText(element, it, typeEvalContext) }
                        .flatten(),
                    separator = ", "
                )
            )
        }
    },

    CLASS_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): List<InlayInfoDetails>? {
            if (type !is PyClassType) return null

            val baseInlayDetail = resolvePsiReference(type, type.declarationElement?.name!!)
            val classInlayDetails = listOf(InlayInfoDetails(baseInlayDetail))

            if (type.isDefinition) {
                val inlayDetail = InlayInfoDetails(
                    TextInlayInfoDetail(PyNames.TYPE.replaceFirstChar { it.titlecaseChar() }),
                    classInlayDetails
                )

                return listOf(inlayDetail)
            }

            if (!type.isDefinition) return classInlayDetails

            return null
        }
    },

    FUNCTION_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): List<InlayInfoDetails>? {
            if (type !is PyFunctionType) return null

            val parametersText = when (type.callable) {
                is PyLambdaExpression -> type.callable.parameterList.getPresentableText(false, typeEvalContext)
                else -> "(${
                    type.callable.parameterList.parameters
                        .filter { !it.isSelf }
                        .joinToString(separator = ", ") { "${HintUtil.getParameterAnnotationValue(it)}" }
                })"
            }

            val callableReturnType = typeEvalContext.getReturnType(type.callable)

            // TODO: Implement open\close inlay presentation
            return listOf(
                InlayInfoDetails(
                    null,
                    listOf(
                        InlayInfoDetails(TextInlayInfoDetail(parametersText)),
                        InlayInfoDetails(TextInlayInfoDetail(" -> ")),
                        InlayInfoDetails(TextInlayInfoDetail("(")),
                        *generateTypeHintText(element, callableReturnType, typeEvalContext).toTypedArray(),
                        InlayInfoDetails(TextInlayInfoDetail(")"))
                    ),
                    separator = "",
                    limit = null
                )
            )
        }
    },

    ANY_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): List<InlayInfoDetails> {
            val baseInlayDetail = resolvePsiReference(type, type?.name ?: PyNames.UNKNOWN_TYPE)

            return listOf(InlayInfoDetails(baseInlayDetail))
        }
    };

    abstract fun handleType(
        element: PyElement,
        type: PyType?,
        typeEvalContext: TypeEvalContext
    ): List<InlayInfoDetails>?

    companion object {

        private fun resolvePsiReference(type: PyType?, name: String): InlayInfoDetail {
            return type?.declarationElement?.navigationElement?.let {
                return PsiInlayInfoDetail(name, it)
            } ?: TextInlayInfoDetail(name)
        }

        fun generateTypeHintText(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): List<InlayInfoDetails> =
            values().firstNotNullOf { it.handleType(element, type, typeEvalContext) }
    }
}


data class InlayInfoDetails(
    val rootInlayInfo: InlayInfoDetail?,
    val details: List<InlayInfoDetails> = listOf(),
    val separator: String = " | ",
    val limit: Int? = 3
)

sealed class InlayInfoDetail(val text: String)

class TextInlayInfoDetail(text: String) : InlayInfoDetail(text)
class PsiInlayInfoDetail(text: String, val element: PsiElement) : InlayInfoDetail(text)