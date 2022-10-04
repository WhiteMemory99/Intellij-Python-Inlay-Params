package space.whitememory.pythoninlayparams

import com.intellij.testFramework.utils.inlays.InlayHintsProviderTestCase
import com.jetbrains.python.psi.LanguageLevel
import space.whitememory.pythoninlayparams.python.PyLightProjectDescriptor


abstract class PythonAbstractInlayHintsTestCase : InlayHintsProviderTestCase() {
    override fun getProjectDescriptor() = PyLightProjectDescriptor(LanguageLevel.getLatest())
}