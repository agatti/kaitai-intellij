/*
 * Copyright (C) 2022 Alessandro Gatti - Frob.it
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package it.frob.kaitaiplugin

import com.intellij.openapi.util.io.OSAgnosticPathUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import java.nio.file.Path

/**
 * Standard types defined in the Kaitai Struct documentation.
 */
internal val STANDARD_TYPES = listOf(
    "f4", "f8", "f4be", "f8be", "f4le", "f8le",
    "s1", "s2", "s4", "s8", "s1be", "s2be", "s4be", "s8be", "s1le", "s2le", "s4le", "s8le",
    "u1", "u2", "u4", "u8", "u1be", "u2be", "u4be", "u8be", "u1le", "u2le", "u4le", "u8le",
    "str", "strz"
)

/**
 * Check if the given type is one of the standard types as per the Kaitai Struct documentation.
 *
 * @param type the type name to check.
 * @return `true` if the given type is standard, `false` otherwise.
 */
internal fun isStandardType(type: String): Boolean =
    STANDARD_TYPES.contains(type.trim()) || type.trim().matches(Regex("""^b\d+$"""))

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
        element.containingFile.originalFile.containingDirectory.virtualFile.toNioPath()
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
internal class ImportsVisitor : PsiElementVisitor() {
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

/**
 * Element visitor that collects custom type definitions as they are found.
 */
internal class TypesVisitor : PsiElementVisitor() {

    /**
     * The collected types.
     */
    private val types: MutableSet<PsiElement> = mutableSetOf()

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        when {
            element.elementType == YAMLElementTypes.KEY_VALUE_PAIR && (element as YAMLKeyValue).keyText == "types" &&
                element.parent.parent is YAMLDocument -> {
                element.children
                    .filter { child -> child.elementType == YAMLElementTypes.MAPPING }
                    .flatMap { child -> (child as YAMLMapping).keyValues }
                    .filterNotNull()
                    .forEach { child ->
                        types.add(child)
                    }
            }

            !types.contains(element) && element.elementType == YAMLElementTypes.SCALAR_PLAIN_VALUE &&
                PsiTreeUtil.findFirstParent(element) { parentPsiElement ->
                (parentPsiElement.elementType == YAMLElementTypes.KEY_VALUE_PAIR) &&
                    (parentPsiElement as YAMLKeyValue).keyText == "imports"
            } != null -> {
                resolveImport(element).file?.let { psiFile ->
                    visitFile(psiFile)
                }
            }

            else -> element.acceptChildren(this)
        }
    }

    /**
     * Start traversing the element tree, collecting custom type definitions.
     *
     * @param element the [PsiElement] to start the traversal from.
     * @return a list of [PsiElement] that contain the custom type definitions.
     */
    fun accept(element: PsiElement): List<PsiElement> {
        types.clear()
        element.accept(this)
        return types.toList()
    }
}
