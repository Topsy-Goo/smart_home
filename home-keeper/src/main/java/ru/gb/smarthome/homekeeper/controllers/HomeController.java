package ru.gb.smarthome.homekeeper.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.gb.smarthome.common.smart.structures.Task;
import ru.gb.smarthome.homekeeper.dtos.HomeDto;
import ru.gb.smarthome.homekeeper.dtos.StateDto;
import ru.gb.smarthome.homekeeper.dtos.TaskDto;
import ru.gb.smarthome.homekeeper.services.HomeService;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.*;

@RestController
@RequestMapping ("/v1/main")    //http://localhost:15550/home/v1/main
@RequiredArgsConstructor
public class HomeController
{
    private final HomeService homeService;
    static ObjectMapper mapper = new ObjectMapper(); //TODO: Убрать из релиза.

    //http://localhost:15550/home/v1/main/home_dto
    @GetMapping ("/home_dto")
    public HomeDto getDevicesList ()
    {
//printf("/home_dto\n");
        return homeService.getHomeDto();
    }

    //Запрос состояния УУ имеющего указаный UUID.
    //http://localhost:15550/home/v1/main/state?uuid=…
    @GetMapping ("/state/{uuid}")
    public StateDto getState (@PathVariable(name="uuid") String uuidStr)
    {
        StateDto dto = homeService.getStateDto(uuidStr);
//printf("/state/{%s}   >>>  %s\n", uuidStr, objectToJsonString(dto));
        return dto;
    }

    //http://localhost:15550/home/v1/main/activate?uuid=…
    @GetMapping ("/activate/{uuid}")
    public boolean toggleActiveState (@PathVariable(name="uuid") String uuidStr)
    {
printf("/activate/{%s}\n", uuidStr);
        return homeService.toggleActiveState (uuidStr);
    }

    //http://localhost:15550/home/v1/main/friendly_name?uuid=…&newFriendlyName=…
    @GetMapping ("/friendly_name/{uuid}/{newFriendlyName}")
    public void changeFriendlyName (@PathVariable(name="uuid") String uuidStr, @PathVariable String newFriendlyName)
    {
        homeService.changeFriendlyName (uuidStr, newFriendlyName);
        printf("/friendly_name/{%s}/{newFriendlyName=%s}\n", uuidStr, newFriendlyName);
    }

    //http://localhost:15550/home/v1/main/uuids
    @GetMapping ("/uuids")
    public String[] getUuids ()
    {
//print("\n/uuids \t");
        String[] arr = homeService.getUuidStrings();
//print(Arrays.toString(arr)+"\n");
        return arr;
    }

    //http://localhost:15550/home/v1/main/launch_task?uuid=…&taskname=…
    @GetMapping ("/launch_task/{uuid}/{taskname}")
    public StringDto launchTask (@PathVariable(name="uuid") String uuidStr, @PathVariable String taskname)
    {
        StringDto dto = new StringDto (homeService.launchTask (uuidStr, taskname));
printf("/task/{%s}/{%s} >>> %s\n", uuidStr, taskname, dto);
        return dto;
    }

    @Data static class StringDto {
        public String s;
        public StringDto(){}
        public StringDto(String s){ this.s = s; }
    }

    static String objectToJsonString (Object o) {
        try {
            return mapper.writeValueAsString(o);
        }
        catch (JsonProcessingException e) { e.printStackTrace(); }
        return format("objectToJsonString() не справился с объектом %s", o.getClass().getSimpleName());
    }

}
