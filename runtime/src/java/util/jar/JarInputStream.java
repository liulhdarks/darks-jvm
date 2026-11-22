package java.util.jar;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarInputStream extends ZipInputStream
{
    public static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    private Manifest manifest;

    public JarInputStream(InputStream input) throws IOException
    {
        this(input, true);
    }

    public JarInputStream(InputStream input, boolean verify) throws IOException
    {
        super(input);
        ZipEntry first = super.getNextEntry();
        if (first != null && "META-INF/".equalsIgnoreCase(first.getName()))
        {
            first = super.getNextEntry();
        }
        if (first != null && MANIFEST_NAME.equalsIgnoreCase(first.getName()))
        {
            manifest = new Manifest(this);
        }
    }

    public Manifest getManifest()
    {
        return manifest;
    }

    public JarEntry getNextJarEntry() throws IOException
    {
        ZipEntry next = getNextEntry();
        return next == null ? null : new JarEntry(next);
    }
}
