/*
 * Copyright 2019-present NAVER Corp.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticsearch.discovery.zookeeper;

import java.util.List;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.ZooKeeper;

public class DiscoverESFromZookeeper {
    private static final Logger logger = LogManager.getLogger(DiscoverESFromZookeeper.class);
    ZooKeeper zk;

    public DiscoverESFromZookeeper(String hostPort) throws KeeperException, InterruptedException {
        final CountDownLatch connectedSignal = new CountDownLatch(1);
        final DiscoverESFromZookeeper thisInstance = this;

        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try {
                    thisInstance.zk = new ZooKeeper(hostPort, 30000, new Watcher() {
                        @Override
                        public void process(WatchedEvent event) {
                            if (event.getState() == Event.KeeperState.SyncConnected) {
                                connectedSignal.countDown();
                            }
                        }
                    });
                } catch (IOException e) {
                    logger.info(e.getMessage());
                }
                return null;
            }
        });
        connectedSignal.await();
    }

    public void close() {
        try {
            zk.close();
        } catch (InterruptedException e) {
            logger.info("Failed to close zookeeper.");
            logger.info(e.getMessage());
        }
    }

    public List<String> getChildren(String path) {
        try {
            return zk.getChildren(path, false);
        } catch (KeeperException | InterruptedException e) {
            logger.info(e.getMessage());
            return Collections.emptyList();
        }
    }

    public byte[] getData(String path) {
        try {
            Stat s = zk.exists(path, false);
            return zk.getData(path, false, s);
        } catch (KeeperException | InterruptedException e) {
            logger.info(e.getMessage());
            return new byte[0];
        }
    }
}