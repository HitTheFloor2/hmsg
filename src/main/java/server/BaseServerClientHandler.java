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
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //删除对应的Channel，由于只有BaseServerClient端保存channelMap，所以只需要此处删除即可
        baseServer.client.removeChannel(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        baseServer.messageManager.receiveMsg(msg);
    }
}
