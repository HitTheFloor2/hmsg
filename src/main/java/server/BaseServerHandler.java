package server;
import io.netty.channel.ChannelInboundHandlerAdapter;
import log.Log;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

public class BaseServerHandler extends ChannelInboundHandlerAdapter {
    public BaseServer baseServer;

    public BaseServerHandler(BaseServer baseServer){
        this.baseServer = baseServer;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        baseServer.messageManager.receiveMsg(msg);
    }

}
