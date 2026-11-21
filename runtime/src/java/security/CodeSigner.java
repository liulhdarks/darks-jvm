package java.security;

public class CodeSigner
{
    private final Object signerCertPath;
    private final Object timestamp;

    public CodeSigner(Object signerCertPath, Object timestamp)
    {
        this.signerCertPath = signerCertPath;
        this.timestamp = timestamp;
    }

    public Object getSignerCertPath()
    {
        return signerCertPath;
    }

    public Object getTimestamp()
    {
        return timestamp;
    }
}
