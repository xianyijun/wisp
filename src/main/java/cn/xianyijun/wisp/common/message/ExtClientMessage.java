package cn.xianyijun.wisp.common.message;

public class ExtClientMessage extends ExtMessage {

    public String getOffsetMsgId() {
        return super.getMsgId();
    }

    public void setOffsetMsgId(String offsetMsgId) {
        super.setMsgId(offsetMsgId);
    }

    @Override
    public String getMsgId() {
        String uniqueID = MessageClientIDSetter.getUniqueID(this);
        if (uniqueID == null) {
            return this.getOffsetMsgId();
        } else {
            return uniqueID;
        }
    }

    @Override
    public void setMsgId(String msgId) {
    }
}
