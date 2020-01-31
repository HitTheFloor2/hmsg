package test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class test1 {
    public AtomicInteger a = new AtomicInteger(0);
    public int a2 = 0;
    public Integer a3 = 0;
    public test1(){

    }
    public static void main(String args[]){
        final test1 test = new test1();
        final CountDownLatch countDownLatch = new CountDownLatch(3);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0;i < 1000; i++){
                    test.a.getAndAdd(1);
                    test.a2++;
                    test.a3++;
                }
                countDownLatch.countDown();
            }
        });
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0;i < 1000; i++){
                    test.a.getAndAdd(1);
                    test.a2++;
                    test.a3++;
                }
                countDownLatch.countDown();
            }
        });
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0;i < 1000; i++){
                    test.a.getAndAdd(1);
                    test.a2++;
                    test.a3++;
                }
                countDownLatch.countDown();
            }
        });
        t.start();t1.start();t2.start();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(test.a.toString());
        System.out.println(test.a2);
        System.out.println(test.a3.toString());

    }

}
