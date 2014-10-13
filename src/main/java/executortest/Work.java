package executortest;

import java.util.Random;

/**
 * Created by moelrue on 10/13/14.
 */
public class Work {

    public Random rand = new Random(13);

    int localState[];

    public Work(int localSize ) {
        this.localState = new int[localSize];
    }

    public int doWork(int iterations) {
        int sum = 0;
        for ( int i = 0; i < iterations; i++ ) {
            int index = rand.nextInt(localState.length);
            sum += localState[index];
            localState[index] = i;
        }
        return sum;
    }

}