package java.nio.charset;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

public class CharsetEncoder
{
    private final Charset charset;
    private CodingErrorAction malformedInputAction = CodingErrorAction.REPORT;
    private CodingErrorAction unmappableCharacterAction = CodingErrorAction.REPORT;
    private String replacement = "?";
    private byte[] pendingBytes = new byte[0];
    private int pendingPosition;

    protected CharsetEncoder(Charset cs)
    {
        this.charset = cs;
    }

    public final Charset charset()
    {
        return charset;
    }

    public final float averageBytesPerChar()
    {
        return 1.0f;
    }

    public final float maxBytesPerChar()
    {
        return 1.0f;
    }

    public final CodingErrorAction malformedInputAction()
    {
        return malformedInputAction;
    }

    public final CharsetEncoder onMalformedInput(CodingErrorAction action)
    {
        if (action == null)
        {
            throw new IllegalArgumentException();
        }
        malformedInputAction = action;
        return this;
    }

    public final CodingErrorAction unmappableCharacterAction()
    {
        return unmappableCharacterAction;
    }

    public final CharsetEncoder onUnmappableCharacter(CodingErrorAction action)
    {
        if (action == null)
        {
            throw new IllegalArgumentException();
        }
        unmappableCharacterAction = action;
        return this;
    }

    public final String replacement()
    {
        return replacement;
    }

    public final CharsetEncoder replaceWith(byte[] newReplacement)
    {
        if (newReplacement == null || newReplacement.length == 0)
        {
            throw new IllegalArgumentException();
        }
        replacement = new String(newReplacement);
        return this;
    }

    public final CharsetEncoder reset()
    {
        pendingBytes = new byte[0];
        pendingPosition = 0;
        malformedInputAction = CodingErrorAction.REPORT;
        unmappableCharacterAction = CodingErrorAction.REPORT;
        return this;
    }

    public final ByteBuffer encode(CharBuffer in) throws CharacterCodingException
    {
        int room = in.remaining() * 4 + 1;
        ByteBuffer out = ByteBuffer.allocate(room);
        CoderResult result = encode(in, out, true);
        if (result.isError())
        {
            result.throwException();
        }
        out.flip();
        return out;
    }

    public CoderResult encode(CharBuffer in, ByteBuffer out, boolean endOfInput)
    {
        if (pendingPosition < pendingBytes.length)
        {
            int room = out.remaining();
            int count = pendingBytes.length - pendingPosition;
            if (count > room)
            {
                count = room;
            }
            out.put(pendingBytes, pendingPosition, count);
            pendingPosition = pendingPosition + count;
            if (pendingPosition < pendingBytes.length)
            {
                return CoderResult.OVERFLOW;
            }
            pendingBytes = new byte[0];
            pendingPosition = 0;
        }
        if (!in.hasRemaining())
        {
            return CoderResult.UNDERFLOW;
        }
        int count = in.remaining();
        char[] chars = new char[count];
        in.get(chars);
        byte[] bytes = charset.encode(new String(chars));
        int room = out.remaining();
        if (bytes.length <= room)
        {
            out.put(bytes);
            return CoderResult.UNDERFLOW;
        }
        out.put(bytes, 0, room);
        pendingBytes = bytes;
        pendingPosition = room;
        return CoderResult.OVERFLOW;
    }

    public final CoderResult flush(ByteBuffer out)
    {
        return CoderResult.UNDERFLOW;
    }

    public boolean canEncode(char value)
    {
        return true;
    }

    public boolean canEncode(CharSequence sequence)
    {
        return true;
    }

    public boolean isLegalReplacement(byte[] replacement)
    {
        return true;
    }
}
