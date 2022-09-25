package space.whitememory.pythoninlayparams

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.codeInsight.hints.Option
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyCallableType
import com.jetbrains.python.psi.types.TypeEvalContext


@Suppress("UnstableApiUsage")
class PythonInlayParameterHintsProvider : InlayParameterHintsProvider {

    companion object {
        val classHints = Option("hints.classes.parameters", { "Class hints" }, true)
        val functionHints = Option("hints.functions.parameters", { "Function hints" }, true)
        val hideOverlaps = Option("hints.overlaps.parameters", { "Hide overlaps" }, true)
    }

    private val forbiddenBuiltinFiles = setOf("builtins.pyi", "typing.pyi")

    override fun getDefaultBlackList() = setOf<String>()

    override fun isBlackListSupported() = false

    override fun getDescription() = "Help you pass correct arguments by showing parameter names at call sites"

    override fun getSupportedOptions() = listOf(classHints, functionHints, hideOverlaps)

    override fun getProperty(key: String?): String? {
        val prefix = "inlay.parameters.hints"
        return when (key) {
            "$prefix.classes.parameters" -> "Show parameter names for class constructors and dataclasses."
            "$prefix.functions.parameters" -> "Show parameter names for function and method calls."
            "$prefix.overlaps.parameters" -> "Hide hints when a parameter name is completely overlapped by a longer argument name."
            else -> null
        }
    }

    override fun getParameterHints(element: PsiElement): MutableList<InlayInfo> {
        val inlayInfos = mutableListOf<InlayInfo>()

        // This method gets every element in the editor,
        // so we have to verify it's a proper Python call expression
        if (element !is PyCallExpression || element is PyDecorator) return inlayInfos

        // Don't show hints if there's no arguments
        // Or the only argument is unpacking (*list, **dict)
        if (element.arguments.isEmpty() || (element.arguments.size == 1 && element.arguments[0] is PyStarArgument)) {
            return inlayInfos
        }

        // Implement settings based on the initial callee object
        val rootCalleeObject = element.callee?.reference?.resolve()
        if (rootCalleeObject is PyClass && !classHints.isEnabled()) return inlayInfos
        if (rootCalleeObject !is PyClass && !functionHints.isEnabled()) return inlayInfos

        // Try to resolve the object that made this call, it can be a dataclass/method/function
        val evalContext = TypeEvalContext.codeCompletion(element.project, element.containingFile)
        val resolvedCallee = element.multiResolveCallee(PyResolveContext.defaultContext(evalContext))
        if (resolvedCallee.isEmpty() || isForbiddenBuiltinCallable(resolvedCallee[0])) return inlayInfos

        // Get the parameters of the call, except `self`, `*` and `/`
        val resolvedParameters = resolvedCallee[0].getParameters(evalContext)?.filter { it ->
            !it.isSelf && it.parameter !is PySingleStarParameter && it.parameter !is PySlashParameter
        } ?: return inlayInfos

        // Don't need a hint if there's only one parameter,
        // Make an exception for *args
        if (resolvedParameters.size == 1 && !resolvedParameters[0].isPositionalContainer) return inlayInfos

        resolvedParameters.zip(element.arguments).forEach { (param, arg) ->
            val paramName = param.name ?: return@forEach
            if (arg is PyStarArgument || arg is PyKeywordArgument) {
                // It's a keyword argument or unpacking,
                // we don't need to show hits after this
                return inlayInfos
            }

            if (param.isPositionalContainer) {
                // This is an *args parameter that takes more than one argument
                // So we show it and stop the further processing of this call expression
                inlayInfos.add(InlayInfo("...$paramName", arg.textOffset))
                return inlayInfos
            } else if (param.isKeywordContainer) {
                // We don't want to show `kwargs` as a hint by accident
                return inlayInfos
            }

            if (isHintNameValid(paramName.lowercase(), arg)) {
                inlayInfos.add(InlayInfo(paramName, arg.textOffset))
            }
        }

        return inlayInfos
    }

    /**
     * Checks if the given parameter name is valid for the given argument.
     * From the point of the argument similarity compared to the parameter name,
     * If the argument is very similar, we don't need to show it.
     */
    private fun isHintNameValid(paramName: String, argument: PyExpression): Boolean {
        if (paramName.startsWith("__") || paramName.length == 1) return false

        val argumentName = if (argument is PySubscriptionExpression) {
            // It's a __getitem__ call (subscription), let's take the argument name from it
            val key = PsiTreeUtil.getChildOfType(argument, PyStringLiteralExpression::class.java)
            key?.stringValue?.lowercase() ?: argument.name?.lowercase() ?: return true
        } else {
            argument.name?.lowercase() ?: return true
        }

        if (hideOverlaps.isEnabled() && paramName in argumentName) return false
        return paramName != argumentName
    }

    /**
     * Checks if the callable is part of the standard library that isn't relevant for these hints.
     */
    private fun isForbiddenBuiltinCallable(callableType: PyCallableType): Boolean {
        // TODO: Implement using PyType.isBuiltin (?),
        //  although we still want some builtins like datetime.datetime
        val fileName = callableType.callable?.containingFile?.name ?: return false
        return fileName in forbiddenBuiltinFiles
    }
}