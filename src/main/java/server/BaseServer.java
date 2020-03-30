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
import message.BaseMsgCallBack;
import message.BaseMsgUtil;
import message.MsgWaiter;
import protobuf.BaseMsgProto;
import message.MessageManager;
import util.MUtil;


import java.net.InetSocketAddress;
import java.util.List;

public class BaseServer {
    public String name;
    public int port;
    public String address;
    public InetSocketAddress inetSocketAddress;
    public ServerBootstrap ssmpServerBootstrap;
    public BaseServerClient client;
    public MessageManager messageManager;
    public DaemonThread daemonThread;
    public List<String> Entrance = null;

    public BaseServer(String name, String ip, int port){
        this.name = name;
        this.port = port;
        this.inetSocketAddress = new InetSocketAddress(ip,this.port);
        this.address = inetSocketAddress.getHostString()+":"+inetSocketAddress.getPort();
        init();

    }
    public BaseServer(String name, String address,List<String> entrance){
        this.name = name;
        this.inetSocketAddress = MUtil.String2InetSocketAddress(address);
        this.address = address;
        this.port  = Integer.valueOf(address.split(":")[1]);
        this.Entrance = entrance;
        init();

    }
    public void init(){
        //启动netty服务端，用来监听其他BaseServer的client发来的连接
        initNettyServer(this);
        //启动私有的client
        this.client = new BaseServerClient(this);
        //启动私有的消息管理器，所有的消息收发都由此工作
        this.messageManager = new MessageManager(this);
        //更新集群信息
        initCluster();
        Log.info(this.address,"initial completed!");
        //启动守护线程
        this.daemonThread = new DaemonThread(this);
        daemonThread.start();
    }
    private void initNettyServer(BaseServer baseServer){
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

        } catch (Exception e) {
            e.printStackTrace();
        }
        //new Thread(this).start();

    }

    private void initCluster(){

        // 未知集群规模，所以needReplyNum为-1
        BaseMsgProto.BaseMsg clusterMsg = BaseMsgUtil.getInstance(
                BaseMsgUtil.CLUSTER,
                messageManager.addMsgID(),
                address,
                "",
                -1,
                3000,
                ""
        );
        //广播
        messageManager.sendMsgWithReply(clusterMsg, null, 3000, new BaseMsgCallBack() {
            @Override
            public void withMsgReceivedComplete(MsgWaiter o) {
                Log.info(address,"cluster update complete, "+o.getContainer().toString());

            }

            @Override
            public void withMsgReceivedFail(MsgWaiter o) {
                Log.info(address,"cluster update failed.");
            }
        });
    }


}
