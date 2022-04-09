package ru.gb.smarthome.common.smart.enums;

/** Как бы «стандартизированные» типы УУ. */
public enum DeviceTypes
{
    SMART { //< умолчальный тип
        public final String typeName = "Умное устройство";
    },
    HOME_KEEPER {
        public final String typeName = "Умный дом";
    },
    METEO_STATION {
        public final String typeName = "Метеостанция";
    },
    CLIMAT_STATION {
        public final String typeName = "Климат-контроль";
    },
    FRIDGE {
        public final String typeName = "Холодильник";
    },
    SEQURITY { //< умолчальный тип для УУ, относящихся к охранным системам
        public final String typeName = "Охранная система";
    },
    SEQURITY_CAMERA {
        public final String typeName = "Камера видеонаблюдения";
    },
    SEQURITY_SENSOR { //< контроллер сенсоров, или умный датчик (умных датчиков пока нет)
        public final String typeName = "Датчик";
    }
}
