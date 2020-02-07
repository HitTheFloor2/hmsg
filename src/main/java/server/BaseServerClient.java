package server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import log.Log;
import message.BaseMsgUtil;
import protobuf.BaseMsgProto;
import manager.ConfigManager;
import util.MapUtil;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * BaseServerClient作为BaseServer的成员对象，用于和其他集群中的Server建立Channel
 * 并且负责断线重连，心跳
 * */
public class BaseServerClient implements Runnable{
    public BaseServer baseServer;
    //集群服务器地址
    public ConcurrentHashMap<Integer, InetSocketAddress> serverMap = new ConcurrentHashMap<Integer, InetSocketAddress>();
    //集群服务器可用Channel
    public ConcurrentHashMap<Integer, Channel> channelMap;
    public Bootstrap bootstrap;


    public BaseServerClient(BaseServer baseServer){
        this.baseServer = baseServer;
        this.channelMap = new ConcurrentHashMap<Integer, Channel>();
        init(this);

        new Thread(this).start();
    }

    public void init(BaseServerClient baseServerClient){
        initNettyClient(this.baseServer);
        initServerMap(baseServerClient);
    }


    /**
     * 同步添加Server
     * 在不可控的网络状态下，如果连接消耗时间长，会在此阻塞
     * */
    public void syncAddServer(int id, InetSocketAddress inetSocketAddress){
        InetSocketAddress local = this.baseServer.inetSocketAddress;
        try{
            //ChannelFuture channelFuture = bootstrap.connect(inetSocketAddress,local).sync();
            ChannelFuture channelFuture = bootstrap.connect(inetSocketAddress);
            Log.logger.info("TestServerClient.syncAddServer connected "+channelFuture.channel().toString());
            this.channelMap.put(new Integer(id),channelFuture.channel());
            return ;
        }catch (Exception e){
            e.printStackTrace();
            return ;
        }

    }

    /**
     * 异步添加Server，只有连接成功的channel被放入channelMap
     * @param id 目标BaseServer的id
     * @param inetSocketAddress 目标BaseServer的inetSocketAddress
     * */
    public void asyncAddServer(int id, InetSocketAddress inetSocketAddress){
        if(channelMap.keySet().contains(id)){
            if(channelMap.get(id).isActive()) {
                return;
            }
        }
        final int fid = id;
        ChannelFuture channelFuture = bootstrap.connect(inetSocketAddress);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if(channelFuture.isSuccess()){
                    channelMap.put(new Integer(fid),channelFuture.channel());
                    Log.info(baseServer.id,channelFuture.channel().toString()+" channel connected!");
                }else{
                    Log.info(baseServer.id,channelFuture.channel().toString()+
                            " channel connect failed!");
                }
            }
        });
    }

    /**
     * 删除关闭的Channel
     * */
    public void removeChannel(Channel channel){
        if(baseServer.client.channelMap.values().contains(channel)){
            for(Map.Entry entry : baseServer.client.channelMap.entrySet()){
                if(entry.getValue().equals(channel)){
                    baseServer.client.channelMap.remove(entry.getKey());
                    Log.info(baseServer.id,"BaseServerClientHandler: channel "+channel.toString()+
                            " is inactive, remove it!");
                }
            }
        }
    }
    /**
     * 从配置文件中初始化ServerMap
     * */
    public void initServerMap(BaseServerClient baseServerClient){
        //遍历servers.properties文件，初始化serverMap，记录其余的server的host
        //深拷贝ConfigManager中的serverMap，因为Client端的serverMap可变

        if(ConfigManager.debug) {
            baseServerClient.serverMap.putAll(ConfigManager.serverMapTest);
        }
        else{
            baseServerClient.serverMap.putAll(ConfigManager.serverMap);
        }

        try {
            //按照serverMap中的记录，建立连接
            for(Integer serverid : this.serverMap.keySet()){
                if(serverid == this.baseServer.id){
                    continue;
                }
                asyncAddServer(serverid,this.serverMap.get(serverid));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 从ServerMap中初始化Channel
     * channelMap的值会不断被ServerMap中的值更新
     * */
    public void initNettyClient(BaseServer baseServer) {
        //init Netty client
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            socketChannel.pipeline().
                                    addLast(new ProtobufDecoder(
                                            BaseMsgProto.BaseMsg.getDefaultInstance()));
                                            //JavaObjProto.JavaObj.getDefaultInstance()));
                            socketChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            socketChannel.pipeline().addLast(new ProtobufEncoder());
                            socketChannel.pipeline().addLast(new BaseServerClientHandler(baseServer));

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    /**
     * BaseServerClient主线程，用于轮询探测可用server，保活现有Channel
     * */
    public void run(){

        while(true){
            Log.info(this.baseServer.id,"client alive, channelMap = "+this.channelMap.toString());
            try{
                Thread.sleep(10000);
                try {
                    //按照serverMap中的记录，建立连接
                    for(Integer serverid : this.serverMap.keySet()){
                        //跳过自身Server和channelMap中已经存在的
                        if(serverid == this.baseServer.id || channelMap.keySet().contains(serverid)){
                            continue;
                        }
                        //尝试添加Channel
                        asyncAddServer(serverid,this.serverMap.get(serverid));
                    }
                    //发送ECHO保活
                    BaseMsgProto.BaseMsg msg = BaseMsgUtil.getInstance(
                            BaseMsgUtil.ECHO,
                            baseServer.messageManager.addMsgID(),
                            baseServer.id,
                            -1,
                            channelMap.keySet().size(),
                            3000);
                    if(baseServer.id == 1){
                        baseServer.messageManager.sendMsgWithReply(msg,-1,10000);
                    }



                } catch(Exception e) {
                    e.printStackTrace();
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }




}
