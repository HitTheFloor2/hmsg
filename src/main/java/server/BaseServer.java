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
import message.MessageManager;

import java.net.InetSocketAddress;

public class BaseServer {
    public int id;
    public int port;
    public InetSocketAddress inetSocketAddress;
    public ServerBootstrap ssmpServerBootstrap;
    public Channel channel;
    public BaseServerClient client;
    public MessageManager messageManager;
    public BaseServer(int id, InetSocketAddress inetSocketAddress){
        this.id = id;
        this.inetSocketAddress = inetSocketAddress;
        this.port = this.inetSocketAddress.getPort();
        init();

    }
    public BaseServer(int id, String ip, int port){
        this.id = id;
        this.port = port;
        this.inetSocketAddress = new InetSocketAddress(ip,this.port);
        init();

    }
    public void init(){
        initNettyServer(this);
        this.client = new BaseServerClient(this);
        this.messageManager = new MessageManager(this);
    }
    public void initNettyServer(BaseServer baseServer){
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
                            socketChannel.pipeline().addLast(new BaseServerHandler(baseServer));

                        }
                    });
            ChannelFuture channelFuture = ssmpServerBootstrap.bind(port).sync();
            Log.logger.info("BaseServer.run() BaseServer "+this.inetSocketAddress.toString()+" started!");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //new Thread(this).start();

    }




}
