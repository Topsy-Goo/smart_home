package ru.gb.smarthome.homekeeper.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.gb.smarthome.homekeeper.dtos.HomeDto;
import ru.gb.smarthome.homekeeper.services.HomeService;

import static ru.gb.smarthome.common.FactoryCommon.printf;

@RestController
@RequestMapping ("/v1/main")    //http://localhost:15550/home/v1/main
@RequiredArgsConstructor
public class HomeController
{
    private final HomeService homeService;

    //http://localhost:15550/home/v1/main/home_dto
    @GetMapping ("/home_dto")
    public HomeDto getDevicesList ()
    {
        return homeService.getHomeDto();
    }

    //http://localhost:15550/home/v1/main/activate?uuid=…
    @GetMapping ("/activate/{uuid}")
    public boolean toggleActiveState (@PathVariable(name="uuid") String uuidStr)
    {
        return homeService.toggleActiveState (uuidStr);
    }

    //http://localhost:15550/home/v1/main/panel?uuid=…&isopened=…
    @GetMapping ("/panel/{uuid}/{isopened}")
    public void toggleHtmlPanelState (@PathVariable(name="uuid") String uuidStr, @PathVariable Boolean isopened)
    {
        homeService.toggleHtmlPanelState (uuidStr, isopened);
    }

    //http://localhost:15550/home/v1/main/friendly_name?uuid=…&newFriendlyName=…
    @GetMapping ("/friendly_name/{uuid}/{newFriendlyName}")
    public boolean changeFriendlyName (@PathVariable(name="uuid") String uuidStr, @PathVariable String newFriendlyName)
    {
        String result = homeService.changeFriendlyName (uuidStr, newFriendlyName);
        return result.equals(newFriendlyName);
    }
}
