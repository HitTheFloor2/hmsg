package message;

import io.netty.channel.Channel;
import log.Log;
import message.BaseMsgUtil;
import server.BaseServer;
import protobuf.BaseMsgProto;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
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
    /**
     * 发送消息
     * 由于在构建msg时已经可以确定消息的发送对象，故在MessageMananger中不做判断
     * */
    public void sendMsg(BaseMsgProto.BaseMsg msg,int receiver_id){
        if(receiver_id == -1){
            for(Channel channel : baseServer.client.channelMap.values()){
                channel.writeAndFlush(msg);
            }
        }else{
            baseServer.client.channelMap.get(receiver_id).writeAndFlush(msg);
        }

    }

    public void sendMsg(BaseMsgProto.BaseMsg msg, List<Integer> receivers){
        for(Integer i : receivers){
            if(baseServer.client.channelMap.keySet().contains(i)){
                baseServer.client.channelMap.get(i).writeAndFlush(msg);
            }
        }
    }

    /**
     * 每一个server实例会拥有一个MessageManager，用来管理消息的收发
     * receiveMsg被netty中handler的channelRead调用，是非阻塞的
     * 使用线程池优化
     * */
    public void receiveMsg(Object Msg) {
        BaseMsgProto.BaseMsg baseMsg = (BaseMsgProto.BaseMsg) Msg;
        int msgType = baseMsg.getMsgType();
        Log.info(baseServer.id, "MessageManager.receiveMsg :\n" + baseMsg.toString());
        switch (msgType) {
            case BaseMsgUtil.SOCKETADDRESS_ANNOUNCE: {
                break;
            }
            case BaseMsgUtil.ECHO:{
                BaseMsgProto.BaseMsg msg = BaseMsgUtil.getInstance(
                        BaseMsgUtil.ECHO_REPLY,-1,baseServer.id,baseMsg.getSenderId(),0);
                sendMsg(msg,msg.getSingleReceiverId());
                break;
            }


        }
    }

}
