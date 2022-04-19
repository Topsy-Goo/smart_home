package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.Task;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ru.gb.smarthome.common.FactoryCommon.*;

@Data
public class AbilitiesDto {

    @SuppressWarnings("unchecked")
    public  static final List<TaskDto> nullTasks = Collections.EMPTY_LIST;
    private              String deviceType = DEF_ABILITIES_DTO_DEVICETYPE;
    private              String vendorName = DEF_ABILITIES_DTO_VENDORNAME;
    private              String uuid       = DEF_ABILITIES_DTO_UUID;
    private              boolean canSleep;
    private              List<TaskDto> tasks = nullTasks;


    public AbilitiesDto (){}

    public static @NotNull AbilitiesDto abilitiesToDto (Abilities a)
    {
        AbilitiesDto dto = new AbilitiesDto();
        List<Task> tlist;
        if (a != null) {
            dto.deviceType = a.getType().typeName;
            dto.vendorName = a.getVendorName();
            dto.uuid = a.getUuid().toString();
            dto.canSleep = a.isCanSleep();
            if ((tlist = a.getTasks()) != null) {
                dto.tasks = tlist.stream().map(TaskDto::taskToDto).collect(Collectors.toList());
            }
        }
        return dto;
    }
}
