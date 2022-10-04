package space.whitememory.pythoninlayparams.python

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightProjectDescriptor
import com.jetbrains.python.psi.LanguageLevel


class PyLightProjectDescriptor private constructor(private val myName: String?, private val myLevel: LanguageLevel) :
    LightProjectDescriptor() {
    constructor(level: LanguageLevel) : this(null, level)

    override fun getSdk(): Sdk {
        return if (myName == null) PythonMockSdk.create(myLevel, *additionalRoots) else PythonMockSdk.create(myName)
    }

    private val additionalRoots: Array<VirtualFile>
        get() = VirtualFile.EMPTY_ARRAY

}