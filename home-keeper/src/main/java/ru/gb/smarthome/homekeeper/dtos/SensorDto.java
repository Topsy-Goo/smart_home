package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.enums.SensorStates;
import ru.gb.smarthome.common.smart.structures.Sensor;

import static java.lang.String.format;

@Data
public class SensorDto {
    String  type;
    String  rawTypeName;
    boolean on;
    boolean alarm;
    String  name;
    boolean bindable;
    String  uuid;
    String  deviceUuid;

    public SensorDto (){}

    @Override public String toString () {
        return format ("[%s %s|%s %s]\n", rawTypeName, on?"O":"o", alarm?"A":"a", uuid);
    }

    public static @NotNull SensorDto sensorToDto (Sensor sensor)
    {
        SensorDto dto = new SensorDto();
        if (sensor != null) {
            dto.type     = sensor.getStype().sntName;
            dto.rawTypeName = sensor.getStype().name();
            dto.name     = sensor.getName();
            dto.uuid     = sensor.getUuid().toString();
            dto.bindable = sensor.isBindable();
            SensorStates sstate = sensor.getSstate();
            dto.on    = sstate.on;
            dto.alarm = sstate.alarm;
        }
        return dto;
    }

    public SensorDto setDeviceUuid (String val) { deviceUuid = val;  return this; }
}
