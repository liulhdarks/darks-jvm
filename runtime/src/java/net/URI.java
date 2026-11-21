package java.net;

/**
 * Minimal RFC 2396 style URI value object.  Parsing covers the components the
 * runtime needs (scheme, authority/user-info/host/port, path, query and
 * fragment).  Percent-encoding is preserved verbatim rather than decoded.
 */
public final class URI implements Comparable<URI>
{
    private final String scheme;
    private final String userInfo;
    private final String host;
    private final int port;
    private final String path;
    private final String query;
    private final String fragment;
    private final boolean opaque;
    private final String opaquePart;
    private final String authority;

    public URI(String spec) throws URISyntaxException
    {
        if (spec == null)
        {
            throw new NullPointerException("spec");
        }
        String rest = spec;
        String parsedScheme = null;
        int colon = schemeEnd(rest);
        if (colon > 0)
        {
            parsedScheme = rest.substring(0, colon);
            rest = rest.substring(colon + 1);
        }
        String parsedFragment = null;
        int hash = rest.indexOf('#');
        if (hash >= 0)
        {
            parsedFragment = rest.substring(hash + 1);
            rest = rest.substring(0, hash);
        }
        String parsedQuery = null;
        int question = rest.indexOf('?');
        if (question >= 0)
        {
            parsedQuery = rest.substring(question + 1);
            rest = rest.substring(0, question);
        }
        if (parsedScheme != null && rest.length() > 0 && rest.charAt(0) != '/')
        {
            this.scheme = parsedScheme;
            this.userInfo = null;
            this.host = null;
            this.port = -1;
            this.path = null;
            this.query = parsedQuery;
            this.fragment = parsedFragment;
            this.opaque = true;
            this.opaquePart = rest;
            this.authority = null;
        }
        else
        {
            String parsedAuthority = null;
            if (rest.startsWith("//"))
            {
                int slash = rest.indexOf('/', 2);
                if (slash < 0)
                {
                    parsedAuthority = rest.substring(2);
                    rest = "";
                }
                else
                {
                    parsedAuthority = rest.substring(2, slash);
                    rest = rest.substring(slash);
                }
            }
            this.scheme = parsedScheme;
            String ui = null;
            String h = null;
            int p = -1;
            if (parsedAuthority != null)
            {
                int at = parsedAuthority.lastIndexOf('@');
                String hostPort = parsedAuthority;
                if (at >= 0)
                {
                    ui = parsedAuthority.substring(0, at);
                    hostPort = parsedAuthority.substring(at + 1);
                }
                if (hostPort.startsWith("["))
                {
                    int close = hostPort.indexOf(']');
                    if (close >= 0)
                    {
                        h = hostPort.substring(0, close + 1);
                        if (close + 1 < hostPort.length() &&
                            hostPort.charAt(close + 1) == ':')
                        {
                            p = parsePort(hostPort.substring(close + 2), spec);
                        }
                    }
                    else
                    {
                        h = hostPort;
                    }
                }
                else
                {
                    int portColon = hostPort.lastIndexOf(':');
                    if (portColon >= 0)
                    {
                        h = hostPort.substring(0, portColon);
                        p = parsePort(hostPort.substring(portColon + 1), spec);
                    }
                    else
                    {
                        h = hostPort;
                    }
                }
            }
            this.userInfo = ui;
            this.host = h;
            this.port = p;
            this.authority = parsedAuthority;
            this.path = rest.length() == 0 && parsedAuthority == null ? null : rest;
            this.query = parsedQuery;
            this.fragment = parsedFragment;
            this.opaque = false;
            this.opaquePart = null;
        }
    }

    public URI(String scheme, String host, String path, String fragment)
        throws URISyntaxException
    {
        this(scheme, null, host, -1, path, null, fragment);
    }

    public URI(String scheme, String authority, String path, String query,
               String fragment) throws URISyntaxException
    {
        this.scheme = scheme;
        this.authority = authority;
        this.userInfo = null;
        this.host = null;
        this.port = -1;
        this.path = path;
        this.query = query;
        this.fragment = fragment;
        this.opaque = false;
        this.opaquePart = null;
    }

    public URI(String scheme, String userInfo, String host, int port,
               String path, String query, String fragment)
        throws URISyntaxException
    {
        this.scheme = scheme;
        this.userInfo = userInfo;
        this.host = host;
        this.port = port;
        this.path = path;
        this.query = query;
        this.fragment = fragment;
        this.opaque = false;
        this.opaquePart = null;
        this.authority = buildAuthority(userInfo, host, port);
    }

    public static URI create(String spec)
    {
        try
        {
            return new URI(spec);
        }
        catch (URISyntaxException ex)
        {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private static int schemeEnd(String spec)
    {
        int colon = spec.indexOf(':');
        if (colon <= 0)
        {
            return -1;
        }
        char first = spec.charAt(0);
        if (!((first >= 'a' && first <= 'z') || (first >= 'A' && first <= 'Z')))
        {
            return -1;
        }
        for (int i = 1; i < colon; i++)
        {
            char c = spec.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') || c == '+' || c == '-' || c == '.';
            if (!ok)
            {
                return -1;
            }
        }
        return colon;
    }

    private static int parsePort(String text, String spec) throws URISyntaxException
    {
        if (text.length() == 0)
        {
            return -1;
        }
        int value = 0;
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (c < '0' || c > '9')
            {
                throw new URISyntaxException(spec, "illegal port number");
            }
            value = value * 10 + (c - '0');
        }
        return value;
    }

    private static String buildAuthority(String userInfo, String host, int port)
    {
        if (host == null)
        {
            return null;
        }
        String result = userInfo == null ? host : userInfo + "@" + host;
        if (port >= 0)
        {
            result = result + ":" + port;
        }
        return result;
    }

    public String getScheme()
    {
        return scheme;
    }

    public String getAuthority()
    {
        return authority;
    }

    public String getUserInfo()
    {
        return userInfo;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getPath()
    {
        return path;
    }

    public String getQuery()
    {
        return query;
    }

    public String getFragment()
    {
        return fragment;
    }

    public String getRawSchemeSpecificPart()
    {
        return buildSchemeSpecificPart();
    }

    public String getSchemeSpecificPart()
    {
        return buildSchemeSpecificPart();
    }

    private String buildSchemeSpecificPart()
    {
        if (opaque)
        {
            return opaquePart;
        }
        String result = authority != null ? "//" + authority : "";
        result = result + (path == null ? "" : path);
        if (query != null)
        {
            result = result + "?" + query;
        }
        return result;
    }

    public boolean isAbsolute()
    {
        return scheme != null;
    }

    public boolean isOpaque()
    {
        return opaque;
    }

    public URL toURL() throws MalformedURLException
    {
        return new URL(toString());
    }

    public URI normalize()
    {
        if (path == null || path.length() == 0)
        {
            return this;
        }
        boolean absolute = path.startsWith("/");
        String[] parts = path.split("/");
        String result = "";
        int kept = 0;
        String[] stack = new String[parts.length + 1];
        for (int i = 0; i < parts.length; i++)
        {
            String part = parts[i];
            if (part.length() == 0 || part.equals("."))
            {
                continue;
            }
            if (part.equals(".."))
            {
                if (kept > 0)
                {
                    kept = kept - 1;
                }
                continue;
            }
            stack[kept] = part;
            kept = kept + 1;
        }
        for (int i = 0; i < kept; i++)
        {
            result = result + "/" + stack[i];
        }
        if (result.length() == 0)
        {
            result = absolute ? "/" : "";
        }
        else if (!absolute && path.startsWith("."))
        {
            result = result.substring(1);
        }
        try
        {
            return new URI(scheme, authority, result, query, fragment);
        }
        catch (URISyntaxException ex)
        {
            return this;
        }
    }

    public URI resolve(URI other)
    {
        if (other == null)
        {
            throw new NullPointerException("other");
        }
        if (other.isAbsolute())
        {
            return other;
        }
        String newPath = other.path == null ? path : other.path;
        if (newPath != null && !newPath.startsWith("/") && authority != null)
        {
            int slash = path == null ? -1 : path.lastIndexOf('/');
            newPath = slash >= 0 ? path.substring(0, slash + 1) + newPath : newPath;
        }
        try
        {
            return new URI(scheme, authority, newPath, other.query, other.fragment);
        }
        catch (URISyntaxException ex)
        {
            return this;
        }
    }

    public URI resolve(String other)
    {
        return resolve(create(other));
    }

    public int compareTo(URI other)
    {
        return toString().compareTo(other.toString());
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof URI))
        {
            return false;
        }
        return toString().equals(((URI)other).toString());
    }

    public int hashCode()
    {
        return toString().hashCode();
    }

    public String toASCIIString()
    {
        return toString();
    }

    public String toString()
    {
        String result = scheme != null ? scheme + ":" : "";
        if (opaque)
        {
            result = result + opaquePart;
        }
        else
        {
            if (authority != null)
            {
                result = result + "//" + authority;
            }
            result = result + (path == null ? "" : path);
            if (query != null)
            {
                result = result + "?" + query;
            }
        }
        if (fragment != null)
        {
            result = result + "#" + fragment;
        }
        return result;
    }
}
