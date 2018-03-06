import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @ClassName DemoThreadPool
 * @Description
 * @Author colin_xun@163.com
 * @CreateTime 2018/3/6 上午10:30
 */
public class DemoThreadPool extends Thread {

    // 线程状态，空闲，运行中，阻塞，结束
    private enum WorkerState {FREE, RUNNABLE, BLOCKED, TERMINATED;}
    // 线程池是否被销毁
    private volatile boolean destroy = false;
    // 核心线程数
    private final static int CORE_THREAD_SIZE = 2;
    // 最大线程数
    private final static int MAX_THREAD_SIZE = 10;
    // 任务队列容器,也可以用Queue<Runnable> 遵循 FIFO 规则
    private final static LinkedList<Runnable> TASK_QUEUE = new LinkedList<>();
    // 设置它的边界值为11
    private final static int TASK_QUEUE_MAX_SIZE = 11;
    // 线程容器
    private final static List<Worker> WORKERS = new ArrayList<>();

    ///////////////////////////////////////////////////////////////

    // 对象创建的时候我们来开启我们自身的线程
    public DemoThreadPool() {
        this.start();
    }

    /**
     * 提交新的任务
     */
    void submit(Runnable runnable) {
        if (destroy) {
            System.out.println("线程池已经销毁了，抛出异常！这里用日志代替");
            return;
        }
        synchronized (TASK_QUEUE) {
            if (WORKERS.size() < CORE_THREAD_SIZE) {
                System.out.println("核心线程数还没有满，直接创建线程");
                createWorkerTask();
            } else if (TASK_QUEUE.size() < TASK_QUEUE_MAX_SIZE) {
                System.out.println("核心线程数已满，但是队列没有满，把任务加到队列");
            } else if (WORKERS.size() < MAX_THREAD_SIZE) {
                System.out.println("核心线程数,队列满了，但是没有达到最大线程数，我们接着创建线程");
                createWorkerTask();
            } else if (WORKERS.size() >= MAX_THREAD_SIZE) {
                System.out.println("达到了最大线程数，应该执行拒绝策略，这里我们就直接返回了");
                return;
            } else {
                System.out.println("情况未知");
            }
            //加入任务队列
            TASK_QUEUE.addLast(runnable);
            // 唤醒所有线程
            TASK_QUEUE.notifyAll();
        }
    }

    /**
     * 关闭一个线程
     */
    void shutdown() {
        int size = WORKERS.size();
        while (size > 0) {
            for (Worker worker : WORKERS) {
                if (worker.workerState == WorkerState.BLOCKED) {
                    worker.close();
                    size--;
                }
            }
        }
        this.destroy = true;
        TASK_QUEUE.clear();
        WORKERS.clear();
        System.out.println("线程池已经关闭！");
    }

    /**
     *  创建一个新的线程
     */
    private void createWorkerTask() {
        Worker worker = new Worker();
        worker.workerState = WorkerState.FREE;
        WORKERS.add(worker);
        worker.start();
    }

    /**
     * @describe 判断线程池是否销毁，没销毁的话每3秒做一次线程回收，遍历所有的线程，如果线程处于BLOCKED
     *           证明当前队列为空了
     */
    @Override
    public void run() {
        while (!destroy) {
            try {
                Thread.sleep(3000);
                synchronized (WORKERS) {
                    Iterator<Worker> iterator = WORKERS.iterator();
                    while (iterator.hasNext()) {
                        Worker worker = iterator.next();
                        if (WORKERS.size() > CORE_THREAD_SIZE && TASK_QUEUE.size() < TASK_QUEUE_MAX_SIZE) {
                            if (worker.workerState != WorkerState.RUNNABLE && worker.workerState != WorkerState.TERMINATED) {
                                worker.close();
                                iterator.remove();
                                System.out.println("[回收了一个线程]");
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Worker extends Thread {
        private WorkerState workerState;
        // 线程编号
        private static int threadInitNumber;
        // 生成线程名
        private static synchronized String nextThreadName() {
            return "Thread-" + (++threadInitNumber);
        }
        Worker() {
            nextThreadName();
        }

        @Override
        public void run() {
            System.out.println("开启了一个新的线程------>" + threadInitNumber);
            Runnable targer;
            OUTER:
            while (this.workerState != WorkerState.TERMINATED) {
                synchronized (TASK_QUEUE) {
                    while (this.workerState == WorkerState.FREE && TASK_QUEUE.isEmpty()) {
                        try {
                            this.workerState = WorkerState.BLOCKED;
                            TASK_QUEUE.wait();
                        } catch (InterruptedException e) {
                            break OUTER;
                        }
                    }
                    targer = TASK_QUEUE.removeFirst();
                    System.out.println("处理任务");
                }
                if (targer != null) {
                    this.workerState = WorkerState.RUNNABLE;
                    targer.run();
                    this.workerState = WorkerState.FREE;
                }
            }
        }

        void close() {
            this.workerState = WorkerState.TERMINATED;
            this.interrupt();
        }

    }

    public static void main(String[] args) throws InterruptedException {
        DemoThreadPool demoThreadPool = new DemoThreadPool();
        IntStream.range(0, 5).forEach(i ->
                demoThreadPool.submit(() -> {
                    System.out.printf("[线程] - [%s] 开始工作...\n", Thread.currentThread().getName());
                    try {
                        Thread.sleep(2_000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.printf("[线程] - [%s] 工作完毕...\n", Thread.currentThread().getName());
                })
        );

        Thread.sleep(3000);

        IntStream.range(0, 30).forEach(i ->
                demoThreadPool.submit(() -> {
                    System.out.printf("[线程] - [%s] 开始工作...\n", Thread.currentThread().getName());
                    try {
                        Thread.sleep(2_000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.printf("[线程] - [%s] 工作完毕...\n", Thread.currentThread().getName());
                })
        );
        demoThreadPool.shutdown();
    }
}
