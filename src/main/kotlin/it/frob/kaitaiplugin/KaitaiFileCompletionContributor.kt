/*
 * Copyright (C) 2022 Alessandro Gatti - Frob.it
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package it.frob.kaitaiplugin

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.io.OSAgnosticPathUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.nio.file.Path

private const val DUMMY_IDENTIFIER = '\u001F'

class KaitaiFileCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement()
                .withLanguage(YAMLLanguage.INSTANCE)
                .with(object : PatternCondition<PsiElement>("isInKaitaiFile") {
                    override fun accepts(element: PsiElement, context: ProcessingContext?): Boolean =
                        isInKaitaiFile(element)
                })
                .withSuperParent(
                    4,
                    PlatformPatterns.psiElement(YAMLKeyValue::class.java)
                        .with(object : PatternCondition<YAMLKeyValue>("") {
                            override fun accepts(element: YAMLKeyValue, context: ProcessingContext?): Boolean {
                                return element.keyText == "imports"
                            }
                        })
                ),
            ImportsCompletionProvider()
        )
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        context.dummyIdentifier = DUMMY_IDENTIFIER.toString()
    }
}

private class ImportsCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val parent: VirtualFile =
            parameters.position.containingFile.originalFile.virtualFile?.parent ?: return

        val completionText =
            parameters.position.text.replace(
                Regex("$DUMMY_IDENTIFIER.*", RegexOption.DOT_MATCHES_ALL),
                ""
            )
                .trim()

        if (completionText.isNotEmpty()) {
            val pathPrefix = completionText.split("/").dropLast(1).joinToString("/")
            val root: VirtualFile? = if (OSAgnosticPathUtil.isAbsolute(completionText)) {
                VfsUtil.findFile(Path.of(pathPrefix), true)
            } else {
                VfsUtil.findFile(parent.toNioPath().resolve(pathPrefix), true)
            }

            result.addAllElements(
                getChildren(
                    if (root?.isDirectory == true) {
                        root
                    } else {
                        root?.parent
                    },
                    "$pathPrefix/"
                )
            )
        } else {
            result.addAllElements(getChildren(parent))
        }
    }

    private fun getChildren(root: VirtualFile?, prefix: String = ""): List<LookupElementBuilder> =
        root?.let { rootPath ->
            rootPath.children.filter { child ->
                child.isDirectory || isKaitaiFile(child)
            }.map { child ->
                prefix + if (!child.isDirectory) {
                    child.name.removeSuffix(KAITAI_FILE_EXTENSION)
                } else {
                    "${child.name}/"
                }
            }.map { child ->
                LookupElementBuilder.create(child)
            }
        } ?: emptyList()
}
