package ru.gb.smarthome.homekeeper.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.gb.smarthome.homekeeper.dtos.*;
import ru.gb.smarthome.homekeeper.services.HomeService;

import java.util.List;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.*;

@RestController
@RequestMapping ("/v1/main")    //http://localhost:15550/home/v1/main
@RequiredArgsConstructor
public class HomeController
{
    private final HomeService homeService;
    static ObjectMapper mapper = new ObjectMapper(); //TODO: Убрать из релиза.


    //Запрос всех данных об УД.
    //http://localhost:15550/home/v1/main/home_dto
    @GetMapping ("/home_dto")
    public HomeDto getDevicesList ()
    {
//printf("/home_dto, поток: %s\n", Thread.currentThread());
        return homeService.getHomeDto();
    }

    //Запрос состояния УУ, имеющего указаный UUID.
    //http://localhost:15550/home/v1/main/state/{uuid}
    @GetMapping ("/state/{uuid}")
    public StateDto getState (@PathVariable(name="uuid") String strUuid)
    {
        StateDto dto = homeService.getStateDto(strUuid);
//printf("/state/{%s}   >>>  %s\n\n", strUuid, /*objectToJsonString*/(dto));
        return dto;
    }

    //Активация/деактивация УУ по UUID.
    //http://localhost:15550/home/v1/main/activate/{uuid}
    @GetMapping ("/activate/{uuid}")
    public boolean toggleActiveState (@PathVariable(name="uuid") String strUuid)
    {
        boolean result = homeService.toggleActiveState (strUuid);
printf("/activate/{%s}, result = %s\n", strUuid, result);
        return result;
    }

    //Изменение пользовательского имени УУ, имеющего указаный UUID.
    //http://localhost:15550/home/v1/main/friendly_name?uuid=…&newFriendlyName=…
    @GetMapping ("/friendly_name/{uuid}/{newFriendlyName}")
    public boolean changeFriendlyName (@PathVariable(name="uuid") String strUuid, @PathVariable String newFriendlyName)
    {
//printf("/friendly_name/{%s}/{newFriendlyName=%s}, поток: %s\n", strUuid, newFriendlyName, Thread.currentThread());
        return homeService.changeFriendlyName (strUuid, newFriendlyName);
    }

    //Запрос UUID-ов всех обнаруженный УУ. С пом.такого запроса фронт определяет, не изменился ли набор обнаруженных УУ.
    //http://localhost:15550/home/v1/main/all-uuids
    @GetMapping ("/all-uuids")
    public String[] getUuids ()
    {   //printf("\n/uuids , поток: %s\t", Thread.currentThread());
        String[] arr = homeService.getUuidStrings();
        //print(Arrays.toString(arr)+"\n");
        return arr;
    }

    //Запрос на запуск указанной задачи указанного устройства.
    //http://localhost:15550/home/v1/main/launch_task/{uuid}/{taskname}
    @GetMapping ("/launch_task/{uuid}/{taskname}")
    public StringDto launchTask (@PathVariable(name="uuid") String strUuid, @PathVariable String taskname)
    {
        StringDto dto = new StringDto (homeService.launchTask (strUuid, taskname));
//printf("/task/{%s}/{%s} >>> %s, поток: %s\n", strUuid, taskname, dto, Thread.currentThread());
        return dto;
    }

    //http://localhost:15550/home/v1/main/slave-list/{uuid}
    @GetMapping ("/slave-list/{uuid}")
    public List<UuidDto> getSlavesList (@PathVariable(name="uuid") String strUuid)
    {
//printf("\n/slave-list/{%s}", strUuid);
        List<UuidDto> dto = homeService.getSlavesList (strUuid);
//println("\nслэйв-лист: "+ dto);
        return dto;
    }

    //Запрос ф-ций указанного УУ, которые могут использоваться сторонним связанным УУ.
    //http://localhost:15550/home/v1/main/bindable-functions/{uuid}
    @GetMapping ("/bindable-functions/{uuid}")
    public List<UuidDto> getBinableFunctionNames (@PathVariable(name="uuid") String strUuid)
    {
        List<UuidDto> list = homeService.getBinableFunctionNames (strUuid);
//lnprintf ("/bindable-functions/{%s} >>> %s\n", strUuid, list);
        return list;
    }

/** запрос на связывание устрйоств контрактом.  */
    //http://localhost:15550/home/v1/main/bind
    @PostMapping ("/bind")
    public boolean bind (@RequestBody BindRequestDto dto)
    {
        boolean ok = homeService.bind (dto, BIND);
lnprintf ("/bind парам:\n%s.\nрезультат = %b\n", dto.toString(), ok);
        return ok;
    }

/** запрос на расторжение контракта между устрйоствами.  */
    //http://localhost:15550/home/v1/main/unbind
    @PostMapping ("/unbind")
    public boolean unbind (@RequestBody BindRequestDto dto)
    {
        boolean ok = homeService.bind (dto, UNBIND);
lnprintf ("/unbind парам:\n%s.\nрезультат = %b\n", dto.toString(), ok);
        return ok;
    }

/** Вкл./выкл указаный датчик. */
    //http://localhost:15550/home/v1/main/sensor-turn
    @PostMapping ("/sensor-turn")
    public boolean sensorTurn (@RequestBody SensorDto senDto)
    {
lnprintf ("/sensor-turn парам:%s.", senDto);
        return (senDto != null) && homeService.sensorTurn (senDto);
    }

/** Вкл.для датчика сигнал тревоги. Сигнал выключится автоматически спусты несколько секунд. */
    //http://localhost:15550/home/v1/main/sensor-alarm
    @PostMapping ("/sensor-alarm")
    public boolean sensorAlarmTest (@RequestBody SensorDto senDto)
    {
lnprintf ("/sensor-alarm парам:\n%s.\n", senDto);
        return (senDto != null) && homeService.sensorAlarmTest (senDto);
    }

    //http://localhost:15550/home/v1/main/contracts/{uuid}
    @GetMapping ("/contracts/{uuid}")
    public List<BinateDto> getContracts (@PathVariable(name="uuid") String strUuid)
    {
        return homeService.getContracts (strUuid);
    }

//Используется только в отладочных целях. Переводит объект в JSON-строку.
    static String objectToJsonString (Object o) {
        try {
            return mapper.writeValueAsString(o);
        }
        catch (JsonProcessingException e) { e.printStackTrace(); }
        return format("objectToJsonString() не справился с объектом %s", o.getClass().getSimpleName());
    }

}
