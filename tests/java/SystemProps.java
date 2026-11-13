public class SystemProps
{
    public static void main(String[] args)
    {
        String[] required = {
            "java.version", "java.vendor", "java.home", "java.class.path",
            "java.library.path", "os.name", "os.arch", "file.separator",
            "path.separator", "line.separator", "user.dir", "user.home",
            "user.name", "java.io.tmpdir", "java.vm.name", "java.vm.version"
        };
        int missing = 0;
        for (int i = 0; i < required.length; i++)
        {
            if (System.getProperty(required[i]) == null)
            {
                missing++;
                System.out.println("missing=" + required[i]);
            }
        }
        System.out.println("props-required=" + (missing == 0 ? "ok" : "missing:" + missing));
        System.out.println("props-flag=" + System.getProperty("compat.flag", "unset"));
        System.out.println("props-vendor=" + System.getProperty("java.vendor"));
        System.out.println("props-tmpdir-present=" + (System.getProperty("java.io.tmpdir").length() > 0));
        if (missing != 0)
        {
            System.exit(1);
        }
    }
}
