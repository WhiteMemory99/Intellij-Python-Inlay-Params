package space.whitememory.pythoninlayparams

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent


@Suppress("UnstableApiUsage")
class PythonInlayTypeHintsProvider : InlayHintsProvider<PythonInlayTypeHintsProvider.Settings> {
    companion object {
        private val settingsKey: SettingsKey<Settings> = SettingsKey("python.inlay.types")
    }

    override fun createSettings(): Settings = Settings()

    data class Settings(var insertBeforeIdentifier: Boolean = false)

    override val name: String = "Type hints"
    override val key: SettingsKey<Settings> = settingsKey
    override val group: InlayGroup = InlayGroup.TYPES_GROUP

    override val previewText: String = ""
    override fun createConfigurable(settings: Settings): ImmediateConfigurable = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JComponent = panel {
            row {
                checkBox("Test")
            }
        }

        override val mainCheckboxText: String = "Test checkbox"
    }

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink
    ): InlayHintsCollector {
        return PythonInlayTypeHintsCollector(editor, settings)
    }
}