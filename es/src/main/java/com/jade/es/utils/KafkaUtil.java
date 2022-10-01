package com.jade.es.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


@Component
@Slf4j
public class KafkaUtil {

    @Value("${kafka.bootstrap.servers}")
    private String brokerList;

    @Value("${kafka.request.timeout.ms}")
    private Integer requestTimeoutMsConfig;

    private AdminClient adminClient;

    private KafkaProducer<String, String> kafkaProducer;

    @PostConstruct
    public void init() {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMsConfig);
        adminClient = AdminClient.create(properties);

        Properties msgProp = new Properties();
        msgProp.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        msgProp.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        msgProp.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        kafkaProducer = new KafkaProducer(msgProp);
    }

    /**
     * 创建主题
     *
     * @param topicName
     * @param numPartitions     分区数
     * @param replicationFactor 副本因子
     */
    public void createTopic(String topicName, int numPartitions, short replicationFactor) {
        NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor);
        CreateTopicsResult result = adminClient.createTopics(Arrays.asList(newTopic));
        try {
            result.all().get();
            log.info("{}创建成功", topicName);
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 删除主题
     * @param topicName
     */
    public void deleteTopic(String... topicName) {
        DeleteTopicsResult result = adminClient.deleteTopics(Arrays.asList(topicName));
        try {
            result.all().get();
            log.info("{}删除成功", topicName);
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 列出主题列表
     */
    public void listTopic() {
        ListTopicsResult result = adminClient.listTopics();
        KafkaFuture<Collection<TopicListing>> listings = result.listings();
        try {
            Collection<TopicListing> topicListings = listings.get();
            List<String> list = new ArrayList<>();
            topicListings.stream().forEach(item -> {
                list.add(item.name());
            });
            log.info("topic列表：" + StrUtil.join(",", list.toArray()));
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 返回主题详细信息
     * @param topicName
     * @return
     */
    public Config describeTopicConfig(String topicName) {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
        DescribeConfigsResult result = adminClient.describeConfigs(Arrays.asList(resource));
        try {
            Config config = result.all().get().get(resource);
            return config;
        } catch (InterruptedException | ExecutionException e) {
            log.error(e.getMessage(), e);
        } catch (UnknownTopicOrPartitionException e) {
            log.error("{}不存在", topicName);
        }
        return null;
    }

    /**
     * 发送消息
     * @param topic
     * @param msg
     */
    public void sendMsg(String topic, String msg) {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, msg);
        Future<RecordMetadata> future = kafkaProducer.send(producerRecord, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if (exception == null) {
                    log.info("{}投递成功", msg);
                }
            }
        });
        try {
            RecordMetadata metadata = future.get();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }
    }
}
