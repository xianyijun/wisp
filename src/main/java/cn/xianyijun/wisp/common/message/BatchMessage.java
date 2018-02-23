package cn.xianyijun.wisp.common.message;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class BatchMessage extends Message implements Iterable<Message>{

    @NonNull
    private final List<Message> messages;

    @Override
    public Iterator<Message> iterator() {
        return messages.iterator();
    }
}
