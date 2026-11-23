package javax.naming.event;

public interface NamespaceChangeListener extends NamingListener
{
    void objectAdded(NamingEvent event);

    void objectRemoved(NamingEvent event);

    void objectRenamed(NamingEvent event);

    void namingExceptionThrown(NamingExceptionEvent event);
}
