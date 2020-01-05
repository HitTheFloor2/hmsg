package manager;

import protobuf.SimpleStringMessageProto;

import java.util.concurrent.CountDownLatch;

public class MessageManager {
    public static volatile CountDownLatch countDownLatch = null;
    public static synchronized void getCountDownLatch(int size){
        MessageManager.countDownLatch = new CountDownLatch(size);
    }
    public static synchronized void dropCountDownLatch(){
        MessageManager.countDownLatch = null;
    }
    public static synchronized void downCountDownLatch(){
        if(MessageManager.countDownLatch == null){
            return;
        }
        MessageManager.countDownLatch.countDown();

        return;
    }

    public void handleMsg(SimpleStringMessageProto.SimpleStringMessage simpleStringMessage){
        if(simpleStringMessage.getLength() == 1){
            downCountDownLatch();
        }
    }
}
