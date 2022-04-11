package ru.gb.smarthome.common.smart.enums;

/** Как бы «стандартизированные» типы УУ. */
public enum DeviceTypes
{
    SMART ("Умное устройство"), //< умолчальный тип
    HOME_KEEPER ("Умный дом"),
    METEO_STATION ("Метеостанция"),
    CLIMAT_STATION ("Климат-контроль"),
    FRIDGE ("Холодильник"),
    SEQURITY ("Охранная система"), //< умолчальный тип для УУ, относящихся к охранным системам
    SEQURITY_CAMERA ("Камера видеонаблюдения"),
    SEQURITY_SENSOR ("Датчик") //< контроллер сенсоров, или умный датчик (умных датчиков пока нет)
    ;
    public final String typeName;
    DeviceTypes (String tname) { typeName = tname; }
}
