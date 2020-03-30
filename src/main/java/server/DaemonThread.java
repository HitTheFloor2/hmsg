package server;

import io.netty.channel.Channel;
import log.Log;
import message.*;
import protobuf.BaseMsgProto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConnectDaemonThread作为守护线程，维持了集群的通信，包括了集群节点的加入和离开
 * 已连接的节点默认为拥有已经存在的channel，使用该channel持续发送ECHO消息，超时或断开则重连
 * 当收到新的节点的JOIN消息时，将新的节点的属性广播，并且将全网节点的信息发送回新的节点
 * */
public class DaemonThread implements Runnable{
    private BaseServer baseServer;
    private MessageManager messageManager;
    DaemonThread(BaseServer baseServer){
        this.baseServer = baseServer;
        this.messageManager = baseServer.messageManager;
    }
    public void start(){
        new Thread(this).start();
    }
    public void cluster(){
        // 对现有的每一个可达节点发送CLUSTER
        for(String address : baseServer.client.getChannelMap().keySet()) {
            BaseMsgProto.BaseMsg cluster = BaseMsgUtil.getInstance(
                    BaseMsgUtil.CLUSTER,
                    messageManager.addMsgID(),
                    baseServer.address,
                    address,
                    1,
                    3000,
                    ""
            );
            messageManager.sendMsgWithReply(cluster, address, 5000, new BaseMsgCallBack() {
                @Override
                public void withMsgReceivedComplete(MsgWaiter o) {
                    Log.info(baseServer.address,"Daemon cluster reply");
                }

                @Override
                public void withMsgReceivedFail(MsgWaiter o) {

                }
            });
        }
    }
    public void echo(){
        // 对现有的每一个可达节点发送ECHO
        for(String address : baseServer.client.getChannelMap().keySet()){
            BaseMsgProto.BaseMsg echo = BaseMsgUtil.getInstance(
                    BaseMsgUtil.ECHO,
                    messageManager.addMsgID(),
                    baseServer.address,
                    address,
                    1,
                    3000,
                    ""
            );
            messageManager.sendMsgWithReply(echo, address, 5000, new BaseMsgCallBack() {
                @Override
                public void withMsgReceivedComplete(MsgWaiter o) {
                    Log.info(baseServer.address,"Daemon echo reply, "+o.getContainer().toString());
                }

                @Override
                public void withMsgReceivedFail(MsgWaiter o) {

                }
            });
        }


    }

    public void run(){
        while(true){

            Log.info(baseServer.address,"Daemon, channelMap = "+baseServer.client.getChannelMap().toString());
            try{
                Thread.sleep(10000);
                // echo();
                cluster();

            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
