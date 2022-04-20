package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.structures.DeviceInfo;

import static ru.gb.smarthome.common.FactoryCommon.*;

@Data
public class DeviceDto {

    public  static final StateDto     nullStateDto     = StateDto.deviceStateToDto (null);
    public  static final AbilitiesDto nullAbilitiesDto = AbilitiesDto.abilitiesToDto(null);

    private AbilitiesDto abilities = nullAbilitiesDto;
    private StateDto     state     = nullStateDto;
    private String  friendlyName    = DEF_DEV_DTO_FRIENDLYNAME;
    private boolean htmlPanelOpened = CLOSED;

    public DeviceDto (){}

    public static @NotNull DeviceDto smartDeviceToDto (DeviceInfo info)
    {
        DeviceDto dto = new DeviceDto();
        ISmartHandler device;
        String s;
        if (info != null && (device = info.device) != null)
        {
            dto.abilities = info.getAbilitiesDto();   //AbilitiesDto.abilitiesToDto (device.getAbilities())
            dto.state     = StateDto.deviceStateToDto (device.getState());
            if ((s = device.getDeviceFriendlyName()) != null)
                dto.friendlyName = s;
            dto.htmlPanelOpened = info.htmlPanelOpened == OPENED;
        }
        return dto;
    }
}
