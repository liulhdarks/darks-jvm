package java.nio.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.FileAttribute;

public final class Files
{
    private Files()
    {
    }

    public static boolean exists(Path path, LinkOption... options)
    {
        return path.toFile().exists();
    }

    public static boolean notExists(Path path, LinkOption... options)
    {
        return !exists(path, options);
    }

    public static boolean isDirectory(Path path, LinkOption... options)
    {
        return path.toFile().isDirectory();
    }

    public static boolean isRegularFile(Path path, LinkOption... options)
    {
        return path.toFile().isFile();
    }

    public static long size(Path path) throws IOException
    {
        File file = path.toFile();
        if (!file.exists())
        {
            throw new IOException("No such file: " + path);
        }
        return file.length();
    }

    public static Path createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException
    {
        if (!dir.toFile().mkdir() && !dir.toFile().isDirectory())
        {
            throw new IOException("Failed to create directory: " + dir);
        }
        return dir;
    }

    public static Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException
    {
        File file = dir.toFile();
        if (!file.isDirectory() && !file.mkdirs() && !file.isDirectory())
        {
            throw new IOException("Failed to create directory: " + dir);
        }
        return dir;
    }

    public static Path createFile(Path path, FileAttribute<?>... attrs) throws IOException
    {
        if (path.toFile().exists())
        {
            throw new IOException("File already exists: " + path);
        }
        OutputStream stream = new FileOutputStream(path.toFile());
        stream.close();
        return path;
    }

    public static OutputStream newOutputStream(Path path, OpenOption... options) throws IOException
    {
        boolean append = false;
        boolean createNew = false;
        for (int i = 0; options != null && i < options.length; i++)
        {
            if (options[i] == StandardOpenOption.APPEND)
            {
                append = true;
            }
            else if (options[i] == StandardOpenOption.CREATE_NEW)
            {
                createNew = true;
            }
        }
        if (createNew && path.toFile().exists())
        {
            throw new IOException("File already exists: " + path);
        }
        return new FileOutputStream(path.toFile(), append);
    }

    public static InputStream newInputStream(Path path, OpenOption... options) throws IOException
    {
        return new FileInputStream(path.toFile());
    }

    public static void delete(Path path) throws IOException
    {
        if (!path.toFile().delete())
        {
            throw new IOException("Failed to delete: " + path);
        }
    }

    public static boolean deleteIfExists(Path path) throws IOException
    {
        return path.toFile().delete();
    }

    public static Path createTempFile(String prefix, String suffix) throws IOException
    {
        return createTempFile(null, prefix, suffix);
    }

    public static Path createTempFile(Path dir, String prefix, String suffix) throws IOException
    {
        File directory = dir == null ? new File(System.getProperty("java.io.tmpdir", ".")) : dir.toFile();
        String name = prefix + System.nanoTime() + (suffix == null ? ".tmp" : suffix);
        File file = new File(directory, name);
        if (!file.createNewFile())
        {
            throw new IOException("Failed to create temp file: " + file);
        }
        return file.toPath();
    }

    public static Path createTempDirectory(String prefix) throws IOException
    {
        return createTempDirectory(null, prefix);
    }

    public static Path createTempDirectory(Path dir, String prefix) throws IOException
    {
        File directory = dir == null ? new File(System.getProperty("java.io.tmpdir", ".")) : dir.toFile();
        File file = new File(directory, prefix + System.nanoTime());
        if (!file.mkdir())
        {
            throw new IOException("Failed to create temp directory: " + file);
        }
        return file.toPath();
    }
}
