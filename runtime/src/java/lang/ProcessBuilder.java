package java.lang;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProcessBuilder
{
    private List<String> command;
    private File directory;
    private boolean redirectErrorStream;
    private final Map<String, String> environment;

    public ProcessBuilder(String... command)
    {
        this(Arrays.asList(command));
    }

    public ProcessBuilder(List<String> command)
    {
        if (command == null)
        {
            throw new NullPointerException("command");
        }
        this.command = new ArrayList<String>(command);
        this.environment = new HashMap<String, String>();
    }

    public ProcessBuilder command(String... command)
    {
        this.command = new ArrayList<String>(Arrays.asList(command));
        return this;
    }

    public ProcessBuilder command(List<String> command)
    {
        if (command == null)
        {
            throw new NullPointerException("command");
        }
        this.command = new ArrayList<String>(command);
        return this;
    }

    public List<String> command()
    {
        return new ArrayList<String>(command);
    }

    public File directory()
    {
        return directory;
    }

    public ProcessBuilder directory(File directory)
    {
        this.directory = directory;
        return this;
    }

    public Map<String, String> environment()
    {
        return environment;
    }

    public boolean redirectErrorStream()
    {
        return redirectErrorStream;
    }

    public ProcessBuilder redirectErrorStream(boolean redirectErrorStream)
    {
        this.redirectErrorStream = redirectErrorStream;
        return this;
    }

    public ProcessBuilder inheritIO()
    {
        return this;
    }

    public Process start() throws IOException
    {
        throw new IOException("process creation is not supported");
    }
}
