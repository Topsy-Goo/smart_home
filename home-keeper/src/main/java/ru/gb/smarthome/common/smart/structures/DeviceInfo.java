package ru.gb.smarthome.common.smart.structures;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.homekeeper.dtos.AbilitiesDto;

import static ru.gb.smarthome.common.FactoryCommon.CLOSED;
import static ru.gb.smarthome.common.FactoryCommon.NOT_ACTIVE;
import static ru.gb.smarthome.homekeeper.dtos.DeviceDto.nullAbilitiesDto;

/** Структура с информацией об устройстве, которую нет смысла хранить ни на стороне клиента, ни в хэндлере.
и */
//@Data
public class DeviceInfo {
    public  final ISmartHandler device;
    @Getter
    private Abilities    abilities;
    @Getter
    private AbilitiesDto abilitiesDto/* = nullAbilitiesDto*/;
    //@Getter @Setter
    public  boolean      htmlPanelOpened = CLOSED;
    //@Getter @Setter private boolean active = NOT_ACTIVE;

    public DeviceInfo (ISmartHandler theDevice) {
        device = theDevice;
        if (device != null) {
            abilities = device.getAbilities();
            if (abilities != null)
                abilitiesDto = AbilitiesDto.abilitiesToDto (abilities);
            //active;
            //htmlPanelOpend = ;
            //...
        }
    }
    public Abilities getAbilities () {
        return (abilities != null)
                    ? abilities
                    : (abilities = device.getAbilities());
    }
    public AbilitiesDto getAbilitiesDto () {
        return (abilitiesDto != null)
                    ? abilitiesDto
                    : (abilitiesDto = AbilitiesDto.abilitiesToDto (getAbilities()));
    }
}
