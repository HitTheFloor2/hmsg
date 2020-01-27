package server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import log.Log;

public class BaseServerClientHandler extends ChannelInboundHandlerAdapter {
    public BaseServer baseServer;

    public BaseServerClientHandler(BaseServer baseServer){
        this.baseServer = baseServer;
    }
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        //Log.logger.info("TestServerClientHandler.channelRegistered() this channel is "+ctx.channel().toString());
        Log.info(baseServer.id,"channelRegistered "+ctx.channel().toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        baseServer.messageManager.receiveMsg(msg);
    }
}
