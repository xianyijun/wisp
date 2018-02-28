package cn.xianyijun.wisp.store;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
public class GetMessageResult {


    private final List<SelectMappedBufferResult> messageMapedList =
            new ArrayList<SelectMappedBufferResult>(100);

    private final List<ByteBuffer> messageBufferList = new ArrayList<ByteBuffer>(100);

    private GetMessageStatus status;
    private long nextBeginOffset;
    private long minOffset;
    private long maxOffset;

    private int bufferTotalSize = 0;

    private boolean suggestPullingFromSlave = false;

    private int msgCount4Commercial = 0;

    public int getMessageCount() {
        return this.messageMapedList.size();
    }

    public void release() {
        for (SelectMappedBufferResult select : this.messageMapedList) {
            select.release();
        }
    }
}
