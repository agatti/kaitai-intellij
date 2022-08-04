/*
 * Copyright (C) 2022 Alessandro Gatti - Frob.it
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package it.frob.kaitaiplugin

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.io.OSAgnosticPathUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.include.FileIncludeInfo
import com.intellij.psi.impl.include.FileIncludeProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.Consumer
import com.intellij.util.indexing.FileContent
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLParserDefinition
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.nio.file.Path

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

/**
 * Data container for import definitions.
 *
 * @param element the [PsiElement] where an import declaration is found.
 * @param file the target [PsiFile] where the import file is defined, if available.
 * @param path the absolute import path as a string.
 */
data class ImportsContainer(val element: PsiElement, val file: PsiFile?, val path: String)

/**
 * Resolve an import declaration from the given element.
 *
 * @param element the element containing the import declaration.
 * @return an [ImportsContainer] instance with the resolved import declaration.
 */
internal fun resolveImport(element: PsiElement): ImportsContainer {
    val pathString = element.text + KAITAI_FILE_EXTENSION
    val absolutePath = if (OSAgnosticPathUtil.isAbsolute(pathString)) {
        Path.of(pathString).toAbsolutePath()
    } else {
        element.containingFile.containingDirectory.virtualFile.toNioPath()
            .resolve(pathString).toAbsolutePath()
    }
    val virtualFile = VfsUtil.findFile(absolutePath, true)
    val psiFile = if (virtualFile != null) {
        PsiManager.getInstance(element.project).findFile(virtualFile)
    } else {
        null
    }

    return ImportsContainer(element, psiFile, absolutePath.toString())
}

/**
 * Element visitor that collects and resolves imports as they are found.
 */
private class ImportsVisitor : PsiElementVisitor() {
    /**
     * A set with the resolved import declarations found so far.
     */
    private val filesMap = mutableMapOf<PsiElement, ImportsContainer>()

    /**
     * Read-only getter for the resolved import declarations.
     */
    val files: Map<PsiElement, ImportsContainer> get() = filesMap

    override fun visitElement(element: PsiElement) {
        PsiTreeUtil.collectElements(element) { psiElement ->
            psiElement.elementType == YAMLElementTypes.SCALAR_PLAIN_VALUE &&
                PsiTreeUtil.findFirstParent(psiElement) { parentPsiElement ->
                (parentPsiElement.elementType == YAMLElementTypes.KEY_VALUE_PAIR) &&
                    (parentPsiElement as YAMLKeyValue).keyText == "imports"
            } != null
        }.forEach { psiElement ->
            filesMap[element] = resolveImport(psiElement)
        }
    }
}
