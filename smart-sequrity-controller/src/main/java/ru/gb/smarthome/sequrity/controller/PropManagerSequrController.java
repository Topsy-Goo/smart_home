package ru.gb.smarthome.sequrity.controller;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.smart.structures.Sensor;
import ru.gb.smarthome.empty.complex.PropManagerComplex;

import javax.annotation.PostConstruct;
import java.util.UUID;

import static ru.gb.smarthome.common.FactoryCommon.BINDABLE;
import static ru.gb.smarthome.common.FactoryCommon.USE_DEF_SENSOR_NAME;
import static ru.gb.smarthome.common.smart.enums.SensorStates.SST_OFF;
import static ru.gb.smarthome.common.smart.enums.SensorTypes.*;

@Component ("seqctrl_propman")
@Scope ("singleton")
public class PropManagerSequrController extends PropManagerComplex
{
    private final String name = "Спайдер-М4";
    private final UUID   uuid = UUID.fromString ("f02d7de3-b738-4f16-bbed-3e5fe073f533");

    @PostConstruct
    @Override public void init() {
        sensors.add (new Sensor (SNT_OPENING, USE_DEF_SENSOR_NAME, SST_OFF, BINDABLE, UUID.fromString ("b413d6ab-6960-4289-bff0-8024fbac82da")));
        sensors.add (new Sensor (SNT_OPENING, USE_DEF_SENSOR_NAME, SST_OFF, BINDABLE, UUID.fromString ("b7c3bac4-2e86-41a9-bbf9-e9a25a5678bf")));
        sensors.add (new Sensor (SNT_MOVE,    USE_DEF_SENSOR_NAME, SST_OFF, BINDABLE, UUID.fromString ("bc1b11ef-ae32-48ec-9249-828832148fad")));
        sensors.add (new Sensor (SNT_FIRE,    USE_DEF_SENSOR_NAME, SST_OFF, BINDABLE, UUID.fromString ("bc852471-11e4-4ae4-98c2-4e48a859259f")));
    }

    @Override public UUID getUuid ()   { return uuid; }
    @Override public String getName () { return name; }
}
