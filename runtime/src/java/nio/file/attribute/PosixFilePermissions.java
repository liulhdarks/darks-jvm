package java.nio.file.attribute;

import java.util.Set;

public final class PosixFilePermissions
{
    private PosixFilePermissions()
    {
    }

    public static Set<PosixFilePermission> fromString(String value)
    {
        java.util.EnumSet<PosixFilePermission> permissions = java.util.EnumSet.noneOf(PosixFilePermission.class);
        if (value == null || value.length() != 9)
        {
            throw new IllegalArgumentException("Invalid mode: " + value);
        }
        PosixFilePermission[] values = PosixFilePermission.values();
        for (int i = 0; i < 9; i++)
        {
            if (value.charAt(i) != '-')
            {
                permissions.add(values[i]);
            }
        }
        return permissions;
    }

    public static String toString(Set<PosixFilePermission> permissions)
    {
        StringBuilder builder = new StringBuilder(9);
        PosixFilePermission[] values = PosixFilePermission.values();
        for (int i = 0; i < values.length; i++)
        {
            builder.append(permissions.contains(values[i]) ? "rwx".charAt(i % 3) : '-');
        }
        return builder.toString();
    }

    public static FileAttribute<Set<PosixFilePermission>> asFileAttribute(Set<PosixFilePermission> permissions)
    {
        if (permissions == null)
        {
            throw new NullPointerException("permissions");
        }
        final Set<PosixFilePermission> copy = new java.util.HashSet<PosixFilePermission>();
        java.util.Iterator<PosixFilePermission> iterator = permissions.iterator();
        while (iterator.hasNext())
        {
            copy.add(iterator.next());
        }
        return new FileAttribute<Set<PosixFilePermission>>()
        {
            public String name()
            {
                return "posix:permissions";
            }

            public Set<PosixFilePermission> value()
            {
                return copy;
            }
        };
    }
}
