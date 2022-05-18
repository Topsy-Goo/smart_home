package ru.gb.smarthome.common.smart.structures;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.smart.enums.DeviceTypes;

import java.io.Serializable;
import java.util.*;

import static ru.gb.smarthome.common.FactoryCommon.*;

/** Класс содержит инфорацию об УУ, которую в него как бы заложил производитель:
содержит некоторые (как бы стандартизированные) сведения об УУ и исчерпывающее описание возможностей УУ.
УД должен строить свою работу с УУ исходя из этого описания. По этой причине синхронизация
доступа к полям Abilities не предусмотрена, — экземпляр Abilities не должен меняться после
его создания.<p>
    Все поля этого класса следует считать сонстантами. Сделать их final нам не позволяют
требования сериализации.
 */
public final class Abilities implements Serializable
{
    @Getter @Setter private DeviceTypes type = DEF_DEVICETYPE;

    /** Описание УУ, которым его снабдил производитель. */
    @Getter private String  vendorString = DEF_ABILITIES_DTO_VENDORSTRING;

    /** Уникальный ID УУ. По идее, он должен быть зашит в УУ, например, в ПЗУ. */
    @Getter private UUID    uuid;

    /** Флаг, указывающий, может ли УУ быть переключено в режим энергосбережения. */
    @Getter private boolean canSleep;

/**
     Список задач, которые УУ может выполнять. Если УУ не может выполнять какие-то действия,
 которые можно было бы считать отдельными задачами, то оно должно остаить этот список
 пустым. Список должен быть неизменяемым.<p>
     Основным критерием для выделение какой-либо операции УУ или сотсояния УУ в отдельную задачу
 должен быть ответ на вопрос: сможем ли мы показать это юзеру в списке задач, которые УУ
 может для него выполнить.<p>
     (Пока мы считаем, что переключение в режим сна не является задачей, хотя, теоретически, ничто
 не мешает сделать это задачей, например, в будущем. Сейчас для переводу УУ в сон используется
 операция CMD_SLEEP.)  */
    @Getter private Set<Task> tasks;

/** Список сенсоров, доступных извне. */
    @Getter private List<Sensor> sensors = Collections.emptyList();

/** Устройство может работать в паре с другим УУ в качестве ведущего устройства. */
    @Getter private boolean master;

/** Устройство может работать в паре с другим УУ в качестве ведомого устройства. */
    @Getter private boolean slave;

/** Типы УУ, которые могут быть ведомыми этим УУ. */
    @Getter private Set<DeviceTypes> slaveTypes;

/* * Внешний порт для трансляции видеопотокка в вёб-интерфейс. */
    //@Getter private int videoPort;


    private Abilities (){} //< для сериализации

/**
@param dt Как бы стандартизированный тип устройства. Берём из {@link ru.gb.smarthome.common.smart.enums.DeviceTypes DeviceTypes}.
@param vname Название УУ, которым его как бы снабдил производтель. Например, «Atlant CR-4022f-GDL» (содержит название производителя, название модели и, возможно, что-то ещё.
@param uu UUID умного устройства (уникальный id экземпляра УУ, как бы зашитый в него производителем).
@param sleepable Принимает значения CAN_SLEEP и CANNOT_SLEEP и указывает, поддерживает ли УУ перевод его в режим энергосбережения (режим сна).
*/
    public Abilities (@NotNull DeviceTypes dt, @NotNull String vname, @NotNull UUID uu,
                      boolean sleepable)
    {   type = dt;
        vendorString = vname;
        uuid = uu;
        canSleep = sleepable;
    }

    public Abilities copy ()
    {
        return new Abilities (type, vendorString, uuid, canSleep)
                     .setTasks (tasks)
                     .setSensors(sensors)
                     .setMaster(master)
                     .setSlave(slave)
                     .setSlaveTypes (PropertyManager.copyDeviceTypesList (slaveTypes))/*
                     .setVideoPort (videoPort)*/;
    }

    public Abilities setTasks   (Set<Task> val) { tasks = val;   return this; }
    public Abilities setMaster  (boolean val)   { master = val;  return this; }
    public Abilities setSlave   (boolean val)   { slave = val;   return this; }
    public Abilities setSlaveTypes (Set<DeviceTypes> val) { slaveTypes = val;  return this; }
    public Abilities setSensors (List<Sensor> val)
    {
        sensors = (val != null) ? val : Collections.emptyList();
        return this;
    }
/*    public Abilities setVideoPort (int val)
    {
        if (val >= 0)
            videoPort = val;
        return this;
    }*/

    public boolean isTaskName (String taskName) {    return taskByName (taskName) != null;    }

    public Task taskByName (String taskName)
    {
        if (tasks != null && isStringsValid (taskName))
            for (Task t : tasks)
                if (t.getName().equals(taskName))
                    return t;
        return null;
    }

    public boolean isSensorUuid (UUID sensorUuid) {   return sensorByUuid (sensorUuid) != null;   }

    public Sensor sensorByUuid (UUID sensorUuid) //TODO:удалить?
    {
        if (sensorUuid != null && sensors != null)
            for (Sensor s : sensors)
                if (s.getUuid().equals (sensorUuid))
                    return s;
        return null;
    }

    public String toString () //< для отладки
    {
        StringBuilder sb = new StringBuilder("Abilities:\n");

        sb.append("\tТип : ").append(type.typeName)
          .append("\n\tНазвание : ").append(vendorString)
          .append("\n\tUUID : ").append(uuid)
          .append("\n\tЭнергосбережение : ").append(canSleep ? "есть" : "нет")
          .append("\n\tЗадачи : ");

        for (Task t : tasks)
            sb.append("\n\t\tname:\t").append(t.getName())
              .append("\n\t\t\tautonomic:\t").append(t.isAutonomic())
              .append(    "\t\tinterruptible:\t").append(t.isInterruptible())
              .append("\n\t\t\tduration:\t").append(t.getDuration())
              .append(    "\t\tremained:\t").append(t.getRemained().get())
              .append(    "\t\telapsed:\t").append(t.getElapsed().get())
              .append("\n\t\t\ttstate:\t").append(t.getTstate().name()).append(" (").append(t.getTstate().tsName).append(")")
              .append("\n\t\t\tmessage:\t").append(t.getMessage())
              .append('.');

        sb.append("\n\tДатчики : ");
        if (sensors.isEmpty())
            sb.append("нет.");
        else
        for (Sensor s : sensors)
            sb.append("\n\t\t").append(s);

        sb.append("\n\tМастер : ").append(master)
          .append("\n\tСлэйв  : ").append(slave)
          .append("\n\tТипы ведомых УУ : ");
        if (slaveTypes == null || slaveTypes.isEmpty())
            sb.append("нет.");
        else
        for (DeviceTypes dt : slaveTypes)
            sb.append("\n\t\t").append(dt.name()).append(" (").append(dt.typeName).append(")");

        return sb.append("\n").toString();
    }
}
