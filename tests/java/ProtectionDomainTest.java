import java.net.URL;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;

public class ProtectionDomainTest
{
    private static int failures;

    private static void check(String name, boolean value)
    {
        if (!value)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args) throws Exception
    {
        URL location = new URL("file:/tmp/app/");
        CodeSource source = new CodeSource(location, null);
        Permission[] permissions = new Permission[] {new RuntimePermission("exitVM")};
        ProtectionDomain domain = new ProtectionDomain(source, permissions);
        check("code-source", domain.getCodeSource() == source);
        PermissionCollection collection = domain.getPermissions();
        check("permission-count", collection.toArray().length == 1);
        check("implies", domain.implies(new RuntimePermission("exitVM")));
        check("not-implies", !domain.implies(new RuntimePermission("setSecurityManager")));

        ProtectionDomain empty = new ProtectionDomain(null, (Permission[])null);
        check("empty-source", empty.getCodeSource() == null);
        check("empty-permissions", empty.getPermissions().toArray().length == 0);
        check("empty-implies", !empty.implies(new RuntimePermission("exitVM")));

        ProtectionDomain own = ProtectionDomainTest.class.getProtectionDomain();
        check("class-domain", own != null);
        check("class-location", own != null && own.getCodeSource() != null
            && own.getCodeSource().getLocation() != null);
        check("primitive-domain", int.class.getProtectionDomain() == null);

        if (failures == 0)
        {
            System.out.println("protectiondomain: ok");
        }
        else
        {
            System.out.println("protectiondomain failures=" + failures);
            System.exit(1);
        }
    }
}
