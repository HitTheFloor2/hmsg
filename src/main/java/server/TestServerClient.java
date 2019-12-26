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
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class TestServerClient implements Runnable{
    public TestServer testServer;
    public ConcurrentHashMap<Integer, InetSocketAddress> serverMap = new ConcurrentHashMap<Integer, InetSocketAddress>();
    public ConcurrentHashMap<Integer, Channel> channelMap;
    public Bootstrap bootstrap;
    public int startPort = 30000;
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


    public void syncAddServer(int id, InetSocketAddress inetSocketAddress){
        InetSocketAddress local = new InetSocketAddress(this.testServer.inetSocketAddress.getHostString(),startPort+id);
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
    public void asyncAddServer(int id, InetSocketAddress inetSocketAddress){
        final int fid = id;
        //InetSocketAddress local = new InetSocketAddress(this.testServer.inetSocketAddress.getHostString(),startPort+id);

        ChannelFuture channelFuture = bootstrap.connect(inetSocketAddress);

        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if(channelFuture.channel().isActive()){
                    channelMap.put(new Integer(fid),channelFuture.channel());
                    Log.logger.info("TestServerClient.addServer connected:"+channelFuture.channel().toString());
                }

                //channelFuture.channel().closeFuture();
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
                                            SimpleStringMessageProto.SimpleStringMessage.getDefaultInstance()));
                                            //JavaObjProto.JavaObj.getDefaultInstance()));
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
            //按照serverMap中的记录，建立连接
            for(Integer serverid : serverMap.keySet()){
                if(serverid == this.testServer.id){
                    continue;
                }
                asyncAddServer(serverid,this.serverMap.get(serverid));
            }
            //System.out.println(prop.getProperty("dbpassword"));
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    public void run(){

        Log.logger.info("server["+this.testServer.id+"]" + "TestServerClient.run: servers connection complete!");
        while(true){
            Log.logger.info("server["+this.testServer.id+"]" + "TestServerClient.run: keepAlive...");
            Log.logger.info("server["+this.testServer.id+"]" + "TestServerClient.run: channelMap = "+this.channelMap.toString());

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
            //System.out.println("No server id = "+serverid);
            return;
        }
        try{
            Channel channel = channelMap.get(new Integer(i));
            if(!channel.isOpen()){
                Log.logger.info("server["+this.testServer.id+"]" + "writeMsg: channel to "+this.serverMap.get(i).toString()+" is not open!");
                return;
            }
            SimpleStringMessageProto.SimpleStringMessage.Builder builder =
                    SimpleStringMessageProto.SimpleStringMessage.newBuilder();
            builder.setMsgID(this.testServer.id);
            builder.setLength(233);
            builder.setName(content);
            SimpleStringMessageProto.SimpleStringMessage demo = builder.build();
            channel.writeAndFlush(demo).sync();
        }catch (Exception e){
            Log.logger.warn("server["+this.testServer.id+"]" + "writeMsg:error to write message!");
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
            if(this.channelMap.get(i).isRegistered()){
                writeMsg(i,"broadcasting");
            }
        }
    }

}
