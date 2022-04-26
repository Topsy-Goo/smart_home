package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.Task;

import java.util.List;
import java.util.stream.Collectors;

import static ru.gb.smarthome.common.FactoryCommon.*;

@Data
public class AbilitiesDto {

    private String deviceType   = DEF_ABILITIES_DTO_DEVICETYPE;
    private String vendorString = DEF_ABILITIES_DTO_VENDORSTRING;
    private String uuid         = DEF_ABILITIES_DTO_UUID;
    private boolean canSleep;
    private List<TaskDto> tasks = TaskDto.nullTasks;

    public  static final AbilitiesDto nullAbilitiesDto = AbilitiesDto.abilitiesToDto(null);

    public AbilitiesDto (){}

    public static @NotNull AbilitiesDto abilitiesToDto (Abilities a)
    {
        AbilitiesDto dto = new AbilitiesDto();
        List<Task> tlist;
        if (a != null) {
            dto.deviceType   = a.getType().typeName;
            dto.vendorString = a.getVendorString();
            dto.uuid         = a.getUuid().toString();
            dto.canSleep     = a.isCanSleep();
            if ((tlist = a.getTasks()) != null) {
                dto.tasks    = tlist.stream().map(TaskDto::taskToDto).collect(Collectors.toList());
            }
        }
        return dto;
    }
}
