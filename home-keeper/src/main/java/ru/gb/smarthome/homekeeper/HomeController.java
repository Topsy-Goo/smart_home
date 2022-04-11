package ru.gb.smarthome.homekeeper;

import lombok.RequiredArgsConstructor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.gb.smarthome.homekeeper.dtos.DeviceDto;

import java.util.List;

@RestController
@RequestMapping ("/home/v1/main")    //http://localhost:15550/home/v1/main
@RequiredArgsConstructor
public class HomeController
{
    private final HomeService homeService;

    //http://localhost:15550/home/v1/main/device_list
    @GetMapping ("/device_list")
    public List<DeviceDto> getProductsPage () {
        return homeService.getDeviceList();
    }

}
