package ru.gb.smarthome.homekeeper.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gb.smarthome.homekeeper.services.HomeService;
import ru.gb.smarthome.homekeeper.dtos.DeviceDto;

import java.util.List;

import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

@RestController
@RequestMapping ("/v1/main")    //http://localhost:15550/home/v1/main
@RequiredArgsConstructor
public class HomeController
{
    private final HomeService homeService;

    //http://localhost:15550/home/v1/main/device_list
    @GetMapping ("/device_list")
    public List<DeviceDto> getDevicesList ()
    {
        List<DeviceDto> list = homeService.getDeviceDtoList();
        if (DEBUG) println (list.toString());
        return list;
    }

}
