package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

import java.util.List;

@Data
public class TypeGroupDto {
    private String          typeName;
    private List<DeviceDto> devices;

    public TypeGroupDto () {}

    public TypeGroupDto (String name, List<DeviceDto> devs) {
        if (name != null)  typeName = name;
        if (devs != null)  devices = devs;
    }
}
