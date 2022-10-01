package com.jade.influx.service;

import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class InfluxBatch implements CommandLineRunner {

    @Resource(name = "influxDB1")
    private InfluxDB influxDB1;

    @Resource(name = "influxDB2")
    private InfluxDB influxDB2;

    @Override
    public void run(String... args) {

        Point point = Point.measurement("test")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("value", 1)
                .tag("name", "test1")
                .build();
        influxDB1.write(point);

        Point point2 = Point.measurement("test")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("value", 2)
                .tag("name", "test2")
                .build();
        influxDB2.write(point2);
    }

}