package message;

import java.util.concurrent.CountDownLatch;

public class BaseMsgReplyHelper {
    /**
     * Overview:
     * 投票等环节中存在消息发出后需要等待一个或多个消息回复的情况，
     * 初始消息（如发起投票的消息）发出后，在MessageManager中异步线程等待，
     * 并在BaseMsgReplyHelper中添加一个记录
     * MessageManager在receiveMsg中收到Msg后检查BaseMsgReplyHelper所有记录，找到对应的记录并且操作
     * 当所有的回复信息都收到后，删除对应的记录并通知初始消息的线程，结束等待
     * */

    class MsgWaiter{
        
        int totalReply;
        CountDownLatch countDownLatch;

    }
}
