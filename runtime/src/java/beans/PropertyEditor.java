package java.beans;

public interface PropertyEditor
{
    void setValue(Object value);

    Object getValue();

    boolean isPaintable();

    String getJavaInitializationString();

    String getAsText();

    void setAsText(String text) throws IllegalArgumentException;

    String[] getTags();

    boolean supportsCustomEditor();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
