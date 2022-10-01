package com.jade.es.dto;

import lombok.Data;

@Data
public class HotelDto {
    private Long id;

    private String title;

    private String businessDistrict;

    private String address;

    private Double lon;

    private Double lat;

    private String city;

    private Double price;

    private String star;

    private boolean fullRoom;

    private String impression;

    private Integer favourablePercent;

    private String pic;

    private String location;

    private String suggestValue;
}
