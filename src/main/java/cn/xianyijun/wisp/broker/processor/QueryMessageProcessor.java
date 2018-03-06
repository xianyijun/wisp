package cn.xianyijun.wisp.broker.processor;

import cn.xianyijun.wisp.broker.BrokerController;
import cn.xianyijun.wisp.broker.pagecache.OneMessageTransfer;
import cn.xianyijun.wisp.broker.pagecache.QueryMessageTransfer;
import cn.xianyijun.wisp.common.MixAll;
import cn.xianyijun.wisp.common.protocol.RequestCode;
import cn.xianyijun.wisp.common.protocol.ResponseCode;
import cn.xianyijun.wisp.common.protocol.header.QueryMessageRequestHeader;
import cn.xianyijun.wisp.common.protocol.header.QueryMessageResponseHeader;
import cn.xianyijun.wisp.common.protocol.header.ViewMessageRequestHeader;
import cn.xianyijun.wisp.exception.RemotingCommandException;
import cn.xianyijun.wisp.remoting.netty.NettyRequestProcessor;
import cn.xianyijun.wisp.remoting.protocol.RemotingCommand;
import cn.xianyijun.wisp.store.QueryMessageResult;
import cn.xianyijun.wisp.store.SelectMappedBufferResult;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.FileRegion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xianyijun
 */
@RequiredArgsConstructor
@Slf4j
public class QueryMessageProcessor implements NettyRequestProcessor {

    private final BrokerController brokerController;

    @Override
    public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
        switch (request.getCode()) {
            case RequestCode.QUERY_MESSAGE:
                return this.queryMessage(ctx, request);
            case RequestCode.VIEW_MESSAGE_BY_ID:
                return this.viewMessageById(ctx, request);
            default:
                break;
        }
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
    public RemotingCommand queryMessage(ChannelHandlerContext ctx, RemotingCommand request) {
        final RemotingCommand response =
                RemotingCommand.createResponseCommand(QueryMessageResponseHeader.class);
        final QueryMessageResponseHeader responseHeader =
                (QueryMessageResponseHeader) response.getCustomHeader();
        final QueryMessageRequestHeader requestHeader =
                (QueryMessageRequestHeader) request
                        .decodeCommandCustomHeader(QueryMessageRequestHeader.class);

        response.setOpaque(request.getOpaque());

        String isUniqueKey = request.getExtFields().get(MixAll.UNIQUE_MSG_QUERY_FLAG);
        if (isUniqueKey != null && "true".equals(isUniqueKey)) {
            requestHeader.setMaxNum(this.brokerController.getMessageStoreConfig().getDefaultQueryMaxNum());
        }

        final QueryMessageResult queryMessageResult =
                this.brokerController.getMessageStore().queryMessage(requestHeader.getTopic(),
                        requestHeader.getKey(), requestHeader.getMaxNum(), requestHeader.getBeginTimestamp(),
                        requestHeader.getEndTimestamp());
        assert queryMessageResult != null;

        responseHeader.setIndexLastUpdatePhyOffset(queryMessageResult.getIndexLastUpdatePhyOffset());
        responseHeader.setIndexLastUpdateTimestamp(queryMessageResult.getIndexLastUpdateTimestamp());

        if (queryMessageResult.getBufferTotalSize() > 0) {
            response.setCode(ResponseCode.SUCCESS);
            response.setRemark(null);

            try {
                FileRegion fileRegion =
                        new QueryMessageTransfer(response.encodeHeader(queryMessageResult
                                .getBufferTotalSize()), queryMessageResult);
                ctx.channel().writeAndFlush(fileRegion).addListener((ChannelFutureListener) future -> {
                    queryMessageResult.release();
                    if (!future.isSuccess()) {
                        log.error("transfer query message by page cache failed, ", future.cause());
                    }
                });
            } catch (Throwable e) {
                log.error("", e);
                queryMessageResult.release();
            }

            return null;
        }

        response.setCode(ResponseCode.QUERY_NOT_FOUND);
        response.setRemark("can not find message, maybe time range not correct");
        return response;
    }

    public RemotingCommand viewMessageById(ChannelHandlerContext ctx, RemotingCommand request)
            throws RemotingCommandException {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);
        final ViewMessageRequestHeader requestHeader =
                (ViewMessageRequestHeader) request.decodeCommandCustomHeader(ViewMessageRequestHeader.class);

        response.setOpaque(request.getOpaque());

        final SelectMappedBufferResult selectMappedBufferResult =
                this.brokerController.getMessageStore().selectOneMessageByOffset(requestHeader.getOffset());
        if (selectMappedBufferResult != null) {
            response.setCode(ResponseCode.SUCCESS);
            response.setRemark(null);

            try {
                FileRegion fileRegion =
                        new OneMessageTransfer(response.encodeHeader(selectMappedBufferResult.getSize()),
                                selectMappedBufferResult);
                ctx.channel().writeAndFlush(fileRegion).addListener((ChannelFutureListener) future -> {
                    selectMappedBufferResult.release();
                    if (!future.isSuccess()) {
                        log.error("Transfer one message from page cache failed, ", future.cause());
                    }
                });
            } catch (Throwable e) {
                log.error("", e);
                selectMappedBufferResult.release();
            }

            return null;
        } else {
            response.setCode(ResponseCode.SYSTEM_ERROR);
            response.setRemark("can not find message by the offset, " + requestHeader.getOffset());
        }

        return response;
    }
}
