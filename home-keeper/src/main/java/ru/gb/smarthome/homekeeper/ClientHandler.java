package ru.gb.smarthome.homekeeper;

import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.IDeviceServer;
import ru.gb.smarthome.common.exceptions.OutOfServiceException;
import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.SmartDevice;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.structures.*;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.*;
import static ru.gb.smarthome.homekeeper.HomeKeeperApp.DEBUG;
import static ru.gb.smarthome.common.FactoryCommon.*;

public class ClientHandler extends SmartDevice implements ISmartHandler
{
    private final Object messagingMonitor = new Object(); //< Если использовать не Object, а, скажем, Message, то отладчик будет вынужден сражаться с NullPointerException-ами.
    private       Port    port;
    private       String  deviceFriendlyName;
    private       SynchronousQueue<Boolean> synQue;
    private       IDeviceServer server;
    private       int pollInterval = 5;

    private final PriorityBlockingQueue<Message> priorityQueue =
        new PriorityBlockingQueue<> (10
                //,(a,b)->(b.getOpCode().ordinal() - a.getOpCode().ordinal())
                //,Comparator.comparingInt(m->m.getOpCode().ordinal())
                );

    //* предоставляет блокирующие операции извлечения
    //* не допускает пустых элементов
    //* iterator и spliterator не гарантируют порядок обхода элементов. Если нужен упорядоченный обход, рассмотрите Arrays.sort(pq.toArray()).
    //* drainTo() — для переноса элементов в другую коллекцию в порядке приоритета.
    //* никаких гарантий относительно упорядочения элементов с равным приоритетом.

    //LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    //* FIFO

    public ClientHandler (Socket s, Port p, SynchronousQueue<Boolean> sQ, IDeviceServer srv)
    {
        if (DEBUG && (s == null || p == null))
            throw new IllegalArgumentException();
        port = p;
        socket = s;
        p.occupy (socket, this);
        synQue = sQ;
        server = srv;
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
            if (DEBUG) {
                printf ("\nClientHandler: отправлено соощение: %s.", CMD_NOPORTS.name());
                println ("\nClientHandler: клиенту отказано в подключении, — нет свободных портов.");
            }
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
            if (DEBUG) e.printStackTrace();
        }
        finally {
            if (synQue != null) synQue.offer (ERROR);   synQue = null;  //< сообщаем в DeviceServerHome, что у нас не получилось начать работу.
            disconnect();
            Thread.yield(); //< возможно, это позволит вовремя вывести сообщение об ошибке.
            if (DEBUG) printf ("\nClientHandler: поток %s завершился. Код завершения: %s.\n", threadRun, code);
        }
    }

/** Очистка в конце работы метода run(). */
    private void disconnect()
    {
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

/** Основной цикл хэндлера УУ. */
    private void mainLoop ()
    {
        Message m = new Message();
        int len;
    //Первый контакт с клиентом:
        if (!writeMessage (oos, m.setOpCode (CMD_CONNECTED)))
            errprint ("\nEmpty.mainLoop(): не удалось отправить CMD_CONNECTED.");
        else
        if (!updateAbilities()  ||  !updateState())
            errprint ("\nEmpty.mainLoop(): не удалось получить первые CMD_ABILITIES и CMD_STATE.");
        else
    //если первый контакт удался, переходим к стадии, выход из которой должен обрабатываться в cleanup().
        try {
            deviceFriendlyName = abilities.getVendorName(); //< потом юзер сможет это изменить.
            getIntoDetectedList();  //< это приведёт к добавлению нас в список обнаруженых УУ.

    //Всё, что происходит дальше, происходит с обнаруженным устройством:
            while (!threadRun.isInterrupted())
            {
            //Проверяем, соблюдение условия: «Клиент только отвечает на наши запросы», —
            // вычитываем из стрима всё, что клиент прислал без спроса, и «выбрасываем»:
                if ((len = ois.available()) > 0) {
                    //errprintf ("\nEmpty.mainLoop(): клиент прислал объект без запроса:\n\t%s:\n\t\t%s.\n", o.getClass().getName(), o.toString());
                    errprintf ("\nEmpty.mainLoop(): клиент прислал что-то без запроса (%d байтов).\n", len);
                    while (len > 0)
                        len -= ois.skipBytes (len);
                }

            //Обновляем state, чтобы понять, как обрабатывать запросы из очереди:
                if (!updateState())
                    throw new OutOfServiceException ("\nEmpty.mainLoop(): Не удалось прочитать состояние УУ.");

                //dispatchStateRequest  (); //< обрабатываем какие-то специальные ситуации, вроде CMD_TASK (окончание выполнения задачи)

            //Читаем очередь запросов от УД к нашему подопечному клиенту (если state.code == CMD_ERROR, то
            // очередь сбрасывается и устанавливается state.active == NOT_ACTIVE):
                if (!state.getCode().equals (CMD_ERROR))
                    readHadlerTaskQueue(); //< выбираем из очереди задачи и обрабатываем их, если их приоритет
                else                       //  больше приоритета state.code.
                    treatErrorState();

            //Пауза между проверками состояния УУ и очереди запросов:
                TimeUnit.SECONDS.sleep (pollInterval);
            }//while
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
        //priorityQueue.clear();
    }

/** Ряд действий, которые повлекут добавление нас в список обнаруженых устройств и которые
выделены в отдельный метод для лучшей читаемости кода. */
    private void getIntoDetectedList () {
        synQue.offer(OK);   synQue = null; //< обнуление явл-ся индикатором того, что мы уже воспользовались synQue.offer(…).
    }

/** Ряд действий, которые повлекут удаление нас из списка обнаруженых устройств и которые
выделены в отдельный метод для лучшей читаемости кода. */
    private void getOutOfDetectedList () {
        if (server !=null)
            server.goodBy(this);    server = null;
    }

/** Смотрим, что у нас есть в очереди — что УД успел пожелать от нашего подопечного УУ.<p>
Выполняем только те задачи, приоритет которых больше приоритета текущего состояния УУ.Остальные задачи оставляем в очереди на потом.  */
    private void readHadlerTaskQueue () throws Exception
    {
        OperationCodes opCode, stCode;
        Message peekedMsg, mR;
        boolean ok;
        Object obj;
        Task task;
        DeviceState dState;
        while ((peekedMsg = priorityQueue.peek()) != null)
        {
            //Сравниваем код запрошеной операции с кодом текущего состония УУ и выполняем операцию только, если её приоритет БОЛЬШЕ приоритета текущего состояния УУ.
            opCode = peekedMsg.getOpCode();
            stCode = state.getCode();
            if (opCode.greaterThan (stCode))
            {
        //Если мы попали в dispatchTaskQueue(), то ошибку в УУ нет.
                switch (opCode)
                {
                    //case CMD_READY:
                    //    break;
                    //case CMD_SLEEP:
                    //    break;

                    case CMD_TASK:
                        treatTaskRequest (peekedMsg);
                        priorityQueue.poll();
                        break;

                    case CMD_STATE: if (!updateState()) throw new OutOfServiceException ();
                        priorityQueue.poll();
                        break;

                    case CMD_ABILITIES: if (!updateAbilities()) throw new OutOfServiceException ();//getAbilities();
                        priorityQueue.poll();
                        break;

                    default:         //TODO: default-код является черновиком.
                        ok = false;
                        mR = requestClient (peekedMsg);
                        if (null != mR)
                        {
                            priorityQueue.poll();
                            ok = true;

                            obj = mR.getData();
                        /*  В качестве ответа на запрос УУ-во может прислать DeviceState, описывающий
                            текущее состояние УУ. В этом случае заменяем наш текущий state на присланый
                            DeviceState, но переносим в него значение state.active. */
                            if (obj instanceof DeviceState) {
                                /*ok = */updateState ((DeviceState) obj);

                                if (state.getCode().equals (CMD_ERROR))
                                    treatErrorState();
                            } else {}
                        }
                        if (!ok) throw new OutOfServiceException (format("\n****** ClientHandler: на запрос \n\t%s\n****** пришёл ответ\n\t%s\n", peekedMsg, mR));
                }
            } else break; //< остальные подождут.
        }//while
    }

/** делаем запрос и ждём в ответ DeviceState с подробностями. Присланый DeviceState не
применяем к нашему state, — нам его прислали только для информирования.
@param peekedMsg сообщение, извлечённое из очереди запросов хэндлера. */
    private void treatTaskRequest (Message peekedMsg)
    {
        Message mR;
        Object obj;
        Task task;
        DeviceState dState;

        if ((mR = requestClient (peekedMsg)) != null
        &&  (obj = mR.getData()) instanceof DeviceState
        &&  (dState = (DeviceState) obj).getCode().equals (CMD_TASK)
        &&  (task = dState.getCurrentTask()) != null)
        {
            server.requestCompleted (this, peekedMsg, task);
            updateState(); //< получаем CMD_BUSY и применяем его к нашему state.
        }
        else {server.requestError (this, peekedMsg);}
    }

//TODO:---------------------- Реализации методов (МЕТОДЫ ВЫЗЫВАЮТСЯ ИЗВНЕ): -------------------------------

/** Геттер на this.abilities. Чтобы не возвращать null, запрашивает abilities у клиента, если abilities == null. */
    @Override public Abilities getAbilities ()
    {
        if (abilities == null) //< При первом вызове abilities может быть == null.
            updateAbilities(); //< При втором, в принципе, тоже, — требуется проверка.
        return abilities;
    }

/** Геттер на this.state. Чтобы не возвращать null, запрашивает state у клиента, если state == null. */
    @Override public DeviceState getState ()
    {
        if (state == null) //< При первом вызове state может быть == null.
            updateState()/*state = requestClientState (NOT_ACTIVE)*/;
        return state;
    }

    @Override public String toString ()  //< для отладки
    {
        //String strSate = "(no state)";
        //if (state != null) {    strSate = state.getCode().name() +", "+ (state.isActive() ? "Акт." : "Неакт.");     }
        return format ("Handler[«%s»,\n\t%s,\n\tstate:\t%s]"
                       ,deviceFriendlyName
                       ,abilities
                       ,state
                       );
    }

/** Переводим УУ в активное/неактивное состояние.
@param value принимает значения ACTIVE и NOT_ACTIVE.
@return TRUE, если устройство переведено в указанное состочние или уже находится в указаном состоянии. */
    @Override public boolean activate (boolean value)
    {
        if (state.isActive() == value)
            return true;

        updateState();
        if (!state.getCode().equals (CMD_ERROR)) {
            state.setActive (NOT_ACTIVE);
        }
        else if (state.isActive() == ACTIVE) {
//TODO: нужно проверить, можно ли УУ деактивировать прямо сейчас.
            state.setActive (NOT_ACTIVE);
        }
        else {
            /*state = requestClientState (NOT_ACTIVE)*/
            state.setActive (ACTIVE);
        }
        return state.isActive() == value;
    }

/** Интервал между извлечениями всех заданий из очереди. */
    @Override public void setPollInterval (int seconds) {  if (seconds > 0)  pollInterval = seconds;  }

//---------------------- Другие полезные методы: ---------------------------

/** Перезаписываем this.abilities экземпляром, считаным из клиента нашего подопечного УУ. */
    private boolean updateAbilities ()
    {
        return (abilities = requestClientAbilities()) != null;
    }

/** Запрашиваем abilities у нашего подопечного УУ. */
    private Abilities requestClientAbilities ()
    {
        Abilities abilities = null;
        Message m = null; Message mA = new Message (CMD_ABILITIES, null, null);
        boolean ok = false;
        Object data = null;
        synchronized (messagingMonitor)
        {
            if (writeMessage (oos, mA.setData(null)))
            {
                m = readMessage(ois); //< блокирующая операция
                if (m != null
                &&  m.getOpCode() == CMD_ABILITIES
                &&  (data = m.getData()) instanceof Abilities)
                {
                    abilities = (Abilities) data;
                    ok = true;
                }
            }
        }
        if (DEBUG && !ok) errprintf (
                    "\nClientHandler: requestClientAbilities() : не удалось запросить abilities из УУ :" +
                    "\n\t** отправили: %s" +
                    "\n\t** получили: %s" +
                    "\n\t** data: %s.\n", mA, m, data);
        return abilities;
    }

/** Запрашиваем DeviceState у нашего подопечного УУ и перезаписываем им наш state.<br>Делаем
 {@link #updateState(DeviceState) updateState (requestClientState())}<br>
 См. также {@link #requestClientState() requestClientState()} */
    private boolean updateState () {
        return updateState (requestClientState());
    }

/** Если {@code newState != null}, то перезаписываем this.state указаным экземпляром newState.
При этом состояние {@code state.active} сохраняем — переносим из прежнего экземпляра в новый.
Если {@code newState == null}, то делаем себе «искусственный» this.state с кодом CMD_ERROR.
<p>
    Если в результате этих действий state.code окажется == CMD_ERROR, то устанавливаем
state.active в значение NOT_ACTIVE. (Неисправное УУ не может быть активным.)
<p>
    При любом исходе прежний экземпляр this.state окажется перезаписан.
<br>
@param newState экземпляр DeviceState, которым требуется заменить {@code this.state}. (Может быть == null.)
@return TRUE, если параметр {@code newState != null};<br>
        FALSE, если state пришлось заменить «искусственным».
*/
    private boolean updateState (DeviceState newState) //< TODO: иногда в этот метод удобно передавать Object. Может проверку на (o instanceof DeviceState) перенести сюда?
    {
        final boolean ok = newState != null;
              boolean currentActiveState = (state !=null) ? state.isActive() : NOT_ACTIVE;

        if (!ok)
            newState = new DeviceState().setCode (CMD_ERROR);

        if (newState.getCode().equals (CMD_ERROR))
            currentActiveState = NOT_ACTIVE;

        state = newState.setActive (currentActiveState);
        return ok;
    }

/** Запрашиваем state у нашего подопечного УУ. (Планируем вызывать этот метод очень часто.)
@return DeviceState, полученный от клиента, или NULL, если запрос не удался. */
    private DeviceState requestClientState ()
    {
        DeviceState newState = null;
        Message m = null; Message mS = new Message (CMD_STATE, null, null);
        boolean ok = false;
        Object data = null;
        synchronized (messagingMonitor)
        {
            if (writeMessage (oos, mS.setData(null)))
            {
                m = readMessage(ois); //< блокирующая операция
                if (m != null
                &&  m.getOpCode() == CMD_STATE
                &&  (data = m.getData()) instanceof DeviceState)
                {
                    newState = (DeviceState) data;
                    ok = true;
                }
            }
        }
        if (DEBUG && !ok) errprintf (
                    "\nClientHandler.requestClientState() : не удалось запросить state из УУ :" +
                    "\n\t** отправили: %s" +
                    "\n\t** получили: %s" +
                    "\n\t** data: %s.\n", mS, m, data);
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
        Message m = null; Message mQ = new Message().setDeviceUUID (null);
        boolean ok = false;
        synchronized (messagingMonitor)
        {
            if (writeMessage (oos, mQ.setOpCode (opCodeQ).setData (dataQ)))
                ok = (m = readMessage(ois)) != null; //< блокирующая операция
        }
        if (DEBUG && !ok) errprintf (
                "\nClientHandler: requestClient() : не удалось запросить %s из УУ (%s) :" +
                "\n\t** отправили: %s" +
                "\n\t** получили: %s" +
                "\n\t** data отпр.: %s" +
                "\n\t** data прин.: %s.\n",
                opCodeQ, deviceFriendlyName, mQ, m, dataQ,
                m != null ? m.getData().toString() : "");
        return m;
    }

/** Юзер может присвоить УУ удобное название. Этот метод имеет смысл использовать только на стороне УД. */
    public boolean setDeviceFriendlyName (String name)
    {
        boolean ok = isStringsValid (name);
        if (ok)
            deviceFriendlyName = name;
        return ok;
    }

    public String getDeviceFriendlyName () { return deviceFriendlyName; }

/** Обрабатываем состояние CMD_ERROR: деактивируем УУ и сбрасываем очередь задач пришедших от УД. */
    private void treatErrorState ()
    {
        state.setActive (NOT_ACTIVE);
        priorityQueue.clear();
    }

    //void f () {        ;    }
//---------------------------------------------------------------------------

}
