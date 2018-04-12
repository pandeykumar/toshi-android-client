/*
 * 	Copyright (c) 2017. Toshi Inc
 *
 *  This program is free software: you can redistribute it and/or modify
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

package com.toshi.util.url

import com.toshi.extensions.getProtocolAndHost
import junit.framework.Assert.assertEquals
import org.junit.Test

class UrlTests {

    private val urls by lazy {
        listOf(
                "https://buy.coinbase.com",
                "http://buy.coinbase.com",
                "https://buy.coinbase.com?code=9a6fb1e3-af41-5677-9b67-8c8a0e365771&address=",
                "http://buy.coinbase.com?code=9a6fb1e3-af41-5677-9b67-8c8a0e365771&address=",
                "https://google.com",
                "http://google.com",
                "https:// invalid .com",
                ""
        )
    }

    @Test
    fun testGettingProtocolAndHostFromCoinbaseUrl() {
        val coinbaseHttps = urls[0].getProtocolAndHost()
        val coinbaseHttp = urls[1].getProtocolAndHost()
        assertEquals(urls[0], coinbaseHttps)
        assertEquals(urls[1], coinbaseHttp)
    }

    @Test
    fun testGettingProtocolAndHostFromCoinbaseUrlWithParams() {
        val coinbaseHttpsWithParams = urls[2].getProtocolAndHost()
        val coinbaseHttpWithParams = urls[3].getProtocolAndHost()
        assertEquals(urls[0], coinbaseHttpsWithParams)
        assertEquals(urls[1], coinbaseHttpWithParams)
    }

    @Test
    fun testGettingProtocolAndHostFromGoogleUrl() {
        val googleHttps = urls[4].getProtocolAndHost()
        val googleHttp = urls[5].getProtocolAndHost()
        assertEquals(urls[4], googleHttps)
        assertEquals(urls[5], googleHttp)
    }

    @Test
    fun testGettingProtocolAndHostFromEmptyString() {
        val emptyUrl = urls[6].getProtocolAndHost()
        assertEquals("", emptyUrl)
    }

    @Test
    fun testGettingProtocolAndHostFromInvalidString() {
        val emptyUrl = urls[7].getProtocolAndHost()
        assertEquals("", emptyUrl)
    }
}