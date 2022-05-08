package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;
import ru.gb.smarthome.common.smart.structures.Binate;

import java.util.UUID;

/** BinateDto делаем только для ведущего УУ (т.е. для мастера). */
@Data
public class BinateDto
{
/** Имя ф-ции ведущего УУ, которая выполняется по контракту. */
    String taskName;

/** mateFriendlyName — Копия deviceFriendlyName ведомого УУ — второго участника контракта.<br><br>
 mateUuid — UUID ведомого УУ. */
    String mateFriendlyName,
           mateUuid;

/** functionFriendlyName — Имя ф-ции ведомого УУ (имя датчика) — ф-ции, на срабатывание которой составлен
 контракт между ведущим и ведомым устройствами.<br><br>
 functionUuid — UUID упомянутой ф-ции. */
    String functionFriendlyName,
           functionUuid;

    public BinateDto (){}
    public BinateDto (String tskName, String matName, String srcName, UUID matUuid, UUID srcUuid)
    {
        taskName = tskName;
        mateFriendlyName = matName;
        mateUuid         = matUuid.toString();
        functionFriendlyName = srcName;
        functionUuid         = srcUuid.toString();
    }

/** Извлекаем из Binate заранее заготовленную dto-шку. Этот фокус пройдёт только
 с мастер-контрактом, — у слэйв-контракта поле dto == null. */
    public static BinateDto binateToDto (Binate bin) {
        if (bin != null)
            return bin.getDto();
        return null;
    }
}
