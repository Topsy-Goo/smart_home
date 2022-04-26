package ru.gb.smarthome.common.smart.structures;

import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.homekeeper.dtos.AbilitiesDto;

/** Структура с информацией об устройстве, которую нет смысла хранить ни на стороне клиента, ни в хэндлере.
и */
public class DeviceInfo {
    public  final ISmartHandler device;
    private Abilities    abilities;
    private AbilitiesDto abilitiesDto;

    public DeviceInfo (ISmartHandler theDevice) {
        device = theDevice;
        if (device != null) {
            abilities = device.getAbilities();
            if (abilities != null)
                abilitiesDto = AbilitiesDto.abilitiesToDto (abilities);
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
