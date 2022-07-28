package space.whitememory.pythoninlayparams.types.variables

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent


@Suppress("UnstableApiUsage")
class PythonVariablesInlayTypeHintsProvider : InlayHintsProvider<PythonVariablesInlayTypeHintsProvider.Settings> {
    companion object {
        private val settingsKey: SettingsKey<Settings> = SettingsKey("python.inlay.types")
    }

    data class Settings(
        var showClassAttributeHints: Boolean = false,
        var showGeneralHints: Boolean = false,
    )

    override val key: SettingsKey<Settings> = settingsKey
    override val name = "Type annotations"
    override val description = "Show the type annotations for variables"
    override val previewText = null

    override val group: InlayGroup = InlayGroup.TYPES_GROUP

    override fun createSettings(): Settings = Settings()

    override fun createConfigurable(settings: Settings): ImmediateConfigurable = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JComponent = panel { }

        override val cases: List<ImmediateConfigurable.Case> = listOf(
            ImmediateConfigurable.Case(
                "Class attribute hints",
                "hints.class.attributes",
                settings::showClassAttributeHints
            ),
            ImmediateConfigurable.Case(
                "General type hints",
                "hints.general",
                settings::showGeneralHints
            )
        )
    }

    override fun getCaseDescription(case: ImmediateConfigurable.Case): String? = when (case.id) {
        "hints.class.attributes" -> "Show type hints for class attributes."
        "hints.general" -> """Enable\Disable type hints for variables."""
        else -> null
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector = PythonVariablesInlayTypeHintsCollector(editor, settings)
}