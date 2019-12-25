package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EServer implements Runnable{
    public int port;
    public ServerBootstrap sendServerBootstrap,receiveServerBootstrap;

    public void run(){

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        sendServerBootstrap = new ServerBootstrap().group(bossGroup,workerGroup);


    }
}
