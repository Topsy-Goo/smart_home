package ru.gb.smarthome.homekeeper.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import ru.gb.smarthome.common.smart.ISmartDevice;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.enums.SensorStates;
import ru.gb.smarthome.common.smart.structures.*;
import ru.gb.smarthome.homekeeper.PropertyManagerHome;
import ru.gb.smarthome.homekeeper.dtos.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.common.smart.enums.BinatStates.BS_CONTRACT;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.CMD_SENSOR;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.CMD_TASK;
import static ru.gb.smarthome.common.smart.enums.SensorStates.*;
import static ru.gb.smarthome.common.smart.enums.TaskStates.TS_IDLE;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

@Service
@RequiredArgsConstructor
public class HomeService {
/*    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(FAIR);
    private final Lock rLock  = rwLock.readLock();
    private final Lock wLock  = rwLock.writeLock();*/

    private final ConcurrentMap<UUID, ISmartHandler>       uuidToHandler = new ConcurrentHashMap<>(SMART_PORTS_COUNT);
    private final ConcurrentMap<ISmartHandler, DeviceInfo> handlerToInfo = new ConcurrentHashMap<>(SMART_PORTS_COUNT);
    private final ConcurrentMap<DeviceTypes, LinkedList<ISmartHandler>> typeGroups = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceTypes, LinkedList<UUID>>    typeToUuidSlaves = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> friendlyNames = new ConcurrentHashMap<>();
    private final PropertyManagerHome propMan;
    //private final


    {
printf("\n{нестатическая инициализация} поток: %s\n", Thread.currentThread());
        for (DeviceTypes t : DeviceTypes.values()) {
             typeGroups.put(t, new LinkedList<>());
             typeToUuidSlaves.put(t, new LinkedList<>());
        }
    }

/**  */
    public boolean bind (BindRequestDto dto)
    {
        UUID masterUuid = UUID.fromString (dto.getMasterUUID());
        UUID slaveUuid  = UUID.fromString (dto.getSlaveUUID());
        ISmartHandler master = uuidToHandler.get (masterUuid);
        ISmartHandler slave  = uuidToHandler.get (slaveUuid);
        String masterTaskName  = dto.getMasterTaskName();
        UUID slaveFunctionUuid = UUID.fromString (dto.getSlaveFuctionUUID());

    //Отправляем сообщения ведомому и ведущему устройствм, но мастеру отсылаем новость в первую очередь,
    // чтобы опередить возможный поток сообщений от слейва:
        Binate bm = new Binate (BS_CONTRACT, MASTER, slaveUuid, masterTaskName);
        master.pair(bm);
        Binate bs = new Binate (BS_CONTRACT, SLAVE, masterUuid, slaveFunctionUuid);
        slave.pair(bs);
        return true;
    }

/** Добавление УУ в список обнаруженых устройств. */
    public void smartDeviceDetected (ISmartHandler device)
    {
        if (device != null)
        {
            handlerToInfo.put (device, new DeviceInfo (device));
            DeviceInfo info = handlerToInfo.get (device);

            uuidToHandler.put (info.uuid, device);

            if (info.slave) {
                LinkedList<UUID> sg = typeToUuidSlaves.get (info.deviceType);
                addIfAbsent (sg, info.uuid);
            } //добавляем в слэйвы

            List<ISmartHandler> handGroup = typeGroups.get (info.deviceType);
            addIfAbsent (handGroup, device);

            printf ("\nHomeService: обнаружено УУ: %s.\n", device.toString());
        }
    }

/** Удаление УУ из списка обнаруженых устройств. */
    //@SuppressWarnings("all")
    public void smartDeviceIsOff (ISmartHandler device)
    {
        if (device != null)
        {
            DeviceInfo info = handlerToInfo.get (device);

            uuidToHandler.remove (info.uuid);
            handlerToInfo.remove (device); //DeviceInfo.removeDeviceInfo (handlerToInfo, device);

            if (info.slave) {
                LinkedList<UUID> sg = typeToUuidSlaves.get (info.deviceType);
                while (sg.remove(info.uuid));
            } //удаляем из слэйвов

            List<ISmartHandler> hanGroup = typeGroups.get (info.deviceType);
            while (hanGroup.remove(device));

            printf ("\nУдалено УУ: %s (%s, %s, %s).\n",
//TODO: лучше сделать аналог lastNews для УД и пулять такие сообщения туда.
                    device.getDeviceFriendlyName(),
                    info.deviceType,
                    info.vendorString,
                    info.uuid);
            if (DEBUG) {
                print("\nHomeService: оставшиеся устройства: \n");
                for (UUID uu : uuidToHandler.keySet())
                {
                    ISmartHandler h = uuidToHandler.get(uu);
                    DeviceInfo inf = handlerToInfo.get(h);
                    printf ("%s (%s), UUID: %s\n",
                            h.getDeviceFriendlyName(),
                            inf.vendorString,
                            inf.uuid);
        }   }   }
    }

    public boolean sensorTurn (SensorDto senDto) {
        return sensorSetState (senDto, senDto.isOn() ? SST_OFF : SST_ON);
    }

    public boolean sensorAlarmTest (SensorDto senDto) {
        //return sensorSetState (senDto, senDto.isAlarm() ? SST_ON : SST_ALARM); //< Alarm test включает/выключает сигнал тревоги.
        return sensorSetState (senDto, SST_ALARM);    //< Alarm test включает сигнал тревоги на время. (см.также DeviceClientEmpty.onCmdSensor())
    }

    private boolean sensorSetState (SensorDto senDto, SensorStates state)
    {
        UUID deviceUuid = UUID.fromString (senDto.getDeviceUuid());
        ISmartHandler device = uuidToHandler.get(deviceUuid);
        if (device != null) {
            Sensor sensor = new Sensor(senDto).setState (state);

            Message message = new Message().setOpCode(CMD_SENSOR)
                                           .setData (sensor);
            return device.offerRequest (message);  //(о результате запроса узнаем позже)
        }
        return false;
    }

/** Отдаём контроллеру список обнаруженых УУ. */
    public HomeDto getHomeDto ()
    {
        HomeDto dto = new HomeDto (getTypeGroupDtos());
        dto.setUuid (propMan.getUuid().toString());
        dto.setName (propMan.getName());
        return dto;
    }

/** Составляем dto-список всех обнаруженых УУ, отсортированный по типам устройств
 (см. {@link DeviceTypes DeviceTypes}).
 @return TypeGroupDto это — список списков. Он содержащий DeviceDto, рассортированные по типам.
*/
    public List<TypeGroupDto> getTypeGroupDtos ()
    {
        List<TypeGroupDto> typeGroupsDto = new ArrayList<> (DeviceTypes.length);
        for (DeviceTypes type : DeviceTypes.values())
        {
            List<ISmartHandler> group = typeGroups.get(type);
            if (group.isEmpty())
                continue;
            List<DeviceDto> groupDto = group.stream()
                                            .map(h->DeviceDto.smartDeviceToDto (handlerToInfo.get(h)))
                                            .collect (Collectors.toList());
            typeGroupsDto.add (new TypeGroupDto (type.typeNameMultiple, groupDto));
        }
        return typeGroupsDto;
    }

/** Меняем строковое представление UUID на хэндлер УУ, которму этот UUID соответствует.
@param uuidStr строковое представление UUID.
@return ISmartHandler устройства, ассоциированного с указаным UUID. */
    private ISmartHandler deviceByUuidString (String uuidStr)
    {
        ISmartHandler device = null;
        UUID uuid = UUID.fromString (uuidStr);
        device = uuidToHandler.get(uuid);
        return device;
    }

/** По строковому представлению UUID отдаём StateDto устройства, которому этот UUID принадлежит.
 @param uuidStr — строковое представление UUID устрйоства, состояние которого нужно отдать.  */
    public StateDto getStateDto (String uuidStr)
    {
        StateDto stateDto = null;
        ISmartHandler device = deviceByUuidString (uuidStr);

        if (device != null) {
            stateDto = StateDto.deviceStateToDto (handlerToInfo.get(device));
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

        if (device != null) {
            DeviceInfo info = handlerToInfo.get(device);
            result = device.activate (!info.device.isActive());
            info.device.activate (result);
        }
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
            if (device.isActive())
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
        ISmartHandler device = deviceByUuidString (uuidStr);
        boolean ok = (device != null  &&  device.setDeviceFriendlyName (newFriendlyName));
/*        DeviceInfo info;
        if (ok && (info = handlerToInfo.get(device)) != null)
            info.setDeviceFriendlyName (newFriendlyName);*/
        return ok;
//TODO: лучше сделать аналог lastNews для УД и пулять сообщения туда.
    }

/** Составляем список строковых представлений UUID-ов всех обнаруженных УУ и отдаём его на фронт.
@return Массив строковых представлений UUID всех обнаруженных УУ. */
    public String[] getUuidStrings () {
        return uuidToHandler.keySet()
                            .stream()
                            .map(UUID::toString)
                            .toArray(String[]::new);
    }

/** По UUID умного устройства получаем список УУ, которые могут работать с ним в паре в качестве ведомых
 устройств.
 @param uuidStr строковое представление UUID умного устройства.
 @return список dto-шек, каждая из которых содержит строку-UUID и название УУ. Название — для пункта
 списка, а UUID-строка — для идентификации этого пункта, если юзер его выберет. */
    public List<UuidDto> getSlavesList (String uuidStr)
    {
        LinkedList<UuidDto> list = null;                list = new LinkedList<>();
        ISmartHandler device;
        DeviceInfo info;

        if ((device = uuidToHandler.get (UUID.fromString(uuidStr))) != null
        &&  (info   = handlerToInfo.get (device)) != null
        &&  info.master)
        {
            Set<DeviceTypes> masterSlaveTypes = info.getSlaveTypes();
            for (DeviceTypes type : masterSlaveTypes)
            {
                LinkedList<UUID> uuids = typeToUuidSlaves.get(type);
                for (UUID uu : uuids)
                {
                    ISmartHandler slave = uuidToHandler.get(uu);
                    if (device != slave) //< Чтобы самих себя не добавить (можно обойтись без equals()).
                        list.add (new UuidDto(slave.getDeviceFriendlyName(), uu.toString()));
        }   }   }
        return (list != null && !list.isEmpty()) ? list : null;
    }

/** По UUID УУ возвращаем список имён его связываеых ф-ций. */
    public @NotNull List<UuidDto> getBinableFunctionNames (String uuidStr)
    {
        List<UuidDto> list = new ArrayList<>();
        ISmartHandler device;
        DeviceInfo info;

        if ((device = uuidToHandler.get (UUID.fromString(uuidStr))) != null
        &&  (info   = handlerToInfo.get (device)) != null)
        {
            List<UuidDto> functions = info.abilities.getBindableFunctionNames (friendlyNames);
            list.addAll (functions);
        }
        return list;
    }
}
