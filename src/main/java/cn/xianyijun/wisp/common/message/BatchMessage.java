package cn.xianyijun.wisp.common.message;

import cn.xianyijun.wisp.common.MixAll;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class BatchMessage extends Message implements Iterable<Message> {

    @NonNull
    private final List<Message> messages;

    public static BatchMessage generateFromList(Collection<Message> messages) {
        assert messages != null;
        assert messages.size() > 0;
        List<Message> messageList = new ArrayList<Message>(messages.size());
        Message first = null;
        for (Message message : messages) {
            if (message.getDelayTimeLevel() > 0) {
                throw new UnsupportedOperationException("TimeDelayLevel in not supported for batching");
            }
            if (message.getTopic().startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
                throw new UnsupportedOperationException("Retry Group is not supported for batching");
            }
            if (first == null) {
                first = message;
            } else {
                if (!first.getTopic().equals(message.getTopic())) {
                    throw new UnsupportedOperationException("The topic of the messages in one batch should be the same");
                }
                if (first.isWaitStoreMsgOK() != message.isWaitStoreMsgOK()) {
                    throw new UnsupportedOperationException("The waitStoreMsgOK of the messages in one batch should the same");
                }
            }
            messageList.add(message);
        }
        BatchMessage batchMessage = new BatchMessage(messageList);

        batchMessage.setTopic(first.getTopic());
        batchMessage.setWaitStoreMsgOK(first.isWaitStoreMsgOK());
        return batchMessage;
    }

    @Override
    public Iterator<Message> iterator() {
        return messages.iterator();
    }

    public byte[] encode() {
        return MessageDecoder.encodeMessages(messages);
    }

}
