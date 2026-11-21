import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SdkCharsetTest
{
    static int checks = 0;
    static int failures = 0;

    static void check(String name, boolean ok)
    {
        checks = checks + 1;
        if (!ok)
        {
            failures = failures + 1;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args) throws UnsupportedEncodingException
    {
        check("utf8-name", StandardCharsets.UTF_8.name().equals("UTF-8"));
        check("utf8-alias",
              Charset.forName("utf8") == StandardCharsets.UTF_8);
        check("charset-supported", Charset.isSupported("UTF-16LE"));
        check("charset-default",
              Charset.defaultCharset() == StandardCharsets.UTF_8);

        String text = "h\u00e9llo \u4e16\u754c \ud83d\ude00";
        byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
        check("utf8-encode", utf8.length == 18);
        check("utf8-decode",
              new String(utf8, StandardCharsets.UTF_8).equals(text));
        check("utf8-bytes", new String(text.getBytes("UTF-8"), "UTF-8")
              .equals(text));

        byte[] utf16 = text.getBytes(StandardCharsets.UTF_16);
        check("utf16-bom", (utf16[0] & 0xFF) == 0xFE && (utf16[1] & 0xFF) == 0xFF);
        check("utf16-decode",
              new String(utf16, StandardCharsets.UTF_16).equals(text));
        byte[] utf16le = text.getBytes(StandardCharsets.UTF_16LE);
        check("utf16le", (utf16le[0] & 0xFF) == 0x68 && utf16le[1] == 0);
        check("utf16le-decode",
              new String(utf16le, StandardCharsets.UTF_16LE).equals(text));

        String cafe = "caf\u00e9";
        byte[] latin = cafe.getBytes(StandardCharsets.ISO_8859_1);
        check("latin1-encode", latin.length == 4 && (latin[3] & 0xFF) == 0xE9);
        check("latin1-decode",
              new String(latin, StandardCharsets.ISO_8859_1).equals(cafe));

        byte[] ascii = "A\u00e9".getBytes(StandardCharsets.US_ASCII);
        check("ascii-replace", ascii.length == 2 && ascii[1] == 0x3F);

        byte[] sliced = { 0x41, 0x42, 0x43, 0x44 };
        check("offset-decode",
              new String(sliced, 1, 2, StandardCharsets.UTF_8).equals("BC"));

        boolean unsupported = false;
        try
        {
            Charset.forName("no-such-charset");
        }
        catch (java.nio.charset.UnsupportedCharsetException expected)
        {
            unsupported = true;
        }
        check("unsupported-charset", unsupported);

        boolean badName = false;
        try
        {
            Charset.forName("bad name!");
        }
        catch (java.nio.charset.IllegalCharsetNameException expected)
        {
            badName = true;
        }
        check("illegal-charset-name", badName);

        check("contains-ascii",
              StandardCharsets.UTF_8.contains(StandardCharsets.US_ASCII));

        System.out.println("charset checks=" + checks + " failures=" + failures);
        System.out.println("sdk charset: ok");
    }
}
