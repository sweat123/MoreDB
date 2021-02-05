package com.laomei.moredb.server.service;

import com.laomei.moredb.common.bean.MoreDBProto.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author luobo.hwz on 2021/01/25 11:46 PM
 */
public interface StorageService {

    CompletableFuture<UpdateResponse> updateAsync(final List<UpdateRequest> updateRequest);

    CompletableFuture<DeleteResponse> deleteAsync(final List<DeleteRequest> deleteRequest);

    CompletableFuture<List<ReadResponse>> readAsync(final List<ReadRequest> readRequest);
}
