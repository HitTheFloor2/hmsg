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
import protobuf.JavaObjProto;
import protobuf.SimpleStringMessageProto;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Properties;
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
    public TestServerClient(TestServer testServer,boolean useConfig){
        this.testServer = testServer;
        this.channelMap = new ConcurrentHashMap<Integer, Channel>();
        init(this);
        new Thread(this).start();
    }

    public Channel syncAddServer(int id, InetSocketAddress inetSocketAddress){
        try{
            ChannelFuture channelFuture = bootstrap.connect(inetSocketAddress).sync();
            Log.logger.info("TestServerClient.syncAddServer connected"+channelFuture.channel().toString());
            return channelFuture.channel();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }
    public void asyncAddServer(int id, InetSocketAddress inetSocketAddress){
        final int fid = id;
        ChannelFuture channelFuture = bootstrap.connect(inetSocketAddress);

        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                channelMap.put(new Integer(fid),channelFuture.channel());
                Log.logger.info("TestServerClient.addServer connected:"+channelFuture.channel().toString());
                channelFuture.channel().closeFuture();
            }

        });

    }

    public void init(TestServerClient testServerClient) {
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
                                            //SimpleStringMessageProto.SimpleStringMessage.getDefaultInstance()));
                                            JavaObjProto.JavaObj.getDefaultInstance()));
                            socketChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            socketChannel.pipeline().addLast(new ProtobufEncoder());
                            socketChannel.pipeline().addLast(new TestServerClientHandler(testServerClient));

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        //遍历servers.properties文件，初始化serverMap，记录其余的server的host
        try {
            String path = System.getProperty("user.dir");
            Properties prop = new Properties();
            prop.load(new FileInputStream(path+"/config/servers.properties"));
            for(String key: prop.stringPropertyNames()){
                String value = prop.getProperty(key);
                String[] vs = value.split(":");
                serverMap.put(Integer.parseInt(key),new InetSocketAddress(vs[0],Integer.parseInt(vs[1])));
            }
            //System.out.println(prop.getProperty("dbpassword"));
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    public void run(){
        //将serverMap中记录的server尝试连接，初始化channelMap
        for(Integer serverid : serverMap.keySet()){
            Channel channel = syncAddServer(serverid,this.serverMap.get(serverid));
            if(channel != null){
                this.channelMap.put(serverid,channel);
                Log.logger.info("TestServerClient.run: connected: "+channel.toString()+", add to channelMap");
            }
        }
        Log.logger.info("TestServerClient.run: servers connection complete!");
        while(true){
            try{
                Thread.sleep(3000);
                broadcasting();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void writeMsg(int serverid,String content){
        int i = serverid;
        if(!channelMap.keySet().contains(serverid)){
            System.out.println("No server id = "+serverid);
            return;
        }
        try{
            Channel channel = channelMap.get(new Integer(i));

            SimpleStringMessageProto.SimpleStringMessage.Builder builder =
                    SimpleStringMessageProto.SimpleStringMessage.newBuilder();
            builder.setMsgID(i);
            builder.setLength(233);
            builder.setName(content);
            SimpleStringMessageProto.SimpleStringMessage demo = builder.build();
            channel.writeAndFlush(demo).sync();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    /**
     * default writeMsg
     * */
    public void writeMsg(int serverid){
        writeMsg(serverid,"Hello");
    }
    /**
     *
     * */
    public void broadcasting(){
        for(Integer i : this.channelMap.keySet()){
            if(i == this.testServer.id){
                continue;
            }
            if(this.channelMap.get(i).isRegistered()){
                writeMsg(i);
            }
        }
    }

}
