package java.util.zip;

public class ZipEntry
{
    public static final int STORED = 0;
    public static final int DEFLATED = 8;

    private String name;
    private long size = -1;
    private long compressedSize = -1;
    private long crc = -1;
    private long time = -1;
    private int method = -1;
    private String comment;
    private byte[] extra;

    public ZipEntry(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        this.name = name;
    }

    public ZipEntry(ZipEntry other)
    {
        if (other == null)
        {
            throw new NullPointerException("entry");
        }
        this.name = other.name;
        this.size = other.size;
        this.compressedSize = other.compressedSize;
        this.crc = other.crc;
        this.time = other.time;
        this.method = other.method;
        this.comment = other.comment;
        this.extra = other.extra;
    }

    public String getName()
    {
        return name;
    }

    public long getSize()
    {
        return size;
    }

    public void setSize(long size)
    {
        if (size < 0)
        {
            throw new IllegalArgumentException("invalid entry size");
        }
        this.size = size;
    }

    public long getCompressedSize()
    {
        return compressedSize;
    }

    public void setCompressedSize(long size)
    {
        this.compressedSize = size;
    }

    public long getCrc()
    {
        return crc;
    }

    public void setCrc(long crc)
    {
        this.crc = crc;
    }

    public int getMethod()
    {
        return method;
    }

    public void setMethod(int method)
    {
        this.method = method;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public byte[] getExtra()
    {
        return extra;
    }

    public void setExtra(byte[] extra)
    {
        this.extra = extra;
    }

    public boolean isDirectory()
    {
        return name != null && name.endsWith("/");
    }

    public String toString()
    {
        return name;
    }

    public int hashCode()
    {
        return name == null ? 0 : name.hashCode();
    }
}
