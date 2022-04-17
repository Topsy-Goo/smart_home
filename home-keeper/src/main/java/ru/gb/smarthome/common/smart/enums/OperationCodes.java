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
<p>    Некоторые коды предназначены только для использования из консоли. Чтобы код мог быть распознан
в консоли, для него здесь нужно задать ckey и ckeyDescription. Чтобы код работал, а не только
распознавался, нужно добавить его обработку в SmartDevice.runConsole().  */
public enum OperationCodes implements Serializable {

    //CMD_ACTIVATE, < эту команду мы не используем. Для (де)активации УУ просто ставим на стороне сервера галку «УУ неактивно».

/** Комада: перевести УУ в состоянии готовности. (В это состояние УУ можно описать
как «бодрое бездействие», и УУ может возвращаться в него из любого состояния, если
это уместно.)<br>
    Состояние: УУ ничего не делает, даже не спит.<br>
    Ответ: текущее состояние УУ. */
    CMD_READY ("/ready",  "Перевод УУ в состояние готовности.", null),

/** Комада: отправить УУ в сон.<br>
    Состояние: УУ спит.<br>
    Ответ: текущее состояние УУ. */
    CMD_SLEEP  (/*"/sleep", "Отправить УУ в сон.", null*/),

/** Комада: пробудить УУ ото сна..<br>
    Ответ: перевод УУ в CMD_READY + отдать текущее состояние УУ. */
    CMD_WAKEUP ("/wake", "Пробудить УУ ото сна.", null),

/** Команда: приказ УУ обрабатывать сигналы от УУ с указаным UUID + поле uuid. */
    CMD_PAIR  (/*"/pair", "Приказ УУ обрабатывать сигналы от УУ с указаным UUID.", "/pair UUID"*/), //< оба УУ должны понимать, что от них хотят (пока в разработке).

/** Команда: приказ УУ выполнять задачу + поле task. */
    CMD_TASK  (/*"/task", "Выполнять задачу NAME в течение N сек.", "/task NAME N (выполнить) или /task (отменить текущую задачу)"*/),

/** Состояние: УУ занято выполнением запроса CMD_TASK.<br> */
    CMD_BUSY  (/*"/busy", "Вкл./выкл. сотсояние «занято» в УУ", "/busy "*/),

/** Команда: запрос состояния.<br>
    Ответ: текущее состояние + уточняющий параметр, если состоянию полагается иметь параметр. */
    CMD_STATE ("/state", "Запрос состояния УУ.", null),

/* * Команда: запрос поддерживаемых задач.<br>
    Ответ: CMD_TASKLIST + (список задач)
    CMD_TASKLIST,

/* * Команда: запрос UUID у УУ.<br>
    Ответ: CMD_UUID + поле uuid.
    CMD_UUID  ("/uuid", "Показать UUID устройства.", null),

/* * Команда: запрос стандартного типа у УУ.<br>
    Ответ: CMD_TYPE + поле deviceType.
    CMD_TYPE  ("/type", "Показать тип устройства.", null), */

/** Команда: запрос возможностей УУ.<br>
    Ответ: CMD_ABILITIES. */
    CMD_ABILITIES ("/abil", "Список возможностей УУ.", null),

/** Состояние: УУ находится в неисправном состоянии + CODE (код ошибки).
<p>
При управлении из консоли параметр CODE может быть любым, — он будет интерпретироваться как строка. Если ошибка произошла при работе УУ, то CODE выбирает и устанавливает УУ.
<p>
Если ошибку исправить, то УУ перейдёт в состояние CMD_READY. (Пока нет никаких рекомендаций по выводу УУ из состояния ошибки — можно сделать это при пом. /err без параметра, или прямым переводом в состояние READY.) */
    CMD_ERROR  ("/err", "Вкл./выкл. состояния ошибки в УУ.", "/err CODE (отсутствие CODE сбросывает состоние ошибки)."),

/** Команда: информирование УУ об успешном подключении к УД. */
    CMD_CONNECTED,

/** Команда: информирование УУ о невозможности подключить его к УД ввиду отсутствия свободных портов. */
    CMD_NOPORTS,

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

    public boolean greaterThan (OperationCodes other) { return (other == null) || this.compareTo (other) > 0; }
}
