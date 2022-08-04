/*
 * Copyright (C) 2022 Alessandro Gatti - Frob.it
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package it.frob.kaitaiplugin

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.include.FileIncludeInfo
import com.intellij.psi.impl.include.FileIncludeProvider
import com.intellij.util.Consumer
import com.intellij.util.indexing.FileContent
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLParserDefinition

/**
 * Custom [FileIncludeProvider] to extract includes from Kaitai Struct files.
 */
class KaitaiFileIncludeProvider : FileIncludeProvider() {
    override fun getId(): String = "kaitai_include_provider"

    override fun acceptFile(file: VirtualFile): Boolean = isKaitaiFile(file)

    override fun registerFileTypesUsedForIndexing(fileTypeSink: Consumer<in FileType>) {
        fileTypeSink.consume(YAMLFileType.YML)
    }

    override fun getIncludeInfos(content: FileContent): Array<FileIncludeInfo> {
        if (!isKaitaiFile(content.file)) {
            return emptyArray()
        }

        return PsiManager.getInstance(content.project).findViewProvider(content.file)?.let { viewProvider ->
            val visitor = ImportsVisitor()
            YAMLParserDefinition().createFile(viewProvider).accept(visitor)
            visitor.files.map { fileEntry ->
                FileIncludeInfo(fileEntry.value.path)
            }.toTypedArray()
        } ?: emptyArray()
    }
}
