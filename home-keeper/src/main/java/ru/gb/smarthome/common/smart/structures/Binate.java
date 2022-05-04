package ru.gb.smarthome.common.smart.structures;

import ru.gb.smarthome.common.smart.enums.BinatStates;

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

/** Данные, тип и назначение которых должно быть очевидно из контекста. */
    Object  data;


    public Binate () {} //< для сериализации

    public Binate (BinatStates state, boolean rol, UUID mat, Object dat) {
        bstate = state;
        mate = mat;
        role = rol;
        data = dat;
    }
    public Binate setBstate (BinatStates val) { bstate = val;  return this; }
    public Binate setRole   (boolean val)     { role   = val;  return this; }
    public Binate setMate   (UUID val)        { mate   = val;  return this; }
    public Binate setData   (Object val)      { data   = val;  return this; }

    public BinatStates bstate () { return bstate; }
    public boolean role () { return role; }
    public UUID    mate () { return mate; }
    public Object  data () { return data; }

    @Override public String toString () {
        return format ("Binate[%s (%s) | %s | %s | %s]",
                bstate.name(), bstate.bsName,
                role? "master":"slave",
                mate.toString(),
                data.toString());
    }
}
