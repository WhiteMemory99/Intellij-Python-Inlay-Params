package space.whitememory.pythoninlayparams

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.codeInsight.hints.InlayParameterHintsProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.types.TypeEvalContext


@Suppress("UnstableApiUsage")
class PythonInlayHintsProvider : InlayParameterHintsProvider {

    private val builtinFiles = setOf("builtins.pyi", "typing.pyi")

    override fun getDefaultBlackList() = setOf<String>()

    override fun isBlackListSupported() = false

    override fun getParameterHints(element: PsiElement): MutableList<InlayInfo> {
        val inlayInfos = mutableListOf<InlayInfo>()

        // This method gets every element in the editor,
        // so we have to verify it's a Python call expression
        if (element !is PyCallExpression || element is PyDecorator) {
            return inlayInfos
        }

        // Get the arguments of the call expression and quit if there are no arguments
        // or the only argument is unpacking (*list, **dict)
        val args = element.arguments
        if (args.isEmpty() || (args.size == 1 && args[0] is PyStarArgument)) {
            return inlayInfos
        }

        // Try to resolve the object that made this call
        var resolved = element.callee?.reference?.resolve() ?: return inlayInfos
        if (isBuiltinElement(resolved)) {
            return inlayInfos
        }

        // Handle lambda expression calls
        if (resolved is PyTargetExpression) {
            // Use the target to find the lambda expression object, and assign it to get its parameters up ahead
            resolved = PsiTreeUtil.getNextSiblingOfType(resolved, PyLambdaExpression::class.java) ?: return inlayInfos
        }

        val dataclassAttributes = mutableListOf<String>()
        var hasExplicitInit = false
        if (resolved is PyClass) {
            // This call is made by a class (initialization), so we want to find the parameters it takes.
            // In order to do so, we first have to check for an init method, and if not found,
            // We will use the class attributes instead. (Handle dataclasses, attrs, etc.)
            resolved.classAttributes.forEach {
                // TODO: Somehow make sure we take only correct attributes
                val attributeName = it.name ?: return@forEach
                dataclassAttributes.add(attributeName)
            }

            val evalContext = TypeEvalContext.codeAnalysis(element.project, element.containingFile)
            val initMethod = resolved.findMethodByName("__init__", false, evalContext)
            resolved = if (initMethod != null) {
                // Take a note that this class has init
                hasExplicitInit = true
                initMethod
            } else {
                // Try to find init in its parent classes instead, otherwise stick to the attributes
                resolved.findMethodByName("__init__", true, evalContext) ?: resolved
            }
        }

        val resolvedParameters = getElementFilteredParameters(resolved)
        // If there's no parameters in the object, we use the dataclass attributes instead, if there is any
        if (resolvedParameters.isEmpty() && dataclassAttributes.isNotEmpty() && !hasExplicitInit) {
            dataclassAttributes.zip(args).forEach {
                inlayInfos.add(InlayInfo(it.first, it.second.textOffset))
            }
            return inlayInfos
        }

        resolvedParameters.zip(args).forEach { (param, arg) ->
            val paramName = param.name ?: return@forEach
            if (param is PyNamedParameter && param.isPositionalContainer) {
                // This is an *args parameter that takes more than one argument
                // So we stop the further processing of this call expression
                inlayInfos.add(InlayInfo("...$paramName", arg.textOffset))
                return inlayInfos
            }

            // The argument is unpacking, we don't want to show hints any further
            // Because we can't be sure what parameters it covers
            if (arg is PyStarArgument) {
                return inlayInfos
            }

            // Skip this parameter if its name starts with __,
            // or equals to the argument provided
            if (arg !is PyKeywordArgument && paramName != arg.name && !paramName.startsWith("__")) {
                // TODO: Add more complex filters
                inlayInfos.add(InlayInfo(paramName, arg.textOffset))
            }
        }

        return inlayInfos
    }

    /**
     * Get the parameters of the element, but filter out the ones that are not needed.
     * For example, if the element is a class method, we don't want to show the __self__ parameter.
     */
    private fun getElementFilteredParameters(element: PsiElement): List<PyParameter> {
        element.children.forEach {
            if (it is PyParameterList) {
                return it.parameters.filter { param -> !param.isSelf }
            }
        }
        return emptyList()
    }

    /**
     * Checks if the element is part of the standard library.
     * It's quite shallow though, as I'm not sure how to properly implement it.
     *
     * @param element The element to check.
     * @return True if the element is part of the stdlib.
     */
    private fun isBuiltinElement(element: PsiElement): Boolean {
        // TODO: Find a better way
        return element.containingFile.name in builtinFiles
    }
}
