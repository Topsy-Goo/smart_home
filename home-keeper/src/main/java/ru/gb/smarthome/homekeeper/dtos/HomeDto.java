package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.DEF_POLL_INTERVAL_FRONT;

@Data
public class HomeDto {
    private List<TypeGroupDto> groups = Collections.emptyList();
    private int    pollInterval = DEF_POLL_INTERVAL_FRONT;
    private String string;
    private String uuid;
    private String name;

    public HomeDto (){}
    public HomeDto (List<TypeGroupDto> tgroups)
    {
        int deviceCount = 0;
        if (tgroups != null)
        {
            groups = tgroups;
            for (TypeGroupDto group : groups)
                deviceCount += group.getDevices().size();
        }
        string = format ("Устройства сгруппированы по типу. Обнаружены %d устройств.", deviceCount);
    }
}
