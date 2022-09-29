package space.whitememory.pythoninlayparams.types.variables

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent


@Suppress("UnstableApiUsage")
class PythonVariablesInlayTypeHintsProvider : InlayHintsProvider<PythonVariablesInlayTypeHintsProvider.Settings> {
    
    data class Settings(
        var showClassAttributeHints: Boolean = false,
        var showGeneralHints: Boolean = false,
    )

    override val key: SettingsKey<Settings> = SettingsKey("python.inlay.types")
    override val name = "Type annotations"
    override val description = "Show the type annotations for variables"
    override val previewText = null

    override val group: InlayGroup = InlayGroup.TYPES_GROUP

    override fun createSettings(): Settings = Settings()

    override fun createConfigurable(settings: Settings): ImmediateConfigurable = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JComponent = panel { }

        override val cases: List<ImmediateConfigurable.Case> = listOf(
            ImmediateConfigurable.Case(
                "Class attributes",
                "hints.class.attributes",
                settings::showClassAttributeHints
            ),
            ImmediateConfigurable.Case(
                "General hints",
                "hints.general",
                settings::showGeneralHints
            )
        )
    }

    override fun getCaseDescription(case: ImmediateConfigurable.Case) = when (case.id) {
        "hints.class.attributes" -> "Show type hints for class attributes."
        "hints.general" -> "Enable type hints for variables."
        else -> null
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector = PythonVariablesInlayTypeHintsCollector(editor, settings)
}