package ru.gb.smarthome.fridge;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.empty.PropertyManagerEmpty;

@Component("fridge_propman")
@Scope ("singleton")
public class PropertyManagerFridge extends PropertyManagerEmpty {

/*    public PropertyManagerFridge () {
        //println("\nконструктор PropertyManagerFridge() : работа в потоке "+ Thread.currentThread().getName());
    }*/

    public void init() {
    }

    public void shutdown() {
    }
}
