package executortest;


import java.util.concurrent.*;
import static executortest.Runner.Mode.Dedicated;
import static executortest.Runner.Mode.FixedThread;
import static executortest.Runner.Mode.WorkStealing;

/**
 * Created by moelrue on 10/13/14.
 */
public class Runner {

    enum Mode {
        WorkStealing,
        FixedThread,
        Dedicated
    }
    final int MAX_WORKER = 6;

    Work workers[] = new Work[MAX_WORKER*15]; // 15 actors per thread

    ExecutorService ex;

    ExecutorService threads[] = new ExecutorService[MAX_WORKER];
    Mode mode = Mode.Dedicated;

    public Runner(Mode mode) {
        this.mode = mode;
    }

    public Runner init(int localSize) {
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Work(localSize);
            if ( mode == Mode.Dedicated && i < MAX_WORKER ) {
                threads[i] = Executors.newSingleThreadScheduledExecutor();
            }
        }
        switch (mode) {
            case WorkStealing:
                ex = Executors.newWorkStealingPool(MAX_WORKER); break;
            case FixedThread:
                ex  = Executors.newFixedThreadPool(MAX_WORKER); break;
        }
        return this;
    }

    public long run(int iter) throws InterruptedException {
        long tim = System.currentTimeMillis();
        for ( int i = 0; i < iter; i++ ) {
            final int finalI = i;
            if ( mode == Mode.Dedicated ) {
                threads[finalI % MAX_WORKER].execute(() -> workers[finalI % workers.length].doWork(500) );
            } else {
                ex.execute( () -> workers[finalI % workers.length].doWork(500) );
            }
        }
        awaitTermination();
        long dur = System.currentTimeMillis()-tim;
//        System.out.println(mode+" Time "+dur);
        return dur;
    }

    private void awaitTermination() throws InterruptedException {
        if (mode == Dedicated) {
            CountDownLatch latch = new CountDownLatch(MAX_WORKER);
            for (int i = 0; i < threads.length; i++) {
                ExecutorService thread = threads[i];
                thread.execute(() -> latch.countDown());
            }
            latch.await();
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            ex.execute( () -> latch.countDown() );
            latch.await();
        }
    }

    void shutdown() throws InterruptedException {
        if ( mode == Mode.Dedicated ) {
            for (int i = 0; i < threads.length; i++) {
                ExecutorService thread = threads[i];
                thread.shutdown();
                thread.awaitTermination(10L, TimeUnit.SECONDS);
            }
        } else {
            ex.shutdown();
            ex.awaitTermination(10L, TimeUnit.SECONDS);
        }
    }

    private static long avgTest(Mode mode, int localSize ) throws InterruptedException {
        long sum = 0;
        Runner runner = new Runner(mode).init(localSize);
        int iters = 1;
        for (int i = 0; i < iters; i++) {
            sum += runner.run(1000 * 400);
//            Thread.sleep(1000);
        }
//        System.out.println();
        System.out.println("*** "+mode + " average "+sum/iters+"  localSize "+localSize*4);
//        System.out.println();
        runner.shutdown();
        return sum/iters;
    }

    public static void main(String arg[]) throws InterruptedException {
        int sizes[] = { 4, 32, 500, 1000, 8000, 80000, 800000 };
        long durations[][] = new long[sizes.length][];
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            for ( int ii = 0; ii < 2; ii++ ) {
                System.out.println("warmup =>");
                avgTest(Dedicated, size);
                avgTest(FixedThread, size);
                avgTest(WorkStealing, size);
            }
            durations[i] =  new long[3];
            int numRuns = 3;
            for ( int ii = 0; ii < numRuns; ii++ ) {
                System.out.println("run => "+ii);
                durations[i][Dedicated.ordinal()] += avgTest(Dedicated, size);
                durations[i][FixedThread.ordinal()] += avgTest(FixedThread, size);
                durations[i][WorkStealing.ordinal()] += avgTest(WorkStealing, size);
            }
            for (int j = 0; j < durations[i].length; j++) {
                durations[i][j] /= numRuns;
            }
        }
        System.out.println("Final results **************");
        for (int i = 0; i < durations.length; i++) {
            long[] duration = durations[i];
            for (int j = 0; j < 3; j++) {
                System.out.println("local state bytes: "+sizes[i]*4+" "+Mode.values()[j]+" avg:"+duration[j]);

            }
        }
    }

}
