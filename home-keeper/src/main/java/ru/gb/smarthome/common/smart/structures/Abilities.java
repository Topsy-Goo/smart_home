package ru.gb.smarthome.common.smart.structures;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.ISmartDevice;
import ru.gb.smarthome.common.smart.enums.DeviceTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.lang.String.format;

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
    @Getter private DeviceTypes type;
    /** Описание УУ, которым его снабдил производитель. */
    @Getter private String      vendorName;
    /** Уникальный ID УУ. По идее, он должен быть зашит в УУ, например, в ПЗУ. */
    @Getter private UUID        uuid;
    /** Флаг, указывающий, может ли УУ быть переключено в режим энергосбережения. */
    @Getter private boolean     canSleep;
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
    @Getter private List<Task> tasks;

    private Abilities () {} //< для сериализации

/**
@param dt Как бы стандартизированный тип устройства. Берём из {@link ru.gb.smarthome.common.smart.enums.DeviceTypes DeviceTypes}.
@param vname Название УУ, которым его как бы снабдил производтель. Например, «Atlant CR-4022f-GDL» (содержит название производителя, название модели и, возможно, что-то ещё.
@param uu UUID умного устройства (уникальный id экземпляра УУ, как бы зашитый в него производителем).
@param tt Список задач, которые УУ может выполнять. null или пустой список означает, что УУ не рассчитано на выполнение к-либо задач.
@param sleepable Принимает значения CAN_SLEEP и CANNOT_SLEEP и указывает, поддерживает ли УУ перевод его в режим энергосбережения (режим сна).
*/
    public Abilities (@NotNull DeviceTypes dt, @NotNull String vname,
                      @NotNull UUID uu,
                      List<Task> tt, boolean sleepable)
    {
        type = dt;
        vendorName = vname;
        uuid = uu;
        if (tt == null)
            tt = new ArrayList<>(0);
        tasks = Collections.unmodifiableList (tt); //< чтобы получать список по @Getter-у
        canSleep = sleepable;
    }

    public Abilities copy () { return new Abilities (type, vendorName, uuid, tasks, canSleep); }

    public String toString () { //< для отладки
        return format("Abilities:\n\t\tТип : %s\n\t\tНазвание : %s\n\t\tUUID : %s\n\t\tЭнергосбережение : %s\n\t\tЗадачи : %s.",
                      type.typeName, vendorName, uuid, canSleep ? "есть" : "нет", tasks);
    }
}
