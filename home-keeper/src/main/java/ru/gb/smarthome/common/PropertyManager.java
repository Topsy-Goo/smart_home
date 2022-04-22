package ru.gb.smarthome.common;

import ru.gb.smarthome.common.smart.structures.Task;

import java.util.*;

import static ru.gb.smarthome.common.FactoryCommon.*;

abstract public class PropertyManager
{
    protected int    serverPort    = SERVER_PORT_DEFAULT;
    protected String serverAddress = SERVER_ADDRESS_DEFAULT;

//----------------------- Методы интерфейса -----------------------------------------------

    public int getServerSocketPort () {    return serverPort;    }
    public String getServerAddress () {    return serverAddress;    }

    public Set<Task> emptyTaskList () {
        return new HashSet<>();
    }

    abstract protected UUID getUuid ();
    abstract protected String getName ();
    abstract protected Set<Task> getAvailableTasks ();
}
