package java.util;

public class Timer
{
    private final ArrayList<TimerTask> tasks = new ArrayList<TimerTask>();
    private final Thread thread;
    private boolean running = true;

    public Timer()
    {
        this(false);
    }

    public Timer(String name)
    {
        this(name, false);
    }

    public Timer(boolean isDaemon)
    {
        this("Timer", isDaemon);
    }

    public Timer(String name, boolean isDaemon)
    {
        thread = new Thread(new TimerRunnable(this), name);
        thread.setDaemon(isDaemon);
        thread.start();
    }

    private static class TimerRunnable implements Runnable
    {
        private final Timer timer;

        TimerRunnable(Timer timer)
        {
            this.timer = timer;
        }

        public void run()
        {
            timer.runLoop();
        }
    }

    private void runLoop()
    {
        while (true)
        {
            TimerTask due = null;
            long now = System.currentTimeMillis();
            synchronized (tasks)
            {
                if (!running)
                {
                    return;
                }
                for (int i = 0; i < tasks.size(); i++)
                {
                    TimerTask task = tasks.get(i);
                    if (task.cancelled)
                    {
                        tasks.remove(i--);
                        continue;
                    }
                    if (task.nextTime <= now)
                    {
                        due = task;
                        if (task.period > 0)
                        {
                            if (task.fixedRate)
                            {
                                task.nextTime += task.period;
                            }
                            else
                            {
                                task.nextTime = now + task.period;
                            }
                        }
                        else
                        {
                            task.scheduled = false;
                            tasks.remove(i);
                        }
                        break;
                    }
                }
            }
            if (due == null)
            {
                try
                {
                    Thread.sleep(1);
                }
                catch (InterruptedException ignored)
                {
                }
                continue;
            }
            try
            {
                due.run();
            }
            catch (RuntimeException ignored)
            {
            }
        }
    }

    public void schedule(TimerTask task, long delay)
    {
        schedule(task, delay, 0);
    }

    public void schedule(TimerTask task, Date time)
    {
        schedule(task, time.getTime() - System.currentTimeMillis(), 0);
    }

    public void schedule(TimerTask task, long delay, long period)
    {
        enqueue(task, System.currentTimeMillis() + delay, period, false);
    }

    public void schedule(TimerTask task, Date firstTime, long period)
    {
        enqueue(task, firstTime.getTime(), period, false);
    }

    public void scheduleAtFixedRate(TimerTask task, long delay, long period)
    {
        enqueue(task, System.currentTimeMillis() + delay, period, true);
    }

    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period)
    {
        enqueue(task, firstTime.getTime(), period, true);
    }

    private void enqueue(TimerTask task, long firstTime, long period, boolean fixedRate)
    {
        if (task == null)
        {
            throw new NullPointerException("task");
        }
        if (period < 0)
        {
            throw new IllegalArgumentException("negative period");
        }
        synchronized (tasks)
        {
            if (!running)
            {
                throw new IllegalStateException("Timer already cancelled");
            }
            if (task.scheduled)
            {
                throw new IllegalStateException("Task already scheduled or cancelled");
            }
            task.nextTime = firstTime;
            task.period = period;
            task.fixedRate = fixedRate;
            task.cancelled = false;
            task.scheduled = true;
            tasks.add(task);
        }
    }

    public void cancel()
    {
        synchronized (tasks)
        {
            running = false;
            tasks.clear();
        }
    }

    public int purge()
    {
        int removed = 0;
        synchronized (tasks)
        {
            for (int i = tasks.size() - 1; i >= 0; i--)
            {
                if (tasks.get(i).cancelled)
                {
                    tasks.remove(i);
                    removed++;
                }
            }
        }
        return removed;
    }
}
