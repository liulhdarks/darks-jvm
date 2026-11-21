package java.nio;

/**
 * Minimal buffer position/limit bookkeeping shared by the NIO buffer types.
 */
public abstract class Buffer
{
    int capacity;
    int limit;
    int position;
    int mark = -1;

    Buffer(int capacity)
    {
        this.capacity = capacity;
        this.limit = capacity;
    }

    public final int capacity()
    {
        return capacity;
    }

    public final int position()
    {
        return position;
    }

    public final Buffer position(int newPosition)
    {
        if (newPosition < 0 || newPosition > limit)
        {
            throw new IllegalArgumentException("position out of range");
        }
        position = newPosition;
        if (mark > position)
        {
            mark = -1;
        }
        return this;
    }

    public final int limit()
    {
        return limit;
    }

    public final Buffer limit(int newLimit)
    {
        if (newLimit < 0 || newLimit > capacity)
        {
            throw new IllegalArgumentException("limit out of range");
        }
        limit = newLimit;
        if (position > limit)
        {
            position = limit;
        }
        if (mark > limit)
        {
            mark = -1;
        }
        return this;
    }

    public final Buffer mark()
    {
        mark = position;
        return this;
    }

    public final Buffer reset()
    {
        if (mark < 0)
        {
            throw new java.nio.InvalidMarkException();
        }
        position = mark;
        return this;
    }

    public final Buffer clear()
    {
        position = 0;
        limit = capacity;
        mark = -1;
        return this;
    }

    public final Buffer flip()
    {
        limit = position;
        position = 0;
        mark = -1;
        return this;
    }

    public final Buffer rewind()
    {
        position = 0;
        mark = -1;
        return this;
    }

    public final int remaining()
    {
        return limit - position;
    }

    public final boolean hasRemaining()
    {
        return position < limit;
    }

    public abstract boolean isReadOnly();

    public abstract boolean hasArray();

    public abstract Object array();

    public abstract int arrayOffset();

    final void checkBounds(int index, int length)
    {
        if (index < 0 || length > limit - index)
        {
            throw new IndexOutOfBoundsException();
        }
    }
}
