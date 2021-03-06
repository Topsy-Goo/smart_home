package ru.gb.smarthome.common.smart.structures;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.enums.SensorStates;
import ru.gb.smarthome.common.smart.enums.SensorTypes;
import ru.gb.smarthome.homekeeper.dtos.SensorDto;

import java.io.Serializable;
import java.util.UUID;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.*;

public class Sensor implements Serializable {
/** Стандартный тип датчика.<br>Неизменяемый параметр. */
    @Getter private SensorTypes   stype;

    //private AtomicReference<SensorStates> state;
    private SensorStates sstate;

/** Изменяемое имя датчика. */
    @Getter private String name;

/** Указывает, может ли датчик использоваться связанным мастер-устройством.<br>Неизменяемый параметр. */
    @Getter private boolean bindable;

/** Неизменяемый параметр. */
    @Getter private UUID uuid;


    public Sensor (){} //< для сериализации
    public Sensor (@NotNull SensorTypes type, String nAme, SensorStates stat, boolean bindabl, UUID uu) {
        stype  = type;
        name   = isStringsValid (nAme) ? nAme : type.sntName;
        sstate = stat;
        bindable = bindabl;
        uuid     = uu;
    }
    public Sensor (SensorDto dto) {
        this (SensorTypes.valueOf (dto.getRawTypeName()),
              dto.getName(),
              SensorStates.get (dto.isOn(), dto.isAlarm()),
              dto.isBindable(),
              UUID.fromString (dto.getUuid()));
    }

    public Sensor copyOf (Sensor sen) {
        return new Sensor (sen.stype, sen.name, sen.sstate, sen.bindable, sen.uuid);
    }

    public Sensor setSstate (SensorStates val) { sstate = val; return this; }
    //public Sensor setUuid  (UUID val)         { uuid = val;  return this; }

    //public String       getName ()   { return  name; }
    public SensorStates getSstate () { return sstate; }

    @Override public String toString () {
        return format ("\n {%s (%s), %s, %s, %s, %s}"//, %s
                       , stype.sntName
                       , stype.name()
                       , name
                       , sstate.name()
                       ,bindable == BINDABLE ? "B":"b"
                       , uuid.toString()
                      );
    }

/** Сравниваются UUID (поле UUID uuid) и состояние (поле SensorStates state).
 Конечно есть и проверка на NULL. */
    @Override public boolean equals (Object o) {
        if (o == this) return true;         //Этот метод используем для сравнения запроса  с результатом;
        if (o instanceof Sensor) {          // если клиент выполнил запрос, то возвращённый им Sensor
            Sensor s = (Sensor) o;          // совпадёт с отправленным.
            return uuid.equals(s.uuid) && stype.equals(s.stype);
        }
        return false;
    }
}
