package ru.gb.smarthome.common.smart.enums;

/** Как бы «стандартизированные» типы УУ. */
public enum DeviceTypes
{
    HOME_KEEPER ("Умный дом"),
    SMART ("Умное устройство"), //< умолчальный тип
    METEO_STATION ("Метеостанция"),
    CLIMAT_STATION ("Климат-контроль"),
    FRIDGE ("Холодильник"),
    SEQURITY ("Охранная система"), //< умолчальный тип для УУ, относящихся к охранным системам
    SEQURITY_CAMERA ("Камера видеонаблюдения"),
    SEQURITY_SENSOR ("Датчик") //< контроллер сенсоров, или умный датчик (умных датчиков пока нет)
    ;
    /** Короткое описание типа УУ. */
    public final String typeName;
    /** Количество элементов в OperationCodes. */
    public static final int length = values().length;

    DeviceTypes (String tname) { typeName = tname; }
}
