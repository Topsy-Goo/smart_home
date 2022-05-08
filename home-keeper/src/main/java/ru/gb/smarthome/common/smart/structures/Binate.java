package ru.gb.smarthome.common.smart.structures;

import lombok.Getter;
import lombok.Setter;
import ru.gb.smarthome.common.smart.enums.BinatStates;
import ru.gb.smarthome.homekeeper.dtos.BinateDto;

import java.io.Serializable;
import java.util.UUID;

import static java.lang.String.format;

public class Binate implements Serializable
{
    BinatStates bstate;

/** Роль, которую УУ исполняет в контракте: MASTER или SLAVE. */
    boolean role;

/** УУ, с которым нужно работать в рамках контракта. */
    UUID    mate;

/** Источник сигнала. Этим источником должен быть какой-то узел УУ, например, датчик. */
    UUID    source;

/** Имя задачи. */
    String  taskName;

/** Dto-шка экземпляра. Её проще изготовить заранее и хранить, чем каждый раз мучаться с составленем
 по требованию. */
    @Getter @Setter BinateDto dto;

    public Binate () {} //< для сериализации
    public Binate (BinatStates state, boolean rol, UUID mat, UUID src, String tskName,
                   BinateDto binDto)
    {
        bstate = state;
        role = rol;
        mate = mat;
        source = src;
        taskName = tskName;
        dto = binDto;
    }

    public Binate setBstate (BinatStates val) { bstate   = val;  return this; }
    public Binate setRole     (boolean val)   { role     = val;  return this; }
    public Binate setMateUuid (UUID val)      { mate     = val;  return this; }
    public Binate setSource   (UUID val)      { source   = val;  return this; }
    public Binate setData     (String val)    { taskName = val;  return this; }

    public BinatStates bstate ()   { return bstate; }
    public boolean     role ()     { return role; }
    public UUID        mateUuid () { return mate; }
    public UUID        source ()   { return source; }
    public String      taskName () { return taskName; }

    @Override public String toString () {
        return format ("Binate[%s (%s) | %s | %s | %s | %s | %s]"
                ,bstate.name(), bstate.bsName
                ,role? "master":"slave"
                ,mate.toString()
                ,source.toString()
                ,taskName
                ,dto != null ? "dto" : null
                );
    }

    @Override public boolean equals (Object o)
    {
        if (this == o) return true;
        if (o instanceof Binate) {
            Binate other = (Binate)o;
            return role == other.role
                && mate.equals(other.mate)
                && source.equals(other.source)
                && taskName.equals(other.taskName);
                //dto не участвует в сравнении, т.к. у слэйва равен null.
        }
        return false;
    }
}
