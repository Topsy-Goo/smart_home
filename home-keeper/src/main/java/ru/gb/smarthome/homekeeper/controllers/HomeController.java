package ru.gb.smarthome.homekeeper.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gb.smarthome.homekeeper.dtos.HomeDto;
import ru.gb.smarthome.homekeeper.services.HomeService;

@RestController
@RequestMapping ("/v1/main")    //http://localhost:15550/home/v1/main
@RequiredArgsConstructor
public class HomeController
{
    private final HomeService homeService;

    //http://localhost:15550/home/v1/main/device_list
    @GetMapping ("/device_list")
    public HomeDto getDevicesList ()
    {
        return homeService.getHomeDto();
    }

}
