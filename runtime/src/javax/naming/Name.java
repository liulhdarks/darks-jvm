package javax.naming;

public interface Name extends Cloneable, java.io.Serializable, Comparable<Object>
{
    Object clone();

    int compareTo(Object obj);

    int size();

    boolean isEmpty();

    String get(int posn);

    java.util.Enumeration<String> getAll();

    Name getPrefix(int posn);

    Name getSuffix(int posn);

    boolean startsWith(Name n);

    boolean startsWith(String prefix);

    boolean endsWith(Name n);

    boolean endsWith(String suffix);

    Name add(String comp) throws InvalidNameException;

    Name add(int posn, String comp) throws InvalidNameException;

    Name addAll(Name suffix) throws InvalidNameException;

    Name addAll(int posn, Name n) throws InvalidNameException;

    Object remove(int posn) throws InvalidNameException;
}
