package ru.gb.smarthome.homekeeper.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.gb.smarthome.homekeeper.entities.SchedRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IScheduleRepo extends CrudRepository<SchedRecord, Long>
{
    //SchedRecord findByDeviceName (String uuid); //device_uuid ->  DeviceUuid

/*    @Query (value = "DELETE FROM schedule_records "+
                           "WHERE device_uuid = :uuid AND "+
                                 "task_name = :taskname AND "+
                                 "date_time = :ldt ;",
            nativeQuery = true)
    Optional<SchedRecord> deleteRecord (@Param ("uuid") String uuid,
                                        @Param ("taskname") String taskName,
                                        @Param ("ldt") LocalDateTime dateTime);*/

/*    @Query (value = "UPDATE FROM schedule_records "+
                           "WHERE device_uuid = :uuid AND "+
                                 "task_name = :taskname AND "+
                                 "date_time = :ldt ;",
            nativeQuery = true)
    Optional<SchedRecord> deleteRecord (@Param ("uuid") String uuid,
                                        @Param ("taskname") String taskName,
                                        @Param ("ldt") LocalDateTime dateTime);*/

    //void updateById (Long id, SchedRecord record);
}
