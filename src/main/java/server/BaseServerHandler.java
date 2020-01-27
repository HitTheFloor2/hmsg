package server;
import io.netty.channel.ChannelInboundHandlerAdapter;
import log.Log;
import io.netty.channel.ChannelHandlerContext;

public class BaseServerHandler extends ChannelInboundHandlerAdapter {
    public BaseServer baseServer;
    //public TestServerHandler(){}
    public BaseServerHandler(BaseServer baseServer){
        this.baseServer = baseServer;
    }
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Log.logger.info("TestServerHandler.channelRegistered() this channel is"+ctx.channel().toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        baseServer.messageManager.receiveMsg(msg);
    }
}
