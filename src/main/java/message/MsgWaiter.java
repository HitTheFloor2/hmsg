package message;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class MsgWaiter{
    /**
     * MsgWaiter 封装了需要被回复的BaseMsg相关信息
     * 在得到了所有的回复或超时后，自动销毁
     * 一个MsgWaiter对应一个需要回复的消息，并且把收到的回复消息中的数据进行存储
     * */
    // 消息的UUID
    private Long msgUUID;
    // 需要的回复个数
    private int totalReplyNum;
    // 超时
    public int timeout;
    // 状态码，-1代表出错，其余保留，目前只判断是否出错
    public Integer status = 0;
    // 同步回复消息
    public CountDownLatch countDownLatch;
    // 储存回复消息中的数据，key代表节点地址，value为回复内容（即消息对象）
    private ConcurrentHashMap<String,Object> container = new ConcurrentHashMap<>();
    /**
     * 初始化方法
     * */
    MsgWaiter(Long msgUUID,int totalReplyNum,int timeout){
        if(totalReplyNum > 0){
            this.totalReplyNum = totalReplyNum;
        }else{
            // totalReplyNum不合法，则只有超时可以解决
            totalReplyNum = 1000;
        }
        this.msgUUID = msgUUID;
        this.timeout = timeout;
        this.countDownLatch = new CountDownLatch(totalReplyNum);
    }

    /**
     * 强行指定状态
     * */
    public void updateStatus(int status){
        synchronized (this){
            this.status = status;
        }
    }

    public ConcurrentHashMap<String,Object> getContainer(){
        return container;
    }

    /**
     * 从回复的消息中更新对应MsgWaiter中的数据
     * @param address 数据来源（节点）
     * @param dataItem 数据条目
     * */
    public void insert(String address,Object dataItem){
        container.put(address,dataItem);
    }

    /**
     * MessageManager收到一个回复时调用一次，用于同步回复消息的数量
     * */
    public synchronized boolean countDown(){
        try{
            countDownLatch.countDown();
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }finally {
            return true;
        }

    }

}