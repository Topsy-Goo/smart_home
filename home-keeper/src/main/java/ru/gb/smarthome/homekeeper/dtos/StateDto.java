package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.structures.DeviceState;
import ru.gb.smarthome.common.smart.structures.Task;

import java.util.List;

import static ru.gb.smarthome.common.FactoryCommon.DEF_STATE_DTO_ERRCODE;
import static ru.gb.smarthome.common.FactoryCommon.DEF_STATE_DTO_OPCODE;

@Data
public class StateDto
{
    private boolean active;
    private String  opCode = DEF_STATE_DTO_OPCODE;
    private String  errCode = DEF_STATE_DTO_ERRCODE;
    private TaskDto      currentTask = TaskDto.nullTaskDto;
    private List<String> lastNews;

    public  static final StateDto     nullStateDto     = StateDto.deviceStateToDto (null);

    public StateDto() {}

    public static @NotNull StateDto deviceStateToDto (ISmartHandler device)
    {
        StateDto dto = new StateDto();
        DeviceState ds;
        String s;
        Task t;
        if (device != null && (ds = device.getState()) != null)
        {
            dto.active = ds.isActive();
            dto.opCode = ds.getOpCode().name();

            if ((s = ds.getErrCode()) != null)
                dto.errCode = s;

            if ((t = ds.getCurrentTask()) != null)
                dto.currentTask = TaskDto.taskToDto (t);

            dto.lastNews  = device.getLastNews();
        }
        return dto;
    }
}
