package javax.net.ssl;

// Minimal SSLParameters surface.  It carries the parameter values used by the
// JSSE API; the actual TLS handshake is not implemented by this runtime.
public class SSLParameters
{
    private String[] cipherSuites;
    private String[] protocols;
    private boolean needClientAuth;
    private boolean wantClientAuth;
    private boolean useCipherSuitesOrder;
    private String endpointIdentificationAlgorithm;
    private String[] applicationProtocols = new String[0];

    public SSLParameters()
    {
    }

    public SSLParameters(String[] cipherSuites)
    {
        setCipherSuites(cipherSuites);
    }

    public SSLParameters(String[] cipherSuites, String[] protocols)
    {
        setCipherSuites(cipherSuites);
        setProtocols(protocols);
    }

    public String[] getCipherSuites()
    {
        return cipherSuites == null ? null : cipherSuites.clone();
    }

    public void setCipherSuites(String[] cipherSuites)
    {
        this.cipherSuites = cipherSuites == null ? null : cipherSuites.clone();
    }

    public String[] getProtocols()
    {
        return protocols == null ? null : protocols.clone();
    }

    public void setProtocols(String[] protocols)
    {
        this.protocols = protocols == null ? null : protocols.clone();
    }

    public boolean getNeedClientAuth()
    {
        return needClientAuth;
    }

    public void setNeedClientAuth(boolean needClientAuth)
    {
        this.needClientAuth = needClientAuth;
        if (needClientAuth)
        {
            this.wantClientAuth = false;
        }
    }

    public boolean getWantClientAuth()
    {
        return wantClientAuth;
    }

    public void setWantClientAuth(boolean wantClientAuth)
    {
        this.wantClientAuth = wantClientAuth;
        if (wantClientAuth)
        {
            this.needClientAuth = false;
        }
    }

    public boolean getUseCipherSuitesOrder()
    {
        return useCipherSuitesOrder;
    }

    public void setUseCipherSuitesOrder(boolean useCipherSuitesOrder)
    {
        this.useCipherSuitesOrder = useCipherSuitesOrder;
    }

    public String getEndpointIdentificationAlgorithm()
    {
        return endpointIdentificationAlgorithm;
    }

    public void setEndpointIdentificationAlgorithm(String endpointIdentificationAlgorithm)
    {
        this.endpointIdentificationAlgorithm = endpointIdentificationAlgorithm;
    }

    public String[] getApplicationProtocols()
    {
        return applicationProtocols.clone();
    }

    public void setApplicationProtocols(String[] applicationProtocols)
    {
        if (applicationProtocols == null)
        {
            throw new IllegalArgumentException("applicationProtocols");
        }
        this.applicationProtocols = applicationProtocols.clone();
    }

    public String toString()
    {
        return "SSLParameters{applicationProtocols=" + java.util.Arrays.toString(applicationProtocols)
            + ", needClientAuth=" + needClientAuth + ", wantClientAuth=" + wantClientAuth + "}";
    }
}
