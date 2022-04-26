package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.structures.DeviceInfo;

import java.util.Collections;
import java.util.List;

import static ru.gb.smarthome.common.FactoryCommon.*;

@Data
public class DeviceDto
{
    private AbilitiesDto abilities = AbilitiesDto.nullAbilitiesDto;
    private StateDto     state     = StateDto.nullStateDto;
    private String   friendlyName  = DEF_DEV_DTO_FRIENDLYNAME;

    @SuppressWarnings("unchecked")
    public  static final List<DeviceDto> nullDevices = Collections.EMPTY_LIST;

    public DeviceDto (){}

    public static @NotNull DeviceDto smartDeviceToDto (DeviceInfo readOnlyInfo)
    {
        DeviceDto dto = new DeviceDto();
        ISmartHandler device;
        //String s;
        if (readOnlyInfo != null && (device = readOnlyInfo.device) != null)
        {
            dto.abilities = readOnlyInfo.getAbilitiesDto();
            dto.state     = StateDto.deviceStateToDto (device);
            dto.friendlyName = device.getDeviceFriendlyName();
        }
        return dto;
    }
}
