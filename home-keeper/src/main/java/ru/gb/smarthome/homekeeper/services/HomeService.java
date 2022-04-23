package ru.gb.smarthome.homekeeper.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.DeviceInfo;
import ru.gb.smarthome.homekeeper.PropertyManagerHome;
import ru.gb.smarthome.homekeeper.dtos.DeviceDto;
import ru.gb.smarthome.homekeeper.dtos.HomeDto;
import ru.gb.smarthome.homekeeper.dtos.StateDto;
import ru.gb.smarthome.homekeeper.dtos.TypeGroupDto;

import java.util.*;
import java.util.stream.Collectors;

import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

@Service
@RequiredArgsConstructor
public class HomeService        //TODO: Нужна синхронизация доступа к спискам обнаруженных УУ.
{
    private final PropertyManagerHome propMan;
    private final Map<UUID, ISmartHandler> handlers = new HashMap<>(SMART_PORTS_COUNT);
    private final Map<DeviceTypes, LinkedList<ISmartHandler>> handlerGroups = new TreeMap<>();
    //private final Map<DeviceTypes, LinkedList<UUID>> uuidGroups = new TreeMap<>();
    private final Map<ISmartHandler, DeviceInfo> handlersInfo = new HashMap<>(SMART_PORTS_COUNT);

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

            handlersInfo.put(device, new DeviceInfo(device));

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

            handlersInfo.remove(device);

            printf ("\nУдалено УУ: %s.\n", device);
            if (DEBUG)
                printf("\nHomeService: оставшиеся устройства: \n%s\n", handlers);
        }
    }

/** Отдаём контроллеру список обнаруженых УУ. */
    public HomeDto getHomeDto () {
        HomeDto dto = new HomeDto (TypeGroupDto.getTypeGroupDtos (handlerGroups, handlersInfo));
        dto.setUuid (propMan.getUuid().toString());
        return dto;
    }

/** Меняем строковое представление UUID на хэндлер УУ, которму этот UUID соответствует.
@param uuidStr строковое представление UUID.
@return ISmartHandler устройства, ассоциированного с указаным UUID. */
    private ISmartHandler deviceByUuidString (String uuidStr)
    {
        ISmartHandler device = null;
        try {
            UUID uuid = UUID.fromString (uuidStr);
            device = handlers.get(uuid);
        }
        catch (IllegalArgumentException e) { e.printStackTrace(); }
        return device;
    }

/** По строковому представлению UUID отдаём StateDto устройства, которому этот UUID принадлежит.
 @param uuidStr — строковое представление UUID устрйоства, состояние которого нужно отдать.  */
    public StateDto getState (String uuidStr) {
        StateDto stateDto = null;
        ISmartHandler device = deviceByUuidString (uuidStr);
        if (device != null) {
            stateDto = StateDto.deviceStateToDto (device.getState());
        }
        return stateDto;
    }

/** Переключаем состояние DeviceState.active по команде из фронта.
 @param uuidStr — строковое представление UUID устрйоства, активное состояние которого нужно изменить.
 @return новое состояние DeviceState.active указанного устройства. */
    public boolean toggleActiveState (String uuidStr)
    {
        boolean result = false;
        ISmartHandler device = deviceByUuidString (uuidStr);
        if (device != null) {
            boolean active = device.getState().isActive();
            if (device.activate (!active))
                result = !active;
        }
        return result;
    }

/** Переключаем состояние DeviceInfo.htmlPanelOpend по команде из фронта.
 @param uuidStr строковое представление UUID устрйоства, панель которого на фронте открылась или закрылась. */
/*    public void toggleHtmlPanelState (String uuidStr, boolean isopened)
    {
        ISmartHandler device = deviceByUuidString (uuidStr);
        DeviceInfo info;
        if (device != null  &&  (info = handlersInfo.get(device)) != null) {
            info.htmlPanelOpened = isopened == OPENED;
        }
    }*/

/** Меняем значение ISmartHandler.deviceFriendlyName на значение, присланое юзером с фронта.
@param uuidStr строковое представление UUID устрйоства.
@param newFriendlyName новое значение для ISmartHandler.deviceFriendlyName.
@return обновлённое значение ISmartHandler.deviceFriendlyName, или NULL в случае неудачи. */
    public String changeFriendlyName (String uuidStr, String newFriendlyName)
    {
        ISmartHandler device = deviceByUuidString(uuidStr);
        if (device != null
        &&  device.setDeviceFriendlyName (newFriendlyName))
        {
            return device.getDeviceFriendlyName();
        }
        return null;
    }

/** Составляем список строковых представлений UUID-ов всех обнаруженных УУ и отдаём его на фронт.
@return Массив строковых представлений UUID всех обнаруженных УУ. */
    public String[] getUuidStrings ()
    {
/*        return new ArrayList<>(handlers.keySet())
                    .stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());*/
        return handlers.keySet()
                       .stream()
                       .map(UUID::toString)
                       .toArray(String[]::new);
    }
}
