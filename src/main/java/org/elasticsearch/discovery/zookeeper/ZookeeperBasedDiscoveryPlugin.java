// This is modifed version of discover-file
// I referred to
// https://github.com/elastic/elasticsearch/blob/6.2/plugins/discovery-file
// /src/main/java/org/elasticsearch/discovery/file/FileBasedDiscoveryPlugin.java
/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.discovery.zookeeper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.elasticsearch.common.network.NetworkService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.plugins.DiscoveryPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.discovery.SeedHostsProvider;
import org.elasticsearch.transport.TransportService;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Plugin for providing zookeeper-based unicast hosts discovery. The list of unicast hosts
 * is obtained by reading znodes' data on zookeeper with {@link ZookeeperBasedSeedHostsProvider#QUORUM_SETTING}.
 * Znodes are located below {@link ZookeeperBasedSeedHostsProvider#ZNODE_PARENT_SETTING}.
 * Each znode has data which is {hostname}:{transport_tcp_port} pair.
 */
public class ZookeeperBasedDiscoveryPlugin extends Plugin implements DiscoveryPlugin {

    private static final Logger logger = LogManager.getLogger(ZookeeperBasedDiscoveryPlugin.class);

    private final Settings settings;
    private final Path configPath;
    private ExecutorService zookeeperBasedDiscoveryExecutorService;

    public ZookeeperBasedDiscoveryPlugin(Settings settings, Path configPath) {
        this.settings = settings;
        this.configPath = configPath;
    }

    @Override
    public Map<String, Supplier<SeedHostsProvider>> getSeedHostProviders(TransportService transportService,
                                                                            NetworkService networkService) {
        return Collections.singletonMap(
            "zookeeper",
            () -> new ZookeeperBasedSeedHostsProvider(
                    new Environment(settings, configPath), transportService));
    }
}
