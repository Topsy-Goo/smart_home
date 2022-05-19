package ru.gb.smarthome.homekeeper.entities;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import ru.gb.smarthome.homekeeper.dtos.SchedRecordDto;

import javax.persistence.*;
import java.time.LocalDateTime;

import static java.lang.String.format;

@Entity
@Table (name="schedule_records")
@Data
public class SchedRecord {
    @Id
    @Column (name="id")
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn (name="device_uuid", nullable=false)
    private FriendlyName deviceName;

    @Column (name="task_name", nullable=false)
    private String  taskName;

    @Column (name="date_time", nullable=false)
    private LocalDateTime dateTime;

    @CreationTimestamp  @Column (name="created_at")
    private LocalDateTime createdAt;

    public SchedRecord (){}

    @Override public String toString () {
        return format ("\nSchedRecord:[%d | %s (%s) | %s | %s]"
                    ,id
                    ,deviceName.getName()
                    ,deviceName.getUuid()
                    ,taskName
                    ,dateTime);
    }

/** Инициализация создание и инициализация SchedRecord (по данным из SchedRecordDto), например,
 перед записью его в базу (поле id не инициализируется и остаётся == null).
 Выполняются проверки:<br>
 • {@code SchedRecordDto != null}<br>
 • {@code FriendlyName != null}<br>
 • {@code SchedRecordDto.dateTime} находится в будущем.<br>

@param dto основной источник данных для инициализации.
@param fName экземпляр FriendlyName, ранее извлечённый из БД по значению из {@code dto.uuid}.
 */
    public static SchedRecord schedRecordFromDto (SchedRecordDto dto, FriendlyName fName, LocalDateTime ldt)
    {
        if (dto == null || fName == null || ldt == null)
            return null;
        SchedRecord rec = new SchedRecord();
        rec.id          = dto.getId();
        rec.deviceName  = fName;
        rec.taskName    = dto.getTaskName();
        rec.dateTime    = ldt;
        return rec;
    }
}
