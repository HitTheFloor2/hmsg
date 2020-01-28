package message;

import protobuf.BaseMsgProto;

public class BaseMsgUtil {
    /**
     * Type of BaseMsg id and usage
     * */
    // Channel建立后，主动建立的一端需要向另一端发送自己的服务端地址
    public static final int SOCKETADDRESS_ANNOUNCE = 1;
    // 定时消息与恢复，用于保活，强制要求设置timeout
    public static final int ECHO = 2;
    public static final int ECHO_REPLY = 3;
    // 单播无回复消息
    public static final int SIMPLE_NO_REPLY = 4;
    // 单播有回复消息
    public static final int SIMPLE = 5;
    public static final int SIMPLE_REPLY = 6;

    /**
     * 打印BaseMsg
     * */
    public String ToString(BaseMsgProto.BaseMsg baseMsg){
        return null;
    }
    /**
     * 生成BaseMsg实例
     * */
    public static BaseMsgProto.BaseMsg getInstance(
            int  msgType , int msgId , int senderId , int timeout){
        BaseMsgProto.BaseMsg.Builder builder =
                BaseMsgProto.BaseMsg.newBuilder();
        //消息类型
        builder.setMsgType(msgType);
        //消息Id
        builder.setMsgId(msgId);
        //发送者id
        builder.setSenderId(senderId);
        //超时
        builder.setTimeout(timeout);
        BaseMsgProto.BaseMsg msg = builder.build();
        return msg;
    }
    public static BaseMsgProto.BaseMsg getInstance(
            int  msgType , int msgId , int senderId , int receiverId ,int timeout){
        BaseMsgProto.BaseMsg.Builder builder =
                BaseMsgProto.BaseMsg.newBuilder();
        //消息类型
        builder.setMsgType(msgType);
        //消息Id
        builder.setMsgId(msgId);
        //发送者id
        builder.setSenderId(senderId);
        //接收者id
        builder.setSingleReceiverId(receiverId);
        //超时
        builder.setTimeout(timeout);
        BaseMsgProto.BaseMsg msg = builder.build();
        return msg;
    }


}
