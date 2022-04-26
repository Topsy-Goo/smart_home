package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.smart.structures.Task;

import java.util.Collections;
import java.util.List;

import static ru.gb.smarthome.common.FactoryCommon.*;

@Data
public class TaskDto
{
    private String  name = DEF_TASK_NAME;
    private String  tstate = DEF_TASK_STATE.tsName;
    private boolean autonomic;
    private boolean interruptible;
    private long    duration;
    private long    remained;
    private long    elapsed;
    private String  message = DEF_TASK_MESSAGE;

    @SuppressWarnings("unchecked")
    public  static final List<TaskDto> nullTasks = Collections.EMPTY_LIST;

    public TaskDto () {}

    public static @NotNull TaskDto taskToDto (Task t)
    {
        TaskDto dto = new TaskDto();
        String s;
        if (t != null)
        {
            if ((s = t.getName()) != null)
                dto.name = s;

            dto.tstate        = t.getTstate().get().tsName;
            dto.autonomic     = t.isAutonomic();
            dto.interruptible = t.isInterruptible();
            dto.duration      = t.getDuration();
            dto.remained      = t.getRemained().get();
            dto.elapsed       = t.getElapsed().get();

            if ((s = t.getMessage().get()) != null)
                dto.message = s;
        }
        return dto;
    }

    public static final TaskDto nullTaskDto = TaskDto.taskToDto (null);
}
