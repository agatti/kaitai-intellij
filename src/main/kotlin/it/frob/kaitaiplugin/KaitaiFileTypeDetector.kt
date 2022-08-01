/*
 * Copyright (C) 2022 Alessandro Gatti - Frob.it
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package it.frob.kaitaiplugin

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry.FileTypeDetector
import com.intellij.openapi.util.io.ByteSequence
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.yaml.YAMLFileType

class KaitaiFileTypeDetector : FileTypeDetector {
    override fun detect(file: VirtualFile, firstBytes: ByteSequence, firstCharsIfText: CharSequence?): FileType? =
        if (isKaitaiFile(file)) {
            YAMLFileType.YML
        } else {
            null
        }
}
