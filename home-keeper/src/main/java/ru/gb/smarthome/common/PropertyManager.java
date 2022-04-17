package ru.gb.smarthome.common;

import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.structures.Task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static ru.gb.smarthome.common.FactoryCommon.*;

abstract public class PropertyManager /*implements IPropertyManager*/ {

    protected int    serverPort    = SERVER_PORT_DEFAULT;
    protected String serverAddress = SERVER_ADDRESS_DEFAULT;


    public boolean setServerSocketPort (int val) {
        boolean ok = val <= 65535 && val >= 0;
        if (ok) serverPort = val;
        return ok;
    }
    public boolean setServerAddress (String val) {
        boolean ok = isStringsValid(val);
        if (ok) serverAddress = val;
        return ok;
    }

    public int getServerSocketPort () {    return serverPort;    }
    public String getServerAddress () {    return serverAddress;    }

    public List<Task> getTaskList_Empty () {
        return new ArrayList<>(0);
    }

    public Set<Task> getAvailableTasks_Fridge ()
    {
        Set<Task> tasks = new HashSet<>();
        tasks.add (new Task ("Разморозка полная", AUTONOMIC, 2, TimeUnit.HOURS, NON_INTERRUPTIBLE));
        //tasks.add (new Task ("Разморозка Мороз.камеры", AUTONOMIC, 120, TimeUnit.SECONDS, NON_INTERRUPTIBLE));
        //tasks.add (new Task ("Разморозка Холод.камеры", AUTONOMIC, 120, TimeUnit.SECONDS, NON_INTERRUPTIBLE));
        return tasks;
    }

}
