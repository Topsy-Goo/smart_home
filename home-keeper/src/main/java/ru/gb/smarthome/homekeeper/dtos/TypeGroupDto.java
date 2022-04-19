package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.enums.DeviceTypes;

import java.util.*;
import java.util.stream.Collectors;

import static ru.gb.smarthome.common.FactoryCommon.DEF_TYPEGROUP_DTO_DEVICETYPE;

@Data
public class TypeGroupDto {

    @SuppressWarnings("unchecked")
    public  static final List<DeviceDto> nullDevices = Collections.EMPTY_LIST;
    private              String          typeName    = DEF_TYPEGROUP_DTO_DEVICETYPE;
    private              List<DeviceDto> devices     = nullDevices;

    public TypeGroupDto () {}

    public TypeGroupDto (String name, List<DeviceDto> devs) {
        if (name != null)  typeName = name;
        if (devs != null)  devices = devs;
    }

/** Составляем массив-dto-шку, описывающую все обнаруженые УУ и отсортированные по типам устройств
 (см. {@link DeviceTypes DeviceTypes}).
 @return TypeGroupDto это — список списков. Он содержащий DeviceDto, рассортированные по типам.
*/
    public static @NotNull List<TypeGroupDto> getTypeGroupDtos (Map<DeviceTypes, LinkedList<ISmartHandler>> mapHandlers)
    {
        List<TypeGroupDto> list = new ArrayList<> (DeviceTypes.length);
        for (DeviceTypes type : DeviceTypes.values())
        {
            LinkedList<ISmartHandler> typeHandlers = mapHandlers.get(type);
            if (typeHandlers.isEmpty())
                continue;
            List<DeviceDto> ldto = typeHandlers.stream().map(DeviceDto::smartDeviceToDto).collect (Collectors.toList());
            TypeGroupDto dto = new TypeGroupDto (type.typeName, ldto);
            list.add(dto);
        }
        return list;
    }
}
