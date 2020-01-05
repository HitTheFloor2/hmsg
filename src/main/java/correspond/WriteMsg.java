package correspond;

import io.netty.channel.Channel;
import log.Log;
import manager.MessageManager;
import protobuf.SimpleStringMessageProto;
import server.TestServer;

import java.util.concurrent.CountDownLatch;

public class WriteMsg {


    public static void asyncSingleWrite(TestServer server,int serverid,int len,String content){
        //仅仅写入channel并且发送，不阻塞
        if(!server.client.channelMap.keySet().contains(serverid)){
            return;
        }
        try{
            Channel channel = server.client.channelMap.get(new Integer(serverid));

            SimpleStringMessageProto.SimpleStringMessage.Builder builder =
                    SimpleStringMessageProto.SimpleStringMessage.newBuilder();
            builder.setMsgID(server.id);
            builder.setLength(len);
            builder.setName(content);
            SimpleStringMessageProto.SimpleStringMessage demo = builder.build();
            //写入channel并且发送，但是不保证成功
            channel.writeAndFlush(demo);
        }catch (Exception e){
            Log.logger.warn("WriteMsg.asyncSingleWrite: error in writing message!");
            e.printStackTrace();
        }
    }
    public static void asyncSingleWrite(TestServer server,int serverid,String content){
        asyncSingleWrite(server,serverid,0,content);
    }


    public static void broadcasting(TestServer server,int len,String content){
        for(Integer i : server.client.channelMap.keySet()){
            asyncSingleWrite(server,i,len,content);
        }
    }

    public static void simpleVote(TestServer server){
        MessageManager.dropCountDownLatch();
        MessageManager.getCountDownLatch(server.client.channelMap.size());
        broadcasting(server,1,"simple vote");
        try{

            MessageManager.countDownLatch.await();
            MessageManager.dropCountDownLatch();

            Log.logger.info("server["+server.id+"] WriteMsg.simpVote Down!");
        }catch (Exception e){}

    }


}
