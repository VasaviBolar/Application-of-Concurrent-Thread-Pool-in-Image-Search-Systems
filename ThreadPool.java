import java.util.LinkedList;
import java.util.List;

public class ThreadPool {

    private final int numThreads;
    private final List<Worker> workers;
    private final LinkedList<Runnable> taskQueue;
    private boolean isStopped = false;

    public ThreadPool(int numThreads) {
        this.numThreads = numThreads;
        this.taskQueue = new LinkedList<>();
        this.workers = new LinkedList<>();

        // Create worker threads
        for (int i = 0; i < numThreads; i++) {
            Worker worker = new Worker("Worker-" + i);
            workers.add(worker);
            worker.start();
        }
    }

    // Submit a task (image upload / search query)
    public void submit(Runnable task) {
        synchronized (taskQueue) {
            if (isStopped) {
                throw new IllegalStateException("ThreadPool is stopped");
            }
            taskQueue.addLast(task);
            taskQueue.notify(); // wake up one worker
        }
    }

    // Graceful shutdown
    public void shutdown() {
        synchronized (taskQueue) {
            isStopped = true;
            taskQueue.notifyAll();
        }
    }

    private class Worker extends Thread {

        public Worker(String name) {
            super(name);
        }

        public void run() {
            while (true) {
                Runnable task;

                synchronized (taskQueue) {
                    while (taskQueue.isEmpty() && !isStopped) {
                        try {
                            taskQueue.wait(); // sleep
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    if (isStopped && taskQueue.isEmpty()) {
                        break;
                    }

                    task = taskQueue.removeFirst();
                }

                try {
                    task.run();
                } catch (Exception e) {
                    System.out.println("Error executing task: " + e.getMessage());
                }
            }
        }
    }
}