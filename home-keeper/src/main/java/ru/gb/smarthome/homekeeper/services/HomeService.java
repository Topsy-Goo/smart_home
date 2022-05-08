package ru.gb.smarthome.homekeeper.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
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
import static ru.gb.smarthome.common.smart.enums.OperationCodes.*;
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
//printf("\n{нестатическая инициализация} поток: %s\n", Thread.currentThread());
        for (DeviceTypes t : DeviceTypes.values()) {
             typeGroups.put(t, new LinkedList<>());
             typeToUuidSlaves.put(t, new LinkedList<>());
        }
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
                info.abilities.addBindableFunctions (friendlyNames);
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
                while (sg.remove (info.uuid)); //sg.removeIf ((e)->e.equals (info.uuid)); //
                info.abilities.removeBindableFunctions (friendlyNames);
            } //удаляем из слэйвов

            List<ISmartHandler> hanGroup = typeGroups.get (info.deviceType);
            while (hanGroup.remove(device)); //hanGroup.removeIf ((d)->d.equals (device)); //

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
        return sensorSetState (senDto, SST_ALARM);
    }

    private boolean sensorSetState (SensorDto senDto, SensorStates state)
    {
        UUID deviceUuid = UUID.fromString (senDto.getDeviceUuid());
        ISmartHandler device = uuidToHandler.get(deviceUuid);
        if (device != null)
        {
            Sensor sensor = new Sensor(senDto).setSstate(state);
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
 @return TRUE, если удалось изменить активность УУ. */
    public boolean toggleActiveState (String uuidStr)
    {
        ISmartHandler device = deviceByUuidString (uuidStr);
        return (device != null) && device.activate (!device.isActive());
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
            if (device.isActive()) {
                Message message = new Message().setOpCode(CMD_TASK).setData (taskname);

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
    //@SuppressWarnings("all")
    public @NotNull List<UuidDto> getBinableFunctionNames (String uuidStr)
    {
        List<UuidDto> list = Collections.emptyList();
        ISmartHandler device;
        DeviceInfo info;

        if ((device = uuidToHandler.get (UUID.fromString(uuidStr))) != null
        &&  (info   = handlerToInfo.get (device)) != null)
        {
            List<UuidDto> functions = info.abilities.getBindableFunctionNames (friendlyNames);
            if (functions != null)
                list = functions;
        }
        return list;
    }

/** Собираем в один список dto-шки мастер-контрактов устройства. */
    public List<BinateDto> getContracts (String strUuid)
    {
        try {
            UUID masterUuid = UUID.fromString (strUuid);
            ISmartHandler master = uuidToHandler.get (masterUuid);
            return master.getMasterContractsDto();
        }
        catch (Exception e) { return null; }
    }

/** Вызывается хэндлером ведомого УУ для передачи хэндлеру ведущего УУ информацию о событии.
 @param masterUuid UUID ведущего УУ, которому адресована информация о событии.
 @param signal Описание события. */
    public void slaveCallback (UUID masterUuid, Signal signal)
    {
        ISmartHandler master = uuidToHandler.get (masterUuid);
        if (master != null && master.isActive())
            master.offerRequest (new Message (CMD_SIGNAL, /*masterUuid,*/ signal));
        //Неактивное УУ не должно принимать сигналы.
    }

/** Связываем два хэндлера контрактом типа ведущий-ведомый, или прекращаем их контаркт.
 @param dto запрос, описывающий характеристики контракта, будущего или существующего.
 @param bind указывает, что именно нужно сделать: создать контракт или удалить. */
    public boolean bind (BindRequestDto dto, boolean bind)
    {
        try {
            UUID masterUuid = UUID.fromString (dto.getMasterUUID());
            UUID slaveUuid  = UUID.fromString (dto.getSlaveUUID());
            ISmartHandler master = uuidToHandler.get (masterUuid);
            ISmartHandler slave  = uuidToHandler.get (slaveUuid);
            String masterTaskName  = dto.getMasterTaskName();
            UUID slaveFunctionUuid = UUID.fromString (dto.getSlaveFuctionUUID());

            Binate bm = new Binate (BS_CONTRACT, MASTER, slaveUuid, slaveFunctionUuid, masterTaskName, null); //< нельзя вместо slaveUuid      передавать null, а то не будет работать equals().
            Binate bs = new Binate (BS_CONTRACT, SLAVE, masterUuid, slaveFunctionUuid, masterTaskName, null); //< нельзя вместо masterTaskName передавать null, а то не будет работать equals().
            if (bind == BIND) {
                //Отправляем сообщения ведомому и ведущему устройствм, но мастеру отсылаем
                // новость в первую очередь, чтобы опередить возможный поток сообщений от слейва:
                if (master.pair(bm)) {
                    if (slave.pair(bs)) {
                        BinateDto binateDto = new BinateDto (masterTaskName, slave.getDeviceFriendlyName(),
                                                             friendlyNames.get (slaveFunctionUuid),
                                                             slaveUuid, slaveFunctionUuid);
                        bm.setDto (binateDto);
                        return true;
                    }
                    master.unpair (bm);
            }   }
            else return slave.unpair(bs) & master.unpair(bm); //< должны выполниться оба метода.
                //Отправляем сообщения ведомому и ведущему устройствм, но слейву отсылаем
                // новость в первую очередь, чтобы он не слал мастеру сообщения, когда мы будем
                // мастера отключать.
        }
        catch (Exception e){ e.printStackTrace(); }
        return false;
    }

}
