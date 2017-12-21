import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @ClassName MyThread
 * @Description
 * @Author colin_xun@163.com
 * @CreateTime 2017/12/20 下午2:35
 */
public class MyThread {

    private final int WORKSIZE;
    private List<Work> works = Collections.synchronizedList(new ArrayList<Work>());
    private LinkedList<Runnable> jobs = new LinkedList<>();

    public static void main(String[] args) {
        MyThread pool = new MyThread(4);
        AtomicLong count1 = new AtomicLong();
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    count1.getAndIncrement();
                }
            });
        }
        System.out.println(System.currentTimeMillis()-begin);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(count1.get());
    }

    public MyThread(int size) {
        WORKSIZE = size;
        initWorkThread();
    }

    /**
     * 初始化了size个线程列表，并分别启动，但此时池里面job的个数为空，所以jobs处于等待状态
     */
    private void initWorkThread() {
        for (int i = 0; i < WORKSIZE; i++) {
            Work work = new Work();
            works.add(work);
            new Thread(work,"workThread-"+i).start();
        }
    }

    public void execute (Runnable job) {
        synchronized (jobs) {
            jobs.add(job);
            jobs.notify();
        }
    }

    class Work implements Runnable {

        //volatile的好处是低消耗的线程同步思路，每个线程对这个变量都有一个副本
        private volatile boolean isRun = true;

        @Override
        public void run() {
            while (isRun) {
                Runnable job = null;
                synchronized (jobs) {
                    while (jobs.isEmpty()) {
                        try {
                            jobs.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    job=jobs.removeFirst();
                }
                if (job != null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    job.run();
                }
            }
        }

        public void shutdown() {
            isRun = false;
        }
    }
}
