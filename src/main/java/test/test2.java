package test;

import java.util.concurrent.*;

public class test2 {
    public static void main(String[] args) {
        FutureTask ft1 = new FutureTask(new Callable() {
            @Override
            public Object call() throws Exception {
                Thread.sleep(8000);
                return "ft1";
            }
        });
        FutureTask ft2 = new FutureTask(new Callable() {
            @Override
            public Object call() throws Exception {
                Thread.sleep(3000); 
                return "ft2";
            }
        });
        ExecutorService executorService = Executors.newCachedThreadPool();
        executorService.submit(ft1);
        executorService.submit(ft2);
        try {
            System.out.println(ft1.get().toString());
            System.out.println(ft2.get().toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
}
