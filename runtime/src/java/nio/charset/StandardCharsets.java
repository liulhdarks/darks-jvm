package java.nio.charset;

public final class StandardCharsets
{
    public static final Charset UTF_8 = new Charset("UTF-8",
        new String[]{"UTF8", "unicode-1-1-utf-8"}, Charset.UTF8);

    public static final Charset UTF_16 = new Charset("UTF-16",
        new String[]{"UTF16", "unicode"}, Charset.UTF16);

    public static final Charset UTF_16BE = new Charset("UTF-16BE",
        new String[]{"UTF16BE", "unicodebigunmarked"}, Charset.UTF16BE);

    public static final Charset UTF_16LE = new Charset("UTF-16LE",
        new String[]{"UTF16LE", "unicodelittleunmarked"}, Charset.UTF16LE);

    public static final Charset US_ASCII = new Charset("US-ASCII",
        new String[]{"ASCII", "ISO646-US", "us"}, Charset.ASCII);

    public static final Charset ISO_8859_1 = new Charset("ISO-8859-1",
        new String[]{"ISO8859-1", "latin1", "l1", "ISO_8859-1"}, Charset.LATIN1);

    private StandardCharsets()
    {
    }
}
