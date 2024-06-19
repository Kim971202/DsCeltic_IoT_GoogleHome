package com.google.smarthome.controller;

import com.google.smarthome.dto.GoogleDTO;
import com.google.smarthome.mapper.GoogleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private GoogleMapper googleMapper;

    @GetMapping("/testMapping")
    public void testController(){

        GoogleDTO result = googleMapper.getInfoByDeviceId("0.2.481.1.1.2045534365636f313353.20202020303833413844434146353435");
        System.out.println(result);

    }

}
