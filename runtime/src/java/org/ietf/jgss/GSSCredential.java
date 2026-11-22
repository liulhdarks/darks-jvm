package org.ietf.jgss;

/** Minimal GSS credential contract; only the lifetime is queried by Tomcat. */
public interface GSSCredential extends Cloneable
{
    int getRemainingLifetime() throws GSSException;

    void dispose() throws GSSException;
}
