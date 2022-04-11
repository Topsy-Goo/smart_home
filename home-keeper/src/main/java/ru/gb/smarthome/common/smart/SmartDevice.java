package ru.gb.smarthome.common.smart;

import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.structures.Message;
import ru.gb.smarthome.common.smart.structures.Port;

import java.io.*;
import java.util.Scanner;
import java.util.UUID;

import static ru.gb.smarthome.common.FactoryCommon.println;

public abstract class SmartDevice implements ISmartDevice
{
    protected ObjectInputStream  ois;
    protected ObjectOutputStream oos;
    protected Thread threadRun;
    protected DeviceTypes deviceType;
    protected UUID        uuid;

    //protected SmartDevice () {}

//------------------------ Реализации интерфейсов ----------------------

    @Override
    public DeviceTypes deviceType () {
        return null;
    }

    @Override public UUID uuid () {  return uuid;  }

    @Override
    public DeviceStatus status () {
        return null;
    }

/*    @Override     < не нужно уже
    public void setPort (Port port) { }*/

    @Override
    public void setFriendlyName (String name) {

    }

    @Override
    public void activate (boolean on) {

    }

/*    @Override    public void turnOff () {    }*/

    @Override
    public void sleepSwitch (boolean sleep) {

    }

/*    @Override    public void wakeUp () {    }*/

    /** запрос на отключение устройства, которое, возможно, занято какой-то операцией. */
    @Override
    public boolean isItSafeTurnOff () {
        return false;
    }

    @Override
    public boolean canBeMaster () {
        return false;
    }

    @Override
    public boolean canBeSlave () {
        return false;
    }
//-------------------- Консольный ввод (для отладки) -------------------

/** Консольное управление Умным Устройством. (Для отладки.) */
    public static void runConsole (ISmartDevice device)
    {
        String helpPrompt    = " Введите команду /? для печати справочника по командам.",
               notRecognized = "Команда не распознана." + helpPrompt;
        String  msg;
        Thread thread = Thread.currentThread();

        println ("Запуск runConsoleReader()." + helpPrompt);
        try (Scanner scanner = new Scanner(System.in))
        {
            if (device == null) throw new IllegalArgumentException();

            while (!thread.isInterrupted())
            if (scanner.hasNext() && (msg = scanner.nextLine()) != null)
            {
                if (msg.equals("/?".trim())) {
                    showConsoleKeysHelp (device);
                    continue;
                }
                OperationCodes opCode = OperationCodes.byCKey (msg);
                if (opCode == null)
                    println (notRecognized);
                else
                switch (opCode)
                {
                //case CMD_SLEEP:
                //    break;
                //case CMD_WAKE:
                //    break;
                case CMD_UUID: println (device.uuid().toString());
                    break;
                case CMD_EXIT:
                    break;
                default: println (notRecognized);
                }
            }
        }
        catch (Exception e) { e.printStackTrace(); }
        finally {  println ("Выход из runConsoleReader().");  }
    }

/** Запрашиваем у УУ, какие команды оно поддерживает, и для поддерживаемых команд
выводим в консоль краткую справку. */
    private static void showConsoleKeysHelp (ISmartDevice device)
    {
        StringBuilder sb = new StringBuilder("Справка ещё не реализована. Вывожу все известные команды:\n");
        OperationCodes[] codes = OperationCodes.values();
        int counter = 0;

        for (OperationCodes oc : codes) {
            if (oc.ckey == null)
                continue;
            sb.append(oc.ckey).append("\t- ").append(oc.ckeyDescription).append("\n");
            counter++;
        }
        sb.append("Пож. примите к сведению, что некоторые из перечисленых команд могут не поддерживаться устройством.\n");
        if (counter > 0) println (sb.toString());
        else
        println ("\nНет команд, распознаваемых из консоли.");
    }
//----------------------------------------------------------------------

//----------------------------------------------------------------------
}
