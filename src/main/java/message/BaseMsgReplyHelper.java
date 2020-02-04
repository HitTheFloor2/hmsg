package message;

import log.Log;
import protobuf.BaseMsgProto;
import server.BaseServer;

import java.util.Hashtable;
import java.util.concurrent.*;

public class BaseMsgReplyHelper {
    /**
     * 投票等环节中存在消息发出后需要等待一个或多个消息回复的情况，
     * 初始消息（如发起投票的消息）发出后，在MessageManager中异步线程等待，
     * 并在BaseMsgReplyHelper中添加一个记录
     * MessageManager在receiveMsg中收到Msg后检查BaseMsgReplyHelper所有记录，找到对应的记录并且操作
     * 当所有的回复信息都收到后，删除对应的记录并通知初始消息的线程，结束等待
     * */
    // BaseServer引用
    private BaseServer baseServer;
    // key中存储了msg的UUID
    private ConcurrentHashMap<Long,MsgWaiter> replyHelperMap;
    // 执行replyHelper的线程池
    private ExecutorService replyHelperExecutor;

    protected BaseMsgReplyHelper(BaseServer baseServer){
        this.baseServer = baseServer;
        this.replyHelperExecutor = Executors.newCachedThreadPool();
        replyHelperMap = new ConcurrentHashMap<Long, MsgWaiter>();
    }
    /**
     * @Param msg 被注册BaseReplyHelper的需要回复的消息
     *
     * */
    public void registerReplyHelper(BaseMsgProto.BaseMsg msg,CountDownLatch senderCountDownLatch,boolean needTimeout){
        replyHelperMap.put(msg.getMsgUuid(),new MsgWaiter(msg.getNeedReplyNum(),msg.getTimeout()));
        MsgWaiter msgWaiter = replyHelperMap.get(msg.getMsgUuid());

        FutureTask futureTask = new FutureTask(new Callable() {
            @Override
            public Object call()  {
                Log.info(baseServer.id,"msg uuid="+msg.getMsgUuid()+" started waiting for reply...");
                // 等待totalReplyNum个回复
                try {
                    if(needTimeout){
                        msgWaiter.countDownLatch.await(msgWaiter.timeout,TimeUnit.MILLISECONDS);
                    }else{
                        msgWaiter.countDownLatch.await();
                    }
                    Log.info(baseServer.id,"msg uuid="+msg.getMsgUuid()+" received full reply!");
                    //收到回复，通知发送方
                    senderCountDownLatch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                    if(e instanceof InterruptedException){
                        // 默认为超时
                    }
                }finally {
                    replyHelperMap.remove(msg.getMsgUuid());
                }
                return null;
            }
        });
        replyHelperExecutor.submit(futureTask);
        return;
    }
    /**
     * 通知发起者msg
     * @param msgUUid 收到的回复msg中回复目的信息的uuid
     * */
    public boolean replyNotify(Long replyMsgUUid,Long msgUUid){
        synchronized (replyHelperMap){
            if(!replyHelperMap.keySet().contains(replyMsgUUid)){
                return true;
            }
            else{
                MsgWaiter msgWaiter = replyHelperMap.get(replyMsgUUid);
                if(msgWaiter.countDown()){
                    Log.info(baseServer.id,"replyMsg uuid="+msgUUid+" to reply msg uuid="+replyMsgUUid+
                            ", received count="+msgWaiter.countDownLatch.getCount());
                    return true;
                }
            }
            return false;
        }

    }


    class MsgWaiter{
        /**
         * MsgWaiter 封装了需要被回复的BaseMsg相关信息
         * 在得到了所有的回复或超时后，自动销毁
         * */
        // 需要的回复个数
        int totalReplyNum;
        // 超时
        int timeout;
        CountDownLatch countDownLatch;
        MsgWaiter(int totalReplyNum,int timeout){
            this.totalReplyNum = totalReplyNum;
            this.timeout = timeout;
            this.countDownLatch = new CountDownLatch(totalReplyNum);
        }
        private synchronized boolean countDown(){
            try{
                countDownLatch.countDown();
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
