package space.whitememory.pythoninlayparams

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class PythonInlayHintsProvider : InlayHintsProvider<PythonInlayHintsProvider.Settings> {

    data class Settings(
        var functionHints: Boolean = true,
        var classHints: Boolean = true,
        var lambdaHints: Boolean = true,
    )

    override val key: SettingsKey<Settings> = SettingsKey("python.inlay.parameters")
    override val name = "Python Inlay Parameters"
    override val description = "Help you pass correct arguments by showing parameter names at call sites"
    override val previewText = null

    override val group = InlayGroup.PARAMETERS_GROUP

    override fun createSettings() = Settings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ) = PythonInlayHintsCollector(editor, settings)

    override fun getCaseDescription(case: ImmediateConfigurable.Case) = when (case.id) {
        "hints.classes.parameters" -> "Show parameter names for class constructors and dataclasses."
        "hints.functions.parameters" -> "Show parameter names for function and method calls."
        "hints.lambda.parameters" -> "Show parameter names for lambda calls."
        else -> null
    }

    override fun createConfigurable(settings: Settings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = panel {}

            override val cases = listOf(
                ImmediateConfigurable.Case(
                    "Class hints",
                    "hints.classes.parameters",
                    settings::classHints
                ),
                ImmediateConfigurable.Case(
                    "Function hints",
                    "hints.functions.parameters",
                    settings::functionHints
                ),
                ImmediateConfigurable.Case(
                    "Lambda hints",
                    "hints.lambda.parameters",
                    settings::lambdaHints
                )
            )
        }
    }
}