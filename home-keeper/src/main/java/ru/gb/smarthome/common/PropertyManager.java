package ru.gb.smarthome.common;

import ru.gb.smarthome.common.smart.structures.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static ru.gb.smarthome.common.FactoryCommon.*;

abstract public class PropertyManager /*implements IPropertyManager*/ {

    protected int    serverPort    = SERVER_PORT_DEFAULT;
    protected String serverAddress = SERVER_ADDRESS_DEFAULT;

//----------------------- Методы интерфейса -----------------------------------------------

    public int getServerSocketPort () {    return serverPort;    }
    public String getServerAddress () {    return serverAddress;    }

    public List<Task> emptyTaskList () {
        return new ArrayList<>(0);
    }

//TODO:------------------ Перенести в спец.классы -----------------------------------------

    public List<Task> getAvailableTasks_Empty () {
        //return emptyTaskList();
        return getAvailableTasks_Fridge();
    }

    public List<Task> getAvailableTasks_Fridge ()
    {
        List<Task> tasks = emptyTaskList();
        tasks.add (new Task ("Разморозка полная", AUTONOMIC, 2, TimeUnit.HOURS, NON_INTERRUPTIBLE));
        //tasks.add (new Task ("Разморозка Мороз.камеры", AUTONOMIC, 120, TimeUnit.SECONDS, NON_INTERRUPTIBLE));
        //tasks.add (new Task ("Разморозка Холод.камеры", AUTONOMIC, 120, TimeUnit.SECONDS, NON_INTERRUPTIBLE));
        return tasks;
    }
}
