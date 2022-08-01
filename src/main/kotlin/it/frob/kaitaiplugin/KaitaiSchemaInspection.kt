/*
 * Copyright (C) 2022 Alessandro Gatti - Frob.it
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package it.frob.kaitaiplugin

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ResourceUtil
import com.jetbrains.jsonSchema.extension.JsonLikePsiWalker
import com.jetbrains.jsonSchema.ide.JsonSchemaService
import com.jetbrains.jsonSchema.impl.JsonComplianceCheckerOptions
import com.jetbrains.jsonSchema.impl.JsonSchemaComplianceChecker
import com.jetbrains.jsonSchema.impl.JsonSchemaObject
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

private val CACHED_KSY_SCHEMA = CachedValuesManager.getManager(ProjectManager.getInstance().defaultProject)
    .createCachedValue(object : CachedValueProvider<JsonSchemaObject> {
        override fun compute(): CachedValueProvider.Result<JsonSchemaObject>? {
            val virtualFile = VfsUtil.findFileByURL(
                ResourceUtil.getResource(
                    javaClass.classLoader,
                    "schema",
                    "ksy.schema.json"
                )
            ) ?: return null
            val schema = JsonSchemaService.Impl.get(ProjectManager.getInstance().defaultProject)
                .getSchemaObjectForSchemaFile(virtualFile) ?: return null

            return CachedValueProvider.Result.create(schema, ModificationTracker.NEVER_CHANGED)
        }
    })

class KaitaiSchemaInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        if (!isKaitaiFile(holder.file)) {
            return PsiElementVisitor.EMPTY_VISITOR
        }
        val documents = (holder.file as YAMLFile).documents
        if (documents.size != 1) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        CACHED_KSY_SCHEMA.value?.let { schemaObject ->
            return object : YamlPsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    val walker = JsonLikePsiWalker.getWalker(element, schemaObject) ?: return
                    if (element != documents.first().topLevelValue) return
                    JsonSchemaComplianceChecker(
                        schemaObject,
                        holder,
                        walker,
                        session,
                        JsonComplianceCheckerOptions(false)
                    ).annotate(element)
                }
            }
        }?.run {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        // Needed only to silence the following compiler error:
        //
        // A 'return' expression required in a function with a block body ('{...}'). If you got this error after the
        // compiler update, then it's most likely due to a fix of a bug introduced in 1.3.0 (see KT-28061 for details)
        return PsiElementVisitor.EMPTY_VISITOR
    }
}
