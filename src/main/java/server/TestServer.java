package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import log.Log;
import protobuf.BaseMsgProto;
import manager.MessageManager;

import java.net.InetSocketAddress;

public class TestServer {
    public int id;
    public int port;
    public InetSocketAddress inetSocketAddress;
    public ServerBootstrap ssmpServerBootstrap;
    public Channel channel;
    public TestServerClient client;
    public MessageManager messageManager;
    public TestServer(int id,InetSocketAddress inetSocketAddress){
        this.id = id;
        this.inetSocketAddress = inetSocketAddress;
        this.port = this.inetSocketAddress.getPort();
        init();

    }
    public TestServer(int id,String ip,int port){
        this.id = id;
        this.port = port;
        this.inetSocketAddress = new InetSocketAddress(ip,this.port);
        init();

    }
    public void init(){
        initNettyServer(this);
        this.client = new TestServerClient(this);
        this.messageManager = new MessageManager(this);
    }
    public void initNettyServer(TestServer testServer){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try{
            ssmpServerBootstrap = new ServerBootstrap();
            ssmpServerBootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    //.handler(null)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel socketChannel){
                            socketChannel.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                            socketChannel.pipeline().
                                    addLast(new ProtobufDecoder(
                                            BaseMsgProto.BaseMsg.getDefaultInstance()));
                            socketChannel.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                            socketChannel.pipeline().addLast(new ProtobufEncoder());
                            socketChannel.pipeline().addLast(new TestServerHandler(testServer));

                        }
                    });
            ChannelFuture channelFuture = ssmpServerBootstrap.bind(port).sync();
            Log.logger.info("TestServer.run() TestServer "+this.inetSocketAddress.toString()+" started!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //new Thread(this).start();

    }




}
