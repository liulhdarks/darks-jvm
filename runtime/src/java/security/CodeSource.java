package java.security;

import java.net.URL;
import java.security.cert.Certificate;

public class CodeSource implements java.io.Serializable
{
    private final URL location;
    private final Certificate[] certificates;

    public CodeSource(URL location, Certificate[] certificates)
    {
        this.location = location;
        this.certificates = certificates == null ? null : copy(certificates);
    }

    private static Certificate[] copy(Certificate[] source)
    {
        Certificate[] result = new Certificate[source.length];
        for (int i = 0; i < source.length; i++)
        {
            result[i] = source[i];
        }
        return result;
    }

    public final URL getLocation()
    {
        return location;
    }

    public final Certificate[] getCertificates()
    {
        return certificates == null ? null : copy(certificates);
    }

    public boolean implies(CodeSource source)
    {
        if (source == null)
        {
            return false;
        }
        return location == null ? source.location == null
            : location.equals(source.location);
    }

    public boolean equals(Object object)
    {
        if (this == object)
        {
            return true;
        }
        if (!(object instanceof CodeSource))
        {
            return false;
        }
        CodeSource other = (CodeSource)object;
        return (location == null ? other.location == null : location.equals(other.location));
    }

    public int hashCode()
    {
        return location == null ? 0 : location.hashCode();
    }

    public String toString()
    {
        return "(" + (location == null ? "<no location>" : location.toString()) + ")";
    }
}
