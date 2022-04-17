package ru.gb.smarthome.homekeeper.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.homekeeper.dtos.DeviceDto;

import java.util.*;
import java.util.stream.Collectors;

import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final Map<UUID, ISmartHandler> handlers = new HashMap<>(SMART_PORTS_COUNT);
    //private final

/*    @PostConstruct private void init () {        if (DEBUG) println ("\nHomeService.init() завершился.\n");    }*/

//Добавление клиента происходит не сразу, а только после запроса его UUID. Поэтому клиент какое-то время обслуживается, не имея записи в handlers.
    public boolean addClientHandler (ISmartHandler device, UUID uuid)
    {
        boolean ok = !handlers.containsKey(uuid);
        if (ok)
            handlers.put (uuid, device);
        return ok;
    }

/** Добавление УУ в список обнаруженых устройств. */
    public void smartDeviceDetected (ISmartHandler device) {
        if (device != null) {
            handlers.put (device.getAbilities().getUuid(), device);
            printf ("\nHomeService: обнаружено УУ: %s.\n", device.toString());
        }
    }

/** Удаление УУ из списка обнаруженых устройств. */
    public void smartDeviceIsOff (ISmartHandler device) {
        if (device != null) {
            handlers.remove (device.getAbilities().getUuid());
            printf ("\nУдалено УУ: %s.\n", device);
            if (DEBUG) {
                printf("\nHomeService: оставшиеся устройства: \n%s\n", handlers);
            }
        }
    }

/** Отдаём контроллеру список обнаруженых УУ. */
    public List<DeviceDto> getDeviceDtoList ()
    {
/*        List<DeviceDto> list = new ArrayList<>();
        for (Map.Entry<UUID, ISmartDevice> e : handlers.entrySet()) { list.add (new DeviceDto (e.getValue())); }*/
//      List<DeviceDto> l0 = handlers.entrySet().stream().map((e)->deviceToDto(e.getValue()))  .collect(Collectors.toList());
//      List<DeviceDto> l1 = handlers.entrySet().stream().map((e)->new DeviceDto(e.getValue())).collect(Collectors.toList());
//      List<DeviceDto> l2 = handlers.values()  .stream().map((o)->deviceToDto(o))             .collect(Collectors.toList());
//      List<DeviceDto> l3 = handlers.values()  .stream().map(HomeService::deviceToDto)        .collect(Collectors.toList());
//      List<DeviceDto> l4 = handlers.values()  .stream().map((o)->new DeviceDto(o))           .collect(Collectors.toList());
//      List<DeviceDto> l5 = handlers.values()  .stream().map(DeviceDto::new)                  .collect(Collectors.toList());

        return handlers.values().stream().map(DeviceDto::new).collect(Collectors.toList());
    }
    //public static DeviceDto deviceToDto (ISmartDevice device) { return new DeviceDto(device); }
}
