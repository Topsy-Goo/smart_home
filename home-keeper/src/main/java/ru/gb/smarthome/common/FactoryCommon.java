package ru.gb.smarthome.common;

import ru.gb.smarthome.common.smart.enums.DeviceTypes;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.enums.SensorStates;
import ru.gb.smarthome.common.smart.enums.TaskStates;
import ru.gb.smarthome.common.smart.structures.Sensor;
import ru.gb.smarthome.common.smart.structures.Signal;
import ru.gb.smarthome.common.smart.structures.Task;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static ru.gb.smarthome.common.smart.enums.DeviceTypes.SMART;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.CMD_BUSY;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.CMD_INVALID;
import static ru.gb.smarthome.common.smart.enums.TaskStates.TS_IDLE;
//import static ru.gb.smarthome.common.smart.enums.TaskStates.TS_NONE;

final public class FactoryCommon
{
    public static final int SMART_PORTS_COUNT  = 5;
    public static final int BUSY_SLEEP_SECONDS = 5;
    public static final int DEF_POLL_INTERVAL_MIN   = 500;
    public static final int DEF_POLL_INTERVAL_BACK  = 2000;
    public static final int DEF_POLL_INTERVAL_FRONT = 2000;
    public static final int    SERVER_PORT_DEFAULT    = 7777;
    public static final String SERVER_ADDRESS_DEFAULT = "localhost";
    public static final boolean ON        = true, OFF           = false;
    public static final boolean ACTIVE    = true, NOT_ACTIVE    = false;
    public static final boolean MASTER    = true, SLAVE         = false;
    public static final boolean CAN_SLEEP = true, CANNOT_SLEEP  = false;
    public static final boolean AUTONOMIC = true, NOT_AUTONOMIC = false;
    public static final boolean OK        = true, ERROR         = false;
    public static final boolean INTERRUPTIBLE = true, NON_INTERRUPTIBLE = false;
    public static final boolean FAIR  = true;
    //public static final boolean ALARM = true, WATCHING = false;
    //public static final boolean IF_INTERRUTIBLE = true, INTERRUT_ANYWAY = false;
    public static final boolean LIP = true, EVENT = false;
    public static final boolean BINDABLE = true;
    public static final boolean BIND = true, UNBIND = false;

    public static final OperationCodes DEF_STATE_DTO_OPCODE  = CMD_INVALID;
    public static final DeviceTypes DEF_DEVICETYPE = SMART;
    public static final TaskStates  DEF_TASK_STATE   = TS_IDLE; /*TS_NONE*/
    public static final String      DEF_TASK_MESSAGE = DEF_TASK_STATE.tsName;
    public static final String      DEF_TASK_NAME    = "Нет задач";
    public static final String DEF_STATE_DTO_ERRCODE = "";
    //public static final String[] DEF_STATE_DTO_NEWS    = {};
    public static final String DEF_DEV_DTO_FRIENDLYNAME = "";
    public static final String DEF_ABILITIES_DTO_DEVICETYPE   = "";
    public static final String DEF_ABILITIES_DTO_VENDORSTRING = "";
    public static final String DEF_ABILITIES_DTO_UUID         = "";
    //public static final String DEF_TYPEGROUP_DTO_DEVICETYPE = "";
    //public static final String FORMAT_LAUNCHING_TASK_ = "Задача «%s» запускается.";
    public static final String FORMAT_CANNOT_LAUNCH_TASK_ = "Не удалось запустить задачу: «%s».";
    public static final String FORMAT_ACTIVATE_DEVICE_FIRST_ = "Устройство «%s» неактивно.\rАктивизируйте его и повторите попытку.";
    public static final String FORMAT_REQUEST_ERROR = "Устройство %s\rне смогло обработать запрос.";
    public static final String SEQURCAMERA_TASKNAME_STREAMING = "Трансляция";
    public static final String SEQURCAMERA_TASKNAME_VRECORDER = "Видеозапись";
    public static final String FRIDGE_TASKNAME_DEFROST = "Разморозка";
    public static final String WASHER_TASKNAME_QUICK = "Быстрая стирка";
    public static final String WASHER_TASKNAME_DENIM = "Стирка джинсов";
    public static final String WASHER_TASKNAME_LIGHT = "Деликатная стирка";
    public static final String WASHER_TASKNAME_HIGHT = "Ночная стирка";
    public static final String WASHER_TASKNAME_SOAK  = "Замачивание";
    public static final String WASHER_TASKNAME_SPIN  = "Отжим";
    public static final String WASHER_TASKNAME_RINSE = "Полоскание";

    public static final String
        promptActivationDuringErrorState = "Активация неисправного устройства невозможна.",
        promptDeactivationIsNotSafeNow = "Деактивировать устройство сейчас нельзя!",
        promptCannotChangeActivityState = "Не удалось изменить активность устроуства.";

    public static final Locale RU_LOCALE = new Locale ("ru", "RU");
    public static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern ("yyyy-MM-dd | HH:mm:ss", RU_LOCALE);
    public static final long SCHEDULER_POLL_INTERVAL_MINUTES = 1L;
    public static final long SCHEDULER_EXTRA_WAIT_MINUTES = 2L;

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
    public static void lnprint (String s) { System.out.println("\n"+ s); }
    public static void lnprintln (String s) { System.out.println("\n"+ s+ "\n"); }
    public static void print (String s) { System.out.print(s); }
    public static void printf (String s, Object... args) { System.out.printf(s, args); }
    public static void lnprintf (String s, Object... args) { System.out.println(); System.out.printf(s, args); }

    public static void errprintln (String s) { System.err.println(s); }
    public static void errprint (String s) { System.err.print(s); }
    public static void errprintf (String s, Object... args) { System.err.printf(s, args); }
    public static void lnerrprintln (String s) { System.err.println("\n"+ s+ "\n"); }


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

/** Добавляем элемент в список, если он там отсутствует.
 @param list список.
 @param t элемент, который нужно добавить в список list.
 @return TRUE, если элемент t добавлен в список list. */
    public static <T> boolean addIfAbsent (List<T> list, T t)
    {
        int index = list.indexOf(t);
        if (index < 0)
            return list.add(t);
        return false;
    }

/*    public static <K,V> void addIfAbsent (Map<K, LinkedList<V>> map, K k, V v)
    {
        LinkedList<V> list = map.get(k);
        if (list == null)
            map.put(k, list = new LinkedList<V>());
        map.get(k).add(v);
    }*/

/* * Убеждаемся, что объект является UUID.
 @param o исследуемый объект.
 @return Объект o, преобразованный к типу UUID, или NULL, если о не является объектом UUID.
    public static UUID uuidFromObject (Object o) {
        if (o instanceof UUID)
            return (UUID) o;
        return null;
    } */

/** Убеждаемся, что объект является Signal.
 @param o исследуемый объект.
 @return Объект o, преобразованный к типу Signal, или NULL, если о не является объектом Signal. */
    public static Signal signalFromObject (Object o) {
        if (o instanceof Signal)
            return (Signal) o;
        return null;
    }

/** Убеждаемся, что объект является строкой, и возвращаем его же, но с правильным типом.
 @param o исследуемый объект.
 @return Объект o, преобразованный к типу String, или NULL, если о не является объектом String. */
    public static String stringFromObject (Object o) {
        if (o instanceof String)
            return (String) o;
        return null;
    }

/** Убеждаемся, что объект является задачей, и возвращаем его же, но с правильным типом.
 @param o исследуемый объект.
 @return Объект o, преобразованный к типу Task, или NULL, если о не является объектом Task. */
    public static Task taskFromObject (Object o) {
        if (o instanceof Task)
            return (Task) o;
        return null;
    }

/** Убеждаемся, что объект является сенсором, и возвращаем его же, но с правильным типом.
 @param o исследуемый объект.
 @return Объект o, преобразованный к типу Sensor, или NULL, если о не является объектом Sensor. */
    public static Sensor sensorFromObject (Object o) {
        if (o instanceof Sensor)
            return (Sensor) o;
        return null;
    }

/*    public static SensorStates sensorStateFromString (String str) {
        try {
            return SensorStates.valueOf (str);
        }
        catch (IllegalArgumentException e) {
            printf ("В SensorStates нет константы: «%s».", str);
            return null;
        }
    }*/

    public static UUID uuidFromString (String strUuid) {
        try {
            return UUID.fromString (strUuid);
        }
        catch (IllegalArgumentException e) {
            printf ("Строка «%s» не может быть преобразована к UUID.", strUuid);
            return null;
        }
    }

    public static LocalDateTime ldtFromLong (Long l)
    {
        Instant instant = Instant.ofEpochMilli (l);
        ZoneId  zoneId  = ZoneId.systemDefault();
        return LocalDateTime.ofInstant (instant, zoneId);
    }

    public static long longFromLdt (LocalDateTime ldt)
    {
        ZonedDateTime zdt1 = ZonedDateTime.of (ldt, ZoneId.systemDefault());
        return zdt1.toInstant().toEpochMilli();
    }
}
