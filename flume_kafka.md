# flume to kafka

***待完善***

本文介绍了flume同步至kafka的部署方法

# 部署计划

| hostname | 角色             |
|:---------|:-----------------|
| flume    | flume            |
| nd1      | zookeeper, kafka |
| nd2      | zookeeper, kafka |
| nd3      | zookeeper, kafka |


# flume部署配置

新建flume运行存储数据目录`/var/lib/flume/checkpoint`和`/var/lib/flume/data`

flume-conf.properties
```
# vim flume-conf.properties
a1.sources = r1
a1.sinks = k1
a1.channels = c1

a1.sources.r1.type = netcat
a1.sources.r1.bind = 0.0.0.0
a1.sources.r1.port = 44444
a1.sources.r1.channels = c1

a1.channels.c1.type = file
a1.channels.c1.checkpointDir = /var/lib/flume/checkpoint
a1.channels.c1.dataDirs = /var/lib/flume/data

a1.sinks.k1.channel = c1
a1.sinks.k1.type = org.apache.flume.sink.kafka.KafkaSink
a1.sinks.k1.brokerList=nd1:9092,nd2:9092,nd3:9092
a1.sinks.k1.batchSize=10
a1.sinks.k1.topic=flume-topic
a1.sinks.k1.request.required.acks=1
```

启动flume

    ./bin/flume-ng agent --conf conf --conf-file conf/flume-conf.properties --name a1 -Dflume.root.logger=WARN,console

# zookeeper配置及部署

在nd1, nd2, nd3上部署zookeeper  
略


# kafka部署配置

新建目录`/var/lib/kafka`

修改配置文件`config/server.properties`
```
# 分别在nd1-3上修改为相应的数字
broker.id=1

log.dirs=/var/lib/kafka
zookeeper.connect=nd1:2181,nd2:2181,nd3:2181
```

启动 kafka

    kafka-server-start.sh config/server.properties

# 测试flume到kafka的消息传递

创建topic
```sh
kafka-topics.sh --create --zookeeper nd1:2181,nd2:2181,nd3:2181 --replication-factor 2 --partitions 2 --topic flume-topic
```

在nd1-3中任意机器中执行kafka-console-consumer.sh以显示信息
```sh
kafka-console-consumer.sh --zookeeper nd1:2181,nd2:2181,nd3:2181 --topic flume-topic --from-beginning
```

在flume机器上发送消息
```sh
>telnet flume 44444
>输入任意字符
```
在kafka-console-consumer上可以看到flume上发送的消息
