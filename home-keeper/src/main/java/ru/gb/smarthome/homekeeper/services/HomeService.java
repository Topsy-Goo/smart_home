package ru.gb.smarthome.homekeeper.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.DeviceInfo;
import ru.gb.smarthome.common.smart.structures.Message;
import ru.gb.smarthome.common.smart.structures.Task;
import ru.gb.smarthome.homekeeper.PropertyManagerHome;
import ru.gb.smarthome.homekeeper.dtos.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.CMD_TASK;
import static ru.gb.smarthome.common.smart.enums.TaskStates.TS_IDLE;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

@Service
@RequiredArgsConstructor
public class HomeService {
/*    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(FAIR);
    private final Lock rLock  = rwLock.readLock();
    private final Lock wLock  = rwLock.writeLock();*/

    private final ConcurrentMap<UUID, ISmartHandler>           handlers = new ConcurrentHashMap<>(SMART_PORTS_COUNT);
    private final ConcurrentMap<ISmartHandler, DeviceInfo> handlersInfo = new ConcurrentHashMap<>(SMART_PORTS_COUNT);
    private final ConcurrentMap<DeviceTypes, LinkedList<ISmartHandler>> handlerGroups = new ConcurrentHashMap<>();
    //private final Map<DeviceTypes, LinkedList<UUID>> uuidGroups = new TreeMap<>();

    private final PropertyManagerHome propMan;

    {
/*        for (DeviceTypes t : DeviceTypes.values())
            uuidGroups.put(t, new LinkedList<>());*/

        for (DeviceTypes t : DeviceTypes.values())
            handlerGroups.put(t, new LinkedList<>());
    }

/** Добавление УУ в список обнаруженых устройств. */
    public void smartDeviceDetected (ISmartHandler device)
    {
        //wLock.lock();
        if (device != null)
        /*try*/ {
            Abilities abilities = device.getAbilities();
            UUID uuid = abilities.getUuid();

            handlers.put (uuid, device);

            //List<UUID> uidGroup = uuidGroups.get (abilities.getType());
            //addIfAbsent (uidGroup, uuid);

            List<ISmartHandler> handGroup = handlerGroups.get (abilities.getType());
            addIfAbsent (handGroup, device);

            handlersInfo.put(device, new DeviceInfo(device));
            printf ("\nHomeService: обнаружено УУ: %s.\n", device.toString());
        }
        //finally { wLock.unlock(); }
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

            printf ("\nУдалено УУ: %s (%s, %s, %s).\n",
                    device.getDeviceFriendlyName(),
                    abilities.getType(),
                    abilities.getVendorString(),
                    uuid);
            if (DEBUG)
                printf("\nHomeService: оставшиеся устройства: \n%s\n", handlers);
        }
    }

/** Отдаём контроллеру список обнаруженых УУ. */
    public HomeDto getHomeDto ()
    {
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
        UUID uuid = UUID.fromString (uuidStr);
        device = handlers.get(uuid);
        return device;
    }

/** По строковому представлению UUID отдаём StateDto устройства, которому этот UUID принадлежит.
 @param uuidStr — строковое представление UUID устрйоства, состояние которого нужно отдать.  */
    public StateDto getStateDto (String uuidStr)
    {
        StateDto stateDto = null;
        ISmartHandler device = deviceByUuidString (uuidStr);

        if (device != null) {
            stateDto = StateDto.deviceStateToDto (device);
        }
        return stateDto;
    }

/** Переключаем состояние DeviceState.active по команде из фронта.
 @param uuidStr строковое представление UUID устрйоства, активное состояние которого нужно изменить.
 @return новое состояние DeviceState.active указанного устройства. */
    public boolean toggleActiveState (String uuidStr)
    {
        boolean result = false;
        ISmartHandler device = deviceByUuidString (uuidStr);

        if (device != null)
            result = device.activate (!device.getState().isActive());

        return result;
    }

/** Пробуем запустить задачу, имя которой пришло от фронта.
 @param uuidStr строковое представление UUID устрйоства, которое будет выполнять задачу.
 @param taskname название задачи из списка задач, которые УУ может выполнить. */
    public String launchTask (String uuidStr, String taskname)
    {
        String param = taskname;
        String result = FORMAT_CANNOT_LAUNCH_TASK_;
        ISmartHandler device = deviceByUuidString (uuidStr);
        if (device != null)
        {
            if (device.getState().isActive())
            {
                Task t = new Task(taskname, TS_IDLE, DEF_TASK_MESSAGE);
                Message message = new Message().setOpCode(CMD_TASK).setData(t);

                if (device.offerRequest (message))
                    result = FORMAT_LAUNCHING_TASK_;
            }
            else { // нельзя запустить задачу, т.к. УУ неактивно.
                result = FORMAT_ACTIVATE_DEVICE_FIRST_;
                param = device.getDeviceFriendlyName();
            }
        }
        return format (result, param);
    }

/** Меняем значение ISmartHandler.deviceFriendlyName на значение, присланое юзером с фронта.
@param uuidStr строковое представление UUID устрйоства.
@param newFriendlyName новое значение для ISmartHandler.deviceFriendlyName.
@return обновлённое значение ISmartHandler.deviceFriendlyName, или NULL в случае неудачи. */
    public boolean changeFriendlyName (String uuidStr, String newFriendlyName)
    {
        ISmartHandler device = deviceByUuidString(uuidStr);
        return (device != null) && device.setDeviceFriendlyName (newFriendlyName);
    }

/** Составляем список строковых представлений UUID-ов всех обнаруженных УУ и отдаём его на фронт.
@return Массив строковых представлений UUID всех обнаруженных УУ. */
    public String[] getUuidStrings ()
    {
            return handlers.keySet()
                           .stream()
                           .map(UUID::toString)
                           .toArray(String[]::new);
    }
}
