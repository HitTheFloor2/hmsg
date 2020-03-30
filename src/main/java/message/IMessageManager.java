package message;

import protobuf.BaseMsgProto;

public interface IMessageManager {
    void sendMsg(Object msg,String receiver_address);
    void sendMsgWithReply(Object msg, String receiver_address, int timeout, Object baseMsgCallBack);
    void receiveMsg(Object msg);
}
