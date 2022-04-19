package ru.gb.smarthome.common;

import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.enums.TaskStates;

import java.lang.reflect.Constructor;
import java.util.List;

import static ru.gb.smarthome.common.smart.enums.DeviceTypes.SMART;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.CMD_BUSY;
import static ru.gb.smarthome.common.smart.enums.TaskStates.TS_IDLE;

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

    public static final TaskStates DEF_TASK_STATE   = TS_IDLE;
    public static final DeviceTypes DEF_DEVICETYPES = SMART;
    public static final String DEF_TASK_NAME    = "";
    public static final String DEF_TASK_MESSAGE = "";
    public static final String DEF_STATE_DTO_ERRCODE = "";
    public static final String DEF_STATE_DTO_OPCODE  = "";
    public static final String DEF_DEV_DTO_FRIENDLYNAME = "";
    public static final String DEF_ABILITIES_DTO_DEVICETYPE = "";
    public static final String DEF_ABILITIES_DTO_VENDORNAME = "";
    public static final String DEF_ABILITIES_DTO_UUID       = "";
    public static final String DEF_TYPEGROUP_DTO_DEVICETYPE = "";
    //public static final String  = "";
    //public static final   = ;
    //public static final   = ;

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

/** Добавляем элемент в список, если он там отсутствует. */
    public static <T> void addIfAbsent (List<T> list, T t)
    {
        int index = list.indexOf(t);
        if (index < 0)
            list.add(t);
    }


}
