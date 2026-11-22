import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.ToLongFunction;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

// Coverage for the JDK surface added while bringing up the embedded Tomcat
// runtime: java.beans, charset/CharBuffer, string readers, date formatting,
// enum/sorted collections, SecureRandom, JMX, File.canRead and Security.
public class M3CompatTest
{
    private static int checks;
    private static int failures;

    private static void check(String name, boolean condition)
    {
        checks++;
        if (!condition)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    private enum Color { RED, GREEN, BLUE }

    private static class RecordingListener implements PropertyChangeListener
    {
        PropertyChangeEvent last;
        int count;

        public void propertyChange(PropertyChangeEvent event)
        {
            last = event;
            count++;
        }
    }

    public static void main(String[] args) throws Exception
    {
        propertyChangeSupport();
        charBuffers();
        ioCompat();
        dateFormatting();
        collections();
        randomSource();
        management();
        filesAndSecurity();
        System.out.println("m3 compat checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
        System.out.println("m3 compat: ok");
    }

    private static void propertyChangeSupport()
    {
        PropertyChangeSupport support = new PropertyChangeSupport(M3CompatTest.class);
        RecordingListener all = new RecordingListener();
        RecordingListener named = new RecordingListener();
        support.addPropertyChangeListener(all);
        support.addPropertyChangeListener("value", named);
        check("has listener", support.hasListeners("value"));

        support.firePropertyChange("value", 1, 2);
        check("all listener fired", all.count == 1);
        check("named listener fired", named.count == 1);
        check("property name", "value".equals(all.last.getPropertyName()));
        check("old value", Integer.valueOf(1).equals(all.last.getOldValue()));
        check("new value", Integer.valueOf(2).equals(all.last.getNewValue()));

        support.firePropertyChange("other", false, true);
        check("named filter", named.count == 1);
        check("all sees other", all.count == 2);

        support.removePropertyChangeListener("value", named);
        support.firePropertyChange("value", 3, 3);
        check("same value not fired", named.count == 1);
        check("removed listener", !support.hasListeners("value") || named.count == 1);
        check("listener array", support.getPropertyChangeListeners().length == 1);
    }

    private static void charBuffers() throws Exception
    {
        CharBuffer text = CharBuffer.wrap("hello");
        check("charbuffer length", text.length() == 5);
        check("charbuffer charAt", text.charAt(1) == 'e');
        check("charbuffer subSequence", "el".equals(text.subSequence(1, 3).toString()));

        CharBuffer target = CharBuffer.allocate(8);
        target.put("abcdef", 1, 4);
        target.flip();
        check("charbuffer put range", "bcd".equals(target.toString()));

        Charset utf8 = Charset.forName("UTF-8");
        check("charset supported", Charset.isSupported("UTF-8"));
        CharBuffer decoded = utf8.decode(ByteBuffer.wrap(new byte[] { 'h', 'i' }));
        check("charset decode", "hi".equals(decoded.toString()));
        check("charset encode", "ok".equals(new String(utf8.encode("ok"))));
        check("charset decode bytes", "abc".equals(utf8.decode(new byte[] { 'a', 'b', 'c' })));
    }

    private static void ioCompat() throws Exception
    {
        StringReader source = new StringReader("one\ntwo\nthree");
        BufferedReader reader = new BufferedReader(source, 8);
        check("readLine one", "one".equals(reader.readLine()));
        check("readLine two", "two".equals(reader.readLine()));
        check("readLine three", "three".equals(reader.readLine()));
        check("readLine end", reader.readLine() == null);

        StringReader reset = new StringReader("abcdef");
        check("mark supported", reset.markSupported());
        reset.mark(16);
        int first = reset.read();
        reset.read();
        reset.reset();
        check("reset", reset.read() == first);
        check("skip", reset.skip(2) == 2);
        check("ready", reset.ready());
        check("read after skip", reset.read() == 'd');

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(bytes, "UTF-8");
        writer.write("abc");
        writer.flush();
        check("outputstreamwriter", "abc".equals(bytes.toString()));
        check("baos size", bytes.size() == 3);

        char[] chars = new char[5];
        "hello".getChars(1, 3, chars, 1);
        check("string getChars", chars[1] == 'e' && chars[2] == 'l' && chars[0] == 0);

        System.setProperty("m3.int", "77");
        check("integer getInteger", Integer.getInteger("m3.int").intValue() == 77);
        check("integer getInteger default", Integer.getInteger("m3.absent", 42).intValue() == 42);
    }

    private static void dateFormatting()
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        check("epoch format", "1970-01-01 00:00:00".equals(format.format(new Date(0L))));
        check("one second format", "1970-01-01 00:00:01".equals(format.format(new Date(1000L))));

        SimpleDateFormat copy = (SimpleDateFormat)format.clone();
        check("clone pattern", copy.toPattern().equals(format.toPattern()));
        check("clone format", "1970-01-01 00:00:00".equals(copy.format(new Date(0L))));
        check("timezone offset", format.getTimeZone().getOffset(0L) == 0);
    }

    private static void collections()
    {
        EnumSet<Color> all = EnumSet.allOf(Color.class);
        check("enumset size", all.size() == 3);
        check("enumset contents", all.contains(Color.RED) && all.contains(Color.BLUE));
        all.remove(Color.GREEN);
        check("enumset remove", all.size() == 2 && !all.contains(Color.GREEN));

        ConcurrentSkipListSet<Integer> sorted = new ConcurrentSkipListSet<Integer>();
        sorted.add(Integer.valueOf(3));
        sorted.add(Integer.valueOf(1));
        sorted.add(Integer.valueOf(2));
        check("skiplist size", sorted.size() == 3);
        check("skiplist first", sorted.first().intValue() == 1);
        check("skiplist last", sorted.last().intValue() == 3);
        StringBuilder order = new StringBuilder();
        for (Integer value : sorted)
        {
            order.append(value.intValue());
        }
        check("skiplist order", "123".equals(order.toString()));

        Comparator<String> byLength = Comparator.comparingLong(new ToLongFunction<String>() {
            public long applyAsLong(String value)
            {
                return value.length();
            }
        });
        check("comparingLong less", byLength.compare("a", "bbb") < 0);
        check("comparingLong equal", byLength.compare("xx", "yy") == 0);

        Hashtable<String, String> table = new Hashtable<String, String>();
        table.put("a", "1");
        table.put("b", "2");
        check("hashtable get", "2".equals(table.get("b")));
        check("hashtable contains", table.containsKey("a") && table.size() == 2);

        Vector<String> vector = new Vector<String>();
        vector.add("x");
        vector.add("y");
        check("vector", vector.size() == 2 && "x".equals(vector.get(0)));
    }

    private static void randomSource() throws Exception
    {
        SecureRandom random = new SecureRandom();
        check("secure random algorithm", "SHA1PRNG".equals(random.getAlgorithm()));
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        check("secure random filled", bytes.length == 16);
        random.setSeed(1234L);
        byte[] seeded = new byte[8];
        random.nextBytes(seeded);
        check("secure random reseeded", seeded.length == 8);
        check("secure random factory", SecureRandom.getInstance("SHA1PRNG") != null);
    }

    private static void management() throws Exception
    {
        check("runtime uptime", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() >= 0L);
        check("thread count", java.lang.management.ManagementFactory.getThreadMXBean().getThreadCount() >= 1);
        check("platform server", java.lang.management.ManagementFactory.getPlatformMBeanServer() != null);

        MBeanServer server = MBeanServerFactory.createMBeanServer("m3compat");
        check("default domain", "m3compat".equals(server.getDefaultDomain()));
        check("initial count", server.getMBeanCount().intValue() == 0);

        ObjectName name = new ObjectName("m3compat:type=Probe");
        ObjectInstance instance = server.registerMBean(new Object(), name);
        check("registered", server.isRegistered(name));
        check("count after register", server.getMBeanCount().intValue() == 1);
        check("instance name", instance.getObjectName().equals(name));
        server.unregisterMBean(name);
        check("unregistered", !server.isRegistered(name));
        check("count after unregister", server.getMBeanCount().intValue() == 0);

        final int[] calls = new int[1];
        MBeanRegistration bean = new MBeanRegistration() {
            public ObjectName preRegister(MBeanServer owner, ObjectName objectName)
            {
                calls[0] += 1;
                return objectName;
            }

            public void postRegister(Boolean done)
            {
                calls[0] += 10;
            }

            public void preDeregister()
            {
                calls[0] += 100;
            }

            public void postDeregister()
            {
                calls[0] += 1000;
            }
        };
        ObjectName lifecycleName = new ObjectName("m3compat:type=Lifecycle");
        server.registerMBean(bean, lifecycleName);
        server.unregisterMBean(lifecycleName);
        check("registration callbacks", calls[0] == 1111);
        MBeanServerFactory.releaseMBeanServer(server);
    }

    private static void filesAndSecurity() throws Exception
    {
        check("cwd readable", new File(".").canRead());
        check("absent file not readable", !new File("m3-no-such-file-xyz").canRead());

        Security.setProperty("m3.sec", "v1");
        check("security get", "v1".equals(Security.getProperty("m3.sec")));
        check("security default", "fallback".equals(Security.getProperty("m3.absent", "fallback")));
        Security.setProperty("m3.num", "9");
        check("security integer", Security.getInteger("m3.num", 0) == 9);

        Locale locale = Locale.US;
        check("locale language", "en".equals(locale.getLanguage()));
        check("locale country", "US".equals(locale.getCountry()));
        check("default locale", Locale.getDefault() != null);

        PropertyResourceBundle bundle = new PropertyResourceBundle(
            new ByteArrayInputStream("a=1\nb=two\n".getBytes()));
        check("bundle value", "two".equals(bundle.getString("b")));
        check("bundle contains", bundle.containsKey("a"));

        Principal principal = new Principal() {
            public String getName()
            {
                return "alice";
            }

            public String toString()
            {
                return "alice";
            }
        };
        check("principal name", "alice".equals(principal.getName()));
        check("principal tostring", "alice".equals(principal.toString()));
    }
}
