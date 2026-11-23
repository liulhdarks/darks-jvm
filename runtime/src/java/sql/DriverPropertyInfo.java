package java.sql;

public class DriverPropertyInfo
{
    public String name;
    public String description;
    public boolean required;
    public String value;
    public String[] choices;

    public DriverPropertyInfo(String name, String value)
    {
        this.name = name;
        this.value = value;
    }
}
