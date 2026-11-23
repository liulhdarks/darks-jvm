/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * An OutputStreamWriter is a bridge from character streams to byte streams:
 * Characters written to it are encoded into bytes using a specified {@link
 * java.nio.charset.Charset charset}.  The charset that it uses
 * may be specified by name or may be given explicitly, or the platform's
 * default charset may be accepted.
 *
 * <p> This runtime keeps the public JDK 11 surface of the class.  The JDK
 * implementation delegates encoding to the internal {@code sun.nio.cs.StreamEncoder}
 * class, which is not ported; here each write is encoded directly through
 * {@link String#getBytes(Charset)} instead.  Consequences (simplifications,
 * kept explicit): there is no internal byte buffer, so {@link #flushBuffer()}
 * is a no-op, and a surrogate pair split across two {@code write} calls is
 * encoded as two replacement characters.
 *
 * @author      Mark Reinhold
 * @since       1.1
 */

public class OutputStreamWriter extends Writer {

    private final OutputStream out;
    private final Charset cs;

    /**
     * Creates an OutputStreamWriter that uses the named charset.
     *
     * @param  out
     *         An OutputStream
     *
     * @param  charsetName
     *         The name of a supported
     *         {@link java.nio.charset.Charset charset}
     *
     * @exception  UnsupportedEncodingException
     *             If the named encoding is not supported
     */
    public OutputStreamWriter(OutputStream out, String charsetName)
        throws UnsupportedEncodingException
    {
        this(out, Charset.forName(charsetName));
    }

    /**
     * Creates an OutputStreamWriter that uses the default character encoding.
     *
     * @param  out  An OutputStream
     */
    public OutputStreamWriter(OutputStream out) {
        this(out, Charset.defaultCharset());
    }

    /**
     * Creates an OutputStreamWriter that uses the given charset.
     *
     * @param  out
     *         An OutputStream
     *
     * @param  cs
     *         A charset
     *
     * @since 1.4
     */
    public OutputStreamWriter(OutputStream out, Charset cs) {
        super(out);
        if (cs == null) {
            throw new NullPointerException("charset");
        }
        this.out = out;
        this.cs = cs;
    }

    /**
     * Creates an OutputStreamWriter that uses the given charset encoder.
     *
     * @param  out
     *         An OutputStream
     *
     * @param  enc
     *         A charset encoder
     *
     * @since 1.4
     */
    public OutputStreamWriter(OutputStream out, CharsetEncoder enc) {
        super(out);
        if (enc == null) {
            throw new NullPointerException("charset encoder");
        }
        this.out = out;
        this.cs = enc.charset();
    }

    /**
     * Returns the name of the character encoding being used by this stream.
     *
     * @return The historical name of this encoding, or null if the stream
     *         has been closed
     */
    public String getEncoding() {
        return cs.name();
    }

    /**
     * Flushes the output buffer to the underlying byte stream.  This runtime
     * encodes eagerly and keeps no byte buffer, so there is nothing to flush.
     */
    void flushBuffer() throws IOException {
    }

    /**
     * Writes a single character.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(int c) throws IOException {
        char[] one = new char[1];
        one[0] = (char) c;
        write(one, 0, 1);
    }

    /**
     * Writes a portion of an array of characters.
     *
     * @param  cbuf  Buffer of characters
     * @param  off   Offset from which to start writing characters
     * @param  len   Number of characters to write
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(char cbuf[], int off, int len) throws IOException {
        synchronized (lock) {
            if (off < 0 || len < 0 || off + len > cbuf.length) {
                throw new IndexOutOfBoundsException("offset " + off +
                    ", length " + len + ", array length " + cbuf.length);
            }
            if (len == 0) {
                return;
            }
            out.write(new String(cbuf, off, len).getBytes(cs));
        }
    }

    /**
     * Writes a portion of a string.
     *
     * @param  str  A String
     * @param  off  Offset from which to start writing characters
     * @param  len  Number of characters to write
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(String str, int off, int len) throws IOException {
        synchronized (lock) {
            if (off < 0 || len < 0 || off + len > str.length()) {
                throw new IndexOutOfBoundsException("offset " + off +
                    ", length " + len + ", string length " + str.length());
            }
            if (len == 0) {
                return;
            }
            out.write(str.substring(off, off + len).getBytes(cs));
        }
    }

    /**
     * Flushes the stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void flush() throws IOException {
        synchronized (lock) {
            out.flush();
        }
    }

    /**
     * Closes the stream, flushing it first.  Once the stream has been closed,
     * further write() or flush() invocations will cause an IOException to be
     * thrown.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void close() throws IOException {
        synchronized (lock) {
            out.close();
        }
    }
}
