package server;
import log.Log;
import protobuf.*;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class TestServerHandler extends ChannelHandlerAdapter {
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
        SimpleStringMessageProto.SimpleStringMessage simpleStringMessage =
                (SimpleStringMessageProto.SimpleStringMessage) msg;
        Log.logger.info("TestServerHandler.channelRead() receive "+simpleStringMessage.toString()+
                "from"+ctx.channel().toString());

    }
}
