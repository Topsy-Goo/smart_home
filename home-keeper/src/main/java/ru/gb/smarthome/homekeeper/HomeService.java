package ru.gb.smarthome.homekeeper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gb.smarthome.common.IDeviceServer;
import ru.gb.smarthome.common.smart.ISmartDevice;
import ru.gb.smarthome.homekeeper.dtos.DeviceDto;

import javax.annotation.PostConstruct;

import java.util.*;

import static ru.gb.smarthome.common.FactoryCommon.SMART_PORTS_COUNT;
import static ru.gb.smarthome.common.FactoryCommon.println;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final Map<UUID, ISmartDevice> handlers = new HashMap<>(SMART_PORTS_COUNT);

    @PostConstruct private void init () {
        //if (DEBUG) println ("\nHomeService.init() завершился.\n");
    }

//Добавление клиента происходит не сразу, а только после запроса его UUID. Поэтому клиент какое-то время обслуживается, не имея записи в handlers.
    public boolean addClientHandler (ISmartDevice device, UUID uuid)
    {
        boolean ok = !handlers.containsKey(uuid);
        if (ok)
            handlers.put (uuid, device);
        return ok;
    }

/** Отдаём контроллеру список обнаруженых УУ. */
    public List<DeviceDto> getDeviceList () {
        return new ArrayList<>();
    }

}
