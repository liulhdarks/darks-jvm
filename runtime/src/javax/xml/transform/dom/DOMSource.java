package javax.xml.transform.dom;

import javax.xml.transform.Source;
import org.w3c.dom.Node;

public class DOMSource implements Source
{
    private Node node;
    private String systemId;

    public DOMSource()
    {
    }

    public DOMSource(Node node)
    {
        setNode(node);
    }

    public DOMSource(Node node, String systemId)
    {
        setNode(node);
        setSystemId(systemId);
    }

    public void setNode(Node node)
    {
        this.node = node;
    }

    public Node getNode()
    {
        return node;
    }

    public void setSystemId(String systemId)
    {
        this.systemId = systemId;
    }

    public String getSystemId()
    {
        return systemId;
    }

    public boolean isEmpty()
    {
        return false;
    }
}
