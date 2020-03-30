package message;

import log.Log;
import protobuf.BaseMsgProto;
import server.BaseServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.concurrent.*;

public class BaseMsgReplyHelper {
    /**
     * 回复消息等待器
     * 用于处理投票、询问等环节中，存在消息发出后需要等待一个或多个消息回复的情况
     * 需要回复的初始消息（如发起投票的消息）发出后，在MessageManager中生成一个线程，并且注册一个等待器
     * MessageManager在receiveMsg中收到Msg后检查BaseMsgReplyHelper所有等待器，找到对应的等待器并且操作
     * 当所有的回复信息都收到或等待器超时后，删除对应的等待器并通知初始消息的线程，结束等待（或者该线程自己超时）
     *
     * 等待器MsgWaiter可以用于存储收到的回复信息中携带的数据，MsgWaiter的对象引用暴露给发送信息的API，
     * 该API可以在回调函数中检查MsgWaiter的数据，执行自己的业务逻辑
     * 回调函数的接口为BaseMsgCallBack
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
     * 注册回复消息工具
     * 被注册的回复消息工具会在timeout时间内存活，将存活时间内收到的回复信息存储
     * @param msg 被注册BaseReplyHelper的需要回复的消息
     * @param senderCountDownLatch
     * */
    public MsgWaiter registerReplyHelper(BaseMsgProto.BaseMsg msg,
                                    CountDownLatch senderCountDownLatch){
        MsgWaiter msgWaiter = new MsgWaiter(msg.getMsgUuid(),msg.getNeedReplyNum(),msg.getTimeout());

        replyHelperMap.put(msg.getMsgUuid(),msgWaiter);

        FutureTask futureTask = new FutureTask(new Callable() {
            @Override
            public Object call()  {
                // Log.info(baseServer.address,"MsgWaiter:msg uuid="+msg.getMsgUuid()+" started waiting for reply...");
                // 等待totalReplyNum个回复或者timeout
                try {
                    msgWaiter.countDownLatch.await(msgWaiter.timeout,TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    // TODO
                    msgWaiter.updateStatus(-1);
                }finally {
                    // 解除调用处(sendMsgWithReply)的等待
                    senderCountDownLatch.countDown();
                    // 将MsgWaiter从存储中删除
                    replyHelperMap.remove(msg.getMsgUuid());
                }
                return null;
            }
        });
        replyHelperExecutor.submit(futureTask);
        return msgWaiter;
    }
    /**
     * 通知发起者msg
     * @param replyMsg 收到的信息
     * @return 执行是否成功
     * */
    public MsgWaiter replyNotify(Object replyMsg){
        if(null == replyMsg){
            return null;
        }
        BaseMsgProto.BaseMsg msg = (BaseMsgProto.BaseMsg) replyMsg;

        synchronized (replyHelperMap){
            // 检查是否存在被回复的目标
            if(!replyHelperMap.keySet().contains(msg.getMsgReplyUuid())){
                // 无被回复的目标
                return null;
            }
            else{
                try {
                    // 找到等待器
                    MsgWaiter msgWaiter = replyHelperMap.get(msg.getMsgReplyUuid());
                    // 尝试countDown
                    if(msgWaiter.countDown()){
                        Log.info(baseServer.address,"replyMsg uuid="+msg.getMsgUuid()+" to reply msg uuid="+msg.getMsgReplyUuid()+
                                ", received count="+msgWaiter.countDownLatch.getCount());
                        // 成功，msg添加等待器中
                        msgWaiter.insert(msg.getServerSenderAddress(),msg);
                        return msgWaiter;
                    }
                }catch (Exception e){
                    if(e instanceof NullPointerException){
                        Log.info(baseServer.address,"invalid replyMsg");
                        return null;
                    }
                }

            }
            return null;
        }

    }



}
