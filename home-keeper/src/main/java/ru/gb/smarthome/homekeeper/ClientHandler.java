package ru.gb.smarthome.homekeeper;

import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.IDeviceServer;
import ru.gb.smarthome.common.exceptions.OutOfServiceException;
import ru.gb.smarthome.common.exceptions.RWCounterException;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.SmartDevice;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.enums.SensorStates;
import ru.gb.smarthome.common.smart.enums.TaskStates;
import ru.gb.smarthome.common.smart.structures.*;
import ru.gb.smarthome.homekeeper.dtos.BinateDto;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.common.smart.enums.BinatStates.BS_CONTRACT;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;

public class ClientHandler extends SmartDevice implements ISmartHandler
{
/** Хэндлер работает в двух потоках: в одном (пК) он общается с клиентом, а во втором —
 с УД (пУ). Для упорядочивания доступа к своим полям хэнлер использует синхронизацию по
 объекту. Это реализовано как попеременное нахождение пК в synchronized-блоке и вне его,
 причём вне блока пК просто спит. */
    private final Object  stateMonitor = new Object();
/** Передача Abilities происходит единажды — в начале работы хэндлера. Сейчас нет необходимости в
 этом мониторе, но пусть останется до поры. */
    private final Object  abilitiesMonitor = new Object(); //TODO: кажется, монитор на Abilities не нужен.
/** Некий объект, который УУ должно получить для того, чтобы получить статус обнаруженного
 устр-ва. Отключаясь от УД, у-во освобождает занимаемы Port. Количество Port-ов в УД ораничено. */
    private       Port    port;
/** Указывает, активно ли в данный момент УУ. Варианты значений: ACTIVE и NOT_ACTIVE.<p>
 Доступ к этому полю выполняется примерно в той же очерёдности, что и доступ к state,
 поэтому лучше не делать это поле AtomicBoolean (для произвольного доступа), чтобы избежать
 ситуаций, когда работая
 со state хэндлер начинает работу с одним значением active, а заканчивает с другим.
 Вобщем, это поле следует считать частью state, как это и было ранее. */
    private       boolean active;
/** UUID у-ва, которое мы представляем на стороне УД (UUID клиента). */
    private       UUID    uuid;
    private final AtomicReference<String>   deviceFriendlyName = new AtomicReference<>("");
/** Рандеву-объект для связи с сервером, — хэндлер сообщает о результате инициализации. */
    private       SynchronousQueue<Boolean> helloSynQue;
    private       IDeviceServer server;
/** Интервал опроса клиента (миллискунды). */
    private       int          pollInterval = DEF_POLL_INTERVAL_BACK;
/** Сообщения, которыми хэндлер счёл нужным поделиться с юзером. В составе StateDto эти сообщения
 уходят на фронт. */
    private final List<String> lastNews     = new LinkedList<>();

/** Контракты, которые УУ должно выполнять в качестве ведомого. */
    private       List<Binate> masterContracts;
/** Контракты, которые УУ должно выполнять в качестве ведущего. */
    private       List<Binate> slaveContracts;

/** Очередь запросов, поступающих извне. Хэндлер извлекает запросы из этой очереди в порядке
 приортетности и выполняет в меру возможностей. */
    private final PriorityBlockingQueue<Message> priorityQueue =
        new PriorityBlockingQueue<>(10, Comparator.reverseOrder()); //< Обратный порядок сравнения приоритетов.
    //* предоставляет блокирующие операции извлечения
    //* не допускает пустых элементов
    //* iterator и spliterator не гарантируют порядок обхода элементов. Если нужен упорядоченный обход,
    //  рассмотрите Arrays.sort(pq.toArray()).
    //* drainTo() — для переноса элементов в другую коллекцию в порядке приоритета.
    //* никаких гарантий относительно упорядочения элементов с равным приоритетом.

    private ISignalCallback slaveCallback;


    public ClientHandler (Socket s, Port p, SynchronousQueue<Boolean> helloSQ,
                          IDeviceServer srv, ISignalCallback slaveCallback)
    {
        if (DEBUG && (s == null || p == null))
            throw new IllegalArgumentException();
        port = p;
        socket = s;
        p.occupy (socket, this);
        helloSynQue = helloSQ;
        server = srv;
        slaveContracts  = new LinkedList<>();
        masterContracts = new LinkedList<>();
        this.slaveCallback = slaveCallback;
    }

/** Конструктор используется для создания временного хэндлера, назначение которого только
в том, чтобы сообщить клиенту об отсутствии свободных портов.<p>
Этот конструктор гасит все исключения, чтобы вызывающая ф-ция могла закрыть socket.  */
    public ClientHandler (Socket s) {
        socket = s;
        try {
            oos = new ObjectOutputStream (socket.getOutputStream());
            ois = new ObjectInputStream (socket.getInputStream());
            writeMessage (oos, new Message().setOpCode (CMD_NOPORTS));

            printf ("\nClientHandler: отправлено соощение: %s.", CMD_NOPORTS.name());
            println ("\nClientHandler: клиенту отказано в подключении, — нет свободных портов.");
        }
        catch (Exception e) { e.printStackTrace(); }
    }

    @Override public void run ()
    {
        threadRun = Thread.currentThread();
        String code = "OK";
        try {
            oos = new ObjectOutputStream (socket.getOutputStream());
            ois = new ObjectInputStream (socket.getInputStream());
            //Совершенно неожиданно оказалось, что одинаковые операции — две ObjectOutputStream или две ObjectInputStream — блокируют друг друга, кода вызываются на обоих концах канала. Поэтому, если на одном конце канала вызывается, например, new ObjectInputStream(…), то на другом нужно обязательно вызвать new ObjectOutputStream(…), чтобы не случилась взаимная блокировка.
            printf ("\nClientHandler: соединение с клиентом установлено: "+
                    "\nsocket : %s (opend: %b)"+
                    "\nois : %s"+
                    "\noos : %s\n", socket, !socket.isClosed(), ois, oos);
            mainLoop();
        }
        catch (Exception e) {
            code = e.getMessage();
            e.printStackTrace();
        }
        finally {
            if (helloSynQue != null) helloSynQue.offer (ERROR);helloSynQue = null;  //< сообщаем в DeviceServerHome, что у нас не получилось начать работу.
            disconnect();
            Thread.yield(); //< возможно, это позволит вовремя вывести сообщение об ошибке.
            printf ("\nClientHandler: поток %s завершился. Код завершения: %s.\n", threadRun, code);
        }
    }

/** Очистка в конце работы метода run(). */
    private void disconnect() {
        try {
            if (socket != null) socket.close();
        }
        catch (Exception e) { e.printStackTrace(); }
        finally {
            if (port != null) port.freePort();
            port = null;
            socket = null;
        }
    }

/** Основной цикл хэндлера УУ. При разработке этого метода придерживаемся следующих принципов:<br>
 • запись в стрим и чтение из стрима делаются только из этого метода (необязательно);<br>
 • сначала выполняем запись в стрим, а потом — чтение из стрима, поскольку клиент работает <u>только</u> в пассивном режиме (обязательно);<br>
 • записав что-то в стрим, нужно обязательно что-то прочитать из стрима (обязательно). Для проверки этого условия введена переменная rwCounter;<br>
 •  ();<br>
 •  ();<br>
*/
    private void mainLoop () throws Exception
    {
        Message mQ = new Message();
        Message mA;
        int len;

        rwCounter.set(0L);
        {
        //Первый контакт с клиентом:
            mA = requestClient (CMD_CONNECTED, null); //< mA.opCode содержит код состояния УУ, но нам пока нечего извлечь из этого факта.
            if (mA == null) {
                errprint ("\nEmpty.mainLoop(): не удалось пообщаться с УУ.");
                return;
            }

            if (!updateAbilities()  ||  !updateState()) {
                errprint ("\nEmpty.mainLoop(): не удалось получить первые CMD_ABILITIES и CMD_STATE.");
                return;
            }
            deviceFriendlyName.set (abilities.getVendorString()); //< потом юзер сможет это изменить.
            uuid = abilities.getUuid();

            if (DEBUG) check(rwCounter.get() == 0L, RuntimeException.class, "блок mainLoop(){перед getIntoDetectedList()}");
            else return;
        }
    //если первый контакт удался, переходим к стадии, выход из которой должен обрабатываться в cleanup().

        if (getIntoDetectedList())  //< это приведёт к добавлению нас в список обнаруженых УУ, и о нас узнаеют объекты УД.
        try {
            while (!threadRun.isInterrupted())
            {
                TimeUnit.MILLISECONDS.sleep (pollInterval); //< Пауза между проверками состояния УУ и очереди запросов.

            //Проверяем, соблюдение условия: «Клиент только отвечает на наши запросы»; при необходимости вычитываем из
            // стрима всё, что клиент прислал без спроса, и «выбрасываем»:
                if (DEBUG)
                while ((len = ois.available()) > 0) {
                    errprintf ("\nEmpty.mainLoop(): клиент прислал что-то без запроса (%d байтов).\n", len);
                    while (len > 0)
                        len -= ois.skipBytes (len);
                }
            //Обновляем state, чтобы понять, как обрабатывать запросы из очереди:
                synchronized (stateMonitor)
                {
                    if (!updateState())
                        throw new OutOfServiceException ("\nEmpty.mainLoop(): Не удалось прочитать состояние УУ.");
                    if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок updateState");

            //Читаем очередь запросов от УД к нашему подопечному клиенту (если state.code == CMD_ERROR, то
            // очередь сбрасывается и устанавливается active == NOT_ACTIVE):
                    if (state.getOpCode().equals (CMD_ERROR))
                        treatErrorState();
                    else
            //Выбираем из очереди задачи и обрабатываем их, если их приоритет больше приоритета state.code.
                        dispatchHadlerTaskQueue();
                }
                check (rwCounter.get() == 0L, RWCounterException.class, "блок mainLoop.while"); //< общая проверка остаётся в чистовой версии.
            }//while
        }
        catch (RWCounterException rwe) {
            errprintf ("\n[rwCounter:%d][%s]\n", rwCounter.get(), rwe.getMessage());
        }
        catch (OutOfServiceException e) {  println ("\n" + e.getMessage());  }
        catch (Exception e) { e.printStackTrace(); }
        finally {
            cleanup();
            println ("\nClientHandler: выход из mainLoop().");
        }
    }

/** Очистка в конце работы метода mainLoop(). */
    private void cleanup () {
        getOutOfDetectedList(); //< делаем это из mainLoop() потому, что в mainLoop() делали getIntoDetectedList().
        priorityQueue.clear();
    }

/** Ряд действий, которые повлекут добавление нас в список обнаруженых устройств и которые
выделены в отдельный метод для лучшей читаемости кода. */
    private boolean getIntoDetectedList () {
        boolean ok = helloSynQue.offer(OK);
        helloSynQue = null; //< обнуление явл-ся индикатором того, что мы уже воспользовались synQue.offer(…).
        return ok;
    }

/** Ряд действий, которые повлекут удаление нас из списка обнаруженых устройств и которые
выделены в отдельный метод для лучшей читаемости кода. */
    private void getOutOfDetectedList () {
        if (server !=null)
            server.goodBy(this);    server = null;
    }

/** Смотрим, что у нас есть в очереди — что УД успел пожелать от нашего подопечного УУ.<p>
Выполняем только те задачи, приоритет которых больше приоритета текущего состояния УУ.Остальные задачи оставляем в очереди на потом.  */
    private void dispatchHadlerTaskQueue () throws Exception
    {
    //Если мы здесь, то у клиента НЕТ состояния ошибки.
        OperationCodes messageOpCode;
        Message peekedMsg, mR;
        boolean ok;
        Object obj;
    //Выполняем запрошенную операцию, только если её приоритет БОЛЬШЕ приоритета текущего состояния УУ.
        while ((peekedMsg = priorityQueue.peek()) != null   &&
        (!state.getOpCode().greaterThan (messageOpCode = peekedMsg.getOpCode())))
        {
            switch (messageOpCode)
            {
                //case CMD_READY:
                //    break;
                //case CMD_SLEEP:
                //    break;
                case CMD_TASK:   treatTaskRequest (peekedMsg);
                    break;
                case CMD_SIGNAL: treatSignalRequest (peekedMsg);
                    break;
                case CMD_SENSOR: treatSensorRequest (peekedMsg);
                    break;
                case CMD_STATE:  if (!updateState()) throw new OutOfServiceException (); //TODO: Удалить?
                    break;
                case CMD_ABILITIES: if (!updateAbilities()) throw new OutOfServiceException (); //TODO: Удалить?
                    break;
                default: if (DEBUG) throw new OutOfServiceException(
                    format ("ClientHandler.dispatchHadlerTaskQueue():switch:default: Message: %s.", peekedMsg));
            }
            //priorityQueue.poll(); < Удивительная Java не гарантирует порядок для однаранговых элементов!…  o_O  А кто будет гарантировать?…
            Message finalPeekedMsg = peekedMsg;
            priorityQueue.removeIf (message->message == finalPeekedMsg);
            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок readHadlerTaskQueue.while "+ messageOpCode.name());
        }//while try
    }

    static final String rejectedTaskMessageFormat = "Устройство %s\rне выполнило задачу %s — %s\r(%s).";

/** Делаем клиенту запрос на выполнение задачи и ждём в ответ Message.data == Task с подробностями.
 Присланый Task используем только для информирования фронта о ходе запроса.
 @param peekedMsg сообщение, извлечённое из очереди запросов хэндлера. В нём находятся подробности
 о запрошеном действии. */
    private void treatTaskRequest (Message peekedMsg)
    {
        Message mR;
        Object obj;
        Task answer;
        String taskName = stringFromObject (peekedMsg.getData()),
               error = format (FORMAT_REQUEST_ERROR, deviceFriendlyName.get());

        if (taskName != null
        &&  (mR = requestClient (CMD_TASK, taskName)) != null
        &&  ((answer = taskFromObject (mR.getData())) != null))
        {
            TaskStates answerTstate = answer.getTstate();
            if (answerTstate.launchingError)
            {
                error = format (rejectedTaskMessageFormat,
                                  deviceFriendlyName.get(),
                                  answer.getName(),
                                  answer.getTstate().tsName, //< стандартное (очень) краткое описание результата
                                  answer.getMessage());      //< строка-сообщение о результате выполнения.
            } else error = null;
        }
        if (error != null && !error.isBlank())
            lastNews.add (error);
    }

    //static final String rejectedBindingMessage = "Запрос не может быть выполнен.";
    static final String rejectedRequestFormat = "Устройство %s\rне выполнило запрос — %s.";

/** Делаем клиенту запрос на изменение состояния одного из сенсоров устройства.
 @param peekedMsg сообщение, извлечённое из очереди запросов хэндлера. В нём находятся подробности
 о запрошеном действии. */
    private void treatSensorRequest (Message peekedMsg)
    {
        Sensor request = sensorFromObject (peekedMsg.getData());
        String error = null;
        Message mR;

        if (request != null  &&  (mR = requestClient (peekedMsg)) != null)
        {
            Sensor answer = sensorFromObject (mR.getData());
            if (!request.equals (answer))
                error = format (rejectedRequestFormat, deviceFriendlyName.get(), request.toString());
        }
        else error = format (FORMAT_REQUEST_ERROR, deviceFriendlyName.get());

        if (error != null && !error.isBlank()) {
            lastNews.add (error);
        }
    }

    private void treatSignalRequest (Message peekedMsg)
    {
        Signal signal;
        ISmartHandler originSmart = null;
        UUID sourceUuid;
        String contractTaskName, currentTaskName;
        boolean putIntoTaskQueue;
        Task taskCurrent;
        TaskStates tstate;
        StringBuilder sb = new StringBuilder();

        if (peekedMsg != null
        &&  (signal = signalFromObject (peekedMsg.getData())) != null
        &&  (originSmart = signal.getOriginHandler()) != null
        &&   originSmart != this
        &&  (sourceUuid = signal.getSource()) != null) //< датчик
        {
            for (Binate bin : searchMasterContracts (sourceUuid))
            {
                contractTaskName = bin.taskName();
                putIntoTaskQueue = false;

                if (abilities.isTaskName (contractTaskName))
                {
                    if ((taskCurrent = state.getCurrentTask()) == null)
                        putIntoTaskQueue = true;    /* нет текущей задачи */
                    else
                    if ((tstate = taskCurrent.getTstate()).canReplace)
                        putIntoTaskQueue = true;    /* нет текущей задачи */
                    else
                    if (tstate.runningState)
                    {
                        if ((currentTaskName = taskCurrent.getName()).equals (contractTaskName))
                            continue;               /* contractTaskName уже запущена */

                        if (taskCurrent.isInterruptible())
                        {                           /* текущую задачу можно прервать */
                            putIntoTaskQueue = interruptCurrentTask();
                            if (!putIntoTaskQueue)
                                sb.append (format ("Не удалось остановить задачу %s.\r", currentTaskName));
                        }
                        else
                        sb.append (format ("Задача %s не может быть запущена, — выполняется задача %s.\r",
                                          contractTaskName, currentTaskName));
                    }
                    else sb.append (format ("Нельзя запустить задачу %s.\r", contractTaskName));

                    if (putIntoTaskQueue)
                        offerRequest (new Message().setOpCode (CMD_TASK).setData (contractTaskName));
                }
                else sb.append ("Контракт не на выполнение задачи.\r");
            }//for
        }
        else sb.append ("переданы некорректные данные.");

        String errStr = sb.toString();
        if (!errStr.isBlank())
        {
            lastNews.add (format ("Устройство %s\rне смогло обработать сигнал от устройства\r%s:\r\r%s",
                deviceFriendlyName,
                (originSmart != null) ? originSmart.getDeviceFriendlyName() : originSmart,
                errStr));
    }   }

    private boolean interruptCurrentTask ()
    {
        Message mR = requestClient (CMD_INTERRUPT, null);
        if (mR != null) {
            Task t = taskFromObject (mR.getData());
            return (t == null) || t.getTstate().canReplace;
        }
        return false;
    }

//---------------------- Реализации методов --------------------------------

    @Override public @NotNull Abilities getAbilities () {
        synchronized (abilitiesMonitor) {
            return abilities;
        }
    }

    @Override public @NotNull DeviceState getState () {
        synchronized (stateMonitor) {
            return state.safeCopy();
        }
    }

    @Override public boolean offerRequest (Message mR)
    {
        boolean ok = false;
        String err = null;
        if (mR != null)
        {
            //if (state.getOpCode().lesserThan (CMD_ERROR))
                ok = priorityQueue.offer(mR);
            //else
            //    err = format ("Устройство %s неисправно, — запрос %s не может быть обработан.",
            //                   deviceFriendlyName.get(), mR.getOpCode().name());
        }
        else err = format ("Некорректный запрос: %s.", mR);

        if (err != null)
            lastNews.add (err);
        return ok;
    }

    @Override public boolean activate (final boolean value)
    {
        String error = promptCannotChangeActivityState;
        if (active == value)
            return true;

        synchronized (stateMonitor)
        {
            updateState();
            if (state.getOpCode().equals (CMD_ERROR))
            {
                error = promptActivationDuringErrorState;
                changeActiveState (NOT_ACTIVE);
            }
            else if (active == ACTIVE)
            {
                if (isItSafeToDeactivate())
                    changeActiveState (NOT_ACTIVE);
                else
                    error = promptDeactivationIsNotSafeNow;
            }
            else changeActiveState (ACTIVE);

            boolean ok = active == value;
            if (!ok)
                lastNews.add (error);
            return ok;
        }
    }

    @Override public boolean isActive () {
        synchronized (stateMonitor) {
            return active;
        }
    }

    @Override public void setPollInterval (int milliseconds) {
        if (milliseconds >= DEF_POLL_INTERVAL_MIN)
            pollInterval = milliseconds;
    }

    @Override public boolean setDeviceFriendlyName (String name) {
        boolean ok = isStringsValid (name);
        if (ok)
            deviceFriendlyName.set (name);
        return ok;
    }

    @Override public @NotNull String getDeviceFriendlyName () { return deviceFriendlyName.get(); }

    @Override public @NotNull List<String> getLastNews () {
        synchronized (stateMonitor) {
            List<String> list = new ArrayList<>(lastNews);
            lastNews.clear();
            return list;
        }
    }

    @Override public String toString () {
        return format ("Handler[«%s»,\n\t%s]"//  state:	%s
                       ,deviceFriendlyName.get()
                       ,abilities.getUuid()
                       //,state
                       );
    } //< для отладки

    @Override public boolean pair (Binate binate)
    {
        boolean ok = false;
        String errCause = "Некорректные данные.";
        synchronized (stateMonitor)
        {
            if (binate != null && binate.bstate().equals (BS_CONTRACT))
            {
                if (binate.role() == SLAVE) {
                    if (abilities.isSlave())
                    {
                        if (!abilities.isSensorUuid (uuidFromObject (binate.source())))
                            errCause = "Нет такой функции.";
                        else
                        if (!(ok = addIfAbsent (slaveContracts, binate)))
                            errCause = "Контракт уже существует.";
                    }
                    else errCause = "Устройство не может быть ведомым.";
                }
                else {
                    if (abilities.isMaster())
                    {
                        if (!abilities.isTaskName (/*stringFromObject*/ (binate.taskName())))
                            errCause = "Нет такой задачи.";
                        else
                        if (!(ok = addIfAbsent (masterContracts, binate)))
                            errCause = "Контракт уже существует.";
                    }
                    else errCause = "Устройство не может быть ведущим.";
                }
            }
            if (!ok)
                lastNews.add (format ("Не удалось выполнть связывание для устройства\r%s.\r%s",
                                      deviceFriendlyName.get(), errCause));
        }return ok;
    }

    @Override public boolean unpair (Binate binate)
    {
        String errCause = "Некорректные данные.";
        Binate contract;
        boolean ok = false;
        synchronized (stateMonitor)
        {
            if (binate != null && binate.bstate().equals (BS_CONTRACT))
            {
                if (binate.role() == SLAVE)
                    ok = slaveContracts.removeIf ((c)->c.equals (binate));
                else
                    ok = masterContracts.removeIf ((c)->c.equals (binate));

                if (!ok)
                    errCause = "Возможно, устройство уже отвязано.";
            }
        }
        if (!ok) lastNews.add (format ("Не удалось отвязывание для устройства\r%s.\r%s",
                                       deviceFriendlyName.get(), errCause));
        return ok;
    }

    @Override public List<BinateDto> getMasterContractsDto ()
    {
        if (masterContracts != null && !masterContracts.isEmpty())
            return masterContracts.stream().map(BinateDto::binateToDto).collect(Collectors.toList());
        return null;
    }
//---------------------- Другие полезные методы: ---------------------------

/** Перезаписываем this.abilities экземпляром, считаным из клиента нашего подопечного УУ. */
    @SuppressWarnings("all")
    private boolean updateAbilities () {
        synchronized (abilitiesMonitor) {
            return (abilities = requestClientAbilities()) != null;
        }
    }

/** Запрашиваем abilities у нашего подопечного УУ. */
    private Abilities requestClientAbilities ()
    {
        Abilities abilities = null;
        Message m = null; Message mA = new Message (CMD_ABILITIES, /*null,*/ null);
        boolean ok = false;
        Object data = null;

        if (writeMessage (oos, mA.setData(null))) {
            m = readMessage(ois); //< блокирующая операция

            if (m != null
            &&  m.getOpCode() == CMD_ABILITIES
            &&  (data = m.getData()) instanceof Abilities)
            {
                abilities = (Abilities) data;
                ok = true;
            }
        }
        if (DEBUG && !ok) errprintf (
                    "\nClientHandler: requestClientAbilities() : не удалось запросить abilities из УУ :" +
                    "\n\t** отправили: %s" +
                    "\n\t** получили: %s" +
                    "\n\t** data: %s.\n", mA, m, data);
        return abilities;
    }

/** Запрашиваем DeviceState у нашего подопечного УУ и перезаписываем им наш state. */
    private boolean updateState () {
        return overideCurrentState (requestClientState());
    }

/** Если {@code newState != null}, то перезаписываем this.state указаным экземпляром newState.<p>
 Если в результате этих действий state.code окажется == CMD_ERROR, то устанавливаем active в значение
 NOT_ACTIVE. (Неисправное УУ не может быть активным.)<br>
 @param newState экземпляр DeviceState, которым требуется заменить {@code this.state}.
 @return TRUE, если state удалось обновить.    */
    private boolean overideCurrentState (DeviceState newState)
    {
        final boolean ok = newState != null;
        if (ok) {
            state = newState;

            if (state.getOpCode().equals (CMD_ERROR))
                changeActiveState (NOT_ACTIVE);

            readSensors();
        }
        else if (DEBUG) throw new RuntimeException ("ClientHandler.updateState(newState==null).");
        return ok;
    }

/** Обрабатываем состояния сенсоров, пришедшие от нашего клиента. */
    private void readSensors ()
    {
        Map<UUID, SensorStates> sensors = state.getSensors();
        for (Map.Entry<UUID, SensorStates> e : sensors.entrySet())
        {
            if (e.getValue().alarm) {
                UUID uuSensor = e.getKey();

                List<Binate> contracts = searchSlaveContracts (uuSensor);
                if (!contracts.isEmpty())
                for (Binate bin : contracts) {
                    slaveCallback.callback (bin.mateUuid(), new Signal (this, uuSensor, null));
                }
                //TODO: Кроме того УУ в ответ на ALARM тут может запустить какую-то свою задачу или обработчик.
            }
        }
    }

/** По указанному UUID события ищем контракты ведомых УУ, — контракты, по которым мы как ведомое УУ должны
 сообщать ведущим УУ о событии UUID.
 @param event UUID события. */
    private @NotNull List<Binate> searchSlaveContracts (UUID event)
    {
        List<Binate> binList = new LinkedList<>();
        if (slaveContracts != null && !slaveContracts.isEmpty())
            for (Binate bin : slaveContracts)
                if (event.equals (uuidFromObject (bin.source())))
                    binList.add (bin);
        return binList;
    }

/** По указанному UUID события ищем контракты, по которым мы как ведущее УУ
 должны выполнить какую-то задачу, если, конечно, у нас такие контракты есть.
 @param event UUID события. */
    private @NotNull List<Binate> searchMasterContracts (UUID event)
    {
        List<Binate> binList = new LinkedList<>();
        if (event != null && masterContracts != null && !masterContracts.isEmpty())
        {
            for (Binate bin : masterContracts)
                if (event.equals (uuidFromObject (bin.source())))
                    binList.add (bin);
        }
        return binList;
    }

    private void changeActiveState (boolean value) {
        active = value;
    }

/** Запрашиваем state у нашего подопечного УУ. (Планируем вызывать этот метод очень часто.)
@return DeviceState, полученный от клиента, или NULL, если запрос не удался. */
    private DeviceState requestClientState ()
    {
        DeviceState newState = null;
        Message mW = new Message (CMD_STATE, /*null,*/ null);
        Message mR = null;
        boolean ok = false;
        Object data = null;

        if (writeMessage (oos, mW.setData(null))) {
            mR = readMessage(ois); //< блокирующая операция

            if (mR != null
            &&  mR.getOpCode() == CMD_STATE
            &&  (data = mR.getData()) instanceof DeviceState)
            {
                newState = (DeviceState) data;
                ok = true;
            }
        }
        if (DEBUG && !ok) errprintf (
                    "\nClientHandler.requestClientState() : не удалось запросить state из УУ :" +
                    "\n\t** отправили: %s" +
                    "\n\t** получили: %s" +
                    "\n\t** data: %s.\n", mW, mR, data);
        return newState;
    }

/** Перенапраовляет вызов в  {@link #requestClient(OperationCodes, Object) requestClient (Message.opCode, Message.data)} */
    private Message requestClient (@NotNull Message mQ)
    {
        return requestClient (mQ.getOpCode(), mQ.getData());
    }

/** Отправляем клиенту сообщение с указанными параметрами и получаем от клиента ответ.
@param opCodeQ — код сообщения для клиента.
@param dataQ — данные, которые нужно отправить клиенту.
@return сообщение (Message), плученое от клиента, или NULL, если обмен данными с клиентом не удался.  */
    private Message requestClient (@NotNull OperationCodes opCodeQ, Object dataQ)
    {
        Message mQ = new Message()/*.setDeviceUUID (null)*/;
        Message mA = null;
        boolean sent = writeMessage (oos, mQ.setOpCode (opCodeQ).setData (dataQ));

        if (sent)
            mA = readMessage(ois); //< блокирующая операция

        if (DEBUG && mA == null) errprintf (
                "\nClientHandler: requestClient() : не удалось запросить %s из УУ (%s) :" +
                "\n\t** отправили: %s" +
                "\n\t** получили: %s" +
                "\n\t** data отпр.: %s.\n",
                opCodeQ, deviceFriendlyName.get(),
                sent ? mQ : "(отправка не состоялась)",
                mA, dataQ);
        return mA;
    }

/** Обрабатываем состояние CMD_ERROR: деактивируем УУ и сбрасываем очередь задач пришедших от УД. */
    private void treatErrorState () throws Exception
    {
        if (active)
            changeActiveState (NOT_ACTIVE);
        priorityQueue.clear();
        if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок treatErrorState");
    }

    private boolean isItSafeToDeactivate ()
    {
        Task t = state.getCurrentTask();
        return t == null || t.isAutonomic() || !t.getTstate().runningState;
    }

    //void f () {        ;    }
//---------------------------------------------------------------------------

}
