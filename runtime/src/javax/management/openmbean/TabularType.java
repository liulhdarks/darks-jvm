package javax.management.openmbean;

public class TabularType extends OpenType<TabularData>
{
    private final CompositeType rowType;
    private final java.util.List<String> indexNames;

    public TabularType(String typeName, String description,
        CompositeType rowType, String[] indexNames) throws OpenDataException
    {
        super(CompositeType.checkName(typeName), description);
        if (rowType == null)
        {
            throw new OpenDataException("null row type");
        }
        this.rowType = rowType;
        java.util.ArrayList<String> names = new java.util.ArrayList<String>();
        if (indexNames != null)
        {
            for (int i = 0; i < indexNames.length; i++)
            {
                names.add(indexNames[i]);
            }
        }
        this.indexNames = names;
    }

    public CompositeType getRowType()
    {
        return rowType;
    }

    public java.util.List<String> getIndexNames()
    {
        return indexNames;
    }
}
