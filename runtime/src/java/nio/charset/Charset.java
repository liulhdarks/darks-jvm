package java.nio.charset;

public class Charset implements Comparable<Charset>
{
    static final int UTF8 = 0;
    static final int UTF16 = 1;
    static final int UTF16BE = 2;
    static final int UTF16LE = 3;
    static final int ASCII = 4;
    static final int LATIN1 = 5;

    private final String name;
    private final String[] aliases;
    private final int kind;

    protected Charset(String name, String[] aliases, int kind)
    {
        this.name = name;
        this.aliases = aliases;
        this.kind = kind;
    }

    public final String name()
    {
        return name;
    }

    public final java.util.Set<String> aliases()
    {
        java.util.HashSet<String> set = new java.util.HashSet<String>();
        int i = 0;
        while (i < aliases.length)
        {
            set.add(aliases[i]);
            i = i + 1;
        }
        return set;
    }

    public String displayName()
    {
        return name;
    }

    public final boolean isRegistered()
    {
        return true;
    }

    public boolean canEncode()
    {
        return true;
    }

    public boolean contains(Charset other)
    {
        if (other == null)
        {
            return false;
        }
        if (kind == UTF8)
        {
            return other.kind != UTF16 && other.kind != UTF16BE && other.kind != UTF16LE;
        }
        if (kind == UTF16 || kind == UTF16BE || kind == UTF16LE)
        {
            return other.kind == UTF16 || other.kind == UTF16BE || other.kind == UTF16LE;
        }
        if (kind == LATIN1)
        {
            return other.kind == LATIN1 || other.kind == ASCII;
        }
        return other.kind == ASCII;
    }

    public byte[] encode(String text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        return encode(text, 0, text.length());
    }

    public byte[] encode(String text, int offset, int length)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        if (offset < 0 || length < 0 || offset + length > text.length())
        {
            throw new IndexOutOfBoundsException("offset");
        }
        if (kind == UTF8)
        {
            return encodeUtf8(text, offset, length, false);
        }
        if (kind == UTF16)
        {
            return encodeUtf16(text, offset, length, true, true);
        }
        if (kind == UTF16BE)
        {
            return encodeUtf16(text, offset, length, true, false);
        }
        if (kind == UTF16LE)
        {
            return encodeUtf16(text, offset, length, false, false);
        }
        if (kind == ASCII)
        {
            return encodeSingleByte(text, offset, length, 0x7F);
        }
        return encodeSingleByte(text, offset, length, 0xFF);
    }

    public String decode(byte[] bytes)
    {
        if (bytes == null)
        {
            throw new NullPointerException("bytes");
        }
        return decode(bytes, 0, bytes.length);
    }

    public String decode(byte[] bytes, int offset, int length)
    {
        if (bytes == null)
        {
            throw new NullPointerException("bytes");
        }
        if (offset < 0 || length < 0 || offset + length > bytes.length)
        {
            throw new IndexOutOfBoundsException("offset");
        }
        if (kind == UTF8)
        {
            return decodeUtf8(bytes, offset, length);
        }
        if (kind == UTF16)
        {
            int start = offset;
            boolean bigEndian = true;
            if (length >= 2)
            {
                int first = bytes[offset] & 0xFF;
                int second = bytes[offset + 1] & 0xFF;
                if (first == 0xFE && second == 0xFF)
                {
                    start = offset + 2;
                    bigEndian = true;
                }
                else if (first == 0xFF && second == 0xFE)
                {
                    start = offset + 2;
                    bigEndian = false;
                }
            }
            return decodeUtf16(bytes, start, offset + length - start, bigEndian);
        }
        if (kind == UTF16BE)
        {
            return decodeUtf16(bytes, offset, length, true);
        }
        if (kind == UTF16LE)
        {
            return decodeUtf16(bytes, offset, length, false);
        }
        if (kind == ASCII)
        {
            return decodeSingleByte(bytes, offset, length, true);
        }
        return decodeSingleByte(bytes, offset, length, false);
    }

    public int compareTo(Charset other)
    {
        if (other == null)
        {
            throw new NullPointerException("other");
        }
        return name.compareTo(other.name);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Charset))
        {
            return false;
        }
        return name.equals(((Charset)other).name);
    }

    public int hashCode()
    {
        return name.hashCode();
    }

    public String toString()
    {
        return name;
    }

    public static Charset defaultCharset()
    {
        return StandardCharsets.UTF_8;
    }

    public static Charset forName(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        if (!isValidName(name))
        {
            throw new IllegalCharsetNameException(name);
        }
        Charset found = lookup(name);
        if (found == null)
        {
            throw new UnsupportedCharsetException(name);
        }
        return found;
    }

    public static boolean isSupported(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        if (!isValidName(name))
        {
            return false;
        }
        return lookup(name) != null;
    }

    private static boolean isValidName(String name)
    {
        if (name.length() == 0)
        {
            return false;
        }
        int i = 0;
        while (i < name.length())
        {
            char c = name.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                  (c >= '0' && c <= '9') || c == '-' || c == '+' ||
                  c == ':' || c == '_' || c == '.'))
            {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    private static Charset lookup(String name)
    {
        Charset[] all = {
            StandardCharsets.UTF_8,
            StandardCharsets.UTF_16,
            StandardCharsets.UTF_16BE,
            StandardCharsets.UTF_16LE,
            StandardCharsets.US_ASCII,
            StandardCharsets.ISO_8859_1
        };
        int i = 0;
        while (i < all.length)
        {
            Charset charset = all[i];
            if (charset.name.equalsIgnoreCase(name))
            {
                return charset;
            }
            int j = 0;
            while (j < charset.aliases.length)
            {
                if (charset.aliases[j].equalsIgnoreCase(name))
                {
                    return charset;
                }
                j = j + 1;
            }
            i = i + 1;
        }
        return null;
    }

    private static byte[] encodeUtf8(String text, int offset, int length,
                                     boolean unused)
    {
        byte[] out = new byte[length + 1];
        int count = 0;
        int i = offset;
        int end = offset + length;
        while (i < end)
        {
            int cp = text.charAt(i);
            i = i + 1;
            if (cp >= 0xD800 && cp <= 0xDBFF && i < end &&
                text.charAt(i) >= 0xDC00 && text.charAt(i) <= 0xDFFF)
            {
                cp = 0x10000 + ((cp - 0xD800) << 10) + (text.charAt(i) - 0xDC00);
                i = i + 1;
            }
            else if (cp >= 0xD800 && cp <= 0xDFFF)
            {
                cp = 0xFFFD;
            }
            if (count + 4 > out.length)
            {
                out = grow(out, count + 4);
            }
            if (cp < 0x80)
            {
                out[count++] = (byte)cp;
            }
            else if (cp < 0x800)
            {
                out[count++] = (byte)(0xC0 | (cp >> 6));
                out[count++] = (byte)(0x80 | (cp & 0x3F));
            }
            else if (cp < 0x10000)
            {
                out[count++] = (byte)(0xE0 | (cp >> 12));
                out[count++] = (byte)(0x80 | ((cp >> 6) & 0x3F));
                out[count++] = (byte)(0x80 | (cp & 0x3F));
            }
            else
            {
                out[count++] = (byte)(0xF0 | (cp >> 18));
                out[count++] = (byte)(0x80 | ((cp >> 12) & 0x3F));
                out[count++] = (byte)(0x80 | ((cp >> 6) & 0x3F));
                out[count++] = (byte)(0x80 | (cp & 0x3F));
            }
        }
        return trim(out, count);
    }

    private static byte[] encodeUtf16(String text, int offset, int length,
                                      boolean bigEndian, boolean bom)
    {
        int units = length + (bom ? 1 : 0);
        byte[] out = new byte[units * 2];
        int count = 0;
        if (bom)
        {
            out[count++] = (byte)0xFE;
            out[count++] = (byte)0xFF;
        }
        int i = offset;
        int end = offset + length;
        while (i < end)
        {
            int unit = text.charAt(i);
            if (bigEndian)
            {
                out[count++] = (byte)(unit >> 8);
                out[count++] = (byte)unit;
            }
            else
            {
                out[count++] = (byte)unit;
                out[count++] = (byte)(unit >> 8);
            }
            i = i + 1;
        }
        return out;
    }

    private static byte[] encodeSingleByte(String text, int offset, int length,
                                           int max)
    {
        byte[] out = new byte[length];
        int i = 0;
        while (i < length)
        {
            int c = text.charAt(offset + i);
            out[i] = (byte)(c <= max ? c : '?');
            i = i + 1;
        }
        return out;
    }

    private static String decodeUtf8(byte[] bytes, int offset, int length)
    {
        char[] out = new char[length > 0 ? length : 1];
        int count = 0;
        int i = offset;
        int end = offset + length;
        while (i < end)
        {
            int b = bytes[i] & 0xFF;
            i = i + 1;
            int cp;
            int extra;
            if (b < 0x80)
            {
                out[count++] = (char)b;
                continue;
            }
            else if ((b & 0xE0) == 0xC0)
            {
                cp = b & 0x1F;
                extra = 1;
            }
            else if ((b & 0xF0) == 0xE0)
            {
                cp = b & 0x0F;
                extra = 2;
            }
            else if ((b & 0xF8) == 0xF0)
            {
                cp = b & 0x07;
                extra = 3;
            }
            else
            {
                out[count++] = '\uFFFD';
                continue;
            }
            boolean valid = true;
            int k = 0;
            while (k < extra)
            {
                if (i >= end || ((bytes[i] & 0xC0) != 0x80))
                {
                    valid = false;
                    break;
                }
                cp = (cp << 6) | (bytes[i] & 0x3F);
                i = i + 1;
                k = k + 1;
            }
            if (!valid)
            {
                out[count++] = '\uFFFD';
                continue;
            }
            if (cp >= 0x10000)
            {
                cp = cp - 0x10000;
                out[count++] = (char)(0xD800 + (cp >> 10));
                out[count++] = (char)(0xDC00 + (cp & 0x3FF));
            }
            else
            {
                out[count++] = (char)cp;
            }
        }
        return new String(trim(out, count), 0, count);
    }

    private static String decodeUtf16(byte[] bytes, int offset, int length,
                                      boolean bigEndian)
    {
        char[] out = new char[length / 2 + 1];
        int count = 0;
        int i = offset;
        int end = offset + length;
        while (i + 1 < end)
        {
            int hi = bytes[i] & 0xFF;
            int lo = bytes[i + 1] & 0xFF;
            out[count++] = (char)(bigEndian ? ((hi << 8) | lo) : ((lo << 8) | hi));
            i = i + 2;
        }
        return new String(trim(out, count), 0, count);
    }

    private static String decodeSingleByte(byte[] bytes, int offset, int length,
                                           boolean ascii)
    {
        char[] out = new char[length];
        int i = 0;
        while (i < length)
        {
            int b = bytes[offset + i] & 0xFF;
            if (ascii && b > 0x7F)
            {
                out[i] = '\uFFFD';
            }
            else
            {
                out[i] = (char)b;
            }
            i = i + 1;
        }
        return new String(out, 0, length);
    }

    private static byte[] grow(byte[] old, int minimum)
    {
        int size = old.length * 2;
        if (size < minimum)
        {
            size = minimum;
        }
        byte[] bigger = new byte[size];
        int i = 0;
        while (i < old.length)
        {
            bigger[i] = old[i];
            i = i + 1;
        }
        return bigger;
    }

    private static byte[] trim(byte[] old, int count)
    {
        if (count == old.length)
        {
            return old;
        }
        byte[] result = new byte[count];
        int i = 0;
        while (i < count)
        {
            result[i] = old[i];
            i = i + 1;
        }
        return result;
    }

    private static char[] trim(char[] old, int count)
    {
        if (count == old.length)
        {
            return old;
        }
        char[] result = new char[count];
        int i = 0;
        while (i < count)
        {
            result[i] = old[i];
            i = i + 1;
        }
        return result;
    }
}
