package correspond;

import manager.MessageManager;
import protobuf.SimpleStringMessageProto;
import server.TestServer;

public class HandleMsg {
    public static void handleMsg(SimpleStringMessageProto.SimpleStringMessage simpleStringMessage, TestServer testServer){
        if(simpleStringMessage.getLength() == 1){
            //vote message
            WriteMsg.asyncSingleWrite(testServer,simpleStringMessage.getMsgID(),2,"vote reply");
            MessageManager.downCountDownLatch();
        }
    }
}
