package com.jade.influx.controller;


import com.jade.influx.domain.Sensor;
import com.jade.influx.util.InfluxDBUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class IndexController1 {

    private final InfluxDBUtil influxDBUtil;

    /**
     * 批量插入第一种方式
     */
    @GetMapping("/index22")
    public void batchInsert() {
        List<Sensor> sensorList = new ArrayList<>();
        Sensor sensor;
        for (int i = 0; i < 50; i++) {
            sensor = new Sensor();
            sensor.setA1(2);
            sensor.setA2(12);
            sensor.setTemp(9);
            sensor.setVoltage(12);
            sensor.setDeviceId("sensor4545-" + i);
            sensorList.add(sensor);
        }
        influxDBUtil.insertBatchByRecords(sensorList);
    }

    /**
     * 批量插入第二种方式
     */
    @GetMapping("/index23")
    public void batchInsert1() {
        List<Sensor> sensorList = new ArrayList<Sensor>();
        Sensor sensor;
        for (int i = 0; i < 50; i++) {
            sensor = new Sensor();
            sensor.setA1(2);
            sensor.setA2(12);
            sensor.setTemp(9);
            sensor.setVoltage(12);
            sensor.setDeviceId("sensor4545-" + i);
            sensorList.add(sensor);
        }
        influxDBUtil.insertBatchByPoints(sensorList);
    }

    /**
     * 获取数据
     */
    @GetMapping("/datas2")
    public void datas(@RequestParam Integer page) {
        int pageSize = 10;
        // InfluxDB支持分页查询,因此可以设置分页查询条件
        String pageQuery = " LIMIT " + pageSize + " OFFSET " + (page - 1) * pageSize;

        String queryCondition = "";  //查询条件暂且为空
        // 此处查询所有内容,如果
        String queryCmd = "SELECT * FROM sensor"
                // 查询指定设备下的日志信息
                // 要指定从 RetentionPolicyName.measurement中查询指定数据,默认的策略可以不加；
                // + 策略name + "." + measurement
                // 添加查询条件(注意查询条件选择tag值,选择field数值会严重拖慢查询速度)
                + queryCondition
                // 查询结果需要按照时间排序
                + " ORDER BY time DESC"
                // 添加分页查询条件
                + pageQuery;

        List<Object> sensorList = influxDBUtil.fetchRecords(queryCmd);
        log.info("query result => {}", sensorList);
    }

    /**
     * 获取数据
     */
    @GetMapping("/datas21")
    public void datas1(@RequestParam Integer page) {
        int pageSize = 10;
        // InfluxDB支持分页查询,因此可以设置分页查询条件
        String pageQuery = " LIMIT " + pageSize + " OFFSET " + (page - 1) * pageSize;

        String queryCondition = "";  //查询条件暂且为空
        // 此处查询所有内容,如果
        String queryCmd = "SELECT * FROM sensor"
                // 查询指定设备下的日志信息
                // 要指定从 RetentionPolicyName.measurement中查询指定数据,默认的策略可以不加；
                // + 策略name + "." + measurement
                // 添加查询条件(注意查询条件选择tag值,选择field数值会严重拖慢查询速度)
                + queryCondition
                // 查询结果需要按照时间排序
                + " ORDER BY time DESC"
                // 添加分页查询条件
                + pageQuery;
        List<Sensor> sensorList = influxDBUtil.fetchResults(queryCmd, Sensor.class);
        //List<Sensor> sensorList = influxdbUtils.fetchResults("*", "sensor", Sensor.class);
        sensorList.forEach(sensor -> log.info("query sensor => {}", sensor));
    }
}
