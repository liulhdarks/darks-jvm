package java.nio;

public abstract class Buffer
{
    int mark;
    int position;
    int limit;
    int capacity;

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
            throw new IllegalArgumentException("position " + newPosition);
        }
        position = newPosition;
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
            throw new IllegalArgumentException("limit " + newLimit);
        }
        limit = newLimit;
        if (position > limit)
        {
            position = limit;
        }
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

    public final Buffer flip()
    {
        limit = position;
        position = 0;
        return this;
    }

    public final Buffer rewind()
    {
        position = 0;
        return this;
    }

    public final Buffer clear()
    {
        position = 0;
        limit = capacity;
        return this;
    }

    public final Buffer mark()
    {
        mark = position;
        return this;
    }

    public final Buffer reset()
    {
        position = mark;
        return this;
    }
}
