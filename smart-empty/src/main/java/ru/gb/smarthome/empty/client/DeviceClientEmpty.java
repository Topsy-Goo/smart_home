package ru.gb.smarthome.empty.client;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.IPropertyManager;
import ru.gb.smarthome.common.exceptions.OutOfServiceException;
import ru.gb.smarthome.common.exceptions.RWCounterException;
import ru.gb.smarthome.common.smart.IConsolReader;
import ru.gb.smarthome.common.smart.SmartDevice;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.enums.SensorStates;
import ru.gb.smarthome.common.smart.enums.TaskStates;
import ru.gb.smarthome.common.smart.structures.*;
import ru.gb.smarthome.empty.PropertyManagerEmpty;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.SMART;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.*;
import static ru.gb.smarthome.common.smart.enums.SensorStates.SST_ON;
import static ru.gb.smarthome.common.smart.enums.TaskStates.*;
import static ru.gb.smarthome.empty.EmptyApp.DEBUG;

@Component
@Scope ("prototype")
public class DeviceClientEmpty extends SmartDevice implements IConsolReader
{
    protected final IPropertyManager propMan;

/** Количество сенсоров на борту. */
    protected       int             sensorsNumber;

/** для синхронизации параллельного доступа к данным класса из методов IConsolReader. */
    private   final Object          consoleMonitor = new Object();

    /** Спонсор потока текущей задачи. */
    protected       ExecutorService taskExecutorService;

    /** Фьючерс окончания текущей задачи. */
    Future<Boolean> taskFuture;

/** StatesManager заботится о правильном перелючении состояний УУ. */
    private   final StatesManager   statesManager = new StatesManager();

/** Периодичность, скоторой УД (хэндлер) опрашивает состояние этого УУ. */
    private   final int pollInterval = DEF_POLL_INTERVAL_BACK;


    @Autowired
    public DeviceClientEmpty (PropertyManagerEmpty pm) {
        propMan = pm;
        state = new DeviceState().setOpCode(CMD_NOT_CONNECTED);
    }

    @PostConstruct public void init ()
    {
        abilities = new Abilities(
                SMART,
                propMan.getName(),
                propMan.getUuid(),
                CAN_SLEEP)
                .setTasks (propMan.getAvailableTasks())
                .setSensors (propMan.getAvailableSensors())
                .setSlaveTypes (propMan.slaveTypes())
                ;
        sensorsNumber = abilities.getSensors().size();

        taskExecutorService = Executors.newSingleThreadExecutor (r->{
                            Thread t = new Thread (r);
                            t.setDaemon (true);
                            return t;});
        //if (DEBUG) {
            Thread threadConsole = new Thread (()->IConsolReader.runConsole (this));
            threadConsole.setDaemon(true);
            threadConsole.start();
        //}
    }

    @Override public void run ()
    {
        threadRun = Thread.currentThread();
        String code = "OK";
        try (Socket socket = connect())
        {
            ois = new ObjectInputStream (socket.getInputStream());
            oos = new ObjectOutputStream (socket.getOutputStream());
            //Совершенно неожиданно оказалось, что одинаковые операции — две ObjectOutputStream или две ObjectInputStream — блокируют друг друга, кода вызываются на обоих концах канала. Поэтому, если на одном конце канала вызывается, например, new ObjectInputStream(…), то на другом нужно обязательно вызвать new ObjectOutputStream(…), чтобы не случилась взаимная блокировка.
            printf ("\nСоединение с сервером установлено (УУ:%s): "+
                    "\nsocket : %s (opend: %b)"+
                    "\nois : %s"+
                    "\noos : %s\n", abilities.getVendorString(), socket, !socket.isClosed(), ois, oos);
            mainLoop();
        }
        catch (Exception e) {
            code = e.getMessage();
            if (DEBUG) e.printStackTrace();
        }
        finally {
            disconnect();
            Thread.yield(); //< возможно, это позволит вовремя вывести сообщение об ошибке.
            printf ("\nПоток %s завершился. Код завершения: %s.\n", threadRun, code);
        }
    }

    private Socket connect () throws IOException
    {
        String address = propMan.getServerAddress();
        int port       = propMan.getServerSocketPort();

        socket = new Socket (address, port);
        //(Стримы нельзя здесь получать, — если они взбрыкнут, то метод не вернёт socket, и мы не
        // сможем его закрыть в try(…){}.)
        printf ("\nСоединение с сервером установлено (адрес:%s, порт:%d).\n", address, port);
        return socket;
    }

/** Очистка в конце работы метода run(). */
    private void disconnect() {
        state.setOpCode(CMD_NOT_CONNECTED);
    }

/** Основной цикл клиента. При разработке этого метода придерживаемся следующих принципов:<br>
 • чтение из стрима и запись в стрим делаются только из этого метода (необязательно);<br>
 • сначала выполняем чтение из стрима, а потом — запись в стрим, поскольку в клиенте мы работаем <u>только</u> в пассивном режиме (обязательно);<br>
 • причитав сообщение из стрима, нужно обязательно что-то отдать в стрим (обязательно). Для проверки этого условия введена переменная rwCounter;<br>
 • приоритеты входящих сообщений сравниваются с приортетом state и, если приоритет state выше,
 то метод не обрабатывает входящее сообщение, — происходит формальная обработка, цель которой — что-то оправить в стрим (необязательно, но удобно);<br>
 •  ();<br>
 •  ();<br>
*/
    private void mainLoop () //throws InterruptedException
    {
        Message mR;
        rwCounter.set(0L);
        try
        {   while (!threadRun.isInterrupted())
            try
            {   mR = readMessage (ois); //< блокирующая операция
                synchronized (consoleMonitor)
                {
                    checkSpecialStates();
                    if (mR == null) {
                        if (DEBUG) throw new RuntimeException ("Неправильный тип считанного объекта.");
                        sendState();
                        if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок readMessage");
                        continue;
                    }
                    final OperationCodes opCode = mR.getOpCode();

                //Если код текущего состояния УУ имеет более высокий приоритет по отношению к коду запроса, то не
                // обрабатываем запрос, а лишь посылаем в ответ state, который покажет вызывающему положение дел.
                //Если у запроса приоритет совпадает с текущим кодом сотсояния, то обрабатываем запрос — на месте
                // разберёмся, как быть, т.к. некоторые команды могут выполняться параллельно, а некоторые — нет.

                    if (state.getOpCode().greaterThan (opCode)) {     //     opCode < state
                        sendState();
                        if (DEBUG) errprintf("\n\n%s.mainLoop(): несвоевременный запрос из УД: state:%s, Msg:%s\n\n",
                                             getClass().getSimpleName(), state.getOpCode().name(), opCode.name());

                        if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок state greaterThan opCode");
                        continue;
                    }

                //(Запросы в switch для удобства выстроены в порядке увеличения их приоритета, хотя приоритет
                // здесь не обрабатывается.)
                //На необрабатываемые запросы игнорируем — обрабатываем их как запрос CMD_STATE.
//TODO: сделать обработчики для case-ов, когда код switch-а устоится.
                    switch (opCode)
                    {
                        case CMD_READY:
                            if (CMD_READY.greaterThan (state.getOpCode()))
                                state.setOpCode(CMD_READY);
                            sendState();
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_READY");
                            break;

                        case CMD_SLEEP:
                            if (canSleepNow())
                                state.setOpCode(CMD_SLEEP);
                            sendState();
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_SLEEP");
                            break;

                        case CMD_WAKEUP:
                            state.setOpCode(CMD_READY);
                            sendState();
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_WAKEUP");
                            break;

                        case CMD_TASK:
                            Task t = startTask (mR.getData());
                            if (t == errorTask) {
                                sendData (CMD_TASK, t.safeCopy());
                            }
                            else { //Если задача создана и запущена, то изменяем state и отвечаем хэндлеру только что созданной Task:
                                statesManager.taskStarts();
                                state.setCurrentTask (t);
                                sendData (CMD_TASK, t);
                            }
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_TASK");
                            break;

                        case CMD_BUSY:  //< не поддерживается.
                            sendState();
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_BUSY");
                            break;

                        case CMD_ERROR: //< не может придти извне (не требуется чтение), и устанавливается по усмотрению самого УУ.
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_ERROR");
                            break;

                        case CMD_SENSOR:
                            Sensor sen = onCmdSensor (mR.getData());
                            sendData (CMD_SENSOR, sen);
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_SENSOR");
                            break;

                        case CMD_STATE:     //< приходит очень часто. Первый запрос является частью инициализации хэндлера
                            sendState();    //  нашего УУ (после него хэндлер добавляется в список обнаруженых УУ).
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_STATE");
                            break;

                        case CMD_ABILITIES:   //< обычно приходит 1 — раз сразу после подключения. Первый запрос
                            sendAbilities();  //  является частью инициализации хэндлера нашего УУ.
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_ABILITIES");
                            break;

                        case CMD_NOT_CONNECTED: /* Это умолчальное состояние УУ, при котором соединение с
                            хэндлером отсутствует. Получение команды CMD_NOT_CONNECTED невозможно, и её
                            обработка здесь является заглушкой. */
                            sendCode (CMD_NOT_CONNECTED);
                            break;

                        case CMD_CONNECTED:
                            println ("\nПодключен."); //< приходит из УД при подключении, когда соединение можно считать
                            // состоявшимся. В этот момент хэндлер нашего УУ ещё не полностью инициализирован
                            // (см. case CMD_ABILITIES и case CMD_STATE).
                            state.setOpCode(CMD_READY);
                            sendCode (CMD_READY);
                            //rwCounter.decrementAndGet(); //< поскольку мы не должны отвечать на это сообщение.
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_CONNECTED");
                            break;

                        case CMD_NOPORTS:   //< приходит из УД при подкючении, когда все порты оказались заняты.
                            state.setOpCode(CMD_NOT_CONNECTED);
                            rwCounter.decrementAndGet(); //< поскольку мы не должны отвечать на это сообщение.
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_NOPORTS");
                            throw new OutOfServiceException("!!! Отказано в подключении, — нет свободных портов. !!!");
                            //break;

                        case CMD_EXIT:   //< вызывается только из консоли; здесь обработчик присутствует на всякий случай.
                            crExit();
                            //if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_EXIT");
                            break;

                        default: {
                                if (DEBUG)
                                     throw new UnsupportedOperationException ("Неизвестный код операции: "+ opCode.name());
                                else sendState();
                            }
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок default");
                    }
                    check (rwCounter.get() == 0L, RWCounterException.class, "блок switch"); //< общая проверка остаётся в чистовой версии.
                }//synchronized (consoleMonitor)
            }
            catch (RWCounterException rwe) {
                if (DEBUG) {
                    errprintf ("\n[rwCounter:%d][%s]\n", rwCounter.get(), rwe.getMessage());
                    if (rwCounter.get() < 0L)
                        rwCounter.set(0L);
                    while (rwCounter.get() > 0L) sendState();
                }
                else throw new OutOfServiceException("Нарушение протокола обмена данными.", rwe);
            }//while try
        }
        catch (OutOfServiceException e) {  println ("\n" + e.getMessage());  }
        //catch (InterruptedException e) { throw new InterruptedException (e.getMessage()); }
        catch (Exception e)    {  e.printStackTrace();  }
        finally {
            cleanup();
            println ("\nВыход из mainLoop().");
        }
    }//mainLoop()

/** Очистка в конце работы метода mainLoop(). */
    private void cleanup () {
        //«… previously submitted tasks are executed, but no new tasks will be accepted.…»
        if (taskExecutorService != null) taskExecutorService.shutdown();
    }

//---------------------------------------------------------------------------

/** Определяем возможность перехода в режим энергосбережения (в режим сна). */
    private boolean canSleepNow()
    {
    //Сперва проверяем, предусмотрена ли в УУ возможность перевода его в сон:
        boolean canSleep = abilities.isCanSleep();
        if (canSleep) {
            state.setOpCode(CMD_SLEEP);
        }
        return canSleep;
    }

/** Отправлем в УД нашу {@code abilities}. */
    private void sendAbilities () throws OutOfServiceException {
        sendData (CMD_ABILITIES, abilities.copy());
    }

/** Отправлем в УД наш {@code state}.<p>
 При изменении этого метода следует помнить, что он используется как умолчальное, ни к чему
 не обязывающее действие для некоторых операций. */
    private void sendState () throws OutOfServiceException
    {
    /*  Делаем копию state и отдаём её в УД. Отсылать оригинал state почему-то нельзя,
        — в УД приходит старое состояние. Наверное, что-то кэширует ссылку на него
        и, если ссылка не меняется, то в УД уходит старая версия. Стоит только начать
        отправлять копии, как в УД начинают приходить актуальные данные. Это поведение
        канала не зависит от final-модификатора отправляемого объекта.
            Похожая ситуация происходит в УД: когда для отправки использовался один и
        тот же экземпляр Message, но с обновлёнными полями, — в УУ приходили устаревшие
        данные. Стоило начать перед отправкой создавать новый экземпляр, как данные,
        приходящие в УУ, стали актуальными.
    */
        DeviceState copy = state.safeCopy();
        //lnprint("sendState() - отправлвет - "+ copy);
        sendData (CMD_STATE, copy);
    }

    private void sendData (OperationCodes opCode, Object data) throws OutOfServiceException
    {
        Message mS = new Message().setOpCode (opCode)
                                  .setData (data)
                                  .setDeviceUUID (abilities.getUuid());
        boolean ok = writeMessage (oos, mS);
        if (!ok)
            throw new OutOfServiceException (format ("\nНе удалось отправить сообщение: %s.\n", mS));
    }

/** Отправляем в УД сообщение, которое содержит только указанный OperationCodes. Другая полезная
нагрузка в сообщении отсутствует.
@param opCode код, который нужно отправить в УД. */
    private void sendCode (OperationCodes opCode) throws OutOfServiceException
    {
        Message mS = new Message().setOpCode (opCode);
        boolean ok = writeMessage (oos, mS);
        if (!ok)
            throw new OutOfServiceException (format ("\nНе удалось отправить код : %s.\n", opCode.name()));
    }

/** Проверяем некоторые особые состояния.
Сейчас метод реагирует на state.code == CMD_BUSY, т.е. проверяет, не завершилась ли задача. */
    private void checkSpecialStates ()
    {
/*
        OperationCodes statecode = state.getOpCode();
        if (statecode.equals (CMD_BUSY))  //Если мы здесь, то sate.opCode <= BUSY.

    Если приоритет текущего состояния выше CMD_BUSY, то завершение задачи не
    обрабатываем, даже если задача уже давно завершена, — ждём, когда приоритет понизится
    хотя бы до CMD_BUSY. Это упрощение позволит передать в УД результат выполнение задачи,
    когда это никому не будет мешать.
       В качестве побочного эффекта, это решение запрещает нам обрабатывать завершение
    задачи в аварийной ситуации, т.е. когда state.code == CMD_ERROR.
*/
        //{
            if (taskFuture != null && taskFuture.isDone()) // задача звершилась сама или была отменена (canceled)
            {
                taskFuture = null;
                statesManager.taskEnds(); // (Выполненная задача останется в state.currentTask.)
            }
        //}
    }

/* * Проверка состояний сенсоров. УУ, имеющий на борту сенсоры, должны переопределить
 этот метод, если они хотят сообщать в УД о состоянии этих сенсоров. * /
    protected void checkSensors() { }   */

//------------ Методы, используемые в IConsoleReader.runConsole -------------

    @Override public void crState () {
        if (DEBUG)
        synchronized (consoleMonitor) {
            lnprintln (state.toString());
        }
    }   //+

    @Override public void crAbilities () {
        if (DEBUG)
        synchronized (consoleMonitor) {
            lnprintln (abilities.toString());
        }
    }   //+

    @Override public void crExit () throws OutOfServiceException {
        if (DEBUG)
        synchronized (consoleMonitor) {
            state.setOpCode(CMD_EXIT);
            //throw new OutOfServiceException ("Выход из приложения по команде /exit.");
            //crGetSocket().close(); < это не нужно, — сокет закроется в run().
            threadRun.interrupt();
        }
    } //+

    @Override public boolean crIsDebugMode ()    { return DEBUG; }  //+

    @Override public void crEnterErrorState (String errCode)  //+
    {
        boolean setError = errCode != null;
        if (DEBUG)
        synchronized (consoleMonitor) {
            if (setError) {
                statesManager.errorOn ();
                state.setErrCode(errCode);
                if (taskFuture != null) //< пусть при ошибке задача прерывается назависимо от флага Task.interruptible
                    taskFuture.cancel(true);
            }
            else {
                statesManager.errorOff();
                state.setErrCode(null);
            }
            lnprintln (state.toString());
        }
    }

/*    @Override public void crEnterBusyState () {
        if (DEBUG)
        synchronized (consoleMonitor) {
            if (state.getOpCode().lesserThan (CMD_BUSY))
                statesManager.taskStarts();
                //state.setOpCode (CMD_BUSY);  //< расчёт на то, что коды в интервале (BUSY; WAKEUP] являются
                // командами, а не состояниями, т.е. выполняются почти мгновенно, а устанавливаются только
                // в потоке кдиента (из конслои их застать нельзя).
            else
                println("Невозможно установить CMD_BUSY.");
            lnprintln (state.toString());
        }
    }   //+*/

    @Override public void crEnterReadyState () {
        if (DEBUG)
        synchronized (consoleMonitor) {
            state.setErrCode(null)
                 .setOpCode(CMD_READY); //< считаем, что переход в этот режим сбрасывае ошибку.
            lnprintln (state.toString());
        }
    }   //+

    @Override public void crExecuteTask (String taskname) throws InterruptedException   //+
    {
        boolean justResetCurrentTaskToIdleState = taskname == null || taskname.isBlank();
        if (DEBUG)
        synchronized (consoleMonitor) {
            if (justResetCurrentTaskToIdleState)
            {
                if (state.getOpCode().greaterThan (CMD_BUSY))
                    println("Невозможно остановить задачу сейчас."); //< объяснение предоставить print(state), вызванный ниже.
                else {
                    Task tcurrent = state.getCurrentTask();
                    if (tcurrent != null) {
                        if (taskFuture != null) {
                            if (!taskFuture.isDone()) {
                                taskFuture.cancel(true);
                                while (!taskFuture.isDone());
                            }
                            taskFuture = null;
                            TimeUnit.MILLISECONDS.sleep(500); //< даём параллельному потоку возможность
                            // обработать завершение задачи, чтобы его результаты попали в state до вызова
                            // print (state), выполняемого ниже. (Вызова yield() оказалось недостаточно.)
                        }
                        else  //elapsed и remained пока не трогаем.
                            tcurrent.setTstate(TS_IDLE).setMessage("Задача остановлена из косоли.");
                    }
                    else println("Нет текущей задачи.");
                    statesManager.taskEnds();
                }
            }
            else if (!state.getOpCode().lesserThan (CMD_BUSY))
                // расчёт на то, что коды в интервале (BUSY; WAKEUP] являются командами, а не состояниями, т.е.
                // выполняются почти мгновенно, а устанавливаются в потоке кдиента на короткий период (из конслои
                // их застать нельзя).
                println("Невозможно запустить задачу сейчас.");
            else {
                //Повторяем поведение обработчика команды CMD_TASK, но без информирования хэндлера.
                Task t = startTask (taskname);
                if (t == errorTask)
                    println("Не удалось запустить задачу: " + errorTask);
                else {
                    statesManager.taskStarts();
                    state.setCurrentTask (t);
            }   }
            lnprintln (state.toString());
        }//synchronized
    }

//-------------------------- про задачи -------------------------------------

/** Используется для ошибок. */
    final Task errorTask = new Task (DEF_TASK_NAME, DEF_TASK_STATE, DEF_TASK_MESSAGE);

/** Запуск указанной задачи. Запрос на запуск задачи будет проигнориован, если:<br>
• state.code == CMD_ERROR (эта проверка нужна на случай, если УД ещё не знает об ошике в нашем УУ. Если бы он знал, то не прислал бы CMD_TASK);<br>
• выполняется другая задача ();<br>
• УУ не нашло указанную задачу в своём списке (нет такой задачи, или передана пустая строка);<br>
• передан некорректный параметр (null или не String).
@param data строка-идентификатор задачи, для поиска её в списке доступных задач устройства.
@return экземпляр Task, который является запущенной задачей или, в случае ошибки, сгодится для информирования о результатах запроса.
*/
    private @NotNull Task startTask (Object data)
    {
        String taskName = "?",
               taskErrorMessage = DEF_TASK_MESSAGE;
        TaskStates errorTState = TS_REJECTED;
        Task newTask = null;

        Task t = findTask (data);
        if (t == null) {
            errorTState = TS_NOT_SUPPORTED;      taskErrorMessage = "Задача не найдена.";
        }
        else {
            Task currentTask = state.getCurrentTask();
            taskName = t.getName();

            if (state.getOpCode().equals (CMD_ERROR)) {
        //если УУ находится в состоянии ошибки, то:
                taskErrorMessage = "УУ неисправно.";
            }
            else if (taskFuture != null) {
        //если почему-либо предыдущая задача не завершена (такого не может быть):
                taskErrorMessage = "Ошибка запуска.";
            }
            else if (currentTask != null && !currentTask.getTstate().get().canReplace) {
        //если текущая задача находится в состоянии, НЕ допускающем запуск параллельной задачи:
                taskErrorMessage = format("Задача не завершена: «%s».", currentTask.getName());
            }
            else {
        //если задачу можно создать и запустить:
                TaskExecutor te = new TaskExecutor (newTask = t.safeCopy());
                taskFuture = taskExecutorService.submit (te);
            }
        }
        if (newTask == null)
            //Если не удалось запустиь задачу, то заполняем errTask, специально используеый для информирования об ошибках.
            newTask = errorTask.setName (taskName).setTstate (errorTState).setMessage (taskErrorMessage);
        return newTask;
    }
//-------------------------- про датчики ------------------------------------

    private SensorStates changeSensorState (UUID senUuid, SensorStates to)
    {
        Map<UUID, SensorStates> sensors = state.getSensors();
        SensorStates from    = sensors.get (senUuid),
                     newStat = SensorStates.calcState (from, to);
        sensors.put (senUuid, newStat);
        return newStat;
    }

/** Обработчик команды CMD_SENSOR. Эту команд а приходит с фронта для проверки работоспособности
 датчика.
 @param data должен являться экземпляром Sensor, по образцу которого нужно изменить
 соответствующий датчик УУ. Сейчас у этого Sensor рассматриваются только поля uuid и stype.
 @return если запрос выполнен, то возвращаем такой же Sensor, какой был в запросе. В
 противном случае Sensor должен отличаться (например, содержать текущее состояние датчика, или
 просто быть равным NULL). */
    protected Sensor onCmdSensor (Object data)
    {
        try {
            Sensor sensor = sensorFromObject (data);
            UUID senUuid = sensor.getUuid();

            SensorStates newStat = changeSensorState (senUuid, sensor.getState());
            sensor.setState (newStat);

lnprintln ("onCmdSensor() - "+ sensor.toString());

            if (newStat.alarm)            //< Закомментировать вызов alarmForAWhile(), если кнопка Alarm test
                alarmForAWhile (senUuid); //  должна работать, как тригер. См.также sensorAlarmTest().
            return sensor;
        }
        catch (Exception e) {  return null;  }
    }

/** Выключаем тревожное состояние датчика спустя указаный промежуток времени.
 @param uuid UUID датчика, который нужно вернуть из тревожного состояния. */
    protected void alarmForAWhile (UUID uuid)
    {
        int millis = Math.max (15000, pollInterval +(pollInterval/2));
        Thread tr = new Thread (()->{
            try {
                statesManager.sensorAlarmIsOn();
                TimeUnit.MILLISECONDS.sleep (millis);
            }
            catch (InterruptedException e) { ; }
            finally {
                if (state.getSensors().get(uuid).on)
                    state.getSensors().put(uuid, SST_ON);
                statesManager.sensorAlarmOff();
//println (state.toString());
            }});
        tr.start();
    }

/** В state создаём список, посредством которого мы сможем передавать хэндлеру актуальную информацию
 о состоянии датчиков. */
    protected void initSensors ()
    {
        state.setSensors (new HashMap<>(sensorsNumber));
        Map<UUID, SensorStates> sensors = state.getSensors();

        for (Sensor s : abilities.getSensors())
            sensors.put (s.getUuid(), s.getState());
    }

/* * Проверяем состояния сенсоров и помещаем результаты проверки в state. Эта проверка
 выполняется на регулярной основе.
    protected void checkSensors() { } */

//---------------------------------------------------------------------------

/** Класс заботится о правильном перелючении состояний УУ. */
    class StatesManager
    {
/*      Когда срабатывает датчик, устанавливается состояние CMD_SENSOR.
        Когда запускается задача, хэндлер сравниветет приоритет текущего состояния с приоритетом CMD_TASK. Если приоритет состония выше, запуск задачи откладывается.
        Приортет состояния CMD_SENSOR больше приоритета команды CMD_TASK, поэтому задача не может быть запущена во время CMD_SENSOR.
        Если задачу запустить можно, то она запускается и устанавливается состояние CMD_BUSY.
        С другой стороны при работающей задаче датчик может сработать, т.к. приоритет CMD_SENSOR > CMD_BUSY.
        Следовательно, наложение состояний BUSY и SENSOR возможно только, когда датчик сработал во время выполнеия задачи.
        Перед включением состояний BUSY и SENSOR мы запоминаем код состояния, чтобы венуться к нему по выходе из этих состояний.
        Ввиду вышеперечисленного имеем два варианта разруливания ситуации с запомненным состоянием:
        1. состояние BUSY   закончилось до выхода из SENSOR;
        2. состояние SENSOR закончилось до выхода из BUSY.

        Поскльку состояния накладываются т.с. в порядке приоритета, то можно сделать так:
        * кадое состояние начинается с помещения в стек кода предыдущего состояния;
        * если какое-либо состояние закончилось, то оно извлекает из стека самый ближний к вершине код состояния, приоритет которого не превышает его собственный приоритет.
*/
/** Стек (список) запомненных кодов состояний. Какое-либо временное событие имеющее приотет, в
 момент своего начала должно поместить свой код OperationCodes в начало этого списка (на вершину стека),
 а по завершении забрать код из списка (стека).<p>
 В обоих случаях событие должно использовать соответствущие методы класса StatesManager, т.к.
 statesStack не совсем стек. */
        private final LinkedList<OperationCodes> statesStack = new LinkedList<>();

//TODO: большинство методов очень похожи. Возможно, их стоит как-то объединить.

/** Запоминаем состояние УУ, в котором задача запускается, и устанавливаем состояние CMD_BUSY. */
        void taskStarts () {
            statesStack.addFirst (state.getOpCode());
            state.setOpCode (CMD_BUSY);
lnprintf("StatesManager.taskStarts() сделала: стек = %s, state.opCode = %s.\n", statesStack.toString(), state.getOpCode());
        }

/** Обновляем состояние УУ по завершении задачи.<p>
 Извлекаем из стека самый ближний к вершине код, не превышающий CMD_BUSY. Если приоритет кода
 состояния УУ также не превышает CMD_BUSY, то меняем его на код, извлечённый из стека. */
        void taskEnds () {
            OperationCodes opCode = popOpCodeLike (CMD_BUSY);
            if (opCode != null && !state.getOpCode().greaterThan (CMD_BUSY))
            {
                resetDeviceStateTo (opCode);
            }
lnprintf("StatesManager.taskEnds() сделала: стек = %s, state.opCode = %s.\n", statesStack.toString(), state.getOpCode());
        }

/** Запоминаем состояние УУ, в котором сработал датчик, и устанавливаем состояние CMD_SENSOR.<p>
 Состояние самого датчика в этот момент уже изменено на alarm. */
        void sensorAlarmIsOn () {
            statesStack.addFirst (state.getOpCode());
            state.setOpCode (CMD_SENSOR);
lnprintf("StatesManager.sensorAlarmIsOn() сделала: стек = %s, state.opCode = %s.\n", statesStack.toString(), state.getOpCode());
        }

/** Восстанавливаем состояние УУ после тревожного состояния датчика.<p>
 Извлекаем из стека самый ближний к вершине код, не превышающий CMD_SENSOR. Если приоритет кода
 состояния УУ также не превышает CMD_SENSOR, то меняем его на код, извлечённый из стека.<p>
 Состояние самого датчика в этот момент уже изменено на ON или OFF. */
        void sensorAlarmOff () {
            OperationCodes opCode = popOpCodeLike (CMD_SENSOR);
            if (opCode != null && !state.getOpCode().greaterThan (CMD_SENSOR))
            {
                resetDeviceStateTo (opCode);
            }
lnprintf("sensorAlarmOff() сделала: стек = %s, state.opCode = %s.\n\n", statesStack.toString(), state.getOpCode());
        }

/** Запоминаем состояние УУ, в котором сработал датчик, и устанавливаем состояние CMD_ERROR. */
        void errorOn () {
            statesStack.addFirst (state.getOpCode());
            state.setOpCode (CMD_ERROR);
lnprintf("StatesManager.errorOn() сделала: стек = %s, state.opCode = %s.\n", statesStack.toString(), state.getOpCode());
        }

/** Восстанавливаем состояние УУ после состояния ошибки.<p>
 Извлекаем из стека самый ближний к вершине код, не превышающий CMD_ERROR. Если приоритет кода
 состояния УУ также не превышает CMD_ERROR, то меняем его на код, извлечённый из стека. */
        void errorOff () {
            OperationCodes opCode = popOpCodeLike (CMD_ERROR);
            if (opCode != null && !state.getOpCode().greaterThan (CMD_ERROR))
            {
                resetDeviceStateTo (opCode);
            }
lnprintf("StatesManager.errorOff() сделала: стек = %s, state.opCode = %s.\n\n", statesStack.toString(), state.getOpCode());
        }

/** Идём от вершины стека (от начала списка {@code statesStack}) и ищем код, приоритет которого не
 превышает приоритет образца, извлекаем найденый код из стека и отдаём его в вызывающую ф-цию.
 @param example Образец, код состояния, на приоритет которого нам нужно ориентироваться во
 время поисков.
 @return Извлечённый из стека код, или NULL, если в стеке не нашёлся код, подходящий под example. */
        private OperationCodes popOpCodeLike (OperationCodes example) {
/*            ListIterator<OperationCodes> iterator = statesStack.listIterator();
            while (iterator.hasNext())
            {
                OperationCodes code = iterator.next();
                if (!code.greaterThan (example))
                {
                    iterator.remove();
                    return code;
                }
            }*/
            for (OperationCodes opCode : statesStack)
                if (!opCode.greaterThan (example))
                {
                    statesStack.remove (opCode);
lnprintf("\nStatesManager.popOpCodeLike(%s) удаляет из стека opCode = %s.", example, opCode);
lnprintf("StatesManager.popOpCodeLike(%s) сделала: стек = %s, state.opCode = %s.\n", example, statesStack.toString(), state.getOpCode());
                    return opCode;
                }
            if (DEBUG) throw new RuntimeException ("В стеке отсутствует код уровня "+ example.name());
            return null;
        }

        private void resetDeviceStateTo (@NotNull OperationCodes opCode) {
            state.setOpCode (opCode); // это уйдёт при первой же отаправке state.
        }
    }
}
/*  stack
        READY   -t
        BUSY    -t
        BUSY    -s
        SENSOR  -s
        SENSOR  -s
    state
        SENSOR
*/