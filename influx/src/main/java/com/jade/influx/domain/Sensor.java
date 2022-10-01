package com.jade.influx.domain;

import lombok.Data;
import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

@Data
@Measurement(name = "sensor")
public class Sensor {

    @Column(name = "deviceId", tag = true)
    private String deviceId;

    @Column(name = "temp")
    private float temp;

    @Column(name = "voltage")
    private float voltage;

    @Column(name = "A1")
    private float A1;

    @Column(name = "A2")
    private float A2;

    @Column(name = "time")
    private String time;

}