// This is modifed version of discover-file
// I referred to
// https://github.com/elastic/elasticsearch/blob/6.2/plugins/discovery-file
// /src/main/java/org/elasticsearch/discovery/file/FileBasedUnicastHostsProvider.java
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
import org.apache.zookeeper.KeeperException;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Setting.Property;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.discovery.SeedHostsProvider;
import org.elasticsearch.env.Environment;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


/**
 * An implementation of {@link SeedHostsProvider} that reads {hostname:transport_tcp_port} data on each znode.
 * Znodes are descendants of znode parent {@link #ZNODE_PARENT_SETTING}.
 *
 * Each unicast {hostname:transport_tcp_port} pair that is part of the discovery process must be stored on znode data.  Example znode data:
 *
 * 67.81.244.11:9305
 * 67.81.244.15:9400
 */
class ZookeeperBasedSeedHostsProvider implements SeedHostsProvider {
    private static final Logger logger = LogManager.getLogger(ZookeeperBasedSeedHostsProvider.class);
    private static final String DEFAULT_ZNODE_PARENT = "/elasticsearch/discovery";
    private static final Setting<String> QUORUM_SETTING = Setting.simpleString(
            "discovery.zookeeper.quorum", "localhost:2181", Property.NodeScope);
    private static final Setting<String> ZNODE_PARENT_SETTING = Setting.simpleString(
            "discovery.zookeeper.znode.parent", DEFAULT_ZNODE_PARENT, Property.NodeScope);

    private final String quorum;
    private final String znodeParent;

    ZookeeperBasedSeedHostsProvider(Environment environment, TransportService transportService) {
        // https://github.com/elastic/elasticsearch/blob/master/plugins/
        // examples/custom-settings/src/main/java/org/elasticsearch/example/customsettings/ExampleCustomSettingsConfig.java
        final Settings settings = environment.settings();
        final Path configDir = environment.configFile();
        final Settings customSettings;
        final Path customSettingsYamlFile = configDir.resolve("discovery-zookeeper/discovery-zookeeper.yml");
        try {
            customSettings = Settings.builder().loadFromPath(customSettingsYamlFile).build();
            assert customSettings != null;
        } catch (IOException e) {
            throw new ElasticsearchException("Failed to load settings", e);
        }
        this.quorum = QUORUM_SETTING.get(customSettings);
        this.znodeParent = ZNODE_PARENT_SETTING.get(customSettings);

    }

    private List<String> getHostsList() throws KeeperException, InterruptedException {
        List<String> hostsList = new ArrayList<>();
        DiscoverESFromZookeeper zk = null;
        try {
            zk = new DiscoverESFromZookeeper(this.quorum);
            List<String> children = zk.getChildren(this.znodeParent);
            for (String child : children){
                byte[] data = zk.getData(this.znodeParent + '/' + child);
                String host = new String(data, "UTF-8");
                if (Pattern.matches("^[a-zA-Z0-9.-]+:[0-9]+$", host)) {
                    logger.info("Found: " + host);
                    hostsList.add(host);
                } else {
                    logger.info("The data in znode(" + child + ") is not valid 'hostname:transport_tcp_port' pair. data: " + host);
                }
            }
        } catch (KeeperException | IOException e) {
            logger.info("Failed to get node information from zookeeper.");
            logger.info(e.getMessage());
            return Collections.emptyList();
        } finally {
            if (zk != null) {
                zk.close();
            }
        }
        return hostsList;
    }

    @Override
    public List<TransportAddress> getSeedAddresses(HostsResolver hostsResolver) {
        try {
            final List<TransportAddress> transportAddresses = hostsResolver.resolveHosts(getHostsList());
            return transportAddresses;
        } catch (InterruptedException | KeeperException e) {
            logger.info("Failed to get node information from zookeeper in buildDynamicHosts.");
            logger.info(e.getMessage());
            return Collections.emptyList();
        }
    }

}
