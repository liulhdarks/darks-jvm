/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * {@code PropertyResourceBundle} is a concrete subclass of
 * {@code ResourceBundle} that manages resources for a locale using a set of
 * static strings from a property file.
 *
 * <p> The JDK 11 implementation reads an {@code InputStream} as UTF-8 and
 * falls back to ISO-8859-1 when the property value
 * {@code java.util.PropertyResourceBundle.encoding} is unset; that fallback
 * decoder ({@code PropertyResourceBundleCharset}) is not ported, so this
 * version reads the stream as ISO-8859-1, the historical behaviour.  The
 * {@code Reader} constructor is identical to the JDK one.
 *
 * @see ResourceBundle
 * @see Properties
 * @since 1.1
 */
public class PropertyResourceBundle extends ResourceBundle {

    private final HashMap<String, Object> lookup;

    /**
     * Creates a property resource bundle from an {@link java.io.InputStream
     * InputStream}.
     *
     * @param stream an InputStream that represents a property file to read from.
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if {@code stream} is null
     * @throws IllegalArgumentException if {@code stream} contains a malformed
     *         Unicode escape sequence.
     */
    public PropertyResourceBundle(InputStream stream) throws IOException {
        this(new InputStreamReader(stream, "ISO-8859-1"));
    }

    /**
     * Creates a property resource bundle from a {@link java.io.Reader Reader}.
     *
     * @param reader a Reader that represents a property file to read from.
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if {@code reader} is null
     * @throws IllegalArgumentException if a malformed Unicode escape sequence
     *         appears from {@code reader}.
     * @since 1.6
     */
    public PropertyResourceBundle(Reader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        lookup = new HashMap<String, Object>();
        Iterator<Object> keys = properties.keySet().iterator();
        while (keys.hasNext()) {
            Object key = keys.next();
            lookup.put(String.valueOf(key), properties.get(key));
        }
    }

    /**
     * Implements {@code ResourceBundle.getObject}; inherits the javadoc
     * specification.
     */
    public Object getObject(String key) {
        Object value = handleGetObject(key);
        if (value == null) {
            ResourceBundle parent = getParent();
            if (parent != null) {
                value = parent.getObject(key);
            }
        }
        if (value == null) {
            throw new MissingResourceException("key not found: " + key,
                getClass().getName(), key);
        }
        return value;
    }

    /**
     * Returns an <code>Enumeration</code> of the keys contained in
     * this <code>ResourceBundle</code> and its parent bundles.
     *
     * @return an <code>Enumeration</code> of the keys contained in
     *         this <code>ResourceBundle</code> and its parent bundles.
     * @see #keySet()
     */
    public Enumeration<String> getKeys() {
        ArrayList<String> keys = new ArrayList<String>(lookup.keySet());
        ResourceBundle parent = getParent();
        if (parent != null) {
            Enumeration<String> inherited = parent.getKeys();
            while (inherited.hasMoreElements()) {
                String key = inherited.nextElement();
                if (!lookup.containsKey(key)) {
                    keys.add(key);
                }
            }
        }
        return Collections.enumeration(keys);
    }

    /**
     * Returns a <code>Set</code> of the keys contained <em>only</em> in
     * this <code>ResourceBundle</code>.
     *
     * @since 1.6
     */
    protected Set<String> handleKeySet() {
        return lookup.keySet();
    }

    /**
     * Returns the object for the given key from this bundle only.
     */
    protected Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return lookup.get(key);
    }
}
