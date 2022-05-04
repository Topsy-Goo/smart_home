package ru.gb.smarthome.common.smart.enums;

import static ru.gb.smarthome.common.FactoryCommon.*;

public enum SensorTypes {

     SNT_OPENING    ( EVENT, "Датчик открывания")
    ,SNT_CLOSING    ( EVENT, "Датчик закрывания")
    ,SNT_TRANSITION ( EVENT, "Датчик смены состояния")
    ,SNT_LEAK       ( EVENT, "Датчик протечки")
    ,SNT_MOVE       (   LIP, "Датчик движения")
    ,SNT_FIRE       (   LIP, "Датчик пожарный") //На дым и тепло
    ,SNT_SMOKE      (   LIP, "Датчик задымления") //только на дым
    ,SNT_SHOCK      (   LIP, "Датчик удара")
    ,SNT_LIGHT      (   LIP, "Датчик освещения")
    ,SNT_NOISE      (   LIP, "Датчик звуковой")
    ;
/** Количество элементов в SensorTypes. */
    public static final int length = values().length;

/** Короткое описание состояния задачи. */
    public final String  sntName;

/** LIP — срабатывание на пороговом уровне измеряемого параметра.<br>
    EVENT — срабатывание при наступлении ожидаемого события. */
    public final boolean lipSensor;

    SensorTypes (boolean levelSen, String sntN) {
        lipSensor = levelSen;
        sntName = sntN;
    }
}
