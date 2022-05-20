package ru.gb.smarthome.homekeeper.services;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.enums.SensorStates;
import ru.gb.smarthome.common.smart.structures.DeviceInfo;
import ru.gb.smarthome.common.smart.structures.Message;
import ru.gb.smarthome.common.smart.structures.Sensor;
import ru.gb.smarthome.common.smart.structures.Signal;
import ru.gb.smarthome.homekeeper.PropertyManagerHome;
import ru.gb.smarthome.homekeeper.dtos.*;
import ru.gb.smarthome.homekeeper.entities.Contract;
import ru.gb.smarthome.homekeeper.entities.FriendlyName;
import ru.gb.smarthome.homekeeper.entities.SchedRecord;
import ru.gb.smarthome.homekeeper.repos.IContractsRepo;
import ru.gb.smarthome.homekeeper.repos.IFriendlyNamesRepo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.*;
import static ru.gb.smarthome.common.smart.enums.SensorStates.*;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ConcurrentMap<UUID, ISmartHandler>       uuidToHandler = new ConcurrentHashMap<>(SMART_PORTS_COUNT);
    private final ConcurrentMap<ISmartHandler, DeviceInfo> handlerToInfo = new ConcurrentHashMap<>(SMART_PORTS_COUNT);
    private final ConcurrentMap<DeviceTypes, LinkedList<ISmartHandler>> typeGroups = new ConcurrentHashMap<>();
    private final ConcurrentMap<DeviceTypes, LinkedList<UUID>>    typeToUuidSlaves = new ConcurrentHashMap<>();

    private final PropertyManagerHome propMan;
    private final IFriendlyNamesRepo  friendlyNamesRepo;
    private final IContractsRepo      contractsRepo;
    private final List<String> lastNews        = new LinkedList<>();
    private final Object       lastNewsMonitor = new Object();


    {
//printf("\n{нестатическая инициализация} поток: %s\n", Thread.currentThread());
        for (DeviceTypes t : DeviceTypes.values()) {
             typeGroups.put(t, new LinkedList<>());
             typeToUuidSlaves.put(t, new LinkedList<>());
        }
    }

/*    public boolean videoOn (String uuidStr)
    {
        return launchTask (uuidStr, SEQURCAMERA_TASKNAME_STREAMING);
    }*/

/*    public boolean videoOff (String uuidStr)
    {
        ISmartHandler device = deviceByUuidString (uuidStr);
        if (device != null) {
            //;
            return device.offerRequest (new Message().setOpCode (CMD_INTERRUPT));
        }
        return false;
    }*/

/** Добавление УУ в список обнаруженых устройств. */
    @Transactional
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

        //Помещаем в friendlyNamesRepo пользовательское имя устройства, если его там нет (его начальным
        // значением будет Abilities.vendorString, а позже юзер сможет его изменить):
            String name = readFriendlyNameOrDefault (info.uuidstr, info.vendorString);
            device.setDeviceFriendlyName (name);

        //Помещаем сенсоры во friendlyNamesRepo:
            List<Sensor> sensors = info.abilities.getSensors();
            if (sensors != null)
                for (Sensor s : sensors)
                    readFriendlyNameOrDefault (s.getUuid().toString(), s.getName());
        }
    }

    private String readFriendlyNameOrDefault (String uuid, String nameDefault)
    {
        //friendlyNamesRepo.existsById (uuid);
        String name = friendlyNameByUuid (uuid);
        if (name == null)
            name = friendlyNamesRepo.save (new FriendlyName (uuid, nameDefault))
                                    .getName();
        return name;
    }

    private String friendlyNameByUuid (String key)
    {
        FriendlyName fName = findFriendlyNameByUuid(key);
        return fName != null ? fName.getName() : null;
    }

    public FriendlyName findFriendlyNameByUuid (String key) {
        return friendlyNamesRepo.findById (key).orElse(null);
    }

/*    private String friendlyNameByUuid (UUID uuid) {
        FriendlyName fName = findFriendlyNameByUuid (uuid.toString());
        return fName != null ? fName.getName() : null;
    }*/

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
                //info.abilities.removeBindableFunctions (friendlyNames);
            } //удаляем из слэйвов

            List<ISmartHandler> hanGroup = typeGroups.get (info.deviceType);
            while (hanGroup.remove(device)); //hanGroup.removeIf ((d)->d.equals (device)); //

            addNews (format ("Удалено устройство:\r%s\r%s\n%s\n%s"
                        ,friendlyNameByUuid (info.uuidstr)
                        ,info.deviceType.typeName
                        ,info.vendorString
                        ,info.uuid));
        }
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
 @return TypeGroupDto это — список списков. Он содержащий DeviceDto, рассортированные по типам. */
    @Transactional
    public List<TypeGroupDto> getTypeGroupDtos ()
    {
        List<TypeGroupDto> typeGroupsDto = new ArrayList<> (DeviceTypes.length);
        for (DeviceTypes type : DeviceTypes.values())
        {
            List<ISmartHandler> group = typeGroups.get(type);
            if (group.isEmpty())
                continue;
            List<DeviceDto> groupDto = group.stream()
                                            .map (this::fromSmartHandler)
                                            .collect (Collectors.toList());
            typeGroupsDto.add (new TypeGroupDto (type.typeNameMultiple, groupDto));
        }
        return typeGroupsDto;
    }

    private DeviceDto fromSmartHandler (ISmartHandler device) {
        return DeviceDto.smartDeviceToDto (handlerToInfo.get(device), this::getContractsDto, this::friendlyNameByUuid);
    }

/** Собираем в один список dto-шки мастер-контрактов устройства. */
    @Transactional
    public List<BinateDto> getContractsDto (String strUuid)
    {
        List<BinateDto> contractsDto = null;
        try {
            List<Contract> all = contractsRepo.findAllByMasterUuid (strUuid);
            if (all != null && !all.isEmpty())
                contractsDto = all.stream().map (this::contractToDto).collect (Collectors.toList());
        }
        catch (Exception e) { e.printStackTrace(); }
        finally { return contractsDto; }
    }

    public BinateDto contractToDto (Contract contract)
    {
        return new BinateDto (contract.getTaskName(),
                              friendlyNameByUuid (contract.getSlaveUuid()),
                              contract.getSlaveUuid(),
                              friendlyNameByUuid (contract.getFunctionUuid()),
                              contract.getFunctionUuid());
    }

/** Меняем строковое представление UUID на хэндлер УУ, которму этот UUID соответствует.
@param uuidStr строковое представление UUID.
@return ISmartHandler устройства, ассоциированного с указаным UUID. */
    private ISmartHandler deviceByUuidString (String uuidStr)
    {
        UUID uuid = uuidFromString (uuidStr);
        return (uuid != null) ? uuidToHandler.get (uuid) : null;
    }

/** По строковому представлению UUID отдаём StateDto устройства, которому этот UUID принадлежит.
 @param uuidStr — строковое представление UUID устрйоства, состояние которого нужно отдать.  */
    @Transactional
    public StateDto getStateDto (String uuidStr)
    {
        StateDto stateDto = null;
        ISmartHandler device = deviceByUuidString (uuidStr);

        if (device != null)
            stateDto = StateDto.deviceStateToDto (handlerToInfo.get(device), this::friendlyNameByUuid);
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
 @param taskname название задачи из списка задач, которые УУ может выполнить.
 @return Строка-сообщение для фронта. */
    @Transactional
    public boolean launchTask (String uuidStr, String taskname)
    {
        boolean ok = false;
        String param = taskname;
        String result = FORMAT_CANNOT_LAUNCH_TASK_;

        ISmartHandler device = deviceByUuidString (uuidStr);
        if (device != null)
        {
            if (device.isActive()) {
                Message message = new Message().setOpCode(CMD_TASK).setData (taskname);
                ok = device.offerRequest (message);
                //if (ok)
                //    result = FORMAT_LAUNCHING_TASK_;
            }
            else { // нельзя запустить задачу, т.к. УУ неактивно.
                result = FORMAT_ACTIVATE_DEVICE_FIRST_;
                param = friendlyNameByUuid (uuidStr);
            }
        }
        //else
        if (!ok)
            addNews (format (result, param));
        return ok;
    }

/** Обрабатываем запрос на остановку текущей задачи устройства, чей UUID указан в паарметре.
 Вся обработка заключается на размещение запроса в очереди запросов хэндлера. */
    public boolean interruptTask (String uuidStr)
    {
        ISmartHandler device = deviceByUuidString (uuidStr);
        if (device != null)
            return device.offerRequest (new Message().setOpCode (CMD_INTERRUPT));
        return false;
    }

/** Меняем значение ISmartHandler.deviceFriendlyName на значение, присланое юзером с фронта.
@param uuidStr строковое представление UUID устрйоства.
@param newFriendlyName новое значение для ISmartHandler.deviceFriendlyName.
@return обновлённое значение ISmartHandler.deviceFriendlyName, или NULL в случае неудачи. */
    @Transactional
    public boolean changeDeviceFriendlyName (String uuidStr, String newFriendlyName)
    {
        if (changeFriendlyName (uuidStr, newFriendlyName)) {
            //friendlyNamesRepo.save (new FriendlyName (uuidStr, newFriendlyName));

            ISmartHandler device = uuidToHandler.get (uuidFromString (uuidStr));
            if (device != null)
                device.setDeviceFriendlyName (newFriendlyName);
            return true;
        }
        return false;
    }

    private boolean changeFriendlyName (String uuidStr, String newName)
    {
        try {
            if (isStringsValid (uuidStr, newName))
                friendlyNamesRepo.save (new FriendlyName (uuidStr, newName));
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Transactional
    public boolean changeSensorFriendlyName (String uuidStr, String newName) {
        return changeFriendlyName (uuidStr, newName);
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
    @Transactional
    public List<UuidDto> getSlavesList (String uuidStr)
    {
        LinkedList<UuidDto> list = new LinkedList<>();
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
                    ISmartHandler slave = uuidToHandler.get (uu);
                    if (device != slave)  //< Чтобы самих себя не добавить (можно обойтись без equals()).
                    {
                        String uuidStrSlave = uu.toString();
                        list.add (new UuidDto (friendlyNameByUuid (uuidStrSlave), uuidStrSlave));
        }   }   }   }
        return (!list.isEmpty()) ? list : null;
    }

/** По UUID УУ возвращаем список имён его связываеых ф-ций. */
    @Transactional
    public @NotNull List<UuidDto> getBindableFunctionNames (String uuidStr)
    {
        List<UuidDto> list = Collections.emptyList();
        ISmartHandler device;
        DeviceInfo info;

        if ((device = uuidToHandler.get (UUID.fromString(uuidStr))) != null
        &&  (info   = handlerToInfo.get (device)) != null)
        {
            List<Sensor> sensors = info.abilities.getSensors();
            if (sensors != null && !sensors.isEmpty())
            {
                List<UuidDto> functions = new LinkedList<>();
                for (Sensor s : sensors)
                {
                    String uuid = s.getUuid().toString();
                    functions.add (new UuidDto (friendlyNameByUuid (uuid), uuid));
                }
                list = functions;
            }
        }
        return list;
    }


/** Связываем два хэндлера контрактом типа ведущий-ведомый, или прекращаем их контаркт.
 @param dto запрос, описывающий характеристики контракта, будущего или существующего.
 @param bind указывает, что именно нужно сделать: создать контракт или удалить. */
    @Transactional
    public boolean bind (BindRequestDto dto, boolean bind)
    {
        try {
            if (dto != null)
            {
                String masterUuid   = dto.getMasterUUID();
                String taskName     = dto.getMasterTaskName();
                String slaveUuid    = dto.getSlaveUUID();
                String functionUuid = dto.getSlaveFuctionUUID();
                List<Contract> list2 = contractsRepo.findAllByMasterUuidAndTaskNameAndSlaveUuidAndFunctionUuid (
                                                               masterUuid, taskName, slaveUuid, functionUuid);
                boolean exists = !list2.isEmpty();
                Contract contract = new Contract (masterUuid, taskName, slaveUuid, functionUuid);

                if (bind == BIND) {
                    if (!exists)
                        contractsRepo.save (contract);
                    else
                        addNews (format ("Устройства уже связаны:\r%s\rи\r%s."
                                         ,friendlyNameByUuid (masterUuid), friendlyNameByUuid (slaveUuid)));
                    return !exists;
                }
                else {
                    if (exists)
                        for (Contract c : list2)
                            contractsRepo.delete (c);
                    return exists;
                }
            }
        }
        catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    @Transactional
    public boolean isTaskName (UUID uuid, String taskName)
    {
        ISmartHandler device = uuidToHandler.get (uuid);
        DeviceInfo info;
        if (device != null && ((info = handlerToInfo.get (device))) != null)
            return info.abilities.isTaskName (taskName);
        return false;
    }

/** Определяем, доступно ли устройство для работы с ним на странице расписания.
 @param deviceUuid UUID устройства, доступность которого нужно проверить.
 @return TRUE, если устройство находится в списке обнаруженных устройств. */
    public boolean isAvalable (UUID deviceUuid)
    {
        return (deviceUuid != null) && uuidToHandler.containsKey (deviceUuid);
    }

/** Вызывается из ScheduleService для запуска задач, время которых подошло.
 @param rec экземпляр SchedRecord, содержащий необходимую инфорацию. */
    public void launchScheduledTask (SchedRecord rec)
    {
        FriendlyName fName;
        if (rec != null  &&  (fName = rec.getDeviceName()) != null)
            /*addNews*/ launchTask (fName.getUuid(), rec.getTaskName());
        else
            addNews ("Не удалось запустить задачу из расписания:\rНеизвестная ошибка.");
    }
//----------------------- Сообщения ------------------------------------

    public List<String> getHomeNews ()
    {
        synchronized (lastNewsMonitor) {
            if (lastNews.isEmpty())
                return null;
            List<String> list = new ArrayList<>(lastNews);
            lastNews.clear();
            return list;
        }
    }

    public void addNews (String... news)
    {
        if (news != null && news.length > 0)
        synchronized (lastNewsMonitor) {
            lastNews.addAll (Arrays.asList (news));
        }
    }

//----------------------- Колбэки --------------------------------------

/** Вызывается хэндлером ведомого УУ для поиска и исполнения контрактов, созданых для реагирования
 на событие, описанное в signal.
 @param signal Описание события. */
    public void slaveCallback (Signal signal)
    {
        List<Contract> contracts = null;
        if (signal != null)
            contracts = contractsRepo.findAllByFunctionUuid (signal.getFunctionUuid().toString());

        if (contracts != null && !contracts.isEmpty())
        for (Contract c : contracts)
        {
            ISmartHandler master = uuidToHandler.get (UUID.fromString (c.getMasterUuid()));
            if (master != null && master.isActive())
            {
                master.offerRequest (new Message (CMD_SIGNAL, signal.setData (c.getTaskName())));
                //Неактивное УУ не должно принимать сигналы.
            }
        }
    }
}
