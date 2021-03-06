package ru.gb.smarthome.homekeeper.controllers;

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
    //static ObjectMapper mapper = new ObjectMapper();


    //Запрос всех данных об УД.
    //http://localhost:15550/home/v1/main/home_dto
    @GetMapping ("/home_dto")
    public HomeDto getDevicesList ()
    {
        return homeService.getHomeDto();
    }

    //Запрос состояния УУ, имеющего указаный UUID.
    //http://localhost:15550/home/v1/main/state/{uuid}
    @GetMapping ("/state/{uuid}")
    public StateDto getState (@PathVariable(name="uuid") String strUuid)
    {
        StateDto dto = homeService.getStateDto (strUuid);
//print("<{s}>");
//if (dto != null) lnprintf("<{%s}>", dto.getVideoImageSource());
        return dto;
    }

    //Активация/деактивация УУ по UUID.
    //http://localhost:15550/home/v1/main/activate/{uuid}
    @GetMapping ("/activate/{uuid}")
    public boolean toggleActiveState (@PathVariable(name="uuid") String strUuid)
    {
        return homeService.toggleActiveState (strUuid);
    }

    //Изменение пользовательского имени УУ, имеющего указаный UUID.
    //http://localhost:15550/home/v1/main/device_friendly_name?uuid=…&newFriendlyName=…
    @GetMapping ("/device_friendly_name/{uuid}/{newFriendlyName}")
    public boolean changeDeviceFriendlyName (@PathVariable(name="uuid") String strUuid,
                                             @PathVariable String newFriendlyName)
    {
        return homeService.changeDeviceFriendlyName (strUuid, newFriendlyName);
    }

    //Изменение пользовательского имени датчика, имеющего указаный UUID.
    //http://localhost:15550/home/v1/main/sensor_friendly_name?uuid=…&newSensorName=…
    @GetMapping ("/sensor_friendly_name/{uuid}/{newSensorName}")
    public boolean changeSensorFriendlyName (@PathVariable(name="uuid") String strUuid,
                                             @PathVariable String newSensorName)
    {
        return homeService.changeSensorFriendlyName (strUuid, newSensorName);
    }

    //Запрос UUID-ов всех обнаруженный УУ. С пом.такого запроса фронт определяет, не изменился ли набор обнаруженных УУ.
    //http://localhost:15550/home/v1/main/all-uuids
    @GetMapping ("/all-uuids")
    public String[] getUuids ()
    {
        return homeService.getUuidStrings();
    }

    //Запрос на запуск указанной задачи указанного устройства.
    //http://localhost:15550/home/v1/main/launch_task/{uuid}/{taskname}
    @GetMapping ("/launch_task/{uuid}/{taskname}")
    public boolean launchTask (@PathVariable(name="uuid") String strUuid, @PathVariable String taskname)
    {
        return (homeService.launchTask (strUuid, taskname));
    }

    //Запрос на остановку указанной задачи указанного устройства.
    //http://localhost:15550/home/v1/main/interrupt_task/{uuid}
    @GetMapping ("/interrupt_task/{uuid}")
    public boolean interruptTask (@PathVariable(name="uuid") String strUuid/*, @PathVariable String taskname*/)
    {
        return (homeService.interruptTask (strUuid/*, taskname*/));
    }

    //http://localhost:15550/home/v1/main/slave-list/{uuid}
    @GetMapping ("/slave-list/{uuid}")
    public List<UuidDto> getSlavesList (@PathVariable(name="uuid") String strUuid)
    {
        return homeService.getSlavesList (strUuid);
    }

    //Запрос ф-ций указанного УУ, которые могут использоваться сторонним связанным УУ.
    //http://localhost:15550/home/v1/main/bindable-functions/{uuid}
    @GetMapping ("/bindable-functions/{uuid}")
    public List<UuidDto> getBindableFunctionNames (@PathVariable(name="uuid") String strUuid)
    {
        List<UuidDto> list = homeService.getBindableFunctionNames(strUuid);
//lnprintf ("/bindable-functions/{%s} >>> %s\n", strUuid, list);
        return list;
    }

/** запрос на связывание устрйоств контрактом.  */
    //http://localhost:15550/home/v1/main/bind
    @PostMapping ("/bind")
    public boolean bind (@RequestBody BindRequestDto dto)
    {
        return homeService.bind (dto, BIND);
    }

/** запрос на расторжение контракта между устрйоствами.  */
    //http://localhost:15550/home/v1/main/unbind
    @PostMapping ("/unbind")
    public boolean unbind (@RequestBody BindRequestDto dto)
    {
        return homeService.bind (dto, UNBIND);
    }

/** Вкл./выкл указаный датчик. */
    //http://localhost:15550/home/v1/main/sensor-turn
    @PostMapping ("/sensor-turn")
    public boolean sensorTurn (@RequestBody SensorDto senDto)
    {
        return (senDto != null) && homeService.sensorTurn (senDto);
    }

/** Вкл.для датчика сигнал тревоги. Сигнал выключится автоматически спусты несколько секунд. */
    //http://localhost:15550/home/v1/main/sensor-alarm
    @PostMapping ("/sensor-alarm")
    public boolean sensorAlarmTest (@RequestBody SensorDto senDto)
    {
        return (senDto != null) && homeService.sensorAlarmTest (senDto);
    }

    //http://localhost:15550/home/v1/main/contracts/{uuid}
    @GetMapping ("/contracts/{uuid}")
    public List<BinateDto> getContracts (@PathVariable(name="uuid") String strUuid)
    {
        return homeService.getContractsDto(strUuid);
    }

    //http://localhost:15550/home/v1/main/home_news
    @GetMapping ("/home_news")
    public List<String> getHomeNews ()
    {
        List<String> list = homeService.getHomeNews();
//if (list != null) lnprintf ("/home_news - ответ: %s.\n", list);
        return list;
    }

    @GetMapping ("/is_task_name/{uuid}/{taskName}")
    public boolean isTaskName (@PathVariable(name="uuid") String strUuid, @PathVariable String taskName)
    {
//lnprintf("isTaskName() - параметры: %s, %s.\n", strUuid, taskName);
        return homeService.isTaskName (uuidFromString (strUuid), taskName);
    }

/*    @GetMapping ("/video_on/{uuid}")
    public boolean videoOn (@PathVariable(name="uuid") String strUuid)
    {
//lnprintf("videoOn() - параметр: %s.\n", strUuid);
        return homeService.videoOn (strUuid);
    }*/

/*    @GetMapping ("/video_off/{uuid}")
    public boolean videoOff (@PathVariable(name="uuid") String strUuid)
    {
//lnprintf("videoOff() - параметр: %s.\n", strUuid);
        return homeService.videoOff (strUuid);
    }*/
}
