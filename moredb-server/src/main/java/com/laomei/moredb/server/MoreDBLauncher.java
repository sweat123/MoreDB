package com.laomei.moredb.server;

import com.laomei.moredb.common.bean.Node;
import com.laomei.moredb.common.exception.MoreDBException;
import com.laomei.moredb.server.properties.MoreDBProperties;
import com.laomei.moredb.server.properties.MoreDBPropertiesLoader;
import com.laomei.moredb.server.raft.RocksDBStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcConfigKeys;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.protocol.RaftPeerId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.RaftServerConfigKeys;
import org.apache.ratis.util.NetUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * @author luobo.hwz on 2021/02/02 7:19 PM
 */
@Slf4j
public class MoreDBLauncher {
    private static final CountDownLatch LOCK = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        log.info(">>>>>>>>>>>>>>>>>>> starting MoreDB server...... >>>>>>>>>>>>>>>>>>>");
        final MoreDBProperties dbProperties = MoreDBPropertiesLoader.load();
        final Node currentNode = dbProperties.getNode();
        final RaftPeer raftPeer = new RaftPeer(RaftPeerId.getRaftPeerId(currentNode.getId()),
                new InetSocketAddress(currentNode.getHostname(), currentNode.getPort()));
        final RaftProperties raftProps = new RaftProperties();
        final File raftStorageDir = new File(dbProperties.getDataPath() + raftPeer.getId().toString());
        RaftServerConfigKeys.setStorageDir(raftProps, Collections.singletonList(raftStorageDir));
        final int port = NetUtils.createSocketAddr(raftPeer.getAddress()).getPort();
        GrpcConfigKeys.Server.setPort(raftProps, port);
        final RocksDBStateMachine machine = new RocksDBStateMachine();
        final String groupId = dbProperties.getGroupId();
        final Set<RaftPeer> groupRaftPeers = raftPeers(dbProperties);
        if (!groupRaftPeers.contains(raftPeer)) {
            throw new MoreDBException("MoreDB node is not in groups");
        }
        final RaftGroup raftGroup = RaftGroup.valueOf(RaftGroupId.valueOf(UUID.fromString(groupId)), groupRaftPeers);
        final RaftServer server;
        try {
            server = RaftServer.newBuilder()
                    .setGroup(raftGroup)
                    .setProperties(raftProps)
                    .setServerId(raftPeer.getId())
                    .setStateMachine(machine)
                    .build();
            log.info("build MoreDB server success");
        } catch (IOException e) {
            throw new MoreDBException("build MoreDB server failed", e);
        }
        try {
            server.start();
            log.info(">>>>>>>>>>>>>>>>>>> MoreDB server started >>>>>>>>>>>>>>>>>>>");
        } catch (IOException e) {
            IOUtils.closeQuietly(server);
            throw new MoreDBException("run MoreDB node failed", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.info(">>>>>>>>>>>>>>>>>>>  starting to close MoreDB server >>>>>>>>>>>>>>>>>>>");
                server.close();
                LOCK.countDown();
            } catch (IOException e) {
                log.error("close MoreDB server failed", e);
            }
        }));
        LOCK.await();
    }

    private static Set<RaftPeer> raftPeers(final MoreDBProperties properties) {
        return properties.getGroupNodes()
                .stream()
                .map(node -> new RaftPeer(RaftPeerId.getRaftPeerId(node.getId()),
                        new InetSocketAddress(node.getHostname(), node.getPort())))
                .collect(Collectors.toSet());
    }
}
