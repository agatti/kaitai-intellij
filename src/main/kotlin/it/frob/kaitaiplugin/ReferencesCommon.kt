/*
 * Copyright (C) 2022 Alessandro Gatti - Frob.it
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package it.frob.kaitaiplugin

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
