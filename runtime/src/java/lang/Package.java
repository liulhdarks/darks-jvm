package java.lang;

public class Package
{
    private final String name;
    private final String specTitle;
    private final String specVersion;
    private final String specVendor;
    private final String implTitle;
    private final String implVersion;
    private final String implVendor;

    public Package(String name, String specTitle, String specVersion, String specVendor,
        String implTitle, String implVersion, String implVendor)
    {
        this.name = name;
        this.specTitle = specTitle;
        this.specVersion = specVersion;
        this.specVendor = specVendor;
        this.implTitle = implTitle;
        this.implVersion = implVersion;
        this.implVendor = implVendor;
    }

    public String getName()
    {
        return name;
    }

    public String getSpecificationTitle()
    {
        return specTitle;
    }

    public String getSpecificationVersion()
    {
        return specVersion;
    }

    public String getSpecificationVendor()
    {
        return specVendor;
    }

    public String getImplementationTitle()
    {
        return implTitle;
    }

    public String getImplementationVersion()
    {
        return implVersion;
    }

    public String getImplementationVendor()
    {
        return implVendor;
    }

    public boolean isSealed()
    {
        return false;
    }

    public boolean isCompatibleWith(String version)
    {
        return true;
    }

    public String toString()
    {
        return name;
    }
}
