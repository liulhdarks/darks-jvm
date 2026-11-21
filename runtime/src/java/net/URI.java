package java.net;

public final class URI
{
    private final String scheme;
    private final String schemeSpecificPart;
    private final String path;
    private final String fragment;

    private URI(String scheme, String schemeSpecificPart, String path, String fragment)
    {
        this.scheme = scheme;
        this.schemeSpecificPart = schemeSpecificPart;
        this.path = path;
        this.fragment = fragment;
    }

    public URI(String spec) throws URISyntaxException
    {
        this(parseSpec(spec));
    }

    private URI(URI parsed)
    {
        this.scheme = parsed.scheme;
        this.schemeSpecificPart = parsed.schemeSpecificPart;
        this.path = parsed.path;
        this.fragment = parsed.fragment;
    }

    private static URI parseSpec(String spec) throws URISyntaxException
    {
        try
        {
            return parse(spec);
        }
        catch (RuntimeException error)
        {
            throw new URISyntaxException(spec, "malformed");
        }
    }

    public static URI create(String spec)
    {
        return parse(spec);
    }

    public static URI parse(String spec)
    {
        if (spec == null)
        {
            throw new NullPointerException("spec");
        }
        int colon = spec.indexOf(':');
        if (colon <= 0)
        {
            throw new IllegalArgumentException("Missing scheme in " + spec);
        }
        String scheme = spec.substring(0, colon);
        String rest = spec.substring(colon + 1);
        String fragment = null;
        int hash = rest.indexOf('#');
        if (hash >= 0)
        {
            fragment = rest.substring(hash + 1);
            rest = rest.substring(0, hash);
        }
        String schemeSpecificPart = rest;
        String path = rest;
        if (rest.startsWith("//"))
        {
            int slash = rest.indexOf('/', 2);
            path = slash < 0 ? "" : rest.substring(slash);
        }
        return new URI(scheme, schemeSpecificPart, path, fragment);
    }

    public String getScheme()
    {
        return scheme;
    }

    public String getSchemeSpecificPart()
    {
        return schemeSpecificPart;
    }

    public String getRawSchemeSpecificPart()
    {
        return schemeSpecificPart;
    }

    public String getPath()
    {
        return path;
    }

    public String getRawPath()
    {
        return path;
    }

    public String getFragment()
    {
        return fragment;
    }

    public boolean isAbsolute()
    {
        return scheme != null;
    }

    public boolean isOpaque()
    {
        return schemeSpecificPart != null && !schemeSpecificPart.startsWith("/")
            && !schemeSpecificPart.startsWith("//");
    }

    public java.net.URL toURL() throws MalformedURLException
    {
        return new URL(toString());
    }

    public String toString()
    {
        String value = scheme + ":" + schemeSpecificPart;
        if (fragment != null)
        {
            value = value + "#" + fragment;
        }
        return value;
    }

    public boolean equals(Object other)
    {
        return other instanceof URI && toString().equals(other.toString());
    }

    public int hashCode()
    {
        return toString().hashCode();
    }
}
