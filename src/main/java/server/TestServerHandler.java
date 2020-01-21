package server;
import io.netty.channel.ChannelInboundHandlerAdapter;
import log.Log;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class TestServerHandler extends ChannelInboundHandlerAdapter {
    public TestServer testServer;
    //public TestServerHandler(){}
    public TestServerHandler(TestServer testServer){
        this.testServer = testServer;
    }
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Log.logger.info("TestServerHandler.channelRegistered() this channel is"+ctx.channel().toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        testServer.messageManager.receiveMsg(msg);
    }
}
