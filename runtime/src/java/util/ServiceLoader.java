package java.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public final class ServiceLoader<S> implements Iterable<S>
{
    private final Class<S> service;
    private final ClassLoader loader;
    private ArrayList<String> providers;

    private ServiceLoader(Class<S> service, ClassLoader loader)
    {
        this.service = service;
        this.loader = loader;
    }

    public static <S> ServiceLoader<S> load(Class<S> service)
    {
        return load(service, Thread.currentThread().getContextClassLoader());
    }

    public static <S> ServiceLoader<S> load(Class<S> service, ClassLoader loader)
    {
        if (service == null)
        {
            throw new NullPointerException("service");
        }
        ClassLoader effective = loader != null ? loader : ClassLoader.getSystemClassLoader();
        return new ServiceLoader<S>(service, effective);
    }

    public static <S> ServiceLoader<S> loadInstalled(Class<S> service)
    {
        return load(service, ClassLoader.getSystemClassLoader());
    }

    public Iterator<S> iterator()
    {
        loadProviders();
        return new Iterator<S>()
        {
            private int index;

            public boolean hasNext()
            {
                return index < providers.size();
            }

            public S next()
            {
                if (index >= providers.size())
                {
                    throw new NoSuchElementException();
                }
                String name = providers.get(index++);
                return instantiate(name);
            }

            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    private void loadProviders()
    {
        if (providers != null)
        {
            return;
        }
        providers = new ArrayList<String>();
        String resource = "META-INF/services/" + service.getName();
        try
        {
            Enumeration<URL> urls = loader.getResources(resource);
            while (urls.hasMoreElements())
            {
                readProviderFile(urls.nextElement());
            }
        }
        catch (Exception error)
        {
            throw new ServiceConfigurationError("Failed reading " + resource, error);
        }
    }

    private void readProviderFile(URL url)
    {
        InputStream input = null;
        try
        {
            input = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String line;
            while ((line = reader.readLine()) != null)
            {
                int comment = line.indexOf('#');
                if (comment >= 0)
                {
                    line = line.substring(0, comment);
                }
                line = line.trim();
                if (line.length() > 0 && !providers.contains(line))
                {
                    providers.add(line);
                }
            }
        }
        catch (Exception error)
        {
            throw new ServiceConfigurationError("Failed reading " + url, error);
        }
        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (Exception ignored)
                {
                }
            }
        }
    }

    private S instantiate(String className)
    {
        try
        {
            Class<?> clazz = Class.forName(className, false, loader);
            if (!service.isAssignableFrom(clazz))
            {
                throw new ServiceConfigurationError(className + " is not a " + service.getName());
            }
            java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor();
            return (S)constructor.newInstance();
        }
        catch (ServiceConfigurationError error)
        {
            throw error;
        }
        catch (Throwable error)
        {
            throw new ServiceConfigurationError("Failed to create " + className, error);
        }
    }
}
