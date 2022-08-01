/*
 * Copyright (C) 2022 Alessandro Gatti - Frob.it
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package it.frob.kaitaiplugin

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

private const val KAITAI_FILE_EXTENSION = ".ksy"

internal fun isKaitaiFile(file: VirtualFile): Boolean = file.nameSequence.endsWith(KAITAI_FILE_EXTENSION)

internal fun isKaitaiFile(file: PsiFile): Boolean = file.name.endsWith(KAITAI_FILE_EXTENSION)
