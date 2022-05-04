package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.enums.SensorStates;
import ru.gb.smarthome.common.smart.structures.DeviceInfo;
import ru.gb.smarthome.common.smart.structures.DeviceState;
import ru.gb.smarthome.common.smart.structures.Sensor;
import ru.gb.smarthome.common.smart.structures.Task;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.DEF_STATE_DTO_ERRCODE;
import static ru.gb.smarthome.common.FactoryCommon.DEF_STATE_DTO_OPCODE;

@Data
public class StateDto
{
    private boolean active;
    private String  opCode      = DEF_STATE_DTO_OPCODE.name();
    private String  errCode     = DEF_STATE_DTO_ERRCODE;
    private TaskDto currentTask = TaskDto.nullTaskDto; //TODO: удалить?
    private List<String> lastNews;
    private List<SensorDto> sensors;

    public StateDto() {}

    @Override public String toString () {
        StringBuilder sb = new StringBuilder("датчики: ");
        for (SensorDto v : sensors) {
            sb.append (v.isOn()?"ON_":"OFF_").append(v.isAlarm()?"ALARM":"WATCH").append(" • ");
        }
        return format ("%s %s %s\n", opCode, active?"A":"a", sensors);
    }

    public static @NotNull StateDto deviceStateToDto (DeviceInfo info) {
        Task t;
        DeviceState ds;
        ISmartHandler device;
        StateDto dto = new StateDto();
        String errCode;
        if (info != null && (device = info.device) != null)
        {
            ds = device.getState();
            dto.opCode = ds.getOpCode().name();
            dto.active = info.device.isActive();
            dto.lastNews  = device.getLastNews();

            if ((errCode = ds.getErrCode()) != null)
                dto.errCode = errCode;

            if ((t = ds.getCurrentTask()) != null)
                dto.currentTask = TaskDto.taskToDto (t);

        //Взяв за основу Abilities.sensors, составляем список SensorDto-шек, применяя к ним текущие состояния датчиков.
            List<Sensor> sensorList = info.abilities.getSensors();
            Map<UUID, SensorStates> sensorsMap = ds.getSensors(); //< текущие состояния

            if (!sensorList.isEmpty() && !sensorsMap.isEmpty()) {
                SensorStates sstate;
                SensorDto    sensordto;

                dto.sensors = new LinkedList<>();
                for (Sensor sen : sensorList)
                {
                    dto.sensors.add (sensordto = SensorDto.sensorToDto (sen).setDeviceUuid (info.uuidstr));
                    sstate = sensorsMap.get (sen.getUuid());
                    sensordto.on    = sstate.on;
                    sensordto.alarm = sstate.alarm;
        }   }   }
        return dto;
    }
}
/*  state.sensors   abilities.sensors
    -------------   -----------------
    uuid            uuid
    SensorStates    state
                    stype
                    name
                    bindable
*/