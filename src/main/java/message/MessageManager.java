package message;

import annotation.Blocking;
import annotation.NonBlocking;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import log.Log;
import server.BaseServer;
import protobuf.BaseMsgProto;
import server.BaseServerClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MessageManager 负责发送和接收消息， 每一个Server独立拥有一个MessageManager
 * 接收消息的来源为netty的channel
 * MessageManager作为消息收发的工具类，原则上不干预BaseServer中的数据
 * 在接收到普通消息时，回复；接收到普通消息的回复消息时，通知等待器
 *
 * */
public class MessageManager implements IMessageManager{
    // 当前BaseServer引用
    private BaseServer baseServer;
    // 当前BaseServerClient引用
    private BaseServerClient baseServerClient;
    // 当前BaseServer发送的BaseMsg个数
    private AtomicLong msgSequenceNum;
    // 处理发送消息的线程池
    private ExecutorService senderExecutor;
    // 处理接受消息的线程池
    private ExecutorService receiveExecutor;
    // 同步回复信息工具
    BaseMsgReplyHelper baseMsgReplyHelper;
    // 解析JSON工具，按照文档的建议是全局单例
    private ObjectMapper mapper;

    public MessageManager(BaseServer baseServer){
        this.baseServer = baseServer;
        this.baseServerClient = baseServer.client;
        this.senderExecutor = Executors.newCachedThreadPool();
        this.receiveExecutor = Executors.newCachedThreadPool();
        this.msgSequenceNum = new AtomicLong(0);
        this.mapper = new ObjectMapper();
        this.baseMsgReplyHelper = new BaseMsgReplyHelper(baseServer);
    }

    /**
     * 获取当前BaseServer的顺序增长的 msgid
     * */
    public synchronized Long getMsgID(){
        return msgSequenceNum.get() ;
    }
    /**
     * msgId的自增
     * */
    public synchronized Long addMsgID(){
        return msgSequenceNum.getAndAdd(1);
    }

    /**
     * 检查Channel的可用性
     * */
    private void checkChannel(){

    }

    /**
     * 发送消息，不等待回复，不关心是否成功。
     * 使用channel的writeAndFlush方法，不阻塞
     * 由于在构建msg时已经可以确定消息的发送对象地址，故在MessageMananger中不做判断
     * @Param message 已经封装好的待发送消息对象
     * @Param receiver_address 被发送的节点address
     * */
    @NonBlocking
    @Override
    public void sendMsg(Object message,String receiver_address){
        BaseMsgProto.BaseMsg msg = (BaseMsgProto.BaseMsg) message;
        if(null == receiver_address){
            baseServerClient.broadcasting(msg);
        }else{
            baseServerClient.singleSend(msg,receiver_address);
        }
        return;
    }

    /**
     * 群发消息
     * @param msg 消息
     * @param receivers 收取列表
     * */
    @NonBlocking
    public void sendMsg(BaseMsgProto.BaseMsg msg, List<String> receivers){
        for(String i : receivers){
            baseServerClient.singleSend(msg,i);
        }
    }

    /**
     * 等待回复的消息发送，用于投票等环节
     * 消息发送之后需要等待对应的回复消息，取得相应的消息
     * 涉及等待、超时等耗时操作，用线程池，并且本函数也会超时关闭
     * 注意timeout应大于msg的timeout，因为此处的timeout是任务的timeout而不是等待回复的timeout
     * @param message 发送的消息
     * @param receiver_address 消息的接收者
     * @param timeout 本次发送的等待超时时限，单位为毫秒
     * */
    @NonBlocking
    @Override
    public void sendMsgWithReply(Object message, String receiver_address, int timeout, Object cb){
        BaseMsgProto.BaseMsg msg = (BaseMsgProto.BaseMsg) message;
        BaseMsgCallBack baseMsgCallBack = (BaseMsgCallBack) cb;
        FutureTask futureTask = new FutureTask(new Callable() {
            @Override
            public Object call() throws Exception {
                // 新建一个countdownlatch，用于同步‘接收回复消息’这一任务的完成
                CountDownLatch senderCountDownLatch = new CountDownLatch(1);
                // 预先开启‘接收回复消息’这一任务
                MsgWaiter msgWaiter = baseMsgReplyHelper.registerReplyHelper(msg,senderCountDownLatch);
                // 发送信息
                sendMsg(msg,receiver_address);
                // 等待‘接收回复消息’任务完成或超时
                try {
                    senderCountDownLatch.await(timeout,TimeUnit.MILLISECONDS);

                } catch (InterruptedException e) {
                    //TODO
                }finally {
                    // MsgWaiter等待结束，检查消息
                    if(msgWaiter.status == -1){
                        baseMsgCallBack.withMsgReceivedFail(msgWaiter);
                    }else{
                        baseMsgCallBack.withMsgReceivedComplete(msgWaiter);
                    }

                }

                return null;
            }
        });
        senderExecutor.submit(futureTask);
    }
    /**
     * 每一个server实例会拥有一个MessageManager，用来管理消息的收发
     * receiveMsg被netty中handler的channelRead调用，是非阻塞的
     * 这个流程对于REPLY形式的消息，处理的逻辑可以由sendMsgWithReply中的回调函数确定
     * 对于已经在BaseMsgUtil中规定的msgType，会固定回复消息
     * @param Msg 从Channel中获取的消息
     * */
    @Override
    public void receiveMsg(Object Msg) {
        BaseMsgProto.BaseMsg received_msg = (BaseMsgProto.BaseMsg) Msg;
        // 每当收到信息之后，尝试添加channel
        baseServerClient.asyncAddServer(received_msg.getServerSenderAddress());
        // 读取msgType作为分类依据
        int msgType = received_msg.getMsgType();
        Log.info(baseServer.address, "MessageManager.receiveMsg : " + BaseMsgUtil.ToString(received_msg));
        try{
            // 分类处理
            switch (msgType) {
                // CLUSTER：被其他节点询问本节点所知的集群信息
                case BaseMsgUtil.CLUSTER:{
                    Log.info(baseServer.address,"RECEIVE CLUSTER:"+received_msg.getServerSenderAddress()+":"+received_msg.getContent());
                    // 打包当前节点所了解的集群信息
                    List<String> stringList = new ArrayList<>();
                    // 不需要自己的地址，因为如果能够传递过来，一定是发送者已知自己的地址
                    for(String address : baseServerClient.getChannelMap().keySet()){
                        stringList.add(address);
                    }
                    // 转为字符串
                    String jsonString = mapper.writeValueAsString(stringList);
                    BaseMsgProto.BaseMsg cluster_reply = BaseMsgUtil.getInstance(
                            BaseMsgUtil.CLUSTER_REPLY,
                            addMsgID(),
                            baseServer.address,
                            received_msg.getServerSenderAddress(),
                            received_msg.getMsgUuid(),
                            3000,
                            jsonString
                    );
                    sendMsg(cluster_reply,cluster_reply.getServerSingleReceiverAddress());
                    Log.info(baseServer.address,"SEND CLUSTER_REPLY -> "+cluster_reply.getServerSingleReceiverAddress());
                    break;
                }
                // CLUSTER_REPLY：收到询问集群信息后的回复
                case BaseMsgUtil.CLUSTER_REPLY:{
                    Log.info(baseServer.address,"RECEIVE CLUSTER_REPLY:"+received_msg.getServerSenderAddress()+":"+received_msg.getContent());
                    // 添加channel
                    List<String> stringList = mapper.readValue(received_msg.getContent(),List.class);
                    for(String address : stringList){
                        baseServerClient.asyncAddServer(address);
                    }
                    baseMsgReplyHelper.replyNotify(received_msg);
                    break;
                }

                // ECHO：回复一个ECHO_REPLY
                case BaseMsgUtil.ECHO:{
                    BaseMsgProto.BaseMsg echo_reply = BaseMsgUtil.getInstance(
                            BaseMsgUtil.ECHO_REPLY,
                            addMsgID(),
                            baseServer.address,
                            received_msg.getServerSenderAddress(),
                            received_msg.getMsgUuid(),
                            3000,
                            baseServer.address+": echo reply!"
                    );
                    sendMsg(echo_reply,echo_reply.getServerSingleReceiverAddress());
                    Log.debug(baseServer.address,"SEND ECHO_REPLY -> "+echo_reply.getServerSingleReceiverAddress());
                    break;
                }
                // ECHO_REPLY：ECHO的回复
                case BaseMsgUtil.ECHO_REPLY:{
                    baseMsgReplyHelper.replyNotify(received_msg);
                    break;
                }
                case BaseMsgUtil.VOTE:{
                    break;}
                case BaseMsgUtil.VOTE_REPLY: {
                    break;
                }
                default:{

                }


            }
        }catch (Exception e){

        }

    }

}
