package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.structures.DeviceInfo;

import java.util.List;
import java.util.function.Function;

import static ru.gb.smarthome.common.FactoryCommon.DEF_DEV_DTO_FRIENDLYNAME;

@Data
public class DeviceDto
{
    private AbilitiesDto  abilities;
    private StateDto      state;
    private String        friendlyName = DEF_DEV_DTO_FRIENDLYNAME;
    private List<UuidDto> slaveList;         //Здесь не заполняем, — будем запрашивать из фронта.
    private List<UuidDto> bindableFunctions; //Здесь не заполняем, — будем запрашивать из фронта.
    private List<BinateDto> contracts;

    public DeviceDto (){}

    public static @NotNull DeviceDto smartDeviceToDto (DeviceInfo info, Function<String, List<BinateDto>> getContractsDto)
    {
        DeviceDto dto = new DeviceDto();
        if (info != null)
        {
            dto.abilities    = info.abilitiesDto;
            dto.state        = StateDto.deviceStateToDto (info);
            dto.friendlyName = info.device.getDeviceFriendlyName();
            dto.contracts    = getContractsDto.apply (info.uuidstr);
            ;
        }
        return dto;
    }
}
