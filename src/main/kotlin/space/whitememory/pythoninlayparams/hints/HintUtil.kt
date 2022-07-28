package space.whitememory.pythoninlayparams.hints

import com.jetbrains.python.psi.PyNamedParameter
import com.jetbrains.python.psi.PyParameter

sealed class HintUtil {
    companion object {
        fun getParameterAnnotationValue(parameter: PyParameter?): String? = when {
            parameter !is PyNamedParameter -> parameter?.name
            parameter.isKeywordContainer -> "**${parameter.name}"
            parameter.isPositionalContainer -> "*${parameter.name}"
            else -> {
                parameter.annotationValue?.let {
                    "${parameter.name}: ${parameter.annotationValue}"
                } ?: parameter.name
            }
        }
    }
}
