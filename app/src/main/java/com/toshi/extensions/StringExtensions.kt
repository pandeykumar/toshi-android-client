/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
@file:JvmName("StringUtils")
package com.toshi.extensions

import android.util.Patterns
import com.toshi.model.local.Group.GROUP_ID_LENGTH
import java.net.URI
import java.net.URISyntaxException

fun String.isGroupId(): Boolean {
    // Todo - check compatability with other clients (i.e. iOS)
    return length == GROUP_ID_LENGTH
}

fun String.isWebUrl() = Patterns.WEB_URL.matcher(this.trim()).matches()

fun String.getProtocolAndHost(): String {
    val uri = getUri(this) ?: return ""
    val scheme = uri.scheme ?: null
    val host = uri.host ?: null
    return when {
        host == null -> ""
        scheme == null -> host
        else -> "$scheme://$host"
    }
}

private fun getUri(value: String): URI? {
    return try {
        URI(value)
    } catch (e: URISyntaxException) {
        null
    }
}