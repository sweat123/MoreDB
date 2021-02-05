package com.laomei.moredb.server.raft;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.laomei.moredb.common.bean.MoreDBProto;
import com.laomei.moredb.common.bean.MoreDBProto.MoreDBRequest;
import com.laomei.moredb.common.bean.MoreDBProto.MoreDBResponse;
import com.laomei.moredb.server.service.RocksDBStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ratis.proto.RaftProtos;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.protocol.RaftClientRequest;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.storage.RaftStorage;
import org.apache.ratis.statemachine.StateMachineStorage;
import org.apache.ratis.statemachine.TransactionContext;
import org.apache.ratis.statemachine.impl.BaseStateMachine;
import org.apache.ratis.statemachine.impl.SimpleStateMachineStorage;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author luobo.hwz on 2021/01/19 16:38
 */
@Slf4j
public class RocksDBStateMachine extends BaseStateMachine {

    private final SimpleStateMachineStorage machineStorage;

    private final RocksDBStorageService rocksDBStorageService;

    private final Function<Throwable, Message> exceptionHandler;

    private final Function<CompletableFuture<MoreDBResponse>, CompletableFuture<Message>> messageBuilder;

    public RocksDBStateMachine() {
        this.machineStorage = new SimpleStateMachineStorage();
        this.rocksDBStorageService = new RocksDBStorageService();
        this.exceptionHandler = t -> error("execute request failed; " + t.toString());
        this.messageBuilder = rspFuture -> rspFuture
            .thenApplyAsync(message -> ByteString.copyFrom(message.toByteArray()))
                .thenApplyAsync(Message::valueOf)
                .exceptionally(t -> {
                    log.error("async error: ", t);
                    return exceptionHandler.apply(t);
                });
        log.info("build rocks db state machine success");
    }

    @Override
    public void initialize(RaftServer raftServer, RaftGroupId raftGroupId, RaftStorage storage) throws IOException {
        super.initialize(raftServer, raftGroupId, storage);
        machineStorage.init(storage);
    }

    @Override
    public StateMachineStorage getStateMachineStorage() {
        return machineStorage;
    }

    @Override
    public TransactionContext startTransaction(RaftClientRequest request) throws IOException {
        return TransactionContext.newBuilder()
                .setClientRequest(request)
                .setStateMachine(this)
                .setServerRole(RaftProtos.RaftPeerRole.LEADER)
                .setLogData(request.getMessage().getContent())
                .build();
    }

    @Override
    public CompletableFuture<Message> query(Message message) {
        try {
            final MoreDBRequest request = MoreDBRequest.parseFrom(message.getContent().toByteArray());
            if (!MoreDBProto.Type.READ.equals(request.getCmdType())) {
                final String msg = "cmd type is not read, type; '" + request.getCmdType() + "'";
                log.error(msg);
                return CompletableFuture.completedFuture(error(msg));
            }
            return executeRequest(request);
        } catch (InvalidProtocolBufferException e) {
            log.error("query failed", e);
            return CompletableFuture.completedFuture(error("query failed; " + e));
        }
    }

    @Override
    public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
        final ByteString messageContent = trx.getStateMachineLogEntry().getLogData();
        try {
            final MoreDBRequest request = MoreDBRequest.parseFrom(messageContent.toByteArray());
            if (request.getCmdType().equals(MoreDBProto.Type.READ)) {
                log.error("cmd type is read, can not execute request");
                return CompletableFuture.completedFuture(error("cmd type is read"));
            }
            return executeRequest(request);
        } catch (InvalidProtocolBufferException e) {
            log.error("apply transaction failed", e);
            return CompletableFuture.completedFuture(error("apply transaction failed; " + e));
        }
    }

    @Override
    public void close() throws IOException {
        rocksDBStorageService.close();
    }

    private CompletableFuture<Message> executeRequest(final MoreDBRequest request) {
        log.info("execute request, request body: {}", request);
        if (request.getCmdType().equals(MoreDBProto.Type.READ)) {
            final CompletableFuture<MoreDBResponse> future = rocksDBStorageService
                    .readAsync(request.getReadRequestList())
                    .thenApplyAsync(readResponses -> MoreDBResponse
                            .newBuilder()
                            .setSuccess(true)
                            .addAllReadResponse(readResponses)
                            .build()
                    );
            return buildMessage(future);
        }
        final CompletableFuture<? extends GeneratedMessageV3> future;
        switch (request.getCmdType()) {
            case UPDATE:
                future = rocksDBStorageService.updateAsync(request.getUpdateRequestList());
                break;
            case DELETE:
                future = rocksDBStorageService.deleteAsync(request.getDeleteRequestList());
                break;
            default:
                log.error("illegal request type");
                return CompletableFuture.completedFuture(error("illegal request type"));
        }
        final MoreDBResponse.Builder builder = MoreDBResponse.newBuilder();
        final CompletableFuture<MoreDBResponse> rspFuture = future
                .thenApplyAsync(message -> {
                    if (request.getCmdType().equals(MoreDBProto.Type.UPDATE)) {
                        builder.setUpdateResponse((MoreDBProto.UpdateResponse) message);
                    } else {
                        builder.setDeleteResponse((MoreDBProto.DeleteResponse) message);
                    }
                    return builder.setSuccess(true).build();
                });
        return buildMessage(rspFuture);
    }

    private CompletableFuture<Message> buildMessage(final CompletableFuture<MoreDBResponse> future) {
        return messageBuilder.apply(future);
    }

    private Message error(final String msg) {
        return Message.valueOf(ByteString.copyFrom(
                MoreDBResponse.newBuilder()
                        .setSuccess(false)
                        .setMsg(msg)
                        .build()
                        .toByteArray()
        ));
    }
}
