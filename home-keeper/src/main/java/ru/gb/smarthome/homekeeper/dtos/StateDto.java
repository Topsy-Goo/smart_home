package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.structures.DeviceState;
import ru.gb.smarthome.common.smart.structures.Task;

import static ru.gb.smarthome.common.FactoryCommon.*;

@Data
public class StateDto {
    private boolean active;
    private String  opCode = DEF_STATE_DTO_OPCODE;
    private String  errCode = DEF_STATE_DTO_ERRCODE;
    private TaskDto currentTask = nullTaskDto;

    public static final TaskDto nullTaskDto = TaskDto.taskToDto (null);

    public StateDto() {}

    public static @NotNull StateDto deviceStateToDto (DeviceState ds)
    {
        StateDto dto = new StateDto();
        String s;
        Task t;
        if (ds != null) {
            dto.active = ds.isActive()/* ? "Активно" : "Неактивно"*/;
            dto.opCode = ds.getOpCode().name();
            if ((s = ds.getErrCode()) != null)
                dto.errCode = s;
            if ((t = ds.getCurrentTask()) != null)
                dto.currentTask = TaskDto.taskToDto (t);
        }
        return dto;
    }
}
