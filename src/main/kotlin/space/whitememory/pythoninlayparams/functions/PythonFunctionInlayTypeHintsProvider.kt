package space.whitememory.pythoninlayparams.functions

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class PythonFunctionInlayTypeHintsProvider: InlayHintsProvider<NoSettings> {

    override val key: SettingsKey<NoSettings> = SettingsKey("python.inlay.function.types")
    override val name = "Function type hints"
    override val description = "Show the return type of functions in the editor"
    override val previewText = null

    override val group = InlayGroup.TYPES_GROUP

    override fun createSettings(): NoSettings = NoSettings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector = PythonFunctionInlayTypeHintsCollector(editor, settings)

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object  : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = panel {  }
        }
    }
}