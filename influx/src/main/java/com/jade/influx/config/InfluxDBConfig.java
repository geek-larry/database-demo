package com.jade.influx.config;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class InfluxDBConfig {

    @Value("${spring.influxdb.username}")
    private String user;

    @Value("${spring.influxdb.password}")
    private String password;

    @Value("${spring.influxdb.url}")
    private String host;

    @Value("${spring.influxdb.database1}")
    private String database1;

    @Value("${spring.influxdb.database2}")
    private String database2;

    @Bean(name = "influxDB1")
    public InfluxDB influxDB1() {
        InfluxDB influxDB = InfluxDBFactory.connect(host,user,password);
        influxDB.setDatabase(database1)
                .enableBatch(20, 200, TimeUnit.MILLISECONDS);

        return influxDB;
    }

    @Bean(name = "influxDB2")
    public InfluxDB influxDB2() {
        InfluxDB influxDB = InfluxDBFactory.connect(host,user,password);
        influxDB.setDatabase(database2)
                .enableBatch(20, 200, TimeUnit.MILLISECONDS);

        return influxDB;
    }

    @Bean("influxDB")
    public InfluxDB influxDB(){
        InfluxDB influxDB = InfluxDBFactory.connect(host, user, password);
        try {

            /**
             * 异步插入：
             * enableBatch这里第一个是point的个数，第二个是时间，单位毫秒
             * point的个数和时间是联合使用的，如果满100条或者60 * 1000毫秒
             * 满足任何一个条件就会发送一次写的请求。
             */
            influxDB.setDatabase(database1).enableBatch(100,1000 * 60, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //设置默认策略
            influxDB.setRetentionPolicy("sensor_retention");
        }
        //设置日志输出级别
        influxDB.setLogLevel(InfluxDB.LogLevel.BASIC);
        return influxDB;
    }

}