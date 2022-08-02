/*
 * Copyright (C) 2022 Alessandro Gatti - Frob.it
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package it.frob.kaitaiplugin

import com.intellij.openapi.util.io.OSAgnosticPathUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.patterns.*
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

class KaitaiFileReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java)
                .withLanguage(YAMLLanguage.INSTANCE)
                .with(object : PatternCondition<YAMLScalar>("isInKaitaiFile") {
                    override fun accepts(element: YAMLScalar, context: ProcessingContext?): Boolean =
                        isInKaitaiFile(element)
                })
                .inside(object : ElementPattern<YAMLKeyValue> {
                    override fun accepts(element: Any?): Boolean {
                        return (element as? YAMLKeyValue)?.keyText == "imports"
                    }

                    override fun accepts(element: Any?, context: ProcessingContext?): Boolean {
                        return (element as? YAMLKeyValue)?.keyText == "imports"
                    }

                    override fun getCondition(): ElementPatternCondition<YAMLKeyValue> {
                        return ElementPatternCondition(object :
                                InitialPatternCondition<YAMLKeyValue>(YAMLKeyValue::class.java) {
                            })
                    }
                }),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(
                    element: PsiElement,
                    context: ProcessingContext
                ): Array<PsiReference> {
                    if (element !is YAMLScalar) return emptyArray()
                    return arrayOf(ImportReference(element))
                }
            }
        )
    }
}

class ImportReference(element: PsiElement) :
    PsiReferenceBase<PsiElement>(element, element.textRangeInParent.shiftLeft(2)) {
    override fun resolve(): PsiElement? {
        val importPath = if (!element.text.endsWith(KAITAI_FILE_EXTENSION)) {
            element.text + KAITAI_FILE_EXTENSION
        } else {
            element.text
        }

        return (
            if (OSAgnosticPathUtil.isAbsolute(importPath)) LocalFileSystem.getInstance()
                .findFileByPath(importPath) else VfsUtil.findRelativeFile(
                importPath,
                element.containingFile.virtualFile.parent
            )
            )?.let { virtualFile ->
            PsiManager.getInstance(element.project).findFile(virtualFile)
        } ?: run { null }
    }
}
