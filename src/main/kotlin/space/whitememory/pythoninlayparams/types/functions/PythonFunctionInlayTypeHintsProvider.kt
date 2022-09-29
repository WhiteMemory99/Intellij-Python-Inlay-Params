package space.whitememory.pythoninlayparams.types.functions

import com.intellij.codeInsight.hints.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

@Suppress("UnstableApiUsage")
class PythonFunctionInlayTypeHintsProvider : InlayHintsProvider<NoSettings> {

    override val key: SettingsKey<NoSettings> = SettingsKey("python.inlay.function.types")
    override val name = "Function return types"
    override val description = "Show the return type of functions"
    override val previewText = null

    override val group = InlayGroup.TYPES_GROUP

    override fun createSettings(): NoSettings = NoSettings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector = PythonFunctionInlayTypeHintsCollector(editor)

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = panel { }
        }
    }
}