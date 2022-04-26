package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.structures.DeviceInfo;

import java.util.*;
import java.util.stream.Collectors;

import static ru.gb.smarthome.common.FactoryCommon.DEF_TYPEGROUP_DTO_DEVICETYPE;

@Data
public class TypeGroupDto {

    private String          typeName    = DEF_TYPEGROUP_DTO_DEVICETYPE;
    private List<DeviceDto> devices     = DeviceDto.nullDevices;

    @SuppressWarnings("unchecked")
    public  static final List<TypeGroupDto> nullGroups = Collections.EMPTY_LIST;

    public TypeGroupDto () {}

    public TypeGroupDto (String name, List<DeviceDto> devs) {
        if (name != null)  typeName = name;
        if (devs != null)  devices = devs;
    }

/** Составляем массив-dto-шку, описывающую все обнаруженые УУ и отсортированные по типам устройств
 (см. {@link DeviceTypes DeviceTypes}).
 @return TypeGroupDto это — список списков. Он содержащий DeviceDto, рассортированные по типам.
*/
    public static @NotNull List<TypeGroupDto> getTypeGroupDtos (
                                Map<DeviceTypes, LinkedList<ISmartHandler>> readOnlyMapHandlers,
                                Map<ISmartHandler, DeviceInfo> readOnlyHandlersInfo)
    {
        List<TypeGroupDto> list = new ArrayList<> (DeviceTypes.length);
        for (DeviceTypes type : DeviceTypes.values())
        {
            LinkedList<ISmartHandler> typeHandlers = readOnlyMapHandlers.get(type);
            if (typeHandlers.isEmpty())
                continue;

            List<DeviceDto> ldto = typeHandlers.stream()
                                    .map(h->DeviceDto.smartDeviceToDto (readOnlyHandlersInfo.get(h)))
                                    .collect (Collectors.toList());

            TypeGroupDto dto = new TypeGroupDto (type.typeNameMultiple, ldto);
            list.add(dto);
        }
        return list;
    }
}
