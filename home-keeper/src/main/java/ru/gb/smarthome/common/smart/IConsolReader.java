package ru.gb.smarthome.common.smart;

import ru.gb.smarthome.common.exceptions.OutOfServiceException;
import ru.gb.smarthome.common.smart.enums.OperationCodes;

import java.util.Scanner;

import static ru.gb.smarthome.common.FactoryCommon.printf;
import static ru.gb.smarthome.common.FactoryCommon.println;

/** Используется классами, реализующими ISmartDevice и использующими консольный ввод. */
public interface IConsolReader extends ISmartDevice {

/** Выясняем, находится ли приложение в режиме отладки (IConsolReader запускается только в режиме отладки). */
    boolean crIsDebugMode ();

/** Выход из приложения. */
    void crExit () throws OutOfServiceException;

/** Вывести на экран структуру Abilities устройства. */
    void crAbilities ();

/** Вывести на экран структуру DeviceState устройства. */
    void crState ();

/** Включение/выключение в УУ режима CMD_ERROR. */
    void crEnterErrorState (String errCode);

/** Включение в УУ режима CMD_BUSY. */
    //void crEnterBusyState ();

/** Включение в УУ режима CMD_READY. */
    void crEnterReadyState ();

/** Выполнить указанную задачу. */
    void crExecuteTask (String taskname) throws InterruptedException;

/* * Перевести указанный сенсор в указанное состояние.
 @param senStrUuid строка-UUID датчика.
 @param strToState строка-требуемое состояние. */
    //void crSetSensorState (String senStrUuid, String strToState);


/** Консольное управление Умным Устройством. (Для отладки.) */
    public static void runConsole (IConsolReader device)
    {
        if (device == null || !device.crIsDebugMode())
            return;

        Thread thread = Thread.currentThread();
        int len;
        String helpPrompt    = " Введите команду /? для печати списка команд.",
               notRecognized = "Команда не распознана [%s]. %s\n",
               msg, param1, param2;

        println ("Запуск runConsoleReader()." + helpPrompt);
        try (Scanner scanner = new Scanner(System.in))
        {
            while (!thread.isInterrupted())
            if (scanner.hasNext() && (msg = scanner.nextLine()) != null)
            {
                if (msg.isBlank())
                    continue;

                if (msg.equals("/?".trim())) {
                    showConsoleKeysHelp (device);
                    continue;
                }

                String[] parts = msg.split("\\s");
                if ((len = parts.length) <= 0)
                    continue;

                OperationCodes opCode = OperationCodes.byCKey (parts[0]);
                param1 = (len > 1  &&  parts[1] != null  &&  !parts[1].isBlank()) ? parts[1] : null;
                param2 = (len > 2  &&  parts[2] != null  &&  !parts[2].isBlank()) ? parts[2] : null;

                if (opCode == null)
                    printf (notRecognized, msg, helpPrompt);
                else
                switch (opCode)
                {
                //case CMD_SLEEP:
                //    break;
                case CMD_READY: device.crEnterReadyState();
                    break;
                case CMD_STATE: device.crState();
                    break;
                case CMD_TASK:  device.crExecuteTask (param1);
                    break;
                //case CMD_BUSY:  device.crEnterBusyState();
                //    break;
                case CMD_ABILITIES: device.crAbilities();
                    break;
                case CMD_ERROR: device.crEnterErrorState(param1); //< отсутствие параметра сбрасывет ошибку (также см.case CMD_READY).
                    break;
                //case CMD_SENSOR:    device.crSetSensorState (param1, param2);
                //    break;
                case CMD_EXIT:
                    thread.interrupt();  //< это завершит поток консоли.
                    device.crExit();
                    break;
                default:
                    printf (notRecognized, msg, helpPrompt);
                }
            }//while
        }
        catch (Exception e) { e.printStackTrace(); }
        finally {  println ("Выход из runConsoleReader().");  }
    }

/** Запрашиваем у УУ, какие команды оно поддерживает, и для поддерживаемых команд
выводим в консоль краткую справку. */
    private static void showConsoleKeysHelp (IConsolReader device)
    {
        StringBuilder sb = new StringBuilder("Список команд:\n");
        OperationCodes[] codes = OperationCodes.values();
        int counter = 0;
        String usage;

        for (OperationCodes oc : codes) {
            if (oc.ckey == null)
                continue;
            sb.append(oc.ckey).append("\t- ").append(oc.ckeyDescription);

            if ((usage = oc.ckeyUsage) != null  &&  !usage.isBlank())
                sb.append("\t\tПример: ").append(usage);

            sb.append("\n");
            counter++;
        }
        sb.append("Некоторые из перечисленых команд могут не поддерживаться устройством.\n")
          .append("Команды предназначены для отладки; для них могут не выполняться необходимые проверки.\n");

        if (counter > 0)
            println (sb.toString());
        else
            println ("\nНет команд, распознаваемых из консоли.");
    }


}
