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
import log.Log;
import protobuf.BaseMsgProto;
import manager.ConfigManager;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class TestServerClient implements Runnable{
    public TestServer testServer;
    public ConcurrentHashMap<Integer, InetSocketAddress> serverMap = new ConcurrentHashMap<Integer, InetSocketAddress>();
    public ConcurrentHashMap<Integer, Channel> channelMap;
    public Bootstrap bootstrap;


    public TestServerClient(TestServer testServer){
        this.testServer = testServer;
        this.channelMap = new ConcurrentHashMap<Integer, Channel>();
        init(this);

        new Thread(this).start();
    }

    public void init(TestServerClient testServerClient){
        initClient(this.testServer);
        initServerMap(testServerClient);
    }


    /**
     * 同步添加Server
     * 在不可控的网络状态下，如果连接消耗时间长，会在此阻塞
     * */
    public void syncAddServer(int id, InetSocketAddress inetSocketAddress){
        InetSocketAddress local = this.testServer.inetSocketAddress;
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
     * 异步添加Server
     * */
    public void asyncAddServer(int id, InetSocketAddress inetSocketAddress){
        final int fid = id;
        ChannelFuture channelFuture = bootstrap.connect(inetSocketAddress);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if(channelFuture.channel().isActive()){
                    channelMap.put(new Integer(fid),channelFuture.channel());
                    Log.logger.info(channelFuture.channel().toString()+" channel isActive!");
                }
            }

        });

    }

    public void initServerMap(TestServerClient testServerClient){
        //遍历servers.properties文件，初始化serverMap，记录其余的server的host
        //深拷贝ConfigManager中的serverMap，因为Client端的serverMap可变

        if(ConfigManager.debug) {
            testServerClient.serverMap.putAll(ConfigManager.serverMapTest);
        }
        else{
            testServerClient.serverMap.putAll(ConfigManager.serverMap);
        }

        try {
            //按照serverMap中的记录，建立连接
            for(Integer serverid : this.serverMap.keySet()){
                if(serverid == this.testServer.id){
                    continue;
                }
                asyncAddServer(serverid,this.serverMap.get(serverid));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void initClient(TestServer testServer) {
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
                            socketChannel.pipeline().addLast(new TestServerClientHandler(testServer));

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void run(){
        Log.info(this.testServer.id,"servers connection completed!");
        while(true){
            Log.info(this.testServer.id,"client alive, channelMap = "+this.channelMap.toString());
            try{
                Thread.sleep(3000);
                BaseMsgProto.BaseMsg.Builder builder =
                        BaseMsgProto.BaseMsg.newBuilder();
                builder.setServerid(this.testServer.id);
                builder.setAimid(-1);
                builder.setMsgid(0);
                builder.setContent("echo");
                BaseMsgProto.BaseMsg msg =
                        builder.build();
                this.testServer.messageManager.sendMsg(builder.getAimid(),msg);

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }




}
