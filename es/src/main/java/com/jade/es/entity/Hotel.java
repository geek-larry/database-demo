package com.jade.es.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jade.elasticsearchdb.annotation.ESFieldAnnotation;
import lombok.Data;

@Data
@TableName("t_hotel")
public class Hotel {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ESFieldAnnotation("id")
    private Long id;

    @TableField("title")
    @ESFieldAnnotation(value = "title", type = "text")
    private String title;

    @TableField("business_district")
    @ESFieldAnnotation(value = "business_district",type = "keyword")
    private String businessDistrict;

    @TableField("address")
    @ESFieldAnnotation(value = "address", type = "text")
    private String address;

    @TableField("lon")
    @ESFieldAnnotation(value = "location.lon", type = "geo_point")
    private Double lon;

    @TableField("lat")
    @ESFieldAnnotation(value = "location.lat", type = "geo_point")
    private Double lat;

    @TableField("city")
    @ESFieldAnnotation(value = "city", type = "keyword")
    private String city;

    @TableField("price")
    @ESFieldAnnotation(value = "price", type = "double")
    private Double price;

    @TableField("star")
    @ESFieldAnnotation(value = "star", type = "keyword")
    private String star;

    @TableField("full_room")
    @ESFieldAnnotation(value = "full_room", type = "boolean")
    private Boolean fullRoom;

    @TableField("impression")
    @ESFieldAnnotation(value = "impression", type = "keyword")
    private String impression;

    @TableField("favourable_percent")
    @ESFieldAnnotation(value = "favourable_percent", type = "integer")
    private Integer favourablePercent;

    @TableField("pic")
    @ESFieldAnnotation(value = "pic", type = "keyword")
    private String pic;
}
