package ru.gb.smarthome.common.smart;

import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.DeviceState;

import java.net.Socket;
import java.util.Scanner;

import static ru.gb.smarthome.common.FactoryCommon.printf;
import static ru.gb.smarthome.common.FactoryCommon.println;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.*;

/** Используется классами, реализующими ISmartDevice и использующими консольный ввод. */
public interface IConsolReader extends ISmartDevice {

    boolean isDebugMode ();

    Socket getSocket ();

    Abilities getAbilities ();

    DeviceState getState();

/** Консольное управление Умным Устройством. (Для отладки.) */
    public static void runConsole (IConsolReader device)
    {
        if (device == null || !device.isDebugMode())
            return;

        Thread thread = Thread.currentThread();
        int len;
        String helpPrompt    = " Введите команду /? для печати справочника по командам.",
               notRecognized = "Команда не распознана [%s]. %s\n",
               msg, param1;

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

                param1 = (len > 1) ? parts[1] : null;
                if (param1 != null && param1.isBlank())
                    param1 = null;

                OperationCodes opCode = OperationCodes.byCKey (parts[0]);
                if (opCode == null)
                    printf (notRecognized, msg, helpPrompt);
                else
                switch (opCode)
                {
                //case CMD_SLEEP:
                //    break;
                case CMD_READY:
                    device.getState().setOpCode(CMD_READY)
                                     .setErrCode(null); //< пока считаем, что переход в этот режим сбрасывае ошибку (также см.case CMD_ERROR).
                case CMD_STATE:
                    println (device.getState().toString());
                    break;

                //case CMD_TASK:
                //    break;

                case CMD_BUSY:
                    if (!device.getState().getOpCode().greaterThan(CMD_BUSY))
                        device.getState().setOpCode(CMD_BUSY);  //< расчёт на то, что коды в интервале (BUSY; WAKEUP] являются командами, а не состояниями т.е. выполняются мгновенно.
                    println (device.getState().toString());
                    break;

                case CMD_ABILITIES: println (device.getAbilities().toString());
                    break;

                case CMD_ERROR:
                    device.getState().setOpCode(param1 != null ? CMD_ERROR : CMD_READY)
                                     .setErrCode(param1); //< отсутствие параметра сбрасывет ошибку (также см.case CMD_READY).
                    println (device.getState().toString());
                    break;

                case CMD_EXIT:
                    thread.interrupt();         //< это завершит поток консоли.
                    device.getSocket().close(); //< это закроет соединение клиента и завершит его поток.
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
        StringBuilder sb = new StringBuilder("Справка ещё не реализована. Вывожу все известные команды:\n");
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
        sb.append("Пож. примите к сведению, что некоторые из перечисленых команд могут не поддерживаться устройством.\n");

        if (counter > 0)
            println (sb.toString());
        else
            println ("\nНет команд, распознаваемых из консоли.");
    }


}
