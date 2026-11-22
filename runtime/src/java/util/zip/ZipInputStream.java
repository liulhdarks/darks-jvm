package java.util.zip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

// Sequential reader for ZIP/jar archives.  It parses the local file headers
// of an archive that is already available from the underlying stream and
// decompresses STORED/DEFLATED entries with java.util.zip.Inflater.
public class ZipInputStream extends InputStream
{
    private static final int LOCAL_HEADER = 0x04034b50;
    private static final int CENTRAL_HEADER = 0x02014b50;
    private static final int END_HEADER = 0x06054b50;
    private static final int DATA_DESCRIPTOR = 0x08074b50;

    private final InputStream input;
    private byte[] archive;
    private int position;
    private ZipEntry entry;
    private byte[] entryData;
    private int entryPosition;

    public ZipInputStream(InputStream input)
    {
        if (input == null)
        {
            throw new NullPointerException("input");
        }
        this.input = input;
    }

    private void loadArchive() throws IOException
    {
        if (archive != null)
        {
            return;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count = input.read(buffer, 0, buffer.length);
        while (count > 0)
        {
            bytes.write(buffer, 0, count);
            count = input.read(buffer, 0, buffer.length);
        }
        archive = bytes.toByteArray();
    }

    public ZipEntry getNextEntry() throws IOException
    {
        closeEntry();
        loadArchive();
        if (position >= archive.length - 4)
        {
            return null;
        }
        int signature = readInt();
        if (signature == CENTRAL_HEADER || signature == END_HEADER)
        {
            position = archive.length;
            return null;
        }
        if (signature != LOCAL_HEADER)
        {
            throw new ZipException("invalid local header signature");
        }
        skip(2);
        int flags = readShort();
        int method = readShort();
        skip(8);
        long compressedSize = readIntUnsigned();
        long uncompressedSize = readIntUnsigned();
        int nameLength = readShort();
        int extraLength = readShort();
        String name = new String(archive, position, nameLength, "UTF-8");
        position += nameLength;
        byte[] extra = null;
        if (extraLength > 0)
        {
            extra = new byte[extraLength];
            System.arraycopy(archive, position, extra, 0, extraLength);
            position += extraLength;
        }
        ZipEntry created = new ZipEntry(name);
        created.setMethod(method);
        created.setCompressedSize(compressedSize);
        if (uncompressedSize >= 0)
        {
            created.setSize(uncompressedSize);
        }
        if (extra != null)
        {
            created.setExtra(extra);
        }
        boolean descriptor = (flags & 0x0008) != 0;
        byte[] data;
        if (method == ZipEntry.STORED)
        {
            if (compressedSize < 0 || compressedSize > archive.length - position)
            {
                throw new ZipException("truncated stored entry");
            }
            data = new byte[(int) compressedSize];
            System.arraycopy(archive, position, data, 0, data.length);
            position += data.length;
            if (descriptor)
            {
                skipDescriptor();
            }
        }
        else if (method == ZipEntry.DEFLATED)
        {
            data = inflate(descriptor ? -1 : (int) compressedSize);
        }
        else
        {
            throw new ZipException("unsupported compression method");
        }
        entry = created;
        entryData = data;
        entryPosition = 0;
        return entry;
    }

    private byte[] inflate(int compressedSize) throws IOException
    {
        int available = archive.length - position;
        int length = compressedSize >= 0 && compressedSize <= available ? compressedSize : available;
        Inflater inflater = new Inflater(true);
        byte[] out = new byte[8192];
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int consumed = 0;
        try
        {
            if (compressedSize >= 0 && compressedSize != length)
            {
                throw new ZipException("truncated deflated entry");
            }
            while (compressedSize < 0 || consumed < compressedSize)
            {
                int feed = compressedSize < 0 ? available - consumed : compressedSize - consumed;
                inflater.setInput(archive, position + consumed, feed);
                int produced = inflater.inflate(out, 0, out.length);
                if (produced > 0)
                {
                    bytes.write(out, 0, produced);
                }
                consumed += feed - inflater.getRemaining();
                if (inflater.finished())
                {
                    break;
                }
                if (produced == 0 && inflater.needsInput())
                {
                    throw new ZipException("truncated deflated entry");
                }
            }
        }
        catch (DataFormatException e)
        {
            throw new ZipException("invalid deflated entry: " + e.getMessage());
        }
        finally
        {
            inflater.end();
        }
        position += consumed;
        if (compressedSize < 0)
        {
            skipDescriptor();
        }
        return bytes.toByteArray();
    }

    private void skipDescriptor() throws IOException
    {
        if (position + 4 <= archive.length && peekInt() == DATA_DESCRIPTOR)
        {
            position += 4;
        }
        if (position + 12 > archive.length)
        {
            throw new ZipException("truncated data descriptor");
        }
        position += 12;
    }

    private int peekInt()
    {
        return (archive[position] & 0xff) | ((archive[position + 1] & 0xff) << 8)
            | ((archive[position + 2] & 0xff) << 16) | ((archive[position + 3] & 0xff) << 24);
    }

    private int readInt() throws IOException
    {
        if (position + 4 > archive.length)
        {
            throw new ZipException("truncated archive");
        }
        int value = peekInt();
        position += 4;
        return value;
    }

    private long readIntUnsigned() throws IOException
    {
        return readInt() & 0xffffffffL;
    }

    private int readShort() throws IOException
    {
        if (position + 2 > archive.length)
        {
            throw new ZipException("truncated archive");
        }
        int value = (archive[position] & 0xff) | ((archive[position + 1] & 0xff) << 8);
        position += 2;
        return value;
    }

    private void skip(int count) throws IOException
    {
        if (position + count > archive.length)
        {
            throw new ZipException("truncated archive");
        }
        position += count;
    }

    public void closeEntry() throws IOException
    {
        entryData = null;
        entryPosition = 0;
        entry = null;
    }

    public int read() throws IOException
    {
        byte[] one = new byte[1];
        int count = read(one, 0, 1);
        return count < 0 ? -1 : (one[0] & 0xff);
    }

    public int read(byte[] target, int offset, int length) throws IOException
    {
        if (target == null)
        {
            throw new NullPointerException("target");
        }
        if (offset < 0 || length < 0 || offset + length > target.length)
        {
            throw new IndexOutOfBoundsException();
        }
        if (entryData == null)
        {
            return -1;
        }
        int remaining = entryData.length - entryPosition;
        if (remaining <= 0)
        {
            return -1;
        }
        int count = length < remaining ? length : remaining;
        System.arraycopy(entryData, entryPosition, target, offset, count);
        entryPosition += count;
        return count;
    }

    public int available() throws IOException
    {
        return entryData == null ? 0 : entryData.length - entryPosition;
    }

    public void close() throws IOException
    {
        closeEntry();
        input.close();
    }
}
