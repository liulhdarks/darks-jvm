package java.lang;

public final class StackTraceElement
{
    private String declaringClass;
    private String methodName;
    private String fileName;
    private int lineNumber;

    public StackTraceElement(String declaringClass, String methodName,
        String fileName, int lineNumber)
    {
        if (declaringClass == null || methodName == null)
        {
            throw new NullPointerException();
        }
        this.declaringClass = declaringClass;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public String getClassName()
    {
        return declaringClass;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public String getFileName()
    {
        return fileName;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public boolean isNativeMethod()
    {
        return lineNumber == -2;
    }

    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append(declaringClass).append('.').append(methodName);
        buffer.append('(');
        if (isNativeMethod())
        {
            buffer.append("Native Method");
        }
        else if (fileName == null)
        {
            buffer.append("Unknown Source");
        }
        else
        {
            buffer.append(fileName);
            if (lineNumber >= 0)
            {
                buffer.append(':').append(lineNumber);
            }
        }
        buffer.append(')');
        return buffer.toString();
    }

    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof StackTraceElement))
        {
            return false;
        }
        StackTraceElement element = (StackTraceElement)other;
        return lineNumber == element.lineNumber
            && declaringClass.equals(element.declaringClass)
            && methodName.equals(element.methodName)
            && (fileName == null ? element.fileName == null
                : fileName.equals(element.fileName));
    }

    public int hashCode()
    {
        int result = declaringClass.hashCode();
        result = 31 * result + methodName.hashCode();
        result = 31 * result + (fileName == null ? 0 : fileName.hashCode());
        result = 31 * result + lineNumber;
        return result;
    }
}
