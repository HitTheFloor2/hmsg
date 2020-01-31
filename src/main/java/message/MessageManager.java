package message;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
/**
 * MessageManager 负责发送和接收消息， 每一个Server独立拥有一个MessageManager
 * 接收消息的来源为netty的channel
 * */
public class MessageManager {
    public BaseServer baseServer;

    public HashMap<BaseMsgProto.BaseMsg,CountDownLatch> replyHelper = new HashMap<BaseMsgProto.BaseMsg,CountDownLatch>();

    public MessageManager(BaseServer baseServer){
        this.baseServer = baseServer;
    }
    /**
     * 发送消息（不等待回复）
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
     * 等待回复的消息发送，用于投票等环节
     * 消息发送之后需要等待receiveMsg取得相应的消息
     * 为了使得receiveMsg在收取信息之后可以获知消息之间的reply关系
     * */
    public void sendMsgWithReply(BaseMsgProto.BaseMsg msg,int receiver_id,int timeout){

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
            case BaseMsgUtil.VOTE:{

            }
            case BaseMsgUtil.VOTE_REPLY:{

            }


        }
    }

}
