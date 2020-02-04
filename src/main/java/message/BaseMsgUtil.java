package message;

import protobuf.BaseMsgProto;


/**
 * 简便使用BaseMsg的工具类
 * 不推荐包含业务逻辑的判断出现在BaseMsgUtil
 * */
public class BaseMsgUtil {
    /**
     * Type of BaseMsg id and usage
     * */
    // Channel建立后，主动建立的一端需要向另一端发送自己的服务端地址
    public static final int SOCKETADDRESS_ANNOUNCE = 1;
    // 定时消息与恢复，用于保活，强制要求设置timeout
    public static final int ECHO = 2;
    public static final int ECHO_REPLY = 3;
    // 投票，等待回复
    public static final int VOTE = 4;
    public static final int VOTE_REPLY = 5;
    // 单播无回复消息
    public static final int SIMPLE_NO_REPLY = 4;
    // 单播有回复消息
    public static final int SIMPLE = 5;
    public static final int SIMPLE_REPLY = 6;


    //用于生成msgUUID的常量
    public static final long UUID_INIT = 1L << 63;

    /**
     * 打印BaseMsg
     * @param baseMsg 被转换成String的BaseMsg
     * @return String类型的结果
     * */
    public static synchronized String ToString(BaseMsgProto.BaseMsg baseMsg){
        StringBuilder res = new StringBuilder();
        res.append("msg_type:"+baseMsg.getMsgType()+"\n");
        res.append("msg_sequence_number:"+baseMsg.getMsgSequenceNumber()+"\n");
        res.append("msg_uuid:"+baseMsg.getMsgUuid()+"\n");
        res.append("msg_reply_uuid:"+baseMsg.getMsgReplyUuid()+"\n");
        res.append("need_reply_num:"+baseMsg.getNeedReplyNum()+"\n");
        res.append("server_sender_id:"+baseMsg.getServerSenderId()+"\n");
        res.append("server_single_receiver_id:"+baseMsg.getServerSingleReceiverId()+"\n");
        res.append("server_multi_receiver_id:"+baseMsg.getServerMultiReceiverIdList().toString()+"\n");
        res.append("msg_timeout:"+baseMsg.getTimeout()+"\n");
        res.append("msg_timestamp:"+baseMsg.getTimestamp()+"\n");
        res.append("msg_content:"+baseMsg.getContent().toString()+"\n");
        return res.toString();
    }

    /**
     * 生成一个默认值的BaseMsg的builder实例
     * */
    private static synchronized BaseMsgProto.BaseMsg.Builder getDefaultBuilder(){
        BaseMsgProto.BaseMsg.Builder builder = BaseMsgProto.BaseMsg.newBuilder();
        return builder;
    }

    /**
     * 生成BaseMsg实例 （建议用于发送单个消息）
     * @param msgType
     * @param msgSeqNum
     * @param senderId
     * @param receiverId
     * @param timeout
     * @return
     * */
    public static synchronized BaseMsgProto.BaseMsg getInstance(
            int msgType,
            int msgSeqNum,
            int senderId,
            int receiverId,
            int needReplyNum,
            int timeout){
        BaseMsgProto.BaseMsg.Builder builder =
                BaseMsgProto.BaseMsg.newBuilder();
        //消息类型
        builder.setMsgType(msgType);
        //消息Id
        builder.setMsgSequenceNumber(msgSeqNum);
        //发送者id
        builder.setServerSenderId(senderId);
        //接收者id
        builder.setServerSingleReceiverId(receiverId);
        //需要被回复的数量
        builder.setNeedReplyNum(needReplyNum);
        //超时
        builder.setTimeout(timeout);
        //时间戳
        builder.setTimestamp(System.currentTimeMillis());
        //uuid
        builder.setMsgUuid(createBaseMsgUUID(senderId,msgSeqNum,builder.getTimestamp()));

        BaseMsgProto.BaseMsg msg = builder.build();
        return msg;
    }
    /**
     * 生成BaseMsg实例 （建议用于回复单个消息）
     * @param msgType
     * @param msgSeqNum
     * @param senderId
     * @param receiverId
     * @param timeout
     * @return
     * */
    public static synchronized BaseMsgProto.BaseMsg getInstance(
            int msgType,
            int msgSeqNum,
            int senderId,
            int receiverId,
            long replyMsgUUid,
            int timeout){
        BaseMsgProto.BaseMsg.Builder builder =
                BaseMsgProto.BaseMsg.newBuilder();
        //消息类型
        builder.setMsgType(msgType);
        //消息Id
        builder.setMsgSequenceNumber(msgSeqNum);
        //发送者id
        builder.setServerSenderId(senderId);
        //接收者id
        builder.setServerSingleReceiverId(receiverId);
        //被回复的msg的uuid
        builder.setMsgReplyUuid(replyMsgUUid);
        //超时
        builder.setTimeout(timeout);
        //时间戳
        builder.setTimestamp(System.currentTimeMillis());
        //uuid
        builder.setMsgUuid(createBaseMsgUUID(senderId,msgSeqNum,builder.getTimestamp()));

        BaseMsgProto.BaseMsg msg = builder.build();
        return msg;
    }

    /**
     * 使用msg的发送者id，当前序列号和时间戳生成一个全局独一无二的uuid
     * uuid长度为64位，使用long类型存储
     * 后32位是序列号，前32位中第一位是1，1-7位是serverid，其余24位是stamp的后24位
     * @param server_sender_id
     * @param msgSeqNum
     * @param timestamp
     * @return
     * */
    public static synchronized long createBaseMsgUUID(int server_sender_id,int msgSeqNum,long timestamp){
        //TODO 优化
        long part1 = (long)msgSeqNum;
        String temp = Long.toBinaryString(timestamp);
        long part2 = Long.parseUnsignedLong(temp.substring(temp.length()-25,temp.length()-1),2) << 32;
        long part3 = (long)server_sender_id << 56;
        return UUID_INIT | part1 | part2 | part3;
    }

}
