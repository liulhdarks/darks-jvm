package java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class CharsetDecoder
{
    private final Charset charset;
    private final float averageCharsPerByte;
    private final float maxCharsPerByte;
    private CodingErrorAction malformedInputAction = CodingErrorAction.REPORT;
    private CodingErrorAction unmappableCharacterAction = CodingErrorAction.REPORT;
    private String replacement = "?";
    private char[] pendingChars = new char[0];
    private int pendingPosition;

    protected CharsetDecoder(Charset cs)
    {
        this(cs, 1.0f, 1.0f);
    }

    protected CharsetDecoder(Charset cs, float averageCharsPerByte,
                             float maxCharsPerByte)
    {
        this.charset = cs;
        this.averageCharsPerByte = averageCharsPerByte;
        this.maxCharsPerByte = maxCharsPerByte;
    }

    public final Charset charset()
    {
        return charset;
    }

    public final float averageCharsPerByte()
    {
        return averageCharsPerByte;
    }

    public final float maxCharsPerByte()
    {
        return maxCharsPerByte;
    }

    public final CodingErrorAction malformedInputAction()
    {
        return malformedInputAction;
    }

    public final CharsetDecoder onMalformedInput(CodingErrorAction action)
    {
        if (action == null)
        {
            throw new IllegalArgumentException();
        }
        malformedInputAction = action;
        implOnMalformedInput(action);
        return this;
    }

    public final CodingErrorAction unmappableCharacterAction()
    {
        return unmappableCharacterAction;
    }

    public final CharsetDecoder onUnmappableCharacter(CodingErrorAction action)
    {
        if (action == null)
        {
            throw new IllegalArgumentException();
        }
        unmappableCharacterAction = action;
        implOnUnmappableCharacter(action);
        return this;
    }

    public final String replacement()
    {
        return replacement;
    }

    public final CharsetDecoder replaceWith(String newReplacement)
    {
        if (newReplacement == null || newReplacement.length() == 0)
        {
            throw new IllegalArgumentException();
        }
        replacement = newReplacement;
        implReplaceWith(newReplacement);
        return this;
    }

    public final CharsetDecoder reset()
    {
        pendingChars = new char[0];
        pendingPosition = 0;
        malformedInputAction = CodingErrorAction.REPORT;
        unmappableCharacterAction = CodingErrorAction.REPORT;
        implReset();
        return this;
    }

    public final CharBuffer decode(ByteBuffer in) throws CharacterCodingException
    {
        int room = in.remaining() + 1;
        CharBuffer out = CharBuffer.allocate(room);
        CoderResult result = decode(in, out, true);
        if (result.isError())
        {
            result.throwException();
        }
        out.flip();
        return out;
    }

    public CoderResult decode(ByteBuffer in, CharBuffer out, boolean endOfInput)
    {
        if (pendingPosition < pendingChars.length)
        {
            int room = out.remaining();
            int count = pendingChars.length - pendingPosition;
            if (count > room)
            {
                count = room;
            }
            out.put(pendingChars, pendingPosition, count);
            pendingPosition = pendingPosition + count;
            if (pendingPosition < pendingChars.length)
            {
                return CoderResult.OVERFLOW;
            }
            pendingChars = new char[0];
            pendingPosition = 0;
        }
        if (!in.hasRemaining())
        {
            return CoderResult.UNDERFLOW;
        }
        int count = in.remaining();
        byte[] bytes = new byte[count];
        in.get(bytes);
        String text = charset.decode(bytes, 0, count);
        char[] chars = text.toCharArray();
        int room = out.remaining();
        if (chars.length <= room)
        {
            out.put(chars);
            return CoderResult.UNDERFLOW;
        }
        out.put(chars, 0, room);
        pendingChars = chars;
        pendingPosition = room;
        return CoderResult.OVERFLOW;
    }

    public final CoderResult flush(CharBuffer out)
    {
        return CoderResult.UNDERFLOW;
    }

    public boolean isAutoDetecting()
    {
        return false;
    }

    public boolean isCharsetDetected()
    {
        return true;
    }

    public Charset detectedCharset()
    {
        return charset;
    }

    protected void implReplaceWith(String newReplacement)
    {
    }

    protected void implOnMalformedInput(CodingErrorAction newAction)
    {
    }

    protected void implOnUnmappableCharacter(CodingErrorAction newAction)
    {
    }

    protected void implReset()
    {
    }
}
