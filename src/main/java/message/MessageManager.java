package message;

import com.sun.org.apache.xpath.internal.operations.Bool;
import io.netty.channel.Channel;
import log.Log;
import message.BaseMsgUtil;
import server.BaseServer;
import protobuf.BaseMsgProto;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MessageManager 负责发送和接收消息， 每一个Server独立拥有一个MessageManager
 * 接收消息的来源为netty的channel
 * */
public class MessageManager {
    // 当前BaseServer引用
    private BaseServer baseServer;
    // 当前BaseServer发送的BaseMsg个数
    private AtomicInteger msgSequenceNum;
    // 处理发送消息的线程池
    private ExecutorService senderExecutor;
    // 处理接受消息的线程池
    private ExecutorService receiveExecutor;
    // 同步回复信息工具
    BaseMsgReplyHelper baseMsgReplyHelper;

    public MessageManager(BaseServer baseServer){
        this.baseServer = baseServer;
        this.senderExecutor = Executors.newCachedThreadPool();
        this.receiveExecutor = Executors.newCachedThreadPool();
        this.msgSequenceNum = new AtomicInteger(0);
        this.baseMsgReplyHelper = new BaseMsgReplyHelper(baseServer);

    }

    /**
     * 获取当前BaseServer的顺序增长的 msgid
     * */
    public synchronized Integer getMsgID(){
        return msgSequenceNum.get() ;
    }
    /**
     * msgId的自增
     * */
    public synchronized Integer addMsgID(){
        return msgSequenceNum.getAndAdd(1);
    }



    /**
     * @Param msg 已经封装好的待发送消息对象
     * @Param receiver_id 被发送的节点id
     * 发送消息，不等待回复，不关心是否成功
     * 由于在构建msg时已经可以确定消息的发送对象，故在MessageMananger中不做判断
     * */
    public void sendMsg(BaseMsgProto.BaseMsg msg,int receiver_id){
        // 这里其实不需要返回值
        FutureTask<Boolean> futureTask = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call(){
                try{
                    if(receiver_id == -1){
                        for(Channel channel : baseServer.client.channelMap.values()){
                            channel.writeAndFlush(msg);
                        }
                    }else{
                        baseServer.client.channelMap.get(receiver_id).writeAndFlush(msg);
                    }
                    return true;
                }catch (Exception e){
                    e.printStackTrace();
                    return false;
                }
            }
        });
        senderExecutor.submit(futureTask);
        return;
    }
    public void sendMsg(BaseMsgProto.BaseMsg msg, List<Integer> receivers){
        for(Integer i : receivers){
            if(baseServer.client.channelMap.keySet().contains(i)){
                sendMsg(msg,i);
            }
        }
    }

    /**
     * 等待回复的消息发送，用于投票等环节
     * 消息发送之后需要等待receiveMsg取得相应的消息
     * 为了使得receiveMsg在收取信息之后可以获知消息之间的reply关系
     * */
    public void sendMsgWithReply(BaseMsgProto.BaseMsg msg,int receiver_id,int timeout){
        FutureTask futureTask = new FutureTask(new Callable() {
            @Override
            public Object call() throws Exception {
                CountDownLatch senderCountDownLatch = new CountDownLatch(1);
                baseMsgReplyHelper.registerReplyHelper(msg,senderCountDownLatch,true);
                sendMsg(msg,receiver_id);
                try {
                    senderCountDownLatch.await();
                    Log.info(baseServer.id,"msg uuid="+msg.getMsgUuid()+" reply down!");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {

                }
                return null;
            }
        });
        senderExecutor.submit(futureTask);
    }
    /**
     * 每一个server实例会拥有一个MessageManager，用来管理消息的收发
     * receiveMsg被netty中handler的channelRead调用，是非阻塞的
     *
     * */
    public void receiveMsg(Object Msg) {
        BaseMsgProto.BaseMsg received_msg = (BaseMsgProto.BaseMsg) Msg;
        int msgType = received_msg.getMsgType();
        Log.info(baseServer.id, "MessageManager.receiveMsg :\n" + BaseMsgUtil.ToString(received_msg));
        switch (msgType) {
            case BaseMsgUtil.SOCKETADDRESS_ANNOUNCE: {
                break;
            }
            // 收到ECHO，回复一个ECHO_REPLY
            case BaseMsgUtil.ECHO:{
                BaseMsgProto.BaseMsg msg = BaseMsgUtil.getInstance(
                        BaseMsgUtil.ECHO_REPLY,baseServer.messageManager.addMsgID(),
                        baseServer.id,
                        received_msg.getServerSenderId(),
                        received_msg.getMsgUuid(),
                        0);
                sendMsg(msg,msg.getServerSingleReceiverId());
                break;
            }
            // 收到ECHO的回复，解除发送端的等待
            case BaseMsgUtil.ECHO_REPLY:{
                baseMsgReplyHelper.replyNotify(received_msg.getMsgReplyUuid(),received_msg.getMsgUuid());
                break;
            }
            case BaseMsgUtil.VOTE:{

            }
            case BaseMsgUtil.VOTE_REPLY:{

            }


        }
    }

}
