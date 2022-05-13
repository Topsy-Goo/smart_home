package ru.gb.smarthome.homekeeper.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gb.smarthome.homekeeper.dtos.SchedRecordDto;
import ru.gb.smarthome.homekeeper.entities.FriendlyName;
import ru.gb.smarthome.homekeeper.entities.SchedRecord;
import ru.gb.smarthome.homekeeper.repos.IScheduleRepo;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static ru.gb.smarthome.common.FactoryCommon.ldtFromLong;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final HomeService   homeService;
    private final IScheduleRepo scheduleRepo;

/** Добавляем или изменяем запись в расписании по требованию из фронта. */
    @Transactional
    public boolean createOrUpdateScheduleRecord (SchedRecordDto dto)
    {
        FriendlyName fName;
        SchedRecord recNew;
        String errStr = null;
        try {
            if (dto != null
            &&  (fName = homeService.findFriendlyNameByUuid (dto.getUuid())) != null
            &&  homeService.isTaskName (UUID.fromString (dto.getUuid()), dto.getTaskName()))
            {
                LocalDateTime ldt = ldtFromLong(dto.getDateTimeLong());
                if (!ldt.isAfter (LocalDateTime.now()))
                    errStr = "Время запуска должно находиться в будущем.";
                else
                if ((recNew = SchedRecord.schedRecordFromDto (dto, fName, ldt)) != null)
                {
                    Long id = recNew.getId();
                    if (id == null) {
                        scheduleRepo.save (recNew);
                        return true;
                    }
                    else {
                        SchedRecord recOld = scheduleRepo.findById (id).orElse(null);
                        if (recOld != null)
                        {
                            //recOld.updateFrom (recNew); <<< Пока обновляем только дату, нет нужды в отдельном методе.
                                recOld.setDateTime (recNew.getDateTime());
                            scheduleRepo.save (recOld);
                            return true;
                        }
                        else errStr = "Запись не найдена.";
                    }
                }
                else errStr = "Ошибка хранилища.";
            }
            else errStr = "Переданы некорректные данные, или отключено соответствующее устройство.";
        }
        catch (Exception e) {
            e.printStackTrace();
            errStr = e.getMessage();
        }
        if (errStr != null)
            homeService.addNews ("Ошибка! Не удалось добавить/изменить запись в расписании.\rПричина:\r" + errStr);
        return false;
    }

    @Transactional
    public List<SchedRecordDto> getSchedule ()
    {
        List<SchedRecordDto> list = new LinkedList<>();
        scheduleRepo.findAll().forEach (rec->list.add (SchedRecordDto.dtoFromRecord (rec, homeService::isAvalable)));
        return list;
    }

/** По требованию фронта удаляем из БД запись, которая сопадёт с парметром dto. */
    @Transactional
    public boolean deleteScheduleRecord (SchedRecordDto dto)
    {
        FriendlyName fName;
        SchedRecord rec;
        String errStr = null;
        try {
            if (dto != null
            &&  (fName = homeService.findFriendlyNameByUuid (dto.getUuid())) != null/*
            &&  homeService.isTaskName (UUID.fromString (dto.getUuid()), dto.getTaskName())*/)
            //homeService.isTaskName(…) не вызываем потому, что он вернёт false, если соотв.УУ не подключено.
            {
                LocalDateTime ldt = ldtFromLong(dto.getDateTimeLong());
                if ((rec = SchedRecord.schedRecordFromDto (dto, fName, ldt)) != null)
                {
                    long count1 = scheduleRepo.count();
                    scheduleRepo.delete(rec);
                    //scheduleRepo.deleteRecord (dto.getUuid(), dto.getTaskName(), ldt);
                    long count2 = scheduleRepo.count();
                    return count1 > count2;
                }
                else errStr = "Запись не найдена.";
            }
            else errStr = "Переданы некорректные данные.";
        }
        catch (Exception e) {
            e.printStackTrace();
            errStr = e.getMessage();
        }
        if (errStr != null)
            homeService.addNews ("Ошибка! Не удалось удалить запись из расписания.\rПричина:\r" + errStr);
        return false;
    }
}
