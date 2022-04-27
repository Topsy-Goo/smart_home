package ru.gb.smarthome.common.smart.enums;

import ru.gb.smarthome.common.smart.structures.Task;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

/** Коды команд, операций и состояний (режимов), которые могут использоваться в приложении.
 Смысл кода — является ли он кодом команды, кодом состояния или кодом операции — зависит от контекста:
 например, УД может дать команду CMD_SLEEP, чтобы заставить УУ перейтив режим сна; после чего УУ
 перейдёт в ржим сна и на команду CMD_STATE (запрос состояния УУ) будет отвечать "CMD_SLEEP".
<p>
    Коды могут сопровождаться дополнительными данными. Например, УД, отдавая команду CMD_TASK, должен
 указать, какую именно задачу нужно выполнить.
<p>
    Коды выстроены в порядке возрастания приоритета. Команда с не может начать
 обрабатываться до того, как будет закончена обработка команды с большим приоритетом или
 пока УУ находится в состоянии с высоким приоритетом.
 Например, невозможен перевод УУ в режим сна, если УУ занято выполнением задачи (УУ находится в
 состоянии CMD_BUSY), но при этом УУ обработает запросы CMD_STATUS или CMD_ABILITIES, которые
 имеют более высокий приоритет по отношению к BUSY.
<p>
Некоторые коды предназначены только для использования из консоли. Чтобы код мог быть распознан
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
    CMD_WAKEUP (/*"/wake", "Пробудить УУ ото сна.", null*/),

/** Команда: приказ УУ обрабатывать сигналы от УУ с указаным UUID + поле uuid. */
    CMD_PAIR  (/*"/pair", "Приказ УУ обрабатывать сигналы от УУ с указаным UUID.", "/pair UUID"*/), //< оба УУ должны понимать, что от них хотят (пока в разработке).

/** Команда: приказ УУ выполнять задачу + поле task. */
    CMD_TASK  ("/task", "Выполнять/отменить задачу.", "/task NAME - выполнить. /task - остановить. Повторный /task отменит остановленную задачу."),

/** Состояние: УУ занято выполнением запроса CMD_TASK.<br> */
    CMD_BUSY  ("/busy", "Вкл. сотсояние «занято» в УУ", null),

/** Состояние: УУ находится в неисправном состоянии + CODE (код ошибки).
<p>
 При управлении из консоли параметр CODE может быть любым или отсутствовать. CODE будет интерпретироваться как
 строка. Если ошибка произошла при работе УУ, то CODE выбирает и устанавливает УУ. Отсутствие CODE расценивается
 как отсутствие ошибки, и УУ переводится в состояние CMD_READY, т.е. команда {@code /err} аналогична команде
 {@code /ready}.
<p>
 Если ошибку исправить, то УУ перейдёт в состояние CMD_READY. (Пока нет никаких рекомендаций по выводу УУ из состояния ошибки — можно сделать это при пом. /err без параметра, или прямым переводом в состояние READY.) */
    CMD_ERROR  ("/err", "Вкл./выкл. состояния ошибки в УУ.", "/err CODE (отсутствие CODE сбросывает состоние ошибки)."),

/** Команда: запрос состояния.<br>
    Ответ: Message с CMD_STATE + уточняющий параметр, если состоянию полагается иметь параметр. */
    CMD_STATE ("/state", "Запрос состояния УУ.", null),

/** Команда: запрос возможностей УУ.<br>
    Ответ: Message с CMD_ABILITIES + Abilities. */
    CMD_ABILITIES ("/abil", "Список возможностей УУ.", null),

    /** Состояние: Это умолчальное состояние: УУ исправно, нет подключения к УД. Его код имеет приоритет ниже, чем
    CMD_CONNECTED для того, чтобы поступление команды CMD_CONNECTED смогло пройти «фильтр приоритетов». */
    CMD_NOT_CONNECTED,

/** Команда: информирование УУ об успешном подключении к УД. Получив её, УУ должен определиться, в
каком состоянии он находится и ответить кодом выбранного состояния.<br>
    Ответ: Message с кодом выбранного состояния. */
    CMD_CONNECTED,

/** Команда: информирование УУ о невозможности подключить его к УД ввиду отсутствия свободных портов.
    Получив эту команду УУ переключается в умолчальное состояние — CMD_NOT_CONNECTED.<br>
    Ответ: УУ не должно отвечать на это сообщение. */
    CMD_NOPORTS,

/** Команда: Должно использоваться только из консоли, т.к. эмулирует физическое выключение УУ или физическое
 отсоединение его от УД.<br>
    Ответ: нет ответа, т.к. команда вводится из консоли.<br>
    Состояние: в этом состоянии УУ выполняет очистку перед завершением работы модуля. */
    CMD_EXIT   ("/exit", "Завершить работу модуля УУ.", null)  //< УУ программно недоступно до его перезапуска.
    ;

/** {@code ckey} — ключ для управления УУ из консоли;<br>
    {@code ckeyDescription} — краткое описание ключа;<br>
    {@code ckeyUsage} — пример использования ключа. */
    public final String ckey, ckeyDescription, ckeyUsage; //< консольная команда, её краткое описание и использование.
    private static final HashMap<String, OperationCodes> mapConKeys; //< для быстрого сопоставления консольных команд с соотв.им кодами операций.

//параметры команд:
    private boolean onOff; //<  ON (сон) или OFF (не сон)
    private UUID uuid;
    private DeviceTypes deviceType;
    private Task task;
    private String errCode; //< код ошибки, специфичный для УУ
    /** Количество элементов в OperationCodes. */
    public static final int length = values().length;

//------------------------------- Конструирование: -----------------------------
    static {
        OperationCodes[] arr = values();

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

/* * Получаем код операции по его имени <i>name</i>. * /
    public static OperationCodes byName (String name)
    {
        есть valueOf()
        return mapNames.get (name.trim().toUpperCase());
    }//*/
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
    public boolean lesserThan (OperationCodes other) { return (other == null) || this.compareTo (other) < 0; }
}
