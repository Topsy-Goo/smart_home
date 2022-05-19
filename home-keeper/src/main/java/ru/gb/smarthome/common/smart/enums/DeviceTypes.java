package ru.gb.smarthome.common.smart.enums;

/** Как бы «стандартизированные» типы УУ. */
public enum DeviceTypes
{
    HOME_KEEPER     ("Умный дом", "Умные дома")
    ,SMART           ("Умное устройство", "Умные устройства") //< умолчальный тип
    ,FRIDGE          ("Холодильник", "Холодильники")
    ,WASHER          ("Стиральная машина", "Стиральные машины")
    //,SEQURITY        ("Охранная система", "Охранные системы") //< умолчальный тип для УУ, относящихся к охранным системам,
    ,SEQURITY_CAMERA ("Камера видеонаблюдения", "Камеры видеонаблюдения")
    ,SEQURITY_CONTROLLLER ("Контроллер", "Контроллеры")
    //,SEQURITY_SENSOR ("Датчик", "Датчики")  //< контроллер сенсоров, или умный датчик (умных датчиков пока нет),
    ,METEO_STATION   ("Метеостанция", "Метеостанции")
    //,CLIMAT_STATION  ("Климат-контроль", "Климат-контроли")
    ;

    /** Короткое описание типа УУ. */
    public final String typeName;

    /** Короткое описание типа УУ во множественном числе. */
    public final String typeNameMultiple;

    /** Количество элементов в OperationCodes. */
    public static final int length = values().length;

    DeviceTypes (String tname, String tfamily) { typeName = tname;  typeNameMultiple = tfamily; }
}
