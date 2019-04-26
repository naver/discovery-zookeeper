# elasticsearch-zookeeper-based-discovery-plugin

## znodes structure from which this plugin fetches elasticsearch cluster metadata

```
{znode_parent}
|
|____ {znode-1}
|
|____ {znode-2}
|
|____ {znode-3}
|
|____ {znode-4}
```


Each {node_name-n} znode has the information of the node.
The information of the node is hostname & transport_tcp_port pair. (e.g example.machine.com:9300)
Znode name is not important. The data which znode has is important.

```
67.81.244.11:9305
67.81.244.15:9400
```


## How to use

1. insert a setting for discovery-zookeeper in elasticsearch.yml
   ```
   discovery.zen.hosts_provider: zookeeper
   ```

2. Install using ```elasticsearch-plugin```
   ```
   # https://www.elastic.co/guide/en/elasticsearch/plugins/6.7/plugin-authors.html#_testing_your_plugin
   $ /usr/share/elasticsearch/bin/elasticsearch-plugin install --batch file:///{some_path}/discovery-zookeeper-6.7.1.zip
   ```

3. Insert zookeeper specific settings in ```discovery-zookeeper.yml```

In ```/usr/share/elasticsearch/config/discovery-zookeeper/discovery-zookeeper.yml```

```
discovery.zookeeper.quorum: zk01.example.com:2181,zk02.example.com:2181,zk03.example.com:2181
discovery.zookeeper.znode.parent: /some/path/to/use/as/znode/parent
```

Caution: Current version 0.0.2 discovery-zookeeper plugin does not set znode data. It just reads znode data. You need to deal with how to set znode data with "{host}:{transport_tcp_port}".

## build environment

- target ES: 6.7.1
- target ZK: 3.4.14
- java 12
- gradle 5.2.1

```
$ gradle -v

------------------------------------------------------------
Gradle 5.2.1
------------------------------------------------------------

Build time:   2019-02-08 19:00:10 UTC
Revision:     f02764e074c32ee8851a4e1877dd1fea8ffb7183

Kotlin DSL:   1.1.3
Kotlin:       1.3.20
Groovy:       2.5.4
Ant:          Apache Ant(TM) version 1.9.13 compiled on July 10 2018
JVM:          12 (Oracle Corporation 12+33)
OS:           Mac OS X 10.14.3 x86_64
```


## How to build

```
$ gradle build

# after build, you can install discovery-zookeeper using discovery-zookeeper-6.7.1.zip
$ ls build/distributions/discovery-zookeeper-6.7.1.zip
build/distributions/discovery-zookeeper-6.7.1.zip
```

## compatible versions

If you need another version, make an issue or contribute it(!) please. :)

|discovery-zookeeper|elasticsearch|zookeeper|
|---|---|---|
|0.0.2|6.7.1|3.4.14|

## LICENSE

```
Copyright 2019-present NAVER Corp.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
