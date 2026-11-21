package java.nio.file;

import java.io.File;
import java.net.URI;

public interface Path extends Comparable<Path>
{
    FileSystem getFileSystem();

    boolean isAbsolute();

    Path getFileName();

    Path getParent();

    int getNameCount();

    Path getName(int index);

    Path resolve(Path other);

    Path resolve(String other);

    Path toAbsolutePath();

    Path normalize();

    boolean startsWith(String other);

    boolean endsWith(String other);

    File toFile();

    URI toUri();

    @Override
    String toString();
}
