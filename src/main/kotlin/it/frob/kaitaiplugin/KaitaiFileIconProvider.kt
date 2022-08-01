/*
 * Copyright (C) 2022 Alessandro Gatti - Frob.it
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package it.frob.kaitaiplugin

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class KaitaiFileIconProvider : FileIconProvider {
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? =
        if (file.nameSequence.endsWith("ksy")) {
            FILE_ICON
        } else null

    companion object {
        private val FILE_ICON = IconLoader.getIcon("/icons/fileicon.svg", KaitaiFileIconProvider::class.java)
    }
}
