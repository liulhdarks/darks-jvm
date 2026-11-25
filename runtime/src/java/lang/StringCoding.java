/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/*
 * darks-jvm: JDK 11 routes byte/char conversion through StringCoding, which
 * relies on the sun.nio.cs thread-local coders.  This runtime keeps its own
 * java.nio.charset implementation instead, so this class is a thin adapter
 * with the same entry points as the JDK version.  The Result layout matches
 * JDK 11 so String's constructors stay unmodified.
 */
final class StringCoding {

    private StringCoding() {
    }

    static final class Result {
        final byte[] value;
        final byte coder;

        Result(byte[] value, byte coder) {
            this.value = value;
            this.coder = coder;
        }
    }

    private static Charset charsetForName(String charsetName)
            throws UnsupportedEncodingException {
        if (charsetName == null) {
            throw new UnsupportedEncodingException("null");
        }
        try {
            return Charset.forName(charsetName);
        } catch (RuntimeException unsupported) {
            UnsupportedEncodingException e = new UnsupportedEncodingException(charsetName);
            e.initCause(unsupported);
            throw e;
        }
    }

    static Result decode(byte[] bytes, int offset, int length) {
        return decode(Charset.defaultCharset(), bytes, offset, length);
    }

    static Result decode(String charsetName, byte[] bytes, int offset, int length)
            throws UnsupportedEncodingException {
        return decode(charsetForName(charsetName), bytes, offset, length);
    }

    static Result decode(Charset charset, byte[] bytes, int offset, int length) {
        String decoded = charset.decode(bytes, offset, length);
        return new Result(decoded.value(), decoded.coder());
    }

    static byte[] encode(byte coder, byte[] value) {
        return encode(Charset.defaultCharset(), coder, value);
    }

    static byte[] encode(String charsetName, byte coder, byte[] value)
            throws UnsupportedEncodingException {
        return encode(charsetForName(charsetName), coder, value);
    }

    static byte[] encode(Charset charset, byte coder, byte[] value) {
        return charset.encodeBytes(new String(value, coder));
    }
}
