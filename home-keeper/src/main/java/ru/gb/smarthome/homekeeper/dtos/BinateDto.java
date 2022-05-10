package ru.gb.smarthome.homekeeper.dtos;

import lombok.Data;

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
    public BinateDto (String tskName, String matName, String matUuid, String srcName, String srcUuid)
    {
        taskName = tskName;
        mateFriendlyName = matName;
        mateUuid         = matUuid;
        functionFriendlyName = srcName;
        functionUuid         = srcUuid;
    }
}
