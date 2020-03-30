package message;

import com.mchange.lang.ByteUtils;
import protobuf.BaseMsgProto;
import util.MUtil;

import java.security.MessageDigest;


/**
 * 简便使用BaseMsg的工具类
 * 不推荐包含业务逻辑的判断出现在BaseMsgUtil
 * */
public class BaseMsgUtil {
    /**
     * Type of BaseMsg id and usage
     * */
    // 询问集群信息
    public static final int CLUSTER = 0;
    public static final int CLUSTER_REPLY = 1;
    // 定时消息与恢复，用于保活，强制要求设置timeout
    public static final int ECHO = 2;
    public static final int ECHO_REPLY = 3;
    // 投票，等待回复
    public static final int VOTE = 4;
    public static final int VOTE_REPLY = 5;
    // 加入集群
    public static final int JOIN = 6;
    public static final int JOIN_REPLY = 7;


    //用于生成msgUUID的常量
    public static final long UUID_INIT = 1L << 63;

    /**
     * 打印BaseMsg
     * @param baseMsg 被转换成String的BaseMsg
     * @return String类型的结果
     * */
    public static synchronized String ToString(BaseMsgProto.BaseMsg baseMsg){
        StringBuilder res = new StringBuilder();
        res.append("msg_type:"+baseMsg.getMsgType()+" , ");
        res.append("msg_sequence_number:"+baseMsg.getMsgSequenceNumber()+" , ");
        res.append("msg_uuid:"+baseMsg.getMsgUuid()+" , ");
        res.append("msg_reply_uuid:"+baseMsg.getMsgReplyUuid()+" , ");
        res.append("need_reply_num:"+baseMsg.getNeedReplyNum()+" , ");
        res.append("server_sender_address:"+baseMsg.getServerSenderAddress()+" , ");
        res.append("server_single_receiver_address:"+baseMsg.getServerSingleReceiverAddress()+" , ");
        res.append("server_multi_receiver_address:"+baseMsg.getServerMultiReceiverAddressList().toString()+" , ");
        res.append("msg_timeout:"+baseMsg.getTimeout()+" , ");
        res.append("msg_timestamp:"+baseMsg.getTimestamp()+" , ");
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
     * @param senderAddress
     * @param receiverAddress
     * @param timeout
     * @return
     * */
    public static synchronized BaseMsgProto.BaseMsg getInstance(
            int msgType,
            Long msgSeqNum,
            String senderAddress,
            String receiverAddress,
            int needReplyNum,
            int timeout,
            String content){
        BaseMsgProto.BaseMsg.Builder builder =
                BaseMsgProto.BaseMsg.newBuilder();
        //消息类型
        builder.setMsgType(msgType);
        //消息Id
        builder.setMsgSequenceNumber(msgSeqNum);
        //发送者id
        builder.setServerSenderAddress(senderAddress);
        //接收者id
        builder.setServerSingleReceiverAddress(receiverAddress);
        //需要被回复的数量
        builder.setNeedReplyNum(needReplyNum);
        //超时
        builder.setTimeout(timeout);
        //时间戳
        builder.setTimestamp(System.currentTimeMillis());
        //uuid
        builder.setMsgUuid(createBaseMsgUUID(senderAddress,msgSeqNum,builder.getTimestamp()));
        //内容
        builder.setContent(content);

        BaseMsgProto.BaseMsg msg = builder.build();
        return msg;
    }
    /**
     * 生成BaseMsg实例 （建议用于回复单个消息）
     * @param msgType
     * @param msgSeqNum
     * @param senderAddress
     * @param receiverAddress
     * @param timeout
     * @return
     * */
    public static synchronized BaseMsgProto.BaseMsg getInstance(
            int msgType,
            Long msgSeqNum,
            String senderAddress,
            String receiverAddress,
            long replyMsgUUid,
            int timeout,
            String content){
        BaseMsgProto.BaseMsg.Builder builder =
                BaseMsgProto.BaseMsg.newBuilder();
        //消息类型
        builder.setMsgType(msgType);
        //消息Id
        builder.setMsgSequenceNumber(msgSeqNum);
        //发送者id
        builder.setServerSenderAddress(senderAddress);
        //接收者id
        builder.setServerSingleReceiverAddress(receiverAddress);
        //被回复的msg的uuid
        builder.setMsgReplyUuid(replyMsgUUid);
        //超时
        builder.setTimeout(timeout);
        //时间戳
        builder.setTimestamp(System.currentTimeMillis());
        //uuid
        builder.setMsgUuid(createBaseMsgUUID(senderAddress,msgSeqNum,builder.getTimestamp()));
        //内容
        builder.setContent(content);

        BaseMsgProto.BaseMsg msg = builder.build();
        return msg;
    }

    /**
     * 使用msg的发送者地址，当前序列号和时间戳生成一个全局独一无二的uuid
     * uuid长度为64位，使用long类型存储
     * 按高位到低位顺序：0-15为发送者地址hash值，16-31为时间戳的后16位，32-64是序列号
     * @param server_sender_address
     * @param msgSeqNum
     * @param timestamp
     * @return
     * */
    public static synchronized long createBaseMsgUUID(String server_sender_address,Long msgSeqNum,long timestamp){
        //TODO 优化

        // address处理
        String part_address1 = "";
        String part_address = "";
        try{
            part_address1 = MUtil.getMD5Str(server_sender_address);
        }catch (Exception e){
            part_address1 = "0000000000000000";
        }
        for(int i = 0;i < 16;i++){
            if(part_address1.length() > i){
                part_address += part_address1.charAt(i);
            }
        }
        // msgSeqNum转字符串并截取后32位，不够则补上零
        String part_seq1 = Long.toBinaryString(msgSeqNum);
        String part_seq = "";
        for(int i = 0;i < 16;i++){
            if(part_seq1.length() > i){
                part_seq += part_seq1.charAt(i);
            }
        }
        // 处理时间戳，从后往前（因为时间戳前方的数据段变动小）
        String part_ts1 = Long.toBinaryString(timestamp);
        String part_ts = "";
        for(int i = 0;i < 32;i++){
            if(part_ts1.length()-1 > i){
                part_ts += part_ts1.charAt(part_ts1.length()-i-1);
            }else{
                part_ts += "0";
            }
        }
        String all = part_address+part_seq+part_ts;
        return Long.parseUnsignedLong(all,2);
    }

}
