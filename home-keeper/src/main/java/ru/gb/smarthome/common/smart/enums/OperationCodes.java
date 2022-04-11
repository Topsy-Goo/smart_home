package ru.gb.smarthome.common.smart.enums;

import ru.gb.smarthome.common.smart.structures.Task;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

import static ru.gb.smarthome.common.FactoryCommon.ON;
import static ru.gb.smarthome.common.FactoryCommon.OFF;

/** Коды команд, операций и состояний (режимов), которые могут использоваться в приложении.
Смысл кода — является ли он кодом команды, кодом состояния или кодом операции — зависит от контекста:
например, УД может дать команду TASK N, чтобы застаить УУ выполнить задачу N; после чего УУ приступит
к выполнению задачи и на команду STATE (запрос состояния УУ) будет отвечать "TASK N".
<p>
    Коды могут сопровождаться дополнительными данными. Например, УД, отдавая команду TASK, должен
указать, какую именно задачу нужно выполнить, а УУ, информируя о выполныемой задаче, сообщает тип
задачи.
<p>
    Коды выстроены в порядке возрастания приоритета. Код с меньшим приоритетом не может начать
обрабатываться до того как будет закончена обработка кода с большим приоритетом. Например, невозможен
перевод УУ в режим сна, если УУ занято выполнением задачи (УУ находится в состоянии TASK),
но при этом УУ обработает запросы STATUS или UUID, т.к. они имеют более высокий приоритет по
отношению к TASK. Или, например, УУ не может реагировать на команды УД, пока оно находится в
состоянии ERROR.
<p>    Некоторые коды предназначены только для использования из консоли.   */
public enum OperationCodes implements Serializable {

/** Команда: отправить УУ в сон или вернуть ото сна + поле onOff.<br>
    Состояние: CMD_SLEEPSWITSH + поле onOff. */
    CMD_SLEEPSWITSH ("/ssw", "Отправить УУ в сон или вернуть ото сна.", "/ssw sleep или /ssw1 wake"),

/** Команда: приказ УУ обрабатывать сигналы от УУ с указаным UUID + поле uuid. */
    CMD_PAIR  ("/pair", "Приказ УУ обрабатывать сигналы от УУ с указаным UUID.", "/pair UUID"), //< оба УУ должны понимать, что от них хотят (пока в разработке).

/** Команда: приказ УУ выполнять задачу + поле task.<br>
    Состояние: CMD_TASK + поле task. */
    CMD_TASK  ("/task", "Приказ УУ выполнять задачу CODE в течение N сек.", "/task CODE 000"),

/** Состояние: УУ занято.<br> */
    CMD_BUSY  ("/busy", "Вкл./выкл. сотсояние «занято» в УУ", "/busy "),

/** Команда: запрос UUID у УУ.<br>
    Ответ: CMD_UUID + поле uuid. */
    CMD_UUID  ("/uuid", "Показать UUID устройства.", null),

/** Команда: запрос стандартного типа у УУ.<br>
    Ответ: CMD_TYPE + поле deviceType. */
    CMD_TYPE  ("/type", "Показать тип устройства.", null),

/** Команда: запрос состояния.<br>
    Ответ: текущее состояние + уточняющи параметр, если состоянию полагается иметь параметр. */
    CMD_STATUS ("/stat", "Запрос статуса УУ.", null),

/** Команда: информирование УУ об успешном подключении к УД. */
    CMD_CONNECTED,

/** Команда: информирование УУ о невозможности подключить его к УД ввиду отсутствия свободных портов. */
    CMD_NOPORTS,

/** Состояние: УУ находится в неисправном состоянии + код ошибки. */
    CMD_ERROR  ("/err", "Вкл./выкл. состояния ошибки в УУ.", "/err CODE (установить) или /err (сбросить)"),

/** Должно использоваться только из консоли */
    CMD_EXIT   ("/exit", "Завершить работу модуля УУ.", null)  //< УУ программно недоступно до его перезапуска.
    ;

/** {@code ckey} — ключ для управления УУ из консоли;<br>
    {@code ckeyDescription} — краткое описание ключа;<br>
    {@code ckeyUsage} — пример использования ключа. */
    public final String ckey, ckeyDescription, ckeyUsage; //< консольная команда, её краткое описание и использование.
    private static final HashMap<String, OperationCodes> mapNames; //< для быстрого поиска кодов операций по соотв.им именам (name).
    private static final HashMap<String, OperationCodes> mapConKeys; //< для быстрого сопоставления консольных команд с соотв.им кодами операций.

//параметры команд:
    private boolean onOff; //<  ON (сон) или OFF (не сон)
    private UUID uuid;
    private DeviceTypes deviceType;
    private Task task;
    private String errCode; //< код ошибки, специфичный для УУ

//------------------------------- Конструирование: -----------------------------
    static {
        OperationCodes[] arr = values();

        mapNames = new HashMap<>(arr.length + 1, 1.0F); //< +1, т.к. loadFactor указывает, сколько % должно быть заполнено, чтобы set увеличился. С +1 у нас не будет 100%-го заполнения.
        for (OperationCodes oc : arr) {
            mapNames.put (oc.name(), oc);
        }
        mapConKeys = new HashMap<>(arr.length + 1, 1.0F);
        for (OperationCodes oc : arr) {
            mapConKeys.put (oc.ckey, oc); //< т.к. у нас тут всё статическое, то ckey уже инициализированы.
        }
    }

    OperationCodes () { ckey = null;   ckeyDescription = null;   ckeyUsage = null; }
    OperationCodes (String ck, String desc, String us) {
    ckey = ck;   ckeyDescription = desc;    ckeyUsage = us;
    }

//---------------------------- Статические методы: -----------------------------

/** Получаем код операции по его имени <i>name</i>. */
    public static OperationCodes byName (String name) {
        return mapNames.get (name.trim().toUpperCase());
    }
/** Получаем код операции по его консольному ключу-строке. */
    public static OperationCodes byCKey (String ckey) {
        return mapConKeys.get (ckey.trim().toLowerCase());
    }

//---------------- Геттеры и сеттеры для параметров команд: --------------------

    public boolean getOnOff ()            { return onOff; }
    public void    setOnOff (boolean val) { onOff = val; }

    public UUID getUuid ()         { return uuid; }
    public void setUuid (UUID val) { uuid = val; }

    public Task getTask ()         { return task; }
    public void setTask (Task val) { task = val; }

    public String getErrCode ()         { return errCode; }
    public void setErrCode (String val) { errCode = val; }

    public DeviceTypes getDeviceType ()         { return deviceType; }
    public void setDeviceType (DeviceTypes val) { deviceType = val; }
}
