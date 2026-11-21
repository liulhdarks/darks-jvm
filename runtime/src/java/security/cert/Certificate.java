package java.security.cert;

public abstract class Certificate
{
    private final String type;

    protected Certificate(String type)
    {
        this.type = type;
    }

    public final String getType()
    {
        return type;
    }

    public abstract byte[] getEncoded() throws CertificateEncodingException;

    public abstract void verify(java.security.PublicKey key)
        throws CertificateException, java.security.NoSuchAlgorithmException;

    public abstract String toString();

    public abstract java.security.PublicKey getPublicKey();
}
