package ru.gb.smarthome.empty;

import ru.gb.smarthome.common.PropertyManager;

//import static ru.gb.smarthome.empty.EmptyApp.DEBUG;

final public class FactoryEmpty {

    public static PropertyManager getPropertyManager () {
        return PropertyManagerEmpty.getInstance();
    }


}
