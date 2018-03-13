package cn.xianyijun.wisp.store.result;

import cn.xianyijun.wisp.store.status.GetMessageStatus;
import cn.xianyijun.wisp.store.stats.BrokerStatsManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xianyijun
 */
@NoArgsConstructor
@Getter
@ToString
public class GetMessageResult {


    private final List<SelectMappedBufferResult> messageMappedList =
            new ArrayList<SelectMappedBufferResult>(100);

    private final List<ByteBuffer> messageBufferList = new ArrayList<ByteBuffer>(100);

    @Setter
    private GetMessageStatus status;
    @Setter
    private long nextBeginOffset;
    @Setter
    private long minOffset;
    @Setter
    private long maxOffset;

    private int bufferTotalSize = 0;

    @Setter
    private boolean suggestPullingFromSlave = false;

    private int msgCount4Commercial = 0;

    public int getMessageCount() {
        return this.messageMappedList.size();
    }

    public void release() {
        for (SelectMappedBufferResult select : this.messageMappedList) {
            select.release();
        }
    }


    public void addMessage(final SelectMappedBufferResult mappedBuffer) {
        this.messageMappedList.add(mappedBuffer);
        this.messageBufferList.add(mappedBuffer.getByteBuffer());
        this.bufferTotalSize += mappedBuffer.getSize();
        this.msgCount4Commercial += (int) Math.ceil(
                mappedBuffer.getSize() / BrokerStatsManager.SIZE_PER_COUNT);
    }
}
