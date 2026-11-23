package java.io;

/**
 * Instances of classes that implement this interface are used to filter
 * filenames.  These instances are used to filter directory listings in the
 * {@code list} method of class {@code File}, and by the Abstract Window
 * Toolkit's FileDialog component.
 *
 * @see java.io.File#list(java.io.FilenameFilter)
 * @since 1.0
 */
public interface FilenameFilter
{
    /**
     * Tests if a specified file should be included in a file list.
     *
     * @param dir  the directory in which the file was found.
     * @param name the name of the file.
     * @return {@code true} if and only if the name should be included in the
     *         file list; {@code false} otherwise.
     */
    boolean accept(File dir, String name);
}
