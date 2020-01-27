package manager;

import io.netty.channel.Channel;
import log.Log;
import server.BaseServer;
import protobuf.BaseMsgProto;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
/**
 * MessageManager 负责发送和接收消息， 每一个Server独立拥有一个MessageManager
 * 接收消息的来源为netty的channel
 * */
public class MessageManager {
    public BaseServer baseServer;

    public ConcurrentHashMap<Object,CountDownLatch> asyncHelper;
    public static volatile CountDownLatch countDownLatch = null;

    public MessageManager(BaseServer baseServer){
        this.baseServer = baseServer;
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
        if(!baseServer.client.channelMap.keySet().contains(aimid)){
            return;
        }
        try{
            Channel channel = baseServer.client.channelMap.get(aimid);
            channel.writeAndFlush(msg);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void broadcasting(Object msg){
        for(Channel channel : baseServer.client.channelMap.values()){
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
        Log.info(baseServer.id ,"MessageManager.receiveMsg "+baseMsg.toString());
        if(msgid == 0){
            //echo
            BaseMsgProto.BaseMsg.Builder builder =
                    BaseMsgProto.BaseMsg.newBuilder();
            builder.setServerid(baseServer.id);
            builder.setAimid(baseMsg.getServerid());
            builder.setMsgid(1);
            builder.setContent("echo reply");
            BaseMsgProto.BaseMsg replyMsg =
                    builder.build();
            sendMsg(baseMsg.getServerid(),replyMsg);
        }
    }
    public void sendMsg(){

    }
}
