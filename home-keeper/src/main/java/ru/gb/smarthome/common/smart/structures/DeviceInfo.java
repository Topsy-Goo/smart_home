package ru.gb.smarthome.common.smart.structures;

import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.homekeeper.dtos.AbilitiesDto;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/** Структура с информацией об устройстве, которую нет смысла хранить ни на стороне клиента, ни в хэндлере.
и */
public class DeviceInfo
{
    public  final ISmartHandler device;
    public  final boolean master;
    public  final boolean slave;
    private final Set<DeviceTypes> slaveTypes;
    public  final AbilitiesDto     abilitiesDto;
    public  final String   vendorString;
    public  final UUID     uuid;
    public  final String   uuidstr;
    public  final DeviceTypes deviceType;
    public  final Abilities   abilities;
    //@Getter @Setter private boolean active;
    //@Getter @Setter private String deviceFriendlyName = "";
    //public final Map<ISmartHandler, String> slaveList; //< String это — UUIS-строка.


    public DeviceInfo (ISmartHandler theDevice/*, ConcurrentMap<ISmartHandler, DeviceInfo> info,
                       ConcurrentMap<DeviceTypes, LinkedList<ISmartHandler>> groups*/)
    {   device       = theDevice;
        abilities    = device.getAbilities();
        master       = abilities.isMaster();
        slave        = abilities.isSlave();
        slaveTypes   = abilities.getSlaveTypes();
        vendorString = abilities.getVendorString();
        uuid         = abilities.getUuid();
        uuidstr      = uuid.toString();
        abilitiesDto = AbilitiesDto.abilitiesToDto (abilities);
        deviceType = abilities.getType();

        //active       = device.isActive();
        //deviceFriendlyName = theDevice.getDeviceFriendlyName();
/*      slaveList = abilities.isMaster() ? new HashMap<>() : null;
        if (slaveList != null)    findFriends (info, groups);*/
    }

    public Set<DeviceTypes> getSlaveTypes () { return Collections.unmodifiableSet (slaveTypes); }
}
