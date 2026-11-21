package java.util.jar;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class JarFile extends java.util.zip.ZipFile
{
    public static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    private Manifest manifest;
    private boolean manifestRead;

    public JarFile(String name) throws IOException
    {
        super(name);
    }

    public JarFile(String name, boolean verify) throws IOException
    {
        super(name);
    }

    public JarFile(File file) throws IOException
    {
        super(file);
    }

    public JarFile(File file, boolean verify) throws IOException
    {
        super(file);
    }

    public Manifest getManifest() throws IOException
    {
        if (!manifestRead)
        {
            manifestRead = true;
            java.util.zip.ZipEntry entry = super.getEntry(MANIFEST_NAME);
            if (entry != null)
            {
                InputStream input = super.getInputStream(entry);
                try
                {
                    manifest = new Manifest(input);
                }
                finally
                {
                    input.close();
                }
            }
        }
        return manifest;
    }

    public JarEntry getJarEntry(String name)
    {
        return getEntry(name);
    }

    public JarEntry getEntry(String name)
    {
        java.util.zip.ZipEntry entry = super.getEntry(name);
        return entry == null ? null : new JarEntry(entry);
    }

    public Enumeration<JarEntry> entries()
    {
        final Enumeration<? extends java.util.zip.ZipEntry> source = super.entries();
        return new Enumeration<JarEntry>()
        {
            public boolean hasMoreElements()
            {
                return source.hasMoreElements();
            }

            public JarEntry nextElement()
            {
                if (!source.hasMoreElements())
                {
                    throw new NoSuchElementException();
                }
                return new JarEntry(source.nextElement());
            }
        };
    }
}
