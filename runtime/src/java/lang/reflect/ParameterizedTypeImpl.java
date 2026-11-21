package java.lang.reflect;

final class ParameterizedTypeImpl implements ParameterizedType, Type
{
    private final Type rawType;
    private final Type[] arguments;
    private final Type ownerType;

    ParameterizedTypeImpl(Type rawType, Type[] arguments, Type ownerType)
    {
        this.rawType = rawType;
        this.arguments = arguments == null ? new Type[0] : arguments;
        this.ownerType = ownerType;
    }

    public Type[] getActualTypeArguments()
    {
        return arguments;
    }

    public Type getRawType()
    {
        return rawType;
    }

    public Type getOwnerType()
    {
        return ownerType;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(rawType instanceof Class
            ? ((Class<?>)rawType).getName() : String.valueOf(rawType));
        if (arguments.length > 0)
        {
            sb.append('<');
            for (int i = 0; i < arguments.length; i++)
            {
                if (i > 0)
                {
                    sb.append(", ");
                }
                sb.append(String.valueOf(arguments[i]));
            }
            sb.append('>');
        }
        return sb.toString();
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ParameterizedType))
        {
            return false;
        }
        ParameterizedType type = (ParameterizedType)other;
        if (!same(rawType, type.getRawType()) ||
            !same(ownerType, type.getOwnerType()))
        {
            return false;
        }
        Type[] theirs = type.getActualTypeArguments();
        if (arguments.length != theirs.length)
        {
            return false;
        }
        for (int i = 0; i < arguments.length; i++)
        {
            if (!same(arguments[i], theirs[i]))
            {
                return false;
            }
        }
        return true;
    }

    public int hashCode()
    {
        int h = rawType == null ? 0 : rawType.hashCode();
        h ^= ownerType == null ? 0 : ownerType.hashCode();
        for (int i = 0; i < arguments.length; i++)
        {
            h ^= arguments[i] == null ? 0 : arguments[i].hashCode();
        }
        return h;
    }

    private static boolean same(Object a, Object b)
    {
        return a == null ? b == null : a.equals(b);
    }
}
