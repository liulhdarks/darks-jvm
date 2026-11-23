package javax.xml.transform.dom;

import javax.xml.transform.Result;
import org.w3c.dom.Node;

public class DOMResult implements Result
{
    private Node node;
    private String systemId;

    public DOMResult()
    {
    }

    public DOMResult(Node node)
    {
        this.node = node;
    }

    public DOMResult(Node node, String systemId)
    {
        this.node = node;
        this.systemId = systemId;
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
}
