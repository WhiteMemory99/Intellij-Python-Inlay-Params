package space.whitememory.pythoninlayparams

import com.intellij.testFramework.utils.inlays.InlayHintsProviderTestCase
import com.intellij.testFramework.utils.inlays.declarative.DeclarativeInlayHintsProviderTestCase
import com.jetbrains.python.psi.LanguageLevel
import space.whitememory.pythoninlayparams.python.PyLightProjectDescriptor


abstract class PythonAbstractInlayHintsTestCase : DeclarativeInlayHintsProviderTestCase() {
    override fun getProjectDescriptor() = PyLightProjectDescriptor(LanguageLevel.getLatest())
}