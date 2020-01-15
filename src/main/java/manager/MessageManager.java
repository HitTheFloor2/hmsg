package manager;

import io.netty.channel.Channel;
import log.Log;
import server.TestServer;
import protobuf.BaseMsgProto;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
/**
 * MessageManager 负责发送和接收消息， 每一个Server独立拥有一个MessageManager
 * 接收消息的来源为netty的channel
 * */
public class MessageManager {
    public TestServer testServer;

    public ConcurrentHashMap<Object,CountDownLatch> asyncHelper;
    public static volatile CountDownLatch countDownLatch = null;

    public MessageManager(TestServer testServer){
        this.testServer = testServer;
    }

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
    public void sendMsg(int aimid,Object msg){
        if(aimid == -1){
            broadcasting(msg);
        }
        else{
            singleSendMsg(aimid,msg);
        }
    }
    public void singleSendMsg(int aimid,Object msg){
        if(!testServer.client.channelMap.keySet().contains(aimid)){
            return;
        }
        try{
            Channel channel = testServer.client.channelMap.get(aimid);
            channel.writeAndFlush(msg);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void broadcasting(Object msg){
        for(Channel channel : testServer.client.channelMap.values()){
            channel.writeAndFlush(msg);
        }
    }

    /**
     * 每一个server实例会拥有一个MessageManager，用来管理消息的收发
     * receiveMsg被netty中handler的channelRead调用，是非阻塞的
     * 使用线程池优化
     * */
    public void receiveMsg(Object msg){
        BaseMsgProto.BaseMsg baseMsg = (BaseMsgProto.BaseMsg) msg;
        int msgid = baseMsg.getMsgid();
        Log.info(testServer.id ,"MessageManager.receiveMsg "+baseMsg.toString());
        if(msgid == 0){
            //echo
            BaseMsgProto.BaseMsg.Builder builder =
                    BaseMsgProto.BaseMsg.newBuilder();
            builder.setServerid(testServer.id);

            builder.setMsgid(1);
            builder.setContent("echo reply");
            BaseMsgProto.BaseMsg replyMsg =
                    builder.build();
            sendMsg(testServer.id,replyMsg);
        }
    }
    public void sendMsg(){

    }
}
