package javax.xml.transform.sax;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ContentHandler;

import javax.xml.transform.Source;

public class SAXSource implements Source
{
    public static final String FEATURE = "http://javax.xml.transform.sax.SAXSource/feature";

    private XMLReader reader;
    private InputSource inputSource;

    public SAXSource()
    {
    }

    public SAXSource(XMLReader reader, InputSource inputSource)
    {
        this.reader = reader;
        this.inputSource = inputSource;
    }

    public SAXSource(InputSource inputSource)
    {
        this.inputSource = inputSource;
    }

    public void setXMLReader(XMLReader reader)
    {
        this.reader = reader;
    }

    public XMLReader getXMLReader()
    {
        return reader;
    }

    public void setInputSource(InputSource inputSource)
    {
        this.inputSource = inputSource;
    }

    public InputSource getInputSource()
    {
        return inputSource;
    }

    public void setSystemId(String systemId)
    {
        if (inputSource == null)
        {
            inputSource = new InputSource(systemId);
        }
        else
        {
            inputSource.setSystemId(systemId);
        }
    }

    public String getSystemId()
    {
        return inputSource == null ? null : inputSource.getSystemId();
    }
}
