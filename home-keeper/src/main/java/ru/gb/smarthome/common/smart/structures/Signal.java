package ru.gb.smarthome.common.smart.structures;

import lombok.Data;
import ru.gb.smarthome.common.smart.ISmartHandler;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.lang.String.format;

/** Используется для передачи инфорации между связанными УУ. */
@Data
public class Signal
{
    /** УУ, на борту которого находится источнк сигнала. Этот параметр может служить для
     предотвращения бесконечных циклов передачи сигнала, если в результате связывания УУ
     образовалась замкнутая цепочка. */
    ISmartHandler originHandler;

    /** Временная метка возникновения события, описываемого этим сигналом. */
    LocalDateTime timeStamp;

/** Источник сигнала. Этим источником должен быть какой-то узел УУ, например, датчик. */
    UUID source;

    /** Информация о событии или иная инфорация, которую ожидает принимающее УУ. */
    Object data;

    public Signal (){}
    public Signal (ISmartHandler orgnSmart, UUID src, Object dat)
    {
        timeStamp = LocalDateTime.now();
        originHandler = orgnSmart;
        source = src;
        data = dat;
    }

    @Override public String toString () {
        return format ("signal[sm:%s (%tH:%tM:%tS.%tL)(%s) | sr:%s | dt:%s]", originHandler,
                       timeStamp, timeStamp, timeStamp, timeStamp, timeStamp,
                       source,
                       (data != null) ? data.toString() : null);
    }
}
