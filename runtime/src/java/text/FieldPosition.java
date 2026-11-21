package java.text;

public class FieldPosition
{
    private final int field;
    private int beginIndex;
    private int endIndex;

    public FieldPosition(int field)
    {
        this.field = field;
    }

    public int getField()
    {
        return field;
    }

    public int getBeginIndex()
    {
        return beginIndex;
    }

    public int getEndIndex()
    {
        return endIndex;
    }

    public void setBeginIndex(int beginIndex)
    {
        this.beginIndex = beginIndex;
    }

    public void setEndIndex(int endIndex)
    {
        this.endIndex = endIndex;
    }
}
