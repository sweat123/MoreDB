package com.laomei.moredb.server.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.laomei.moredb.server.data.Pair;
import com.laomei.moredb.common.exception.MoreDBException;
import com.laomei.moredb.server.storage.RocksDBStorage;
import com.laomei.moredb.common.bean.MoreDBProto.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author luobo.hwz on 2021/01/25 11:51 PM
 */
@Slf4j
public class RocksDBStorageService implements StorageService, Closeable {

    private final RocksDBStorage rocksDBStorage;

    private final Consumer<UpdateRequest> updateConfigs;

    private final Consumer<DeleteRequest> deleteConfigs;

    private final Function<ReadRequest, Pair> readConfigs;

    private final ExecutorService threadPool;

    public RocksDBStorageService() {
        this.rocksDBStorage = new RocksDBStorage();
        this.updateConfigs = request -> {
            final String key = request.getKey();
            final byte[] value = request.getValue().toByteArray();
            rocksDBStorage.save(key, value);
            log.info("save data success; key: '{}', value: '{}'", key, value);
        };
        this.deleteConfigs = request -> {
            rocksDBStorage.delete(request.getKey());
            log.info("delete data success; delete key: '{}'", request.getKey());
        };
        this.readConfigs = request -> {
            final String key = request.getKey();
            final byte[] value = rocksDBStorage.find(key);
            log.info("get data success; key: '{}', value: '{}'", key, value);
            return Pair.pair(key, value);
        };
        this.threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2,
                new ThreadFactoryBuilder().setNameFormat("rocks-db-storage-service-thread-%d").build());
    }

    @Override
    public CompletableFuture<UpdateResponse> updateAsync(List<UpdateRequest> updateRequests) {
        return async(
                updateRequests, request -> {
                    updateConfigs.accept(request);
                    return null;
                },
                ignore -> UpdateResponse.newBuilder().build()
        ).exceptionally(this::exceptionHandler);
    }

    @Override
    public CompletableFuture<DeleteResponse> deleteAsync(List<DeleteRequest> deleteRequests) {
        return async(
                deleteRequests, request -> {
                    deleteConfigs.accept(request);
                    return null;
                },
                ignore -> DeleteResponse.newBuilder().build()
        ).exceptionally(this::exceptionHandler);
    }

    @Override
    public CompletableFuture<List<ReadResponse>> readAsync(List<ReadRequest> readRequests) {
        return async(
                readRequests, readConfigs, configs -> configs.stream()
                        .map(config -> ReadResponse.newBuilder()
                                .setKey(config.getKey())
                                .setValue(config.getValue())
                                .build()
                        ).collect(Collectors.toList())
        ).exceptionally(this::exceptionHandler);

    }

    private <T> T exceptionHandler(final Throwable t) {
        if (t != null) {
            throw new MoreDBException("execute failed", t);
        }
        return null;
    }

    private <T, E, R> CompletableFuture<R> async(final List<T> requests, final Function<T, E> handler,
            final Function<List<E>, R> responseBuilder) {
        final List<CompletableFuture<E>> futures = requests.stream()
                .map(request -> CompletableFuture.supplyAsync(() -> handler.apply(request), threadPool))
                .collect(Collectors.toList());
        final CompletableFuture<List<E>> future = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignore -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
                );
        return future.thenApply(responseBuilder);
    }

    @Override
    public void close() throws IOException {
        threadPool.shutdown();
    }
}
