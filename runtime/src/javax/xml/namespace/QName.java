package javax.xml.namespace;

import javax.xml.XMLConstants;

public class QName implements java.io.Serializable
{
    private final String namespaceURI;
    private final String localPart;
    private final String prefix;

    public QName(String localPart)
    {
        this(XMLConstants.NULL_NS_URI, localPart, "");
    }

    public QName(String namespaceURI, String localPart)
    {
        this(namespaceURI, localPart, "");
    }

    public QName(String namespaceURI, String localPart, String prefix)
    {
        if (localPart == null)
        {
            throw new IllegalArgumentException("local part cannot be null");
        }
        this.namespaceURI = namespaceURI == null ? "" : namespaceURI;
        this.localPart = localPart;
        this.prefix = prefix == null ? "" : prefix;
    }

    public String getNamespaceURI()
    {
        return namespaceURI;
    }

    public String getLocalPart()
    {
        return localPart;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public String toString()
    {
        return toString(namespaceURI, localPart);
    }

    public static String toString(String namespaceURI, String localPart)
    {
        if (localPart == null)
        {
            throw new IllegalArgumentException("local part cannot be null");
        }
        if (namespaceURI == null || namespaceURI.length() == 0)
        {
            return localPart;
        }
        return "{" + namespaceURI + "}" + localPart;
    }

    public static QName valueOf(String qNameAsString)
    {
        if (qNameAsString == null)
        {
            throw new IllegalArgumentException("null");
        }
        if (qNameAsString.length() > 0 && qNameAsString.charAt(0) == (char)123)
        {
            int end = qNameAsString.indexOf((char)125);
            if (end < 0)
            {
                throw new IllegalArgumentException("Invalid QName: " + qNameAsString);
            }
            return new QName(qNameAsString.substring(1, end),
                qNameAsString.substring(end + 1));
        }
        return new QName("", qNameAsString);
    }

    public boolean equals(Object other)
    {
        if (other == this)
        {
            return true;
        }
        if (!(other instanceof QName))
        {
            return false;
        }
        QName qname = (QName)other;
        return localPart.equals(qname.localPart) && namespaceURI.equals(qname.namespaceURI);
    }

    public int hashCode()
    {
        return localPart.hashCode() ^ namespaceURI.hashCode();
    }
}
