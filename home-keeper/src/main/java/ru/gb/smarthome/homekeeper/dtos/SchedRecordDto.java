package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import ru.gb.smarthome.homekeeper.entities.FriendlyName;
import ru.gb.smarthome.homekeeper.entities.SchedRecord;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.*;

@Data
public class SchedRecordDto {
    private Long    id;
    private String  deviceName;
    private String  uuid;
    private String  taskName;
    private String  dateTime;
    private Long    dateTimeLong;
    private boolean available;

    public SchedRecordDto (){}

    @Override public String toString () {
        return format ("SchedRecordDto:[%d | %s (%s) — %s | %s | %d | %b]" // | %s
                ,id
                ,deviceName
                ,uuid
                ,taskName
                ,dateTime
                ,dateTimeLong
                ,available);
    }

    public static SchedRecordDto dtoFromRecord (SchedRecord record, Function<UUID, Boolean> isAvalable)
    {
        SchedRecordDto dto = new SchedRecordDto();
        FriendlyName fName = record.getDeviceName();

        dto.id           = record.getId();
        dto.deviceName   = fName.getName();
        dto.uuid         = fName.getUuid();
        dto.taskName     = record.getTaskName();

        LocalDateTime ldt = record.getDateTime();
        dto.dateTime     = ldt.format (dtf);
        dto.dateTimeLong = longFromLdt (ldt);
        dto.available    = (dto.uuid != null) ? isAvalable.apply (uuidFromString (dto.uuid)) : false;
        return dto;
    }
}
