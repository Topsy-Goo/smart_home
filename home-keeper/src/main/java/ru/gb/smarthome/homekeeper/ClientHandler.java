package ru.gb.smarthome.homekeeper;

import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.IDeviceServer;
import ru.gb.smarthome.common.exceptions.OutOfServiceException;
import ru.gb.smarthome.common.exceptions.RWCounterException;
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
    private final Object stateMonitor = new Object(); //< для синхронизации метода getState() с работой класса.
    private final Object abilitiesMonitor = new Object(); //< для синхронизации метода getAbilities() с работой класса.
    private       Port    port;
    private       String  deviceFriendlyName;
    private       SynchronousQueue<Boolean> helloSynQue;
    private       IDeviceServer server;
    private       int pollInterval = DEF_POLL_INTERVAL;

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

    public ClientHandler (Socket s, Port p, SynchronousQueue<Boolean> helloSQ, IDeviceServer srv)
    {
        if (DEBUG && (s == null || p == null))
            throw new IllegalArgumentException();
        port = p;
        socket = s;
        p.occupy (socket, this);
        helloSynQue = helloSQ;
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
print(" wMnp_");
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
            if (helloSynQue != null) helloSynQue.offer (ERROR);helloSynQue = null;  //< сообщаем в DeviceServerHome, что у нас не получилось начать работу.
            disconnect();
            Thread.yield(); //< возможно, это позволит вовремя вывести сообщение об ошибке.
            if (DEBUG) printf ("\nClientHandler: поток %s завершился. Код завершения: %s.\n", threadRun, code);
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
    private void mainLoop () throws Exception {
        Message mQ = new Message();
        Message mA;
        int len;

        rwCounter.set(0L);

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
        deviceFriendlyName = abilities.getVendorName(); //< потом юзер сможет это изменить.

        if (DEBUG)
            check(rwCounter.get() == 0L, RuntimeException.class, "блок mainLoop(){перед getIntoDetectedList()}");
        else
            return;

    //если первый контакт удался, переходим к стадии, выход из которой должен обрабатываться в cleanup().

        if (getIntoDetectedList())  //< это приведёт к добавлению нас в список обнаруженых УУ.
        try {
    //Всё, что происходит дальше, происходит с обнаруженным устройством, т.е. теперь о нас знаеют объекты, которые могут вызывать наши public-методы.
            while (!threadRun.isInterrupted())
            try
            {
            //Пауза между проверками состояния УУ и очереди запросов:
                TimeUnit.SECONDS.sleep (pollInterval);

            //Проверяем, соблюдение условия: «Клиент только отвечает на наши запросы», —
            // вычитываем из стрима всё, что клиент прислал без спроса, и «выбрасываем»:
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
            // очередь сбрасывается и устанавливается state.active == NOT_ACTIVE):
                    if (!state.getOpCode().equals (CMD_ERROR))
                        dispatchHadlerTaskQueue(); //< выбираем из очереди задачи и обрабатываем их, если их приоритет
                    else                       //  больше приоритета state.code.
                        treatErrorState();
                }
                check (rwCounter.get() == 0L, RWCounterException.class, "блок mainLoop.while"); //< общая проверка остаётся в чистовой версии.
            }
            catch (RWCounterException rwe) {
                if (DEBUG) {
                    errprintf ("\n[rwCounter:%d][%s]\n", rwCounter.get(), rwe.getMessage());
                    rwCounter.set(0L); //< не знаем, как ещё можно обработать.
                }
                else throw new OutOfServiceException("Нарушение протокола обмена данными.", rwe);
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
            stCode = state.getOpCode();
            if (opCode.greaterThan (stCode))
            {
        //Если мы попали в dispatchTaskQueue(), то ошибки в УУ нет.
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

                    case CMD_ABILITIES: if (!updateAbilities()) throw new OutOfServiceException ();
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

                                if (state.getOpCode().equals (CMD_ERROR))
                                    treatErrorState();
                            } //else {}
                        }
                        if (!ok) throw new OutOfServiceException (
                            format("\n****** ClientHandler: на запрос \n\t%s\n****** пришёл ответ\n\t%s\n", peekedMsg, mR));
                }
            } else break; //< остальные будут ждать, когда приоритет state понизится.
            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок readHadlerTaskQueue.while "+ opCode.name());
        }//while try
    }

/** делаем запрос и ждём в ответ DeviceState с подробностями. Присланый DeviceState не
применяем к нашему state, — нам его прислали только для информирования.
@param peekedMsg сообщение, извлечённое из очереди запросов хэндлера. */
    private void treatTaskRequest (Message peekedMsg) {
        Message mR;
        Object obj;
        Task task;
        DeviceState dState;

        if ((mR = requestClient (peekedMsg)) != null
        &&  (obj = mR.getData()) instanceof DeviceState
        &&  (dState = (DeviceState) obj).getOpCode().equals (CMD_TASK)
        &&  (task = dState.getCurrentTask()) != null)
        {
            server.requestCompleted (this, peekedMsg, task);
            updateState(); //< получаем CMD_BUSY и применяем его к нашему state.
        }
        else {server.requestError (this, peekedMsg);}
    }

//---------------------- Реализации методов --------------------------------

    @Override public Abilities getAbilities () {
        synchronized (abilitiesMonitor) {
            return abilities;
        }
    }

    @Override public @NotNull DeviceState getState () {
        synchronized (stateMonitor) {
            return state.safeCopy();
        }
    }

    @Override public void offerRequest (Message mRequest) {
        if (mRequest != null)
            priorityQueue.offer(mRequest);
    }

    @Override public boolean activate (final boolean value)
    {
        if (state.isActive() == value)
            return true;

        updateState();
        if (state.getOpCode().equals (CMD_ERROR)) {
            state.setActive (NOT_ACTIVE);
        }
        else if (state.isActive() == ACTIVE) {
//TODO: нужно проверить, можно ли УУ деактивировать прямо сейчас.
            Task t = state.getCurrentTask();
            if (t == null  ||  t.isAutonomic())
                state.setActive (NOT_ACTIVE);
        }
        else {
            /*state = requestClientState (NOT_ACTIVE)*/
            state.setActive (ACTIVE);
        }
//printf ("\n%s->%s\n", value ? "acitivate":"deactivate", state.isActive() ? "acitive":"notActive");
printf("\n%s : %s\n", deviceFriendlyName, state);
        return state.isActive() == value;
    }

    @Override public void setPollInterval (int seconds) {
        if (seconds > 0)  pollInterval = seconds;
    }

    @Override public boolean setDeviceFriendlyName (String name) {
        boolean ok = isStringsValid (name);
        if (ok)
            deviceFriendlyName = name;
        return ok;
    }

    @Override public String getDeviceFriendlyName () { return deviceFriendlyName; }

    @Override public String toString () {
        return format ("Handler[«%s»,\n\t%s,\n\tstate:\t%s]"
                       ,deviceFriendlyName
                       ,abilities
                       ,state
                       );
    } //< для отладки

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
        Message m = null; Message mA = new Message (CMD_ABILITIES, null, null);
        boolean ok = false;
        Object data = null;
        //synchronized (messagingMonitor)
        {
            if (writeMessage (oos, mA.setData(null))) {
print(" wMa_");
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
            newState = new DeviceState().setOpCode(CMD_ERROR);

        if (newState.getOpCode().equals (CMD_ERROR))
            currentActiveState = NOT_ACTIVE;

        state = newState.setActive (currentActiveState);
        return ok;
    }

/** Запрашиваем state у нашего подопечного УУ. (Планируем вызывать этот метод очень часто.)
@return DeviceState, полученный от клиента, или NULL, если запрос не удался. */
    private DeviceState requestClientState ()
    {
        DeviceState newState = null;
        Message mW = new Message (CMD_STATE, null, null);
        Message mR = null;
        boolean ok = false;
        Object data = null;
        //synchronized (messagingMonitor)
        {
            if (writeMessage (oos, mW.setData(null))) {
print(" wMs_");
                mR = readMessage(ois); //< блокирующая операция

                if (mR != null
                &&  mR.getOpCode() == CMD_STATE
                &&  (data = mR.getData()) instanceof DeviceState)
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
        Message mQ = new Message().setDeviceUUID (null);
        Message mA = null;
        boolean sent = writeMessage (oos, mQ.setOpCode (opCodeQ).setData (dataQ));
print(" wMr_");
        if (sent)
            mA = readMessage(ois); //< блокирующая операция

        if (DEBUG && mA == null) errprintf (
                "\nClientHandler: requestClient() : не удалось запросить %s из УУ (%s) :" +
                "\n\t** отправили: %s" +
                "\n\t** получили: %s" +
                "\n\t** data отпр.: %s.\n",
                opCodeQ, deviceFriendlyName,
                sent ? mQ : "(отправка не состоялась)",
                mA, dataQ);
        return mA;
    }

/** Обрабатываем состояние CMD_ERROR: деактивируем УУ и сбрасываем очередь задач пришедших от УД. */
    private void treatErrorState () throws Exception
    {
        state.setActive (NOT_ACTIVE);
        priorityQueue.clear();
        if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок treatErrorState");
    }

    //void f () {        ;    }
//---------------------------------------------------------------------------

}
