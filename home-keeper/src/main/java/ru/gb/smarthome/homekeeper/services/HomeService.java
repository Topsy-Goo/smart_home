package ru.gb.smarthome.homekeeper.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.homekeeper.dtos.HomeDto;
import ru.gb.smarthome.homekeeper.dtos.TypeGroupDto;

import java.util.*;

import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

@Service
@RequiredArgsConstructor
public class HomeService
{
    private final Map<UUID, ISmartHandler> handlers = new HashMap<>(SMART_PORTS_COUNT);
    private final Map<DeviceTypes, LinkedList<ISmartHandler>> handlerGroups = new TreeMap<>();
    //private final Map<DeviceTypes, LinkedList<UUID>> uuidGroups = new TreeMap<>();


    {
/*        for (DeviceTypes t : DeviceTypes.values())
            uuidGroups.put(t, new LinkedList<>());*/

        for (DeviceTypes t : DeviceTypes.values())
            handlerGroups.put(t, new LinkedList<>());
    }

/** Добавление УУ в список обнаруженых устройств. */
    public void smartDeviceDetected (ISmartHandler device)
    {
        if (device != null) {
            Abilities abilities = device.getAbilities();
            UUID uuid = abilities.getUuid();

            handlers.put (uuid, device);

            //List<UUID> uidGroup = uuidGroups.get (abilities.getType());
            //addIfAbsent (uidGroup, uuid);

            List<ISmartHandler> hanGroup = handlerGroups.get (abilities.getType());
            addIfAbsent (hanGroup, device);

            printf ("\nHomeService: обнаружено УУ: %s.\n", device.toString());
        }
    }

/** Удаление УУ из списка обнаруженых устройств. */
    //@SuppressWarnings("all")
    public void smartDeviceIsOff (ISmartHandler device)
    {
        if (device != null) {
            Abilities abilities = device.getAbilities();
            UUID uuid = abilities.getUuid();

            handlers.remove (abilities.getUuid());

            //List<UUID> uidGroup = uuidGroups.get (abilities.getType());
            //while (uidGroup.remove(uuid));

            List<ISmartHandler> hanGroup = handlerGroups.get (abilities.getType());
            while (hanGroup.remove(device));

            printf ("\nУдалено УУ: %s.\n", device);
            if (DEBUG)
                printf("\nHomeService: оставшиеся устройства: \n%s\n", handlers);
        }
    }

/** Отдаём контроллеру список обнаруженых УУ. */
    public HomeDto getHomeDto () {
        return new HomeDto (TypeGroupDto.getTypeGroupDtos (handlerGroups));
    }

}
