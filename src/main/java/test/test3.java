package test;

import io.netty.util.concurrent.CompleteFuture;
import io.netty.util.concurrent.FutureListener;

import java.util.concurrent.*;

public class test3 {
    public static void main (String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        FutureTask<Integer> futureTask1 = new FutureTask<Integer>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                System.out.println("returning...");
                return 1;
            }
        });

        futureTask1.run();
        try {
            futureTask1.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        //executorService.submit(futureTask1);
        //futureTask1.get();


    }
}
