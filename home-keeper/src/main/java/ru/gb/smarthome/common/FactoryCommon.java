package ru.gb.smarthome.common;

import ru.gb.smarthome.common.smart.enums.OperationCodes;

import java.lang.reflect.Constructor;

import static ru.gb.smarthome.common.smart.enums.OperationCodes.CMD_BUSY;

final public class FactoryCommon
{
    public static final int SMART_PORTS_COUNT  = 2;
    public static final int BUSY_SLEEP_SECONDS = 5;
    public static final int    SERVER_PORT_DEFAULT    = 7777;
    public static final String SERVER_ADDRESS_DEFAULT = "localhost";
    public static final boolean ON        = true, OFF           = false;
    public static final boolean ACTIVE    = true, NOT_ACTIVE    = false;
    public static final boolean CAN_SLEEP = true, CANNOT_SLEEP  = false;
    public static final boolean AUTONOMIC = true, NOT_AUTONOMIC = false;
    public static final boolean OK        = true, ERROR         = false;
    public static final boolean INTERRUPTIBLE = true, NON_INTERRUPTIBLE = false;

    public static final OperationCodes OPCODE_INITIAL = CMD_BUSY; //< состояние, в котором оказывается УУ при запуске его модуля.

/** Проверяет строку на пригодность.
@return true, если ни одна из строк не равна null, не является пустой и не состоит только из пробельных сиволов. */
    public static boolean isStringsValid (String... lines) {
        boolean result = lines != null;
        if (result) {
            for (String s : lines) {
                if (s == null || s.isBlank()) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    public static void println (String s) { System.out.println(s); }
    public static void print (String s) { System.out.print(s); }
    public static void printf (String s, Object... args) { System.out.printf(s, args); }

    public static void errprintln (String s) { System.err.println(s); }
    public static void errprint (String s) { System.err.print(s); }
    public static void errprintf (String s, Object... args) { System.err.printf(s, args); }

/** Проверка условия с выбрасыванием указанного исключения. Вынесена в отдельный метод, чтобы не загромождать
код проверочными конструкциями.
@param condition условие, проверка выполнения которого выполняется.
@param exclass класс исключения, которое нужно бросить при невыполнении условия condition.
*/
    public static void check (boolean condition,
                              Class<? extends Exception> exclass)
                       throws Exception
    {
        check (condition, exclass, "Не выполнено необходимое условие.");
    }

/** Проверка условия с выбрасыванием указанного исключения. Вынесена в отдельный метод, чтобы не загромождать
код проверочными конструкциями.
@param condition условие, проверка выполнения которого выполняется.
@param exclass класс исключения, которое нужно бросить при невыполнении условия condition.
@param msg строка сообщения.
*/
    public static void check (boolean condition,
                              Class<? extends Exception> exclass,
                              String msg)
                       throws Exception
    {
        if (!condition) {
            Constructor<? extends Exception> constructor = exclass.getConstructor(String.class);
            throw constructor.newInstance(msg);
        }
    }
}
