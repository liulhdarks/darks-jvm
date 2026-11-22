package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface GatheringByteChannel extends WritableByteChannel
{
    long write(ByteBuffer[] sources) throws IOException;

    long write(ByteBuffer[] sources, int offset, int length) throws IOException;
}
