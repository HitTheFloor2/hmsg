package message;

public interface BaseMsgCallBack {
    public void withMsgReceivedComplete(MsgWaiter o);
    public void withMsgReceivedFail(MsgWaiter o);
}
