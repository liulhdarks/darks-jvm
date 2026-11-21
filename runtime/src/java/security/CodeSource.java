package java.security;

import java.net.URL;

/**
 * Code origin of a protection domain.  Class files loaded by this runtime do
 * not carry location or signer information, so the location may be null.
 */
public class CodeSource
{
    private final URL location;

    public CodeSource(URL url, Object[] certs)
    {
        this.location = url;
    }

    public final URL getLocation()
    {
        return location;
    }

    public String toString()
    {
        return "(" + location + ")";
    }
}
