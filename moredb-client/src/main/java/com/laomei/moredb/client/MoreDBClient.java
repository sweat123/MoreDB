package com.laomei.moredb.client;

import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.laomei.moredb.client.data.DeleteResponse;
import com.laomei.moredb.client.data.ReadResponse;
import com.laomei.moredb.client.data.UpdateResponse;
import com.laomei.moredb.common.bean.MoreDBProto.MoreDBResponse;
//import com.laomei.moredb.common.bean.Node;
import com.laomei.moredb.common.exception.MoreDBException;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import com.laomei.moredb.common.bean.MoreDBProto;
import com.laomei.moredb.common.bean.MoreDBProto.MoreDBRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcFactory;
import org.apache.ratis.protocol.*;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author luobo.hwz on 2021/02/03 8:41 PM
 */
@Slf4j
public class MoreDBClient implements Closeable {

//    public static void main(String[] args) throws ExecutionException, InterruptedException {
//        final ClientProperties properties = new ClientProperties();
//        properties.setGroupId("8f313a97-616a-45b1-afe2-d57a096b7d55");
//        final List<Node> nodes = new ArrayList<>();
//        nodes.add(new Node("node1", "localhost", 8081));
//        nodes.add(new Node("node2", "localhost", 8082));
//        nodes.add(new Node("node3", "localhost", 8083));
//        properties.setNodes(nodes);
//        final MoreDBClient moreDBClient = new MoreDBClient(properties);
//        moreDBClient.updateAsync(Collections.singletonMap("kkk", "aaa".getBytes())).get();
//        moreDBClient.readAsync(Collections.singleton("kkk")).get().getData()
//                .entrySet()
//                .forEach(System.out::println);
//        moreDBClient.deleteAsync(Collections.singleton("kkk"));
//    }

    private final RaftClient dbClient;

    public MoreDBClient(final ClientProperties properties) {
        final RaftPeer[] raftPeers = properties.getNodes()
                .stream()
                .map(node -> new RaftPeer(
                        RaftPeerId.getRaftPeerId(node.getId()),
                        new InetSocketAddress(node.getHostname(), node.getPort())
                )).toArray(RaftPeer[]::new);
        log.info("raft peer nodes: {}", Arrays.asList(raftPeers));
        final RaftGroup raftGroup = RaftGroup.valueOf(
                RaftGroupId.valueOf(UUID.fromString(properties.getGroupId())), raftPeers);
        final RaftProperties raftProperties = new RaftProperties();
        final RaftClient.Builder builder = RaftClient.newBuilder()
                .setProperties(raftProperties)
                .setRaftGroup(raftGroup)
                .setClientRpc(
                        new GrpcFactory(new Parameters())
                                .newRaftClientRpc(ClientId.randomId(), raftProperties)
                );
        dbClient = builder.build();
        log.info("build MoreDB raft client success");
    }

    public CompletableFuture<UpdateResponse> updateAsync(final Map<String, byte[]> values) {
        final List<MoreDBProto.UpdateRequest> updateRequests = getMoreDBRequest(values.entrySet(),
                entry -> MoreDBProto.UpdateRequest
                    .newBuilder()
                    .setKey(entry.getKey())
                    .setValue(com.google.protobuf.ByteString.copyFrom(entry.getValue()))
                    .build()
        );
        final MoreDBRequest request = MoreDBRequest
                .newBuilder()
                .setCmdType(MoreDBProto.Type.UPDATE)
                .addAllUpdateRequest(updateRequests)
                .build();
        return reply(dbClient.sendAsync(transform(request)), rsp -> new UpdateResponse(rsp.getSuccess(), rsp.getMsg()));
    }

    public CompletableFuture<DeleteResponse> deleteAsync(final Collection<String> keys) {
        final List<MoreDBProto.DeleteRequest> requests = getMoreDBRequest(keys, k -> MoreDBProto.DeleteRequest
                .newBuilder()
                .setKey(k)
                .build()
        );
        final MoreDBRequest request = MoreDBRequest.newBuilder()
                .setCmdType(MoreDBProto.Type.DELETE)
                .addAllDeleteRequest(requests)
                .build();
        return reply(dbClient.sendAsync(transform(request)), rsp -> new DeleteResponse(rsp.getSuccess(), rsp.getMsg()));
    }

    public CompletableFuture<ReadResponse> readAsync(final Collection<String> keys) {
        final List<MoreDBProto.ReadRequest> readRequests = getMoreDBRequest(keys,
                k -> MoreDBProto.ReadRequest.newBuilder()
                        .setKey(k)
                        .build()
        );
        final MoreDBRequest request = MoreDBRequest
                .newBuilder()
                .setCmdType(MoreDBProto.Type.READ)
                .addAllReadRequest(readRequests)
                .build();
        return reply(dbClient.sendReadOnlyAsync(transform(request)), rsp ->{
            final Map<String, byte[]> results = rsp.getReadResponseList()
                    .stream()
                    .collect(Collectors.toMap(MoreDBProto.ReadResponse::getKey,
                            r -> r.getValue().toByteArray())
                    );
            return new ReadResponse(rsp.getSuccess(), rsp.getMsg(), results);
        });
    }

    private <T> CompletableFuture<T> reply(final CompletableFuture<RaftClientReply> future,
            final Function<MoreDBResponse, T> rsp) {
        return future.thenApplyAsync(reply -> {
            try {
                final MoreDBResponse response = MoreDBResponse
                        .parseFrom(reply.getMessage().getContent().toByteArray());
                return rsp.apply(response);
            } catch (InvalidProtocolBufferException e) {
                throw new MoreDBException("parse response error", e);
            }
        }).exceptionally(e -> {
            log.error("request error", e);
            throw new MoreDBException("request error", e);
        });
    }

    private Message transform(final AbstractMessageLite data) {
        return Message.valueOf(ByteString.copyFrom(data.toByteArray()));
    }

    private <T, K> List<T> getMoreDBRequest(final Collection<K> data, final Function<K, T> requestBuilder) {
        return data.stream()
                .map(requestBuilder)
                .collect(Collectors.toList());
    }

    @Override
    public void close() throws IOException {
        dbClient.close();
    }
}
