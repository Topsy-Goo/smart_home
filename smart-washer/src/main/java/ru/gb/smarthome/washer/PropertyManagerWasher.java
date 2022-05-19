package ru.gb.smarthome.washer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.structures.Sensor;
import ru.gb.smarthome.common.smart.structures.Task;
import ru.gb.smarthome.empty.PropertyManagerEmpty;

import javax.annotation.PostConstruct;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.common.smart.enums.SensorStates.SST_ON;
import static ru.gb.smarthome.common.smart.enums.SensorTypes.SNT_LEAK;

@Component ("washer_propman")
@Scope ("singleton")
public class PropertyManagerWasher extends PropertyManagerEmpty
{
    private final String name = "Indesit IWUB 4085 (CIS)";
    private final UUID   uuid = UUID.fromString("8d6ead41-d76e-4bcf-8769-0434ce6a0998");

    @PostConstruct
    @Override public void init() {
        tasks.add (new Task ("Быстрая стирка",    AUTONOMIC, NON_INTERRUPTIBLE, 20, TimeUnit.SECONDS));
        tasks.add (new Task ("Стирка джинсов",    AUTONOMIC, NON_INTERRUPTIBLE, 25, TimeUnit.SECONDS));
        tasks.add (new Task ("Деликатная стирка", AUTONOMIC, NON_INTERRUPTIBLE, 30, TimeUnit.SECONDS));
        tasks.add (new Task ("Ночная стирка",     AUTONOMIC, NON_INTERRUPTIBLE, 45, TimeUnit.SECONDS));
        tasks.add (new Task ("Замачивание",       AUTONOMIC, NON_INTERRUPTIBLE, 60, TimeUnit.SECONDS));
        tasks.add (new Task ("Отжим",             AUTONOMIC, NON_INTERRUPTIBLE, 10, TimeUnit.SECONDS));
        tasks.add (new Task ("Полоскание",        AUTONOMIC, NON_INTERRUPTIBLE, 15, TimeUnit.SECONDS));
        sensors.add (new Sensor (SNT_LEAK, "Датчик протечки", SST_ON, BINDABLE, UUID.fromString("b29dbbae-dc6a-454e-9f5d-9aaeeec47801")));  //встроенный датчик протечки (пока некуда его прикрутить, но он есть)
    }

    @Override public UUID getUuid ()   { return uuid; }
    @Override public String getName () { return name; }
}
