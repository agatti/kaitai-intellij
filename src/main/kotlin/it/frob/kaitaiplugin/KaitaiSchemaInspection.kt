/*
 * Copyright (C) 2022 Alessandro Gatti - Frob.it
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package it.frob.kaitaiplugin

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.util.ResourceUtil
import com.jetbrains.jsonSchema.extension.JsonLikePsiWalker
import com.jetbrains.jsonSchema.ide.JsonSchemaService
import com.jetbrains.jsonSchema.impl.JsonComplianceCheckerOptions
import com.jetbrains.jsonSchema.impl.JsonSchemaComplianceChecker
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YamlPsiElementVisitor

class KaitaiSchemaInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ): PsiElementVisitor {
        val documents = (holder.file as YAMLFile).documents
        if (documents.size != 1) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        val virtualFile =
            VfsUtil.findFileByURL(ResourceUtil.getResource(javaClass.classLoader, "schema", "ksy.schema.json"))
                ?: return PsiElementVisitor.EMPTY_VISITOR
        val schema = JsonSchemaService.Impl.get(holder.file.project).getSchemaObjectForSchemaFile(virtualFile)
            ?: return PsiElementVisitor.EMPTY_VISITOR

        return object : YamlPsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                val walker = JsonLikePsiWalker.getWalker(element, schema) ?: return
                if (element != documents.first().topLevelValue) return
                JsonSchemaComplianceChecker(
                    schema,
                    holder,
                    walker,
                    session,
                    JsonComplianceCheckerOptions(false)
                ).annotate(element)
            }
        }
    }
}
