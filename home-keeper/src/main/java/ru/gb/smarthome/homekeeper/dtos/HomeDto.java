package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

import java.util.List;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.DEF_POLL_INTERVAL_FRONT;

@Data
public class HomeDto
{
    private List<TypeGroupDto> groups = TypeGroupDto.nullGroups;
    private String string;
    private int    pollInterval = DEF_POLL_INTERVAL_FRONT;
    private String uuid;

    public HomeDto (){}

    public HomeDto (List<TypeGroupDto> readOnlyTgroups) {
        int deviceCount = 0;
        if (readOnlyTgroups != null)
        {
            groups = readOnlyTgroups;
            for (TypeGroupDto group : groups)
                deviceCount += group.getDevices().size();
        }
        string = format ("Устройства сгруппированы по типу. Обнаружены %d устройств.", deviceCount);
    }
}
