package com.jade.influx.controller;

import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
public class IndexController {

    @Autowired
    @Qualifier("influxDB")
    private InfluxDB influxDB;

    private final String measurement = "sensor";

    @Value("${spring.influxdb.database1}")
    private String database;

    /**
     * 批量插入第一种方式
     */
    @GetMapping("/index")
    public void insert() {
        List<String> lines = new ArrayList<>();
        Point point;
        for (int i = 0; i < 50; i++) {
            point = Point.measurement(measurement)
                    .tag("deviceId", "sensor" + i)
                    .addField("temp", 3)
                    .addField("voltage", 145 + i)
                    .addField("A1", "4i")
                    .addField("A2", "4i").build();
            lines.add(point.lineProtocol());
        }
        //写入
        influxDB.write(lines);
    }

    /**
     * 批量插入第二种方式
     */
    @GetMapping("/index1")
    public void batchInsert() {
        BatchPoints batchPoints = BatchPoints
                .database(database)
                .consistency(InfluxDB.ConsistencyLevel.ALL)
                .build();
        //遍历sqlserver获取数据
        for (int i = 0; i < 50; i++) {
            //创建单条数据对象——表名
            Point point = Point.measurement(measurement)
                    //tag属性——只能存储String类型
                    .tag("deviceId", "sensor" + i)
                    .addField("temp", 3)
                    .addField("voltage", 145 + i)
                    .addField("A1", "4i")
                    .addField("A2", "4i").build();
            //将单条数据存储到集合中
            batchPoints.point(point);
        }
        //批量插入
        influxDB.write(batchPoints);
    }

    /**
     * 获取数据
     */
    @GetMapping("/datas")
    public void datas(@RequestParam Integer page) {
        int pageSize = 10;
        // InfluxDB支持分页查询,因此可以设置分页查询条件
        String pageQuery = " LIMIT " + pageSize + " OFFSET " + (page - 1) * pageSize;

        String queryCondition = "";  //查询条件暂且为空
        // 此处查询所有内容,如果
        String queryCmd = "SELECT * FROM "
                // 查询指定设备下的日志信息
                // 要指定从 RetentionPolicyName.measurement中查询指定数据,默认的策略可以不加；
                // + 策略name + "." + measurement
                + measurement
                // 添加查询条件(注意查询条件选择tag值,选择field数值会严重拖慢查询速度)
                + queryCondition
                // 查询结果需要按照时间排序
                + " ORDER BY time DESC"
                // 添加分页查询条件
                + pageQuery;

        QueryResult queryResult = influxDB.query(new Query(queryCmd, database));
        log.info("query result => {}", queryResult);
    }
}
