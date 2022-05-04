package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

import java.util.List;

@Data
public class TypeGroupDto {
    private String          typeName/* = DEF_TYPEGROUP_DTO_DEVICETYPE*/;
    private List<DeviceDto> devices/*  = DeviceDto.nullDevices*/;

    public TypeGroupDto () {}

    public TypeGroupDto (String name, List<DeviceDto> devs) {
        if (name != null)  typeName = name;
        if (devs != null)  devices = devs;
    }
}
