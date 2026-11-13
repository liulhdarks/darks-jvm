import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

// Reads a text file and reports line, word and character counts plus the
// most frequent words.  -Dwordcount.top=N changes how many words are printed.
public class WordCount
{
    public static void main(String[] args) throws Exception
    {
        if (args.length != 1)
        {
            System.out.println("usage: WordCount <file>");
            return;
        }
        int top = Integer.parseInt(System.getProperty("wordcount.top", "3"));
        HashMap<String, Integer> counts = new HashMap<String, Integer>();
        int lines = 0;
        int words = 0;
        int chars = 0;
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(args[0])));
        String line;
        while ((line = reader.readLine()) != null)
        {
            lines++;
            chars += line.length();
            int start = -1;
            for (int i = 0; i <= line.length(); i++)
            {
                boolean space = i == line.length() || line.charAt(i) == ' ' ||
                                line.charAt(i) == '\t';
                if (!space && start < 0)
                {
                    start = i;
                }
                else if (space && start >= 0)
                {
                    String word = line.substring(start, i).toLowerCase();
                    words++;
                    Integer old = counts.get(word);
                    counts.put(word, Integer.valueOf(old == null ? 1 : old.intValue() + 1));
                    start = -1;
                }
            }
        }
        reader.close();

        ArrayList<String> keys = new ArrayList<String>();
        java.util.Iterator<String> it = counts.keySet().iterator();
        while (it.hasNext())
        {
            keys.add(it.next());
        }
        // Insertion sort: count descending, then word ascending.
        for (int i = 1; i < keys.size(); i++)
        {
            String value = keys.get(i);
            int j = i - 1;
            while (j >= 0 && before(value, keys.get(j), counts))
            {
                keys.set(j + 1, keys.get(j));
                j--;
            }
            keys.set(j + 1, value);
        }
        StringBuilder topLine = new StringBuilder();
        int limit = top < keys.size() ? top : keys.size();
        for (int i = 0; i < limit; i++)
        {
            if (i > 0)
            {
                topLine.append(",");
            }
            String word = keys.get(i);
            topLine.append(word).append(":").append(counts.get(word).intValue());
        }

        System.out.println("lines=" + lines);
        System.out.println("words=" + words);
        System.out.println("chars=" + chars);
        System.out.println("top=" + topLine.toString());
    }

    static boolean before(String a, String b, HashMap<String, Integer> counts)
    {
        int ca = counts.get(a).intValue();
        int cb = counts.get(b).intValue();
        if (ca != cb)
        {
            return ca > cb;
        }
        return a.compareTo(b) < 0;
    }
}
