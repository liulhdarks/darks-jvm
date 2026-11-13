import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

// Command-line todo list persisted in a text file.  The JNI helper loaded via
// java.library.path computes a small checksum so the example also proves
// System.loadLibrary search-path handling.
public class TodoList
{
    static native int add(int a, int b);

    public static void main(String[] args) throws Exception
    {
        System.loadLibrary("e2eadd");
        String file = System.getProperty("todo.file", "todo.txt");
        ArrayList<String> tasks = load(file);
        String command = args.length > 0 ? args[0] : "list";
        if (command.equals("add") && args.length > 1)
        {
            tasks.add(args[1]);
            save(file, tasks);
            System.out.println("added=" + args[1]);
        }
        else if (command.equals("remove") && args.length > 1)
        {
            int index = Integer.parseInt(args[1]);
            if (index >= 0 && index < tasks.size())
            {
                System.out.println("removed=" + tasks.remove(index));
                save(file, tasks);
            }
            else
            {
                System.out.println("removed=<none>");
            }
        }
        else if (command.equals("clear"))
        {
            tasks.clear();
            save(file, tasks);
        }
        else
        {
            for (int i = 0; i < tasks.size(); i++)
            {
                System.out.println("task" + i + "=" + tasks.get(i));
            }
        }
        System.out.println("count=" + tasks.size());
        System.out.println("native=" + add(tasks.size(), 1));
    }

    static ArrayList<String> load(String path) throws Exception
    {
        ArrayList<String> tasks = new ArrayList<String>();
        File file = new File(path);
        if (!file.exists())
        {
            return tasks;
        }
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(file)));
        String line;
        while ((line = reader.readLine()) != null)
        {
            if (!line.isEmpty())
            {
                tasks.add(line);
            }
        }
        reader.close();
        return tasks;
    }

    static void save(String path, ArrayList<String> tasks) throws Exception
    {
        PrintWriter writer = new PrintWriter(new FileWriter(path));
        for (int i = 0; i < tasks.size(); i++)
        {
            writer.println(tasks.get(i));
        }
        writer.close();
    }
}
