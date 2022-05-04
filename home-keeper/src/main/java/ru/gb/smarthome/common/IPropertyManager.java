package ru.gb.smarthome.common;

import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.structures.Sensor;
import ru.gb.smarthome.common.smart.structures.Task;

import java.util.*;

public interface IPropertyManager
{
    int getServerSocketPort ();
    String getServerAddress ();

    UUID getUuid ();
    String getName ();

/** Пустой список задач УУ. */
    Set<Task> emptyTaskList ();
/** Умолчальная реализация в PropertyManager возвращает пустой список. УУ, которое может выполнять задачи,
 должно переопределить этот метод. */
    Set<Task> getAvailableTasks ();

/** Пустой упорядоченный список сенсоров УУ. */
    List<Sensor> emptySensorsList();
/** Умолчальная реализация в PropertyManager возвращает пустой список. УУ, которое оснащено датчиками,
 должно переопределить этот метод. */
    List<Sensor> getAvailableSensors ();

/** Пустой список типов УУ. */
    Set<DeviceTypes> emptyDeviceTypesList ();
/** Мастер-УУ (ведущее устр-во) должно вернуть список поддерживаемых типов УУ, которые могут служить
 ему ведомыми устройствами.<p>
 Умолчальная реализация в PropertyManager возвращает пустой список. УУ, которое может быть мастером,
 должно переопределить этот метод.  */
    Set<DeviceTypes> slaveTypes ();
}
