package javax.net.ssl;

import java.nio.ByteBuffer;

// Minimal SSLEngine surface.  The class exposes the JSSE engine contract so
// that libraries can link against it and query the default application
// protocol; running a TLS handshake is not implemented.
public abstract class SSLEngine
{
    private final String peerHost;
    private final int peerPort;

    protected SSLEngine()
    {
        this(null, -1);
    }

    protected SSLEngine(String peerHost, int peerPort)
    {
        this.peerHost = peerHost;
        this.peerPort = peerPort;
    }

    public String getPeerHost()
    {
        return peerHost;
    }

    public int getPeerPort()
    {
        return peerPort;
    }

    public String getApplicationProtocol()
    {
        return "";
    }

    public String getHandshakeApplicationProtocol()
    {
        return null;
    }

    public abstract SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException;

    public abstract SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length,
        ByteBuffer dst) throws SSLException;

    public abstract SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException;

    public abstract SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset,
        int length) throws SSLException;

    public abstract Runnable getDelegatedTask();

    public abstract void closeInbound() throws SSLException;

    public abstract boolean isInboundDone();

    public abstract void closeOutbound();

    public abstract boolean isOutboundDone();

    public abstract String[] getSupportedCipherSuites();

    public abstract String[] getEnabledCipherSuites();

    public abstract void setEnabledCipherSuites(String[] suites);

    public abstract String[] getSupportedProtocols();

    public abstract String[] getEnabledProtocols();

    public abstract void setEnabledProtocols(String[] protocols);

    public abstract void beginHandshake() throws SSLException;

    public abstract SSLEngineResult.HandshakeStatus getHandshakeStatus();

    public abstract void setUseClientMode(boolean mode);

    public abstract boolean getUseClientMode();

    public abstract void setNeedClientAuth(boolean need);

    public abstract boolean getNeedClientAuth();

    public abstract void setWantClientAuth(boolean want);

    public abstract boolean getWantClientAuth();

    public abstract void setEnableSessionCreation(boolean flag);

    public abstract boolean getEnableSessionCreation();
}
