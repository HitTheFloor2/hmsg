package server;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import log.Log;
import protobuf.BaseMsgProto;

public class TestServerClientHandler extends ChannelHandlerAdapter {
    public TestServer testServer;

    public TestServerClientHandler(TestServer testServer){
        this.testServer = testServer;
    }
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        //Log.logger.info("TestServerClientHandler.channelRegistered() this channel is "+ctx.channel().toString());
        Log.info(testServer.id,"channelRegistered "+ctx.channel().toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        testServer.messageManager.receiveMsg(msg);
    }
}
