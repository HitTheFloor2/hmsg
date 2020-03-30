package server;

import annotation.Blocking;
import annotation.NonBlocking;
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
import util.MUtil;
import util.MapUtil;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static util.MUtil.String2InetSocketAddress;

/**
 * BaseServerClient作为BaseServer的成员对象，用于和其他集群中的Server建立Channel并管理
 * 是BaseServer用来给其他节点发送信息的通道
 * */
public class BaseServerClient {
    // 本服务器
    private BaseServer baseServer;
    // 集群服务器地址
    private ConcurrentSkipListSet<String> serverList = new ConcurrentSkipListSet<String>();
    // 集群服务器可用Channel
    private ConcurrentHashMap<String, Channel> channelMap;
    private Bootstrap bootstrap;


    public BaseServerClient(BaseServer baseServer){
        if(null != baseServer){
            this.baseServer = baseServer;
            this.channelMap = new ConcurrentHashMap<String, Channel>();
            init(this);
        }

    }

    public void init(BaseServerClient baseServerClient){
        //初始化netty客户端
        initNettyClient(this.baseServer);
        //读入节点列表并且建立连接
        initServerMap(baseServerClient);

    }

    /**
     * 异步添加Server，只有连接成功的channel被放入channelMap
     * @param address 目标BaseServer的address
     * */
    @NonBlocking
    public void asyncAddServer(String address){
        if(address.equals(baseServer.address)){
            // 跳过自己的地址
            return;
        }
        InetSocketAddress inetSocketAddress = String2InetSocketAddress(address);
        // 检查channel是否已经被记录，避免重复添加
        if(channelMap.keySet().contains(address)){
            if(channelMap.get(address).isActive()) {
                return;
            }
        }
        // 添加channel
        final String faddress = address;
        ChannelFuture channelFuture = bootstrap.connect(inetSocketAddress);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) {

                if(channelFuture.isSuccess()){
                    channelMap.put(faddress,channelFuture.channel());
                    Log.info(baseServer.address,faddress +" channel connected!");
                }else{
                    Log.info(baseServer.address,faddress +
                            " channel connect failed!");
                }
            }
        });
    }


    /**
     * 删除关闭的Channel
     * */
    @NonBlocking
    public void removeChannel(Channel channel){
        if(channelMap.values().contains(channel)){
            for(Map.Entry entry : baseServer.client.channelMap.entrySet()){
                if(entry.getValue().equals(channel)){
                    channelMap.remove(entry.getKey());
                    Log.info(baseServer.address,"BaseServerClientHandler: channel "+channel.toString()+
                            " is inactive, remove it!");
                }
            }
        }
    }
    /**
     * 获取channelMap
     * */
    public ConcurrentHashMap<String,Channel> getChannelMap(){
        return this.channelMap;
    }

    /**
     * 从配置文件中初始化ServerMap，只在初始化时执行
     * */
    public void initServerMap(BaseServerClient baseServerClient){
        //遍历servers.properties文件，初始化serverMap，记录其余的server的host
        //深拷贝ConfigManager中的serverMap，因为Client端的serverMap可变
        if(null != baseServer.Entrance){
            baseServerClient.serverList.addAll(baseServer.Entrance);
        } else {
            if(ConfigManager.debug) {
                baseServerClient.serverList.addAll(ConfigManager.serverTestList);
            }
            else{
                baseServerClient.serverList.addAll(ConfigManager.serverList);
            }
        }


        try {
            //按照serverMap中的记录，建立连接
            for(String address : this.serverList){
                asyncAddServer(address);
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
            bootstrap.group(group).
                    channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,1000)
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
     * 封装channel的方法，目的是保证client和其他模块隔离
     * */
    public Boolean singleSend(Object msg,String address){
        if(channelMap.keySet().contains(address)){
            channelMap.get(address).writeAndFlush(msg);
            return true;
        }else{
            Log.info(baseServer.address,"singleSend(): no connection with "+address);
            return false;
        }
    }
    public Boolean broadcasting(Object msg){
        if(channelMap.values().size() <= 0){
            Log.info(baseServer.address,"broadcasting(): no connection ");
            return false;
        }
        for(Channel channel : channelMap.values()){
            channel.writeAndFlush(msg);

        }
        return true;
    }


}
