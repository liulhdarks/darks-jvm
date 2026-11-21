package java.security;

public interface PublicKey
{
    String getAlgorithm();

    String getFormat();

    byte[] getEncoded();
}
