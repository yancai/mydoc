# 分布式 HA HDFS YARN SPARK HBASE部署

本文介绍了支持HA方式的HDFS + YARN + HBase + Spark的部署方式


# 部署计划

## 机器环境

**操作系统**：CentOS-7

默认在`/root`中执行操作  
如果对网络端口设置不熟悉，请先关闭防火墙
```sh
# 停止firewall
systemctl stop firewalld.service

# 禁止firewall开机启动
systemctl disable firewalld.service
```

## 机器划分
| hostname  | 包含角色                                                                            |
|----------|------------------------------------------------------------------------------------|
| nd1（主） | namenode, datanode, resourcemanager, nodemanager, zookeeper, hmaster, hregionserver |
| nd2（主） | namenode, datanode, resourcemanager, nodemanager, zookeeper, hmaster, hregionserver |
| nd3       | datanode, nodemanager, zookeeper, hregionserver                                     |

## 安装包准备

 - jdk-7u75-linux-x64.rpm
 - hadoop-2.6.4.tar.gz
 - zookeeper-3.4.8.tar.gz
 - hbase-0.98.18-hadoop2-bin.tar.gz
 - spark-1.6.1-bin-hadoop2.6.tgz

# 基础环境配置

## 配置hostname及hosts
选三台机器，按照部署计划配置hosts和hostname

## 配置ssh登录

 1. 生成ssh远程登录公钥-密钥对

        ssh-keygen -t rsa

    连续回车enter确认

 2. 由于nd1和nd2为master角色，因此nd1和nd2需要可以访问所有主机，将nd1和nd2的公钥分发给所有机器  

    >如果nd1希望可以ssh无密钥访问nd2，那么需要将nd1自己的`~/.ssh/id_rsa.pub`中的公钥添加至nd2的`~/.ssh/authorized_keys`中。ssh提供了`ssh-copy-id`来简化复制粘贴的操作。  

    例如在nd1上执行：

    ```sh
    # 将nd1的公钥分发给nd1，如有提示，输入yes确认，输入nd1登录密码传输公钥
    ssh-copy-id nd1

    # 将nd1的公钥分发给nd2，如有提示，输入yes确认，输入nd2登录密码传输公钥
    ssh-copy-id nd2

    # 将nd1的公钥分发给nd3，如有提示，输入yes确认，输入nd3登录密码传输公钥
    ssh-copy-id nd3
    ```
    在nd2上重复如上分发动作，将nd2的公钥分发给所有机器  
    在nd3上，将nd3的公钥分发给nd3自己

    在nd1上执行`ssh-copy-id nd1`以确保nd1可以ssh登录nd1自身；  
    在nd1上执行`ssh-copy-id nd2`以确保nd2可以ssh登录nd2自身；  
    在nd1上执行`ssh-copy-id nd3`以确保nd3可以ssh登录nd3自身；  

 3. 验证ssh是否可远程至目标机器，使用`ssh hostname`验证是否可远程免密登录指定机器。在本例中，nd1和nd2可以远程登录nd1、nd2以及nd3。

# 安装配置Java

 1. 安装Java

    ```sh
    rpm -ivh jdk-7u75-linux-x64.rpm
    ```

 2. 配置环境变量  
    打开/etc/profile，`vim /etc/profile`，最下方添加如下内容：

    ```sh
    export JAVA_HOME=/usr/java/jdk1.7.0_75
    export PATH=$JAVA_HOME/bin:$PATH
    export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
    ```

    导入环境变量`source /etc/profile`，输入`java -version`测试Java是否配置成功，正常输出如下信息：
    ```
    java version "1.7.0_75"
    Java(TM) SE Runtime Environment (build 1.7.0_75-b13)
    Java HotSpot(TM) 64-Bit Server VM (build 24.75-b04, mixed mode)
    ```

# 部署zookeeper

 1. 解压并重命名  

    ```sh
    # 解压
    tar -xvf zookeeper-3.4.8.tar.gz

    # 重命名
    mv zookeeper-3.4.8 zookeeper
    ```

 2. 配置环境变量  

    打开`/etc/profile`并修改添加如下内容：

    ```sh
    export ZOOKEEPER_HOME=/root/zookeeper
    export PATH=$PATH:$ZOOKEEPER_HOME/bin
    ```

    导入环境变量`source /etc/profile`

 3. 配置zookeeper  

    新建文件夹存储zookeeper文件`mkdir /var/lib/zookeeper`  
    进入zookeeper程序目录，`cd /root/zookeeper`  
    重命名配置文件`mv conf/zoo_sample.cfg conf/zoo.cfg`  
    修改zoo.cfg，修改添加如下内容：

    ```sh
    dataDir=/var/lib/zookeeper
    server.1=nd1:2888:3888
    server.2=nd2:2888:3888
    server.3=nd3:2888:3888
    ```

    在nd1上执行：`echo 1 > /var/lib/zookeeper/myid`
    在nd2上执行：`echo 2 > /var/lib/zookeeper/myid`
    在nd3上执行：`echo 3 > /var/lib/zookeeper/myid`

 4. 启动并验证  

    启动方式：`./bin/zkServer.sh start`  

    验证方式：输入jps查询进程可以看到QuorumPeerMain，则说明zookeeper已启动。  
    使用`./bin/zkCli.sh`进入zookeeper客户端命令行，并执行：

    ```sh
    input: ls /
    # 看到[zookeeper]即说明服务运行正常
    output: [zookeeper]
    ```

# 部署Hadoop(HDFS + YARN)
 1. 解压并重命名  

    ```sh
    cd ~
    tar -xvf hadoop-2.6.4.tar.gz
    mv hadoop-2.6.4 hadoop
    ```

 2. 配置环境变量  

    打开`vim /etc/profile`并修改添加如下内容：  

    ```sh
    export HADOOP_HOME=/root/hadoop
    export PATH=$PATH:$HADOOP_HOME/bin:$HADOOP_HOME/sbin
    ```

    导入环境变量`source /etc/profile`

 3. 配置hadoop  

    新建文件夹存储hadoop文件`mkdir /var/lib/hadoop`  
    进入hadoop程序目录，`cd /root/hadoop`

    **修改`etc/hadoop/hadoop-env.sh`**

    ```sh
    export JAVA_HOME=/usr/java/jdk1.7.0_75
    ```

    **修改`vim etc/hadoop/core-site.xml`**  
    在`<configuration></configuration>`间添加如下内容：

    ```xml
    <configuration>

        <property>
            <name>hadoop.tmp.dir</name>
            <value>file:/var/lib/hadoop</value>
        </property>

        <property>
            <name>fs.defaultFS</name>
            <value>hdfs://mycluster</value>
        </property>

        <property>
            <name>ha.zookeeper.quorum</name>
            <value>nd1:2181,nd2:2181,nd3:2181</value>
        </property>

    </configuration>
    ```

    **修改`vim etc/hadoop/hdfs-site.xml`**  
    在`<configuration></configuration>`间添加如下内容：  

    ```xml
    <configuration>

        <property>
            <name>dfs.nameservices</name>
            <value>mycluster</value>
        </property>

        <property>
            <name>dfs.ha.namenodes.mycluster</name>
            <value>nd1,nd2</value>
        </property>

        <property>
            <name>dfs.namenode.rpc-address.mycluster.nd1</name>
            <value>nd1:8020</value>
        </property>

        <property>
            <name>dfs.namenode.http-address.mycluster.nd1</name>
            <value>nd1:50070</value>
        </property>

        <property>
            <name>dfs.namenode.rpc-address.mycluster.nd2</name>
            <value>nd2:8020</value>
        </property>

        <property>
            <name>dfs.namenode.http-address.mycluster.nd2</name>
            <value>nd2:50070</value>
        </property>

        <property>
            <name>dfs.namenode.shared.edits.dir</name>
            <value>qjournal://nd1:8485;nd2:8485;nd3:8485/journal</value>
        </property>

        <property>
            <name>dfs.journalnode.edits.dir</name>
            <value>/val/lib/hadoop</value>
        </property>

        <property>
            <name>dfs.ha.automatic-failover.enabled</name>
            <value>true</value>
        </property>

        <property>
            <name>dfs.client.failover.proxy.provider.mycluster</name>
            <value>org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider</value>
        </property>

        <property>
            <name>dfs.ha.fencing.methods</name>
            <value>
                sshfence
                shell(/bin/true)
            </value>
        </property>

        <property>
            <name>dfs.ha.fencing.ssh.private-key-files</name>
            <value>/root/.ssh/id_rsa</value>
        </property>

        <property>
            <name>dfs.ha.fencing.ssh.connect-timeout</name>
            <value>30000</value>
        </property>

        <property>
            <name>dfs.namenode.name.dir</name>
            <value>file:/var/lib/hadoop/nn</value>
        </property>

        <property>
            <name>dfs.datanode.data.dir</name>
            <value>file:/var/lib/hadoop/dn</value>
        </property>

    </configuration>
    ```

    复制mapred-site配置，`cp etc/hadoop/mapred-site.xml.template etc/hadoop/mapred-site.xml`  
    **修改`vim etc/hadoop/mapred-site.xml`**  
    在`<configuration></configuration>`间添加如下内容：  

    ```xml
    <configuration>

        <property>
            <name>mapreduce.framework.name</name>
            <value>yarn</value>
        </property>

    </configuration>
    ```

    **修改`vim etc/hadoop/yarn-site.xml`**  
    在`<configuration></configuration>`间添加如下内容：  

    ```xml
    <configuration>

        <property>
            <name>yarn.resourcemanager.ha.enabled</name>
            <value>true</value>
        </property>

        <property>
            <name>yarn.resourcemanager.cluster-id</name>
            <value>YARN-HA</value>
        </property>

        <property>
            <name>yarn.resourcemanager.ha.rm-ids</name>
            <value>rm1,rm2</value>
        </property>

        <property>
            <name>yarn.resourcemanager.hostname.rm1</name>
            <value>nd1</value>
        </property>

        <property>
            <name>yarn.resourcemanager.hostname.rm2</name>
            <value>nd2</value>
        </property>

        <property>
            <name>yarn.resourcemanager.recovery.enabled</name>
            <value>true</value>
        </property>

        <property>
            <name>yarn.resourcemanager.store.class</name>
            <value>org.apache.hadoop.yarn.server.resourcemanager.recovery.ZKRMStateStore</value>
        </property>

        <property>
            <name>yarn.resourcemanager.zk-address</name>
            <value>nd1:2181,nd2:2181,nd2:2181</value>
        </property>

        <property>
            <name>yarn.nodemanager.aux-service</name>
            <value>mapreduce_shuffle</value>
        </property>

    </configuration>
    ```

    **修改`vim etc/hadoop/slaves`**  
    添加如下内容：  

    ```
    nd1
    nd2
    nd3
    ```

 4. 格式化并启动服务  

    启动journalnode，在nd1，nd2，nd3上执行`hadoop-daemon.sh start journalnode`  
    格式化namenode，在nd1上执行`hdfs namenode -format`，拷贝nd1的namenode的目录至nd2的相同目录中`scp -r /var/lib/hadoop/nn root@nd2:/var/lib/hadoop/`  
    有类似如下信息输出则为正常：  

    ```
    16/04/06 09:06:16 INFO common.Storage: Storage directory /var/lib/hadoop/nn has been successfully formatted.
    16/04/06 09:06:17 INFO namenode.NNStorageRetentionManager: Going to retain 1 images with txid >= 0
    16/04/06 09:06:17 INFO util.ExitUtil: Exiting with status 0
    ```

    *ps1: 也可以在nd2上先执行格式化在拷贝目录至nd1*  
    *ps2: 此处可使用网络共享磁盘方式使得nd1和nd2同时读写同一个目录，**生产环境建议使用此方法***

    格式化zookeeper中的hadoop信息，在nd1或者nd2上执行`hdfs zkfc -formatZK`  
    启动hdfs，在nd1或者nd2上执行`start-dfs.sh`，在nd1和nd2上使用jps查看会有如下进程：  

    ```
    JournalNode
    QuorumPeerMain
    DFSZKFailoverController
    NameNode
    DataNode
    ```

    在nd3上执行jps查看会有如下进程：  

    ```
    JournalNode
    QuorumPeerMain
    DataNode
    ```

    浏览器中打开`http://nd1:50070`和`http://nd2:50070`中可以看到其中一个为**`active`**，另一个为**`standby`**，则说明hadoop namenode的HA配置成功  
    或者使用`hdfs haadmin -getServiceState nd1`来查询nd1的当前状态  
    可以在active的机器上强制杀死namenode或者使用`hadoop-daemon.sh stop namenode`来模拟active namenode故障，之后刷新另一个节点的页面可以看到状态已经由`standby`变成了`active`  
    由此可以验证hdfs的HA成功  

    启动yarn  
    在nd1上执行`start-yarn.sh`，启动nd1的resourcemanager以及nd1-3的nodemanager  
    再在nd2上执行`yarn-daemon.sh start resourcemanager`，启动nd2的resourcemanager  
    使用jps查询进程可以在nd1和nd2上看到包含有进程：`resourcemanager`和`nodemanager`  
    在nd3上可以看到包含有进程：`nodemanager`  
    在浏览器打开`http://nd1:8088`可以看到yarn的applications界面，打开`http://nd2:8088`会自动跳转至nd1的界面  
    或者使用`yarn rmadmin -getServiceState rm1`来查询rm1或rm2的状态  
    在nd1上杀死resourcemanager的进程或者使用`yarn-daemon.sh stop resourcemanager`停止resourcemanager进程，这时访问`http:nd2:8080`，可以看到resourcemanager已经由nd2接替。或者使用`yarn rmadmin -getServiceState xxx`来查询每个resourcemanager的状态  
    由此可以验证yarn的HA成功

# 部署HBase

 1. 解压并重命名  

    ```sh
    cd ~
    tar -xvf hbase-0.98.18-hadoop2-bin.tar.gz
    mv hbase-0.98.18-hadoop2-bin hbase
    ```

 2. 配置环境变量  

    打开`vim /etc/profile`并修改添加如下内容：  

    ```sh
    export HBASE_HOME=/root/hbase
    export PATH=$PATH:$HBASE_HOME/bin
    ```

    导入环境变量`source /etc/profile`

 3. 配置HBase  

    新建文件夹存储HBase文件`mkdir /var/lib/hbase`  
    进入hbase程序目录，`cd /root/hbase`  

    修改`vim conf/hbase-env.sh`，在最下放添加如下内容：  

    ```sh
    export JAVA_HOME=/usr/java/jdk1.7.0_75/
    export HBASE_MANAGES_ZK=false
    ```

    **修改`vim conf/hbase-site.xml`**  
    在`<configuration></configuration>`间添加如下内容：  

    ```xml
    <configuration>

        <property>
            <name>hbase.rootdir</name>
            <value>hdfs://mycluster/hbase</value>
        </property>

        <property>
            <name>hbase.cluster.distributed</name>
            <value>true</value>
        </property>

        <property>
            <name>hbase.zookeeper.quorum</name>
            <value>nd1,nd2,nd3</value>
        </property>

        <property>
            <name>hbase.zookeeper.property.dataDir</name>
            <value>/var/lib/zookeeper/</value>
        </property>

        <property>
            <name>dfs.replication</name>
            <value>3</value>
        </property>

        <property>
            <name>hbase.zookeeper.property.clientPort</name>
            <value>2181</value>
        </property>

        <property>
            <name>hbase.master</name>
            <value>60000</value>
        </property>

    </configuration>
    ```

    **修改`vim conf/regionservers`**  
    在文件中添加如下内容：  

    ```
    nd1
    nd2
    nd3
    ```

    将hadoop中的`core-site.xml`，`hdfs-site.xml`拷贝至`hbase/conf/`中  
    或者使用软连接方式，推荐使用软连接方式：

    ```sh
    # 建立软连接
    ln -vs /root/hadoop/etc/hadoop/core-site.xml /root/hbase/conf/core-site.xml
    ln -vs /root/hadoop/etc/hadoop/hdfs-site.xml /root/hbase/conf/hdfs-site.xml
    ```

 4. 启动验证服务  

    在nd1上执行`start-hbase.sh`，在nd2上执行`hbase-daemon.sh start master`  
    在nd1和nd2上使用jps查询可以看到  

    ```
    HMaster
    HRegionServer
    ```

    浏览器中打开`http://nd1:60010`或`http://nd2:60010`可以看到当前HMaster的状态

    在状态为master的机器上执行`hbase-daemon.sh stop master`或者直接杀死HMaster进程，访问另一个节点对应的HBase web页面，则可看到状态已由Backup变为Master
    由此可验证HBase的HA切换成功

# 部署 Spark On YARN

 >spark主要的部署方式有：YARN模式、Standalone模式、HA模式
 由于已经部署了YARN HA，因此采用Spark On YARN时，spark仅作为提交作业的客户端，具体的map-reduce任务交由YARN去处理，因此仅需要在一台机器上部署spark即可。

 1. 解压并重命名  

    ```sh
    # 解压
    tar -xvf spark-1.6.1-bin-hadoop2.6.tgz

    # 重命名
    mv spark-1.6.1-bin-hadoop2.6 spark
    ```

 2. 配置环境变量  

    打开`/etc/profile`并修改添加如下内容：

    ```sh
    export SPARK_HOME=/root/zookeeper
    export PATH=$PATH:$SPARK_HOME/bin:$SPARK_HOME/sbin
    ```

    导入环境变量`source /etc/profile`

 3. 配置spark  

    进入spark程序目录，`cd /root/spark`  
    复制spark-env.sh配置，`cp conf/spark-env.sh.template conf/spark-env.sh`  
    修改`vim conf/spark-env.sh`，在最下放添加如下内容：
    ```sh
    HADOOP_CONF_DIR=/root/hadoop/etc/hadoop
    ```

 4. 测试提交作业  

    使用`spark-submit`提交作业，参数Master URL指定为yarn即可，例如：
    ```
    spark-submit --class com.xxx --master yarn xxx.jar
    ```
