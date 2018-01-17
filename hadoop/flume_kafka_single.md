# flume kafka single

本文介绍了flume和kafka单节点部署和使用的方法


# 安装包准备

 - kafka: `kafka_2.11-1.0.0.tgz`
 - flume: `apache-flume-1.8.0-bin.tar.gz`

# kafka部署配置

 1. 解压kafka安装包至指定目录`/opt/kafka`
 2. 启动zookeeper（也可单独部署zookeeper）
    ```sh
    ./bin/zookeeper-server-start.sh -daemon ./config/zookeeper.properties
    ```
    启动成功后可以查询2181端口已确认是否启动成功

 3. 启动 kafka
    ```sh
    ./bin/kafka-server-start.sh -daemon ./config/server.properties
    ```
    启动成功后可以查询9092端口已确认是否启动成功

# flume部署配置

 1. 解压flume安装包至指定目录`/opt/flume`
 2. 修改flume配置文件`./conf/flume-conf.properties`
    ```
    a1.sources = r1
    a1.sinks = k1
    a1.channels = c1

    a1.sources.r1.type = netcat
    a1.sources.r1.bind = 0.0.0.0
    a1.sources.r1.port = 44444
    a1.sources.r1.channels = c1

    a1.channels.c1.type = file
    a1.channels.c1.checkpointDir = /var/log/flume/checkpoint
    a1.channels.c1.dataDirs = /var/log/flume/data

    a1.sinks.k1.channel = c1
    a1.sinks.k1.type = org.apache.flume.sink.kafka.KafkaSink
    a1.sinks.k1.brokerList=localhost:9092
    a1.sinks.k1.batchSize=10
    a1.sinks.k1.topic=flume-topic
    a1.sinks.k1.request.required.acks=1
    ```

 3. 启动flume
    ```sh
    ./bin/flume-ng agent --conf conf --conf-file conf/flume-conf.properties --name a1 -Dflume.root.logger=WARN,console
    ```

 4. 使用jps或者ps查询flume进程是否启动成功

# 测试flume到kafka的消息传递

 1. 向flume发送消息
    ```sh
    >telnet flume 44444
    >输入任意字符
    ```

 2. 从kafka读取消费消息
    ```sh
    ./bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic flume-topic --from-beginning
    ```
