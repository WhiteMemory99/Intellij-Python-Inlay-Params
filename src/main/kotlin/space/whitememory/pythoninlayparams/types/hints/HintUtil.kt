package space.whitememory.pythoninlayparams.types.hints

import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyConditionalExpression
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyNamedParameter
import com.jetbrains.python.psi.PyParameter
import kotlin.math.exp

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


data class ExpressionOperands(val leftOperand: PyExpression, val rightOperand: PyExpression?) {
    companion object {
        fun fromPyExpression(expression: PyExpression): ExpressionOperands? {
            if (expression is PyBinaryExpression) {
                return ExpressionOperands(
                    leftOperand = expression.leftExpression,
                    rightOperand = expression.rightExpression
                )
            }

            if (expression is PyConditionalExpression) {
                return ExpressionOperands(
                    leftOperand = expression.truePart,
                    rightOperand = expression.falsePart
                )
            }

            return null
        }
    }
}