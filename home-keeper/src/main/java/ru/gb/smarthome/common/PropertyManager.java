package ru.gb.smarthome.common;

import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.structures.Sensor;
import ru.gb.smarthome.common.smart.structures.Task;

import java.util.*;

import static ru.gb.smarthome.common.FactoryCommon.*;

abstract public class PropertyManager implements IPropertyManager
{
    protected       int    serverPort    = SERVER_PORT_DEFAULT;
    protected       String serverAddress = SERVER_ADDRESS_DEFAULT;
    protected final Set<Task>        tasks      = emptyTaskList();
    protected final List<Sensor>     sensors    = emptySensorsList();
    protected final Set<DeviceTypes> slaveTypes = emptyDeviceTypesList();

//----------------------- Методы интерфейса -----------------------------------------------

    @Override public int getServerSocketPort () { return serverPort; }
    @Override public String getServerAddress () { return serverAddress; }

    @Override public Set<Task> emptyTaskList () { return new HashSet<>(); }
    @Override public Set<Task> getAvailableTasks () { return Collections.unmodifiableSet (tasks); }

    @Override public List<Sensor> emptySensorsList() { return new LinkedList<>(); }
    @Override public List<Sensor> getAvailableSensors () { return Collections.unmodifiableList (sensors); }

    @Override public Set<DeviceTypes> emptyDeviceTypesList () { return new HashSet<>(); }
    @Override public Set<DeviceTypes> slaveTypes () { return Collections.unmodifiableSet (slaveTypes); }

/** Копирование списка поддерживаемых ведомых типов УУ. */
    public static Set<DeviceTypes> copyDeviceTypesList (Collection<? extends DeviceTypes> c)
    {
        return (c != null) ? new HashSet<>(c) : null;
    }
}
