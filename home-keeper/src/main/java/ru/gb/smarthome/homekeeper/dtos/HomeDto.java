package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class HomeDto {

    @SuppressWarnings("unchecked")
    public  static final List<TypeGroupDto> nullGroups = Collections.EMPTY_LIST;
    private              List<TypeGroupDto> groups     = nullGroups;

    public HomeDto (){}

    public HomeDto (List<TypeGroupDto> tgroups) {
        if (tgroups != null)  groups = tgroups;
    }
}
