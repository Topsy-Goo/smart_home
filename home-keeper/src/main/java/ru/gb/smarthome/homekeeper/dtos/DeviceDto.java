package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.ISmartHandler;

import static ru.gb.smarthome.common.FactoryCommon.*;

@Data
public class DeviceDto {

    public  static final StateDto     nullStateDto     = StateDto.deviceStateToDto (null);
    public  static final AbilitiesDto nullAbilitiesDto = AbilitiesDto.abilitiesToDto(null);
    private              AbilitiesDto abilities    = nullAbilitiesDto;
    private              StateDto     state        = nullStateDto;
    private              String       friendlyName = DEF_DEV_DTO_FRIENDLYNAME;

    public DeviceDto (){}

    public static @NotNull DeviceDto smartDeviceToDto (ISmartHandler device)
    {
        DeviceDto dto = new DeviceDto();
        String s;
        if (device != null) {
            dto.abilities = AbilitiesDto.abilitiesToDto (device.getAbilities());
            dto.state     = StateDto.deviceStateToDto (device.getState());
            if ((s = device.getDeviceFriendlyName()) != null)
                dto.friendlyName = s;
        }
        return dto;
    }
}
