package space.whitememory.pythoninlayparams.types.hints

import com.intellij.psi.PsiElement
import com.jetbrains.python.PyNames
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.types.*

enum class HintGenerator {
    UNION_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): InlayElement? {
            if (type !is PyUnionType) return null

            return InlayElement(
                children = type.members
                    .filterNotNull()
                    .map {
                        generateTypeHintText(element, it, typeEvalContext)
                    }
                    .distinct()
            )
        }
    },

    COLLECTION_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): InlayElement? {
            if (type is PyTypedDictType && !type.isInferred()) return null
            if (type !is PyCollectionType || type.name == null || type is PyTupleType || type.elementTypes.isEmpty()) {
                return null
            }

            val collectionName = when (type) {
                is PyTypedDictType -> "dict"
                else -> type.name
            } ?: return null

            val baseElement = InlayElement(
                element = TextElement(type, collectionName)
            )

            if (type.elementTypes.all { it == null }) {
                return baseElement
            }

            val elements =
                type.elementTypes.mapNotNull { generateTypeHintText(element, it, typeEvalContext) }

            if (elements.isEmpty()) {
                return baseElement
            }

            return InlayElement(
                element = baseElement.element,
                children = elements,
                separatorType = InlaySeparator.COMMA
            )
        }
    },

    TUPLE_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): InlayElement? {
            if (type !is PyTupleType) return null

            val baseElement = TextElement(type, PyNames.TUPLE)

            if (type.elementCount == 0 || type.elementTypes.filterNotNull().isEmpty()) {
                return InlayElement(element = baseElement)
            }

            if (type.elementCount > 2) {
                val firstElement = generateTypeHintText(element, type.elementTypes[0], typeEvalContext)
                val secondElement = generateTypeHintText(element, type.elementTypes[1], typeEvalContext)

                return InlayElement(
                    element = baseElement,
                    children = listOf(
                        firstElement,
                        secondElement,
                        InlayElement(
                            element = TextElement("...")
                        )
                    ),
                    separatorType = InlaySeparator.COMMA
                )
            }

            return InlayElement(
                element = baseElement,
                children = type.elementTypes.mapNotNull { generateTypeHintText(element, it, typeEvalContext) },
                separatorType = InlaySeparator.COMMA
            )
        }
    },

    CLASS_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): InlayElement? {
            if (type !is PyClassType) return null

            val baseElement = TextElement(type, type.declarationElement?.name!!)

            val classInlayElement = InlayElement(element = baseElement)

            if (type.isDefinition) {
                val inlayDetail = InlayElement(
                    element = TextElement(text = PyNames.TYPE.replaceFirstChar { it.titlecaseChar() }),
                    children = listOf(classInlayElement)
                )
                return inlayDetail
            }

            return classInlayElement
        }
    },

    FUNCTION_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): InlayElement? {
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

            val generatedHint = generateTypeHintText(element, callableReturnType, typeEvalContext)

            return InlayElement(
                children = listOf(
                    InlayElement(TextElement("$parametersText -> (")),
                    generatedHint,
                    InlayElement(TextElement(")"))
                ),
                separatorType = InlaySeparator.EMPTY,
                inlayType = InlayType.CALLABLE
            )
        }
    },

    ANY_TYPE {
        override fun handleType(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): InlayElement {
            return InlayElement(element = TextElement(type, type?.name ?: PyNames.UNKNOWN_TYPE))
        }
    };

    abstract fun handleType(
        element: PyElement,
        type: PyType?,
        typeEvalContext: TypeEvalContext
    ): InlayElement?

    companion object {
        fun generateTypeHintText(
            element: PyElement,
            type: PyType?,
            typeEvalContext: TypeEvalContext
        ): InlayElement =
            values().firstNotNullOf { it.handleType(element, type, typeEvalContext) }
    }
}


data class TextElement(
    val text: String,
    val psiElement: PsiElement? = null,
) {
    constructor(type: PyType?, name: String) : this(name, type?.declarationElement?.navigationElement)
}


enum class InlaySeparator(val separator: String) {
    COMMA(", "),
    LINE(" | "),
    EMPTY(""),
}

enum class InlayType(val shortPrefix: String) {
    TYPE("Union"),
    CALLABLE("Callable")
}

data class InlayElement(
    val element: TextElement? = null,
    val children: List<InlayElement> = listOf(),
    val separatorType: InlaySeparator = InlaySeparator.LINE,
    val inlayType: InlayType = InlayType.TYPE
) {
    fun isEmpty(): Boolean {
        return element == null && children.isEmpty()
    }

    fun isGenericType(): Boolean {
        return element != null && children.isNotEmpty()
    }

    fun isTooLong(): Boolean {
        return contextLength > 4
    }

    private val contextLength: Short by lazy {
        children.fold(1) { acc, inlayElement ->
            (acc + inlayElement.contextLength).toShort()
        }
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
class PsiInlayInfoDetail(text: String, val element: PsiElement) : InlayInfoDetail(text) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is PsiInlayInfoDetail) return false

        if (text != other.text) return false

        if (element != other.element) return false

        return true
    }

    override fun hashCode(): Int {
        return element.hashCode()
    }
}