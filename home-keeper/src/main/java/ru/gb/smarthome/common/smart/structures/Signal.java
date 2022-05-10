package ru.gb.smarthome.common.smart.structures;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import ru.gb.smarthome.common.smart.ISmartHandler;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.lang.String.format;

/** Используется для передачи инфорации между связанными УУ. */
public class Signal
{
/** Временная метка возникновения события, описываемого этим сигналом. */
    @Getter private LocalDateTime timeStamp;

/** УУ, на борту которого находится источнк сигнала. Этот параметр может служить для
     предотвращения бесконечных циклов передачи сигнала, если в результате связывания УУ
     образовалась замкнутая цепочка. */
    @Getter private ISmartHandler slaveHandler;

/**  */
    @Getter private UUID slaveUuid;

/** Источник сигнала. Этим источником должен быть какой-то узел УУ, например, датчик. */
    @Getter private UUID functionUuid;

    @Getter private Object data;


    public Signal (){}
    public Signal (ISmartHandler slave, UUID uuSlave, UUID uuFunction, Object dat)
    {
        timeStamp = LocalDateTime.now();
        slaveHandler = slave;
        slaveUuid    = uuSlave;
        functionUuid = uuFunction;
        data = dat;
    }

    public Signal setData (Object val) { data = val;  return this; }

    @Override public String toString () {
        return format ("signal[sm:%s (%s) (%s) | fn:%s | dat:%s]" //(%tH:%tM:%tS.%tL)
                       , slaveHandler.getDeviceFriendlyName()
                       , slaveUuid.toString()
                       , /*timeStamp, timeStamp, timeStamp, timeStamp,*/ timeStamp
                       , functionUuid
                       , data);
    }
}
