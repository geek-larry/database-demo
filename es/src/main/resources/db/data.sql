create database if not exists es_data
default character set utf8;

use es_data;

create table if not exists t_hotel
(
    id bigint(20) not null primary key ,
    title varchar(200) not null default '' comment '酒店名称',
    business_district varchar(200) comment '商业区',
    address varchar(500) comment '酒店地址',
    lon decimal comment '经度',
    lat decimal comment '纬度',
    city varchar(20) comment '所在城市',
    price decimal comment '价格',
    star varchar(20),
    full_room bit comment '是否满了',
    impression varchar(100),
    favourable_percent bigint,
    pic varchar(50)
)

