import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SdkRegexTest
{
    static int checks = 0;
    static int failures = 0;

    static void check(String name, boolean ok)
    {
        checks = checks + 1;
        if (!ok)
        {
            failures = failures + 1;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args)
    {
        Matcher matcher = Pattern.compile("a+b").matcher("xxaaabyy");
        check("find", matcher.find());
        check("find-start", matcher.start() == 2 && matcher.end() == 6);
        check("find-group", matcher.group().equals("aaab"));
        check("find-next", !matcher.find());

        check("matches-true", Pattern.compile("\\d+").matcher("123").matches());
        check("matches-false", !Pattern.compile("\\d+").matcher("12a").matches());
        check("lookingAt",
              Pattern.compile("[a-z]+").matcher("abc123").lookingAt());
        check("static-matches", Pattern.matches("[a-z]+\\d+", "abc123"));

        Matcher groups = Pattern.compile("(\\d+)-(\\w+)").matcher("id=123-abc;");
        check("groups-found", groups.find());
        check("groups-count", groups.groupCount() == 2);
        check("groups-one", groups.group(1).equals("123"));
        check("groups-two", groups.group(2).equals("abc"));
        check("groups-zero", groups.group(0).equals("123-abc"));
        check("groups-start", groups.start(1) == 3 && groups.end(2) == 10);

        check("alternation",
              Pattern.compile("cat|dog").matcher("hotdog").find());

        Matcher counter = Pattern.compile("\\d+").matcher("a1b22c333");
        int found = 0;
        while (counter.find())
        {
            found = found + 1;
        }
        check("find-loop", found == 3);

        check("replaceAll",
              Pattern.compile("\\d+").matcher("a1b22c333").replaceAll("#")
                  .equals("a#b#c#"));
        check("replaceFirst",
              Pattern.compile("\\d+").matcher("a1b22c333").replaceFirst("#")
                  .equals("a#b22c333"));
        check("replaceGroups",
              Pattern.compile("(\\w+)@(\\w+)").matcher("user@host")
                  .replaceAll("$2:$1").equals("host:user"));
        check("quoteReplacement",
              Pattern.compile("x").matcher("x").replaceAll(
                  Matcher.quoteReplacement("$1\\")).equals("$1\\"));

        String[] parts = Pattern.compile("[,;]").split("a,b;c");
        check("pattern-split",
              parts.length == 3 && parts[1].equals("b") && parts[2].equals("c"));

        check("string-matches", "abc123".matches("[a-z]+\\d+"));
        check("string-matches-false", !"abc".matches("\\d+"));

        String[] simple = "a,b,,c".split(",");
        check("string-split", simple.length == 4 && simple[2].isEmpty());
        String[] dotted = "a.b.c".split("\\.");
        check("string-split-regex", dotted.length == 3);
        String[] limited = "a,b,c".split(",", 2);
        check("string-split-limit",
              limited.length == 2 && limited[1].equals("b,c"));
        String[] characters = "abc".split("");
        check("string-split-empty", characters.length == 3 &&
                                    characters[1].equals("b"));
        String[] trailing = "a,b,,".split(",");
        check("string-split-trailing", trailing.length == 2);

        check("case-insensitive",
              Pattern.compile("hello", Pattern.CASE_INSENSITIVE)
                  .matcher("HeLLo").matches());
        check("dotall",
              Pattern.compile("a.b", Pattern.DOTALL).matcher("a\nb").matches());
        check("literal",
              Pattern.compile("a.b", Pattern.LITERAL).matcher("a.b").matches());

        check("quote",
              Pattern.compile(Pattern.quote("a.b")).matcher("a.b").matches());
        check("quote-negative",
              !Pattern.compile(Pattern.quote("a.b")).matcher("axb").matches());

        boolean syntaxCaught = false;
        try
        {
            Pattern.compile("([");
        }
        catch (PatternSyntaxException expected)
        {
            syntaxCaught = true;
        }
        check("syntax-error", syntaxCaught);

        System.out.println("regex checks=" + checks + " failures=" + failures);
        System.out.println("sdk regex: ok");
    }
}
