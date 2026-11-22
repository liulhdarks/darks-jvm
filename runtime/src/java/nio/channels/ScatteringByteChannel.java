package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface ScatteringByteChannel extends ReadableByteChannel
{
    long read(ByteBuffer[] destinations) throws IOException;

    long read(ByteBuffer[] destinations, int offset, int length) throws IOException;
}
