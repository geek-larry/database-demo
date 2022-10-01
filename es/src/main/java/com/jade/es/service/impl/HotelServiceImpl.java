package com.jade.es.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jade.elasticsearchdb.entity.Hotel;
import com.jade.elasticsearchdb.mapper.HotelMapper;
import com.jade.elasticsearchdb.service.IHotelService;
import org.springframework.stereotype.Service;

@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

}
