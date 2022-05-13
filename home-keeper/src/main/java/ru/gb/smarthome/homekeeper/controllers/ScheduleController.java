package ru.gb.smarthome.homekeeper.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.gb.smarthome.homekeeper.dtos.SchedRecordDto;
import ru.gb.smarthome.homekeeper.services.ScheduleService;

import java.util.List;

import static ru.gb.smarthome.common.FactoryCommon.lnprintf;
import static ru.gb.smarthome.common.FactoryCommon.lnprintln;

@RestController
@RequestMapping ("/v1/schedule")    //http://localhost:15550/home/v1/schedule
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    //http://localhost:15550/home/v1/schedule/new_schedule_record
/** Запрос на внесение одной записи в базу. */
    @PostMapping ("/new_schedule_record")
    public boolean addScheduleRecord (@RequestBody SchedRecordDto schedRecordDto)
    {
lnprintln ("addScheduleRecord() - получила: "+ schedRecordDto);
        return scheduleService.createOrUpdateScheduleRecord(schedRecordDto);
    }

    //http://localhost:15550/home/v1/schedule/schedule
/** Загрузка всех записей расписания. */
    @GetMapping ("/schedule")
    public List<SchedRecordDto> getSchedule ()
    {
        List<SchedRecordDto> list = scheduleService.getSchedule();
if (list != null) lnprintf ("/schedule - ответ: %s.\n", list);
        return list;
    }

    //http://localhost:15550/home/v1/schedule/schedule_delete_record
    @PostMapping ("/schedule_delete_record")
    public boolean deleteScheduleRecord (@RequestBody SchedRecordDto schedRecordDto)
    {
lnprintln ("deleteScheduleRecord() - получила: "+ schedRecordDto);
        return scheduleService.deleteScheduleRecord (schedRecordDto);
    }

}
