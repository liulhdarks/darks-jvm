package java.util;

public class EmptyStackException extends RuntimeException
{
    public EmptyStackException()
    {
        super("stack is empty");
    }
}
