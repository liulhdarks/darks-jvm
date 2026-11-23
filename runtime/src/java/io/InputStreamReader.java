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

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

/**
 * An InputStreamReader is a bridge from byte streams to character streams: It
 * reads bytes and decodes them into characters using a specified {@link
 * java.nio.charset.Charset charset}.  The charset that it uses
 * may be specified by name or may be given explicitly, or the platform's
 * default charset may be accepted.
 *
 * <p> This runtime keeps the public JDK 11 surface of the class.  The JDK
 * implementation delegates decoding to the internal {@code sun.nio.cs.StreamDecoder}
 * class, which is not ported; here the UTF-8 / UTF-16 / ISO-8859-1 / US-ASCII
 * decoders below read directly from the byte stream.  Simplifications, kept
 * explicit: malformed input produces U+FFFD instead of honouring the decoder's
 * {@code CodingErrorAction}, and {@code ready()} reports whether the underlying
 * stream has bytes buffered rather than consulting a decoder buffer.
 *
 * @author      Mark Reinhold
 * @since       1.1
 */

public class InputStreamReader extends Reader {

    private final InputStream in;
    private final Charset cs;

    /**
     * Creates an InputStreamReader that uses the default charset.
     *
     * @param  in   An InputStream
     */
    public InputStreamReader(InputStream in) {
        this(in, Charset.defaultCharset());
    }

    /**
     * Creates an InputStreamReader that uses the named charset.
     *
     * @param  in
     *         An InputStream
     *
     * @param  charsetName
     *         The name of a supported
     *         {@link java.nio.charset.Charset charset}
     *
     * @exception  UnsupportedEncodingException
     *             If the named encoding is not supported
     */
    public InputStreamReader(InputStream in, String charsetName)
        throws UnsupportedEncodingException
    {
        this(in, Charset.forName(charsetName));
    }

    /**
     * Creates an InputStreamReader that uses the given charset.
     *
     * @param  in       An InputStream
     * @param  cs       A charset
     *
     * @since 1.4
     */
    public InputStreamReader(InputStream in, Charset cs) {
        super(in);
        if (cs == null) {
            throw new NullPointerException("charset");
        }
        this.in = in;
        this.cs = cs;
    }

    /**
     * Creates an InputStreamReader that uses the given charset decoder.
     *
     * @param  in       An InputStream
     * @param  dec      A charset decoder
     *
     * @since 1.4
     */
    public InputStreamReader(InputStream in, CharsetDecoder dec) {
        super(in);
        if (dec == null) {
            throw new NullPointerException("charset decoder");
        }
        this.in = in;
        this.cs = dec.charset();
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
     * Reads a single character.
     *
     * @return The character read, or -1 if the end of the stream has been
     *         reached
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read() throws IOException {
        if (cs != null && !cs.equals(StandardCharsets.UTF_8)) {
            return readEncoded();
        }
        int first = in.read();
        if (first < 0) {
            return -1;
        }
        if (first < 0x80) {
            return first;
        }
        int extra;
        int value;
        if ((first & 0xE0) == 0xC0) {
            extra = 1;
            value = first & 0x1F;
        } else if ((first & 0xF0) == 0xE0) {
            extra = 2;
            value = first & 0x0F;
        } else if ((first & 0xF8) == 0xF0) {
            extra = 3;
            value = first & 0x07;
        } else {
            return 0xFFFD;
        }
        int i = 0;
        while (i < extra) {
            int next = in.read();
            if (next < 0) {
                return 0xFFFD;
            }
            value = (value << 6) | (next & 0x3F);
            i = i + 1;
        }
        return value;
    }

    private int readEncoded() throws IOException {
        String name = cs.name();
        if (name.equals("ISO-8859-1")) {
            int value = in.read();
            return value < 0 ? -1 : value;
        }
        if (name.equals("US-ASCII")) {
            int value = in.read();
            return value < 0 ? -1 : (value & 0x7F);
        }
        boolean bigEndian = !name.equals("UTF-16LE");
        if (name.equals("UTF-16")) {
            // The first code unit decides the byte order; there is no build
            // time platform default to consult, so big endian is assumed.
            bigEndian = true;
        }
        int first = in.read();
        if (first < 0) {
            return -1;
        }
        int second = in.read();
        if (second < 0) {
            return 0xFFFD;
        }
        return bigEndian ? ((first << 8) | second) : ((second << 8) | first);
    }

    /**
     * Reads characters into a portion of an array.
     *
     * @param  cbuf  Destination buffer
     * @param  offset   Offset at which to start storing characters
     * @param  length   Maximum number of characters to read
     *
     * @return The number of characters read, or -1 if the end of the
     *         stream has been reached
     *
     * @exception  IOException  If an I/O error occurs
     */
    public int read(char cbuf[], int offset, int length) throws IOException {
        synchronized (lock) {
            if (offset < 0 || length < 0 || offset + length > cbuf.length) {
                throw new IndexOutOfBoundsException("offset " + offset +
                    ", length " + length + ", array length " + cbuf.length);
            }
            if (length == 0) {
                return 0;
            }
            int count = 0;
            while (count < length) {
                int value = read();
                if (value < 0) {
                    break;
                }
                cbuf[offset + count] = (char) value;
                count = count + 1;
            }
            return count == 0 ? -1 : count;
        }
    }

    /**
     * Tells whether this stream is ready to be read.
     *
     * @return True if the next read() is guaranteed not to block for input
     *
     * @exception  IOException  If an I/O error occurs
     */
    public boolean ready() throws IOException {
        return in.available() > 0;
    }

    /**
     * Closes the stream and releases any system resources associated with it.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void close() throws IOException {
        in.close();
    }
}
