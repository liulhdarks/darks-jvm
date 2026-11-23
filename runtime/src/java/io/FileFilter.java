package java.io;

/**
 * A filter for abstract pathnames.
 *
 * <p> Instances of this interface may be passed to the
 * {@link File#listFiles(FileFilter)} method of the {@link File} class.
 *
 * @since 1.2
 */
public interface FileFilter
{
    /**
     * Tests whether or not the specified abstract pathname should be included
     * in a pathname list.
     *
     * @param pathname the abstract pathname to be tested
     * @return {@code true} if and only if {@code pathname} should be included
     */
    boolean accept(File pathname);
}
