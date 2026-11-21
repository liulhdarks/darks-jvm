package java.lang.reflect;

import java.util.ArrayList;

/**
 * Parses the generic signature grammar from the class file Signature
 * attribute (JVMS 4.7.9.1) into the Type model exposed by java.lang.reflect.
 */
public final class GenericSignatureParser
{
    private GenericSignatureParser()
    {
    }

    public static Type[] getClassTypeParameters(Class<?> clazz)
    {
        String signature = clazz.getSignature0();
        if (signature == null)
        {
            return new Type[0];
        }
        Parser parser = new Parser(signature, clazz, clazz, null);
        parser.parseFormalTypeParameters();
        return parser.scope.types();
    }

    public static Type getGenericSuperclass(Class<?> clazz)
    {
        if (clazz.isInterface())
        {
            return null;
        }
        String signature = clazz.getSignature0();
        if (signature == null)
        {
            return clazz.getSuperclass();
        }
        Parser parser = new Parser(signature, clazz, clazz, null);
        parser.parseFormalTypeParameters();
        return parser.parseType();
    }

    public static Type[] getGenericInterfaces(Class<?> clazz)
    {
        String signature = clazz.getSignature0();
        if (signature == null)
        {
            Class<?>[] raw = clazz.getInterfaces();
            Type[] result = new Type[raw.length];
            for (int i = 0; i < raw.length; i++)
            {
                result[i] = raw[i];
            }
            return result;
        }
        Parser parser = new Parser(signature, clazz, clazz, null);
        parser.parseFormalTypeParameters();
        parser.parseType();
        ArrayList<Type> list = new ArrayList<Type>();
        while (parser.hasMore())
        {
            list.add(parser.parseType());
        }
        return toTypes(list);
    }

    public static Type getGenericReturnType(Method method)
    {
        String signature = method.getSignature0();
        if (signature == null)
        {
            return method.getReturnType();
        }
        Parser parser = methodParser(signature, method);
        parser.parseFormalTypeParameters();
        parser.expect('(');
        while (parser.peek() != ')')
        {
            parser.parseType();
        }
        parser.expect(')');
        return parser.parseType();
    }

    public static Type[] getGenericParameterTypes(Method method)
    {
        String signature = method.getSignature0();
        if (signature == null)
        {
            Class<?>[] raw = method.getParameterTypes();
            Type[] result = new Type[raw.length];
            for (int i = 0; i < raw.length; i++)
            {
                result[i] = raw[i];
            }
            return result;
        }
        Parser parser = methodParser(signature, method);
        parser.parseFormalTypeParameters();
        return parser.parseParameterList();
    }

    public static Type[] getGenericExceptionTypes(Method method)
    {
        String signature = method.getSignature0();
        ArrayList<Type> list = new ArrayList<Type>();
        if (signature == null)
        {
            Class<?>[] raw = method.getExceptionTypes();
            for (int i = 0; i < raw.length; i++)
            {
                list.add(raw[i]);
            }
            return toTypes(list);
        }
        Parser parser = methodParser(signature, method);
        parser.parseFormalTypeParameters();
        parser.parseParameterList();
        parser.parseType();
        while (parser.peek() == '^')
        {
            parser.pos++;
            list.add(parser.parseType());
        }
        return toTypes(list);
    }

    public static Type[] getMethodTypeParameters(Method method)
    {
        String signature = method.getSignature0();
        if (signature == null)
        {
            return new Type[0];
        }
        Parser parser = methodParser(signature, method);
        parser.parseFormalTypeParameters();
        return parser.scope.typesFrom(parser.methodScopeBase);
    }

    public static Type[] getGenericConstructorParameterTypes(Constructor<?> ctor)
    {
        String signature = ctor.getSignature0();
        if (signature == null)
        {
            Class<?>[] raw = ctor.getParameterTypes();
            Type[] result = new Type[raw.length];
            for (int i = 0; i < raw.length; i++)
            {
                result[i] = raw[i];
            }
            return result;
        }
        Parser parser = constructorParser(signature, ctor);
        parser.parseFormalTypeParameters();
        return parser.parseParameterList();
    }

    public static Type[] getGenericConstructorExceptionTypes(Constructor<?> ctor)
    {
        String signature = ctor.getSignature0();
        ArrayList<Type> list = new ArrayList<Type>();
        if (signature == null)
        {
            Class<?>[] raw = ctor.getExceptionTypes();
            for (int i = 0; i < raw.length; i++)
            {
                list.add(raw[i]);
            }
            return toTypes(list);
        }
        Parser parser = constructorParser(signature, ctor);
        parser.parseFormalTypeParameters();
        parser.parseParameterList();
        parser.parseType();
        while (parser.peek() == '^')
        {
            parser.pos++;
            list.add(parser.parseType());
        }
        return toTypes(list);
    }

    public static Type[] getConstructorTypeParameters(Constructor<?> ctor)
    {
        String signature = ctor.getSignature0();
        if (signature == null)
        {
            return new Type[0];
        }
        Parser parser = constructorParser(signature, ctor);
        parser.parseFormalTypeParameters();
        return parser.scope.typesFrom(parser.methodScopeBase);
    }

    public static Type getGenericFieldType(Field field)
    {
        String signature = field.getSignature0();
        if (signature == null)
        {
            return field.getType();
        }
        Parser parser = new Parser(signature, field.getDeclaringClass(), null,
            classScope(field.getDeclaringClass()));
        return parser.parseType();
    }

    private static Parser methodParser(String signature, Method method)
    {
        return new Parser(signature, method.getDeclaringClass(), method,
            classScope(method.getDeclaringClass()));
    }

    private static Parser constructorParser(String signature, Constructor<?> ctor)
    {
        return new Parser(signature, ctor.getDeclaringClass(), ctor,
            classScope(ctor.getDeclaringClass()));
    }

    private static Scope classScope(Class<?> clazz)
    {
        Scope scope = new Scope();
        TypeVariable<?>[] parameters = clazz.getTypeParameters();
        for (int i = 0; i < parameters.length; i++)
        {
            if (parameters[i] instanceof TypeVariableImpl)
            {
                scope.add((TypeVariableImpl)parameters[i]);
            }
        }
        return scope;
    }

    private static Type[] toTypes(ArrayList<Type> list)
    {
        Type[] result = new Type[list.size()];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = list.get(i);
        }
        return result;
    }

    private static Class<?> classForName(String internalName)
    {
        String dotted = internalName.replace('/', '.');
        try
        {
            return Class.forName(dotted);
        }
        catch (ClassNotFoundException missing)
        {
            return Object.class;
        }
    }

    private static Class<?> arrayClass(Class<?> component)
    {
        String descriptor;
        if (component == boolean.class)
        {
            descriptor = "[Z";
        }
        else if (component == byte.class)
        {
            descriptor = "[B";
        }
        else if (component == char.class)
        {
            descriptor = "[C";
        }
        else if (component == short.class)
        {
            descriptor = "[S";
        }
        else if (component == int.class)
        {
            descriptor = "[I";
        }
        else if (component == long.class)
        {
            descriptor = "[J";
        }
        else if (component == float.class)
        {
            descriptor = "[F";
        }
        else if (component == double.class)
        {
            descriptor = "[D";
        }
        else
        {
            String name = component.getName();
            if (name.length() > 0 && name.charAt(0) == '[')
            {
                descriptor = "[" + name;
            }
            else
            {
                descriptor = "[L" + name.replace('.', '/') + ";";
            }
        }
        try
        {
            return Class.forName(descriptor);
        }
        catch (ClassNotFoundException missing)
        {
            return Object.class;
        }
    }

    static final class Scope
    {
        private String[] names = new String[4];
        private TypeVariableImpl[] variables = new TypeVariableImpl[4];
        private int count;

        void add(TypeVariableImpl variable)
        {
            if (count == names.length)
            {
                String[] biggerNames = new String[count * 2];
                TypeVariableImpl[] biggerVariables = new TypeVariableImpl[count * 2];
                System.arraycopy(names, 0, biggerNames, 0, count);
                System.arraycopy(variables, 0, biggerVariables, 0, count);
                names = biggerNames;
                variables = biggerVariables;
            }
            names[count] = variable.getName();
            variables[count] = variable;
            count++;
        }

        TypeVariableImpl find(String name)
        {
            for (int i = count - 1; i >= 0; i--)
            {
                if (names[i].equals(name))
                {
                    return variables[i];
                }
            }
            return null;
        }

        Scope copy()
        {
            Scope copy = new Scope();
            for (int i = 0; i < count; i++)
            {
                copy.add(variables[i]);
            }
            return copy;
        }

        Type[] types()
        {
            Type[] result = new Type[count];
            for (int i = 0; i < count; i++)
            {
                result[i] = variables[i];
            }
            return result;
        }

        Type[] typesFrom(int start)
        {
            Type[] result = new Type[count - start];
            for (int i = start; i < count; i++)
            {
                result[i - start] = variables[i];
            }
            return result;
        }
    }

    static final class Parser
    {
        private final String signature;
        final Scope scope;
        private final Class<?> declaringClass;
        private final GenericDeclaration declaration;
        private final int methodScopeBase;
        int pos;

        Parser(String signature, Class<?> declaringClass,
            GenericDeclaration declaration, Scope parent)
        {
            this.signature = signature;
            this.declaringClass = declaringClass;
            this.declaration = declaration;
            this.scope = parent == null ? new Scope() : parent.copy();
            this.methodScopeBase = this.scope.count;
        }

        boolean hasMore()
        {
            return pos < signature.length();
        }

        char peek()
        {
            return pos < signature.length() ? signature.charAt(pos) : '\0';
        }

        void expect(char expected)
        {
            if (peek() != expected)
            {
                throw new IllegalArgumentException(
                    "malformed generic signature: " + signature);
            }
            pos++;
        }

        private String readUntil(char terminator)
        {
            int start = pos;
            while (pos < signature.length() && signature.charAt(pos) != terminator)
            {
                pos++;
            }
            return signature.substring(start, pos);
        }

        private String readClassNamePart()
        {
            int start = pos;
            while (pos < signature.length())
            {
                char c = signature.charAt(pos);
                if (c == '<' || c == '.' || c == ';')
                {
                    break;
                }
                pos++;
            }
            return signature.substring(start, pos);
        }

        void parseFormalTypeParameters()
        {
            if (peek() != '<')
            {
                return;
            }
            int start = pos;
            pos++;
            ArrayList<String> names = new ArrayList<String>();
            while (peek() != '>' && pos < signature.length())
            {
                names.add(readUntil(':'));
                expect(':');
                if (peek() != ':' && peek() != '>')
                {
                    skipType();
                }
                while (peek() == ':')
                {
                    pos++;
                    skipType();
                }
            }
            pos = start + 1;

            TypeVariableImpl[] variables = new TypeVariableImpl[names.size()];
            for (int i = 0; i < variables.length; i++)
            {
                Object owner = declaration != null ? declaration : declaringClass;
                variables[i] = new TypeVariableImpl(names.get(i),
                    (GenericDeclaration)owner);
                scope.add(variables[i]);
            }
            int index = 0;
            while (peek() != '>' && pos < signature.length())
            {
                readUntil(':');
                expect(':');
                ArrayList<Type> bounds = new ArrayList<Type>();
                if (peek() != ':' && peek() != '>')
                {
                    bounds.add(parseType());
                }
                while (peek() == ':')
                {
                    pos++;
                    bounds.add(parseType());
                }
                if (bounds.size() > 0)
                {
                    variables[index].setBounds(toTypes(bounds));
                }
                index++;
            }
            expect('>');
        }

        Type[] parseParameterList()
        {
            expect('(');
            ArrayList<Type> parameters = new ArrayList<Type>();
            while (peek() != ')')
            {
                parameters.add(parseType());
            }
            expect(')');
            return toTypes(parameters);
        }

        Type parseType()
        {
            char c = peek();
            switch (c)
            {
            case 'B':
                pos++;
                return byte.class;
            case 'C':
                pos++;
                return char.class;
            case 'D':
                pos++;
                return double.class;
            case 'F':
                pos++;
                return float.class;
            case 'I':
                pos++;
                return int.class;
            case 'J':
                pos++;
                return long.class;
            case 'S':
                pos++;
                return short.class;
            case 'Z':
                pos++;
                return boolean.class;
            case 'V':
                pos++;
                return void.class;
            case '[':
                pos++;
                Type component = parseType();
                if (component instanceof Class)
                {
                    return arrayClass((Class<?>)component);
                }
                return new GenericArrayTypeImpl(component);
            case 'T':
                pos++;
                {
                    String name = readUntil(';');
                    expect(';');
                    TypeVariableImpl variable = scope.find(name);
                    if (variable == null)
                    {
                        Object owner = declaration != null ? declaration : declaringClass;
                        variable = new TypeVariableImpl(name, (GenericDeclaration)owner);
                        scope.add(variable);
                    }
                    return variable;
                }
            case 'L':
                return parseClassType();
            default:
                throw new IllegalArgumentException(
                    "malformed generic signature: " + signature);
            }
        }

        private Type parseClassType()
        {
            expect('L');
            String name = "";
            Type owner = null;
            Type[] arguments = null;
            while (true)
            {
                name += readClassNamePart();
                char c = peek();
                if (c == '<')
                {
                    arguments = parseTypeArguments();
                    c = peek();
                }
                if (c == '.')
                {
                    pos++;
                    Class<?> raw = classForName(name);
                    owner = arguments == null ? (Type)raw
                        : new ParameterizedTypeImpl(raw, arguments, owner);
                    name += "$";
                    arguments = null;
                    continue;
                }
                expect(';');
                Class<?> raw = classForName(name);
                if (arguments == null)
                {
                    return raw;
                }
                return new ParameterizedTypeImpl(raw, arguments, owner);
            }
        }

        private Type[] parseTypeArguments()
        {
            expect('<');
            ArrayList<Type> arguments = new ArrayList<Type>();
            while (peek() != '>')
            {
                arguments.add(parseTypeArgument());
            }
            expect('>');
            return toTypes(arguments);
        }

        private Type parseTypeArgument()
        {
            char c = peek();
            if (c == '*')
            {
                pos++;
                return new WildcardTypeImpl(new Type[] { Object.class }, new Type[0]);
            }
            if (c == '+')
            {
                pos++;
                return new WildcardTypeImpl(new Type[] { parseType() }, new Type[0]);
            }
            if (c == '-')
            {
                pos++;
                return new WildcardTypeImpl(new Type[] { Object.class },
                    new Type[] { parseType() });
            }
            return parseType();
        }

        private void skipType()
        {
            char c = peek();
            if (c == '[')
            {
                pos++;
                skipType();
                return;
            }
            if (c == 'T')
            {
                pos++;
                readUntil(';');
                expect(';');
                return;
            }
            if (c == 'L')
            {
                pos++;
                skipClassType();
                return;
            }
            pos++;
        }

        private void skipClassType()
        {
            while (pos < signature.length())
            {
                char c = signature.charAt(pos);
                if (c == '<')
                {
                    pos++;
                    while (peek() != '>')
                    {
                        skipType();
                    }
                    pos++;
                }
                else if (c == ';')
                {
                    pos++;
                    return;
                }
                else
                {
                    pos++;
                }
            }
        }
    }
}
