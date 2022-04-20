package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

@Data
public class HomeDto {

    @SuppressWarnings("unchecked")
    public  static final List<TypeGroupDto> nullGroups = Collections.EMPTY_LIST;
    private              List<TypeGroupDto> groups     = nullGroups;
    private              String string;

    public HomeDto (){}

    public HomeDto (List<TypeGroupDto> tgroups) {
        int /*typeNumber = 0,*/ deviceCount = 0;
        if (tgroups != null) {
            groups = tgroups;
            //typeNumber = tgroups.size();
            for (TypeGroupDto group : groups)
                deviceCount += group.getDevices().size();
        }
        string = format ("Устройства сгруппированы по типу. Обнаружены %d устройств.", deviceCount);
    }
}
