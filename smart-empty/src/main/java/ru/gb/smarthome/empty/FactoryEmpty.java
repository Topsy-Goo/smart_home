package ru.gb.smarthome.empty;

import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.exceptions.OutOfServiceException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static ru.gb.smarthome.empty.EmptyApp.DEBUG;
import static ru.gb.smarthome.common.FactoryCommon.*;

final public class FactoryEmpty {

    public static PropertyManager getPropertyManager () {
        return PropertyManagerEmpty.getInstance();
    }


}
