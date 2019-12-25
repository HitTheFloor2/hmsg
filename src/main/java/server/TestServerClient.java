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

import java.net.InetSocketAddress;
import java.util.HashMap;

public class TestServerClient implements Runnable{
    public HashMap<Integer, Channel> channelMap;
    public Bootstrap bootstrap;
    public TestServerClient(){
        this.channelMap = new HashMap<Integer, Channel>();
        init();
        new Thread(this).start();
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

    public void init() {
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
                            socketChannel.pipeline().addLast(new TestServerHandler());

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(){
        while(true){
            try{
                for(Integer integer : this.channelMap.keySet()){
                    if(this.channelMap.get(integer).isRegistered()){
                        writeMsg(integer);
                    }
                }
                //writeMsg();
                Thread.sleep(3000);
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
    public void writeMsg(int serverid){
        writeMsg(serverid,"Hello");
    }

}
