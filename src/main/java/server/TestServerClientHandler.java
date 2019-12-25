package server;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import log.Log;
import protobuf.SimpleStringMessageProto;

public class TestServerClientHandler  extends ChannelHandlerAdapter {
    public TestServerClient testServerClient;

    public TestServerClientHandler(TestServerClient testServerClient){
        this.testServerClient = testServerClient;
    }
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        Log.logger.info("TestServerClientHandler.channelRegistered() this channel is"+ctx.channel().toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        SimpleStringMessageProto.SimpleStringMessage simpleStringMessage =
                (SimpleStringMessageProto.SimpleStringMessage) msg;
        Log.logger.info("TestServerClientHandler.channelRead() receive "+simpleStringMessage.toString()+
                " from "+ctx.channel().toString());

    }
}
