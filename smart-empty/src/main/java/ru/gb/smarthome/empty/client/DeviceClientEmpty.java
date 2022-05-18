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
    private         Future<Boolean> taskFuture;

/** StatesManager заботится о правильном перелючении состояний УУ. */
    private   final StatesManager   statesManager = new StatesManager();

/** Периодичность, скоторой УД (хэндлер) опрашивает состояние этого УУ. */
    private   final int pollInterval = DEF_POLL_INTERVAL_BACK;


    @Autowired
    public DeviceClientEmpty (PropertyManagerEmpty pm) {
        propMan = pm;
        state = new DeviceState();
        overideCurrentState (CMD_NOT_CONNECTED);
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
        overideCurrentState (CMD_NOT_CONNECTED);
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
        Task t;
        rwCounter.set(0L);
        try
        {   while (!threadRun.isInterrupted())
            {
                final Message mR = readMessage (ois); //< блокирующая операция
                synchronized (consoleMonitor)
                {
                    checkSpecialStates();
                    if (!isMessageValid (mR))   //mR;
                        continue;

                    final OperationCodes opCode = mR.getOpCode();
                    if (!checkRequestPriority (opCode))
                        continue;

                //(Запросы в switch для удобства выстроены в порядке увеличения их приоритета, хотя приоритет
                // здесь не обрабатывается.)
                //На необрабатываемые запросы игнорируем — обрабатываем их как запрос CMD_STATE.
//TODO: сделать обработчики для case-ов, когда код switch-а устоится.
                    switch (opCode)
                    {
                        /*case CMD_READY: //< не реализовано.
                            sendCode (CMD_ERROR);
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_READY");
                            break;

                        case CMD_SLEEP: //< не реализовано.
                            if (canSleepNow()) {
                                statesManager.sleepOn();
                                sendCode (CMD_SLEEP);
                            }
                            else sendCode (CMD_ERROR);
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_SLEEP");
                            break;

                        case CMD_WAKEUP: //< не реализовано.
                            if (state.getOpCode().equals(CMD_SLEEP)) {
                                statesManager.sleepOff();
                                sendCode (CMD_WAKEUP);
                            }
                            else sendCode (CMD_ERROR);
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_WAKEUP");
                            break;*/

                        case CMD_TASK:  onCmdTask (mR.getData());
                            break;

                        /*case CMD_BUSY:  //< не должно приходить.
                            sendState();
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_BUSY");
                            break;*/

                        case CMD_INTERRUPT:
                            t = state.getCurrentTask();
                            if (t == null)
                                sendData (CMD_INTERRUPT, null);
                            else {
                                //if (!t.getTstate().canReplace)
                                interruptCurrentTask();
                                sendData (CMD_INTERRUPT, t.safeCopy());
                            }
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
                            // состоявшимся. В этот момент хэндлер нашего УУ ещё не полностью инициализирован, — ждём
                            // первых запросов CMD_ABILITIES и CMD_STATE.
                            overideCurrentState (CMD_READY);
                            sendCode (CMD_READY);
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_CONNECTED");
                            break;

                        case CMD_NOPORTS:   //< приходит из УД при подкючении, когда все порты оказались заняты.
                            overideCurrentState (CMD_NOT_CONNECTED);
                            //rwCounter.decrementAndGet(); //< поскольку мы не должны отвечать на это сообщение.
                            //if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_NOPORTS");
                            throw new OutOfServiceException("!!! Отказано в подключении, — нет свободных портов. !!!");
                            //break;

                        case CMD_EXIT:   //< вызывается только из консоли; здесь обработчик присутствует на всякий случай.
                            crExit();
                            break;

                        default: {
                                if (DEBUG)
                                     throw new UnsupportedOperationException ("Неизвестный код операции: "+ opCode.name());
                                sendCode (CMD_ERROR);
                            }
                            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок default");
                    }
                    check (rwCounter.get() == 0L, RWCounterException.class, "блок switch");
                }//synchronized (consoleMonitor)
            }//while try
        }
        catch (RWCounterException rwe) {
            errprintf ("\n[rwCounter:%d][%s]\n", rwCounter.get(), rwe.getMessage());
        }
        catch (OutOfServiceException e) {  println ("\n" + e.getMessage());  }
        //catch (InterruptedException e) { throw new InterruptedException (e.getMessage()); }
        catch (Exception e) {  e.printStackTrace();  }
        finally {
            cleanup();
            println ("\nВыход из mainLoop().");
        }
    }//mainLoop()

/** Очистка в конце работы метода mainLoop(). */
    protected void cleanup () {
        //«… previously submitted tasks are executed, but no new tasks will be accepted.…»
        if (taskExecutorService != null) taskExecutorService.shutdown();
    }

/** Разгружаем код метода mainLoop(), перенося всякие разные проверки в методы с говорящими названиями.<p>
 Метод вызывает sendState(), если проверка провалилась. */
    private boolean isMessageValid (Message mR) throws Exception
    {
        boolean ok = mR != null;
        if (!ok) {
            if (DEBUG) throw new RuntimeException ("Неправильный тип считанного объекта.");
            sendCode (CMD_ERROR);
            if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок readMessage");
        }
        return ok;
    }

/** Продолжаем разгружать код метода mainLoop(): сравниваем приоритет кода состояния УУ с приоритетом кода
 запроса. Если тест не пройден, то вызываем sendState(), как заведено протоколом.
 @param opCode Код запроса.
 @return TRUE если приоритет запроса не выше приоритета состояния. */
    private boolean checkRequestPriority (OperationCodes opCode) throws Exception
    {
/*  Если код текущего состояния УУ имеет более высокий приоритет по отношению к коду запроса, то не
 обрабатываем запрос, а лишь посылаем в ответ state, который покажет вызывающему положение дел.
    Если у запроса приоритет совпадает с текущим кодом сотсояния, то обрабатываем запрос — на месте
 разберёмся, как быть, т.к. некоторые команды могут выполняться параллельно, а некоторые — нет.
*/
        boolean ok = !state.getOpCode().greaterThan (opCode);
        if (!ok) {
            sendCode (CMD_ERROR);
            if (DEBUG) {
                errprintf("\n\n%s.mainLoop(): несвоевременный запрос из УД: state:%s, Msg:%s\n\n",
                                 getClass().getSimpleName(), state.getOpCode().name(), opCode.name());
                check (rwCounter.get() == 0L, RWCounterException.class, "блок state greaterThan opCode");
            }
        }
        return ok;
    }
//---------------------------------------------------------------------------

/** Определяем возможность перехода в режим энергосбережения (в режим сна). */
    private boolean canSleepNow() {
        return abilities.isCanSleep();
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
/*  Делаем копию state и отдаём её в УД. Отсылать оригинал state почему-то нельзя, — в УД приходит старое
 состояние. Наверное, что-то кэширует ссылку на него. Стоит только начать отправлять копии, как в УД начинают
 приходить актуальные данные. Это не зависит от final-модификатора отправляемого объекта.
    Похожая ситуация происходит в УД: когда для отправки использовался один и тот же экземпляр Message, но
 с обновлёнными полями, — в УУ приходили устаревшие данные. Стоило начать перед отправкой создавать новый
 экземпляр, как данные, приходящие в УУ, стали актуальными.
    Позже оказалось, что копии тоже не всякие годятся. Например, передавать Collections.unmodifiableMap (sensors)
 тоже нельзя.
    Я что-то пропустил в учебной программе? Или Object(I/O)Stream писали вечером в пятницу?
*/
        DeviceState copy = state.safeCopy();
        sendData (CMD_STATE, copy);
    }

    private void sendData (OperationCodes opCode, Object data) throws OutOfServiceException
    {
        Message mS = new Message().setOpCode (opCode)
                                  .setData (data)/*
                                  .setDeviceUUID (abilities.getUuid())*/;
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
        {   */
            if (taskFuture != null && taskFuture.isDone()) // задача звершилась сама или была отменена (canceled)
                onTaskEndOrInterrupted();
        //}
    }

    protected void onTaskEndOrInterrupted () {
        taskFuture = null;
        statesManager.taskEnds(); // (Выполненная задача останется в state.currentTask.)
    }

//------------ Методы, используемые в IConsoleReader.runConsole -------------

    @Override public boolean crIsDebugMode ()    { return DEBUG; }  //+

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
            overideCurrentState (CMD_EXIT);
            threadRun.interrupt();
        }
    } //+

    @Override public void crEnterErrorState (String errCode)  //+
    {
        boolean setError = errCode != null;
        if (DEBUG)
        synchronized (consoleMonitor) {
            if (state.getOpCode().greaterThan (CMD_ERROR))
                println("Невозможно выполнить команду сейчас."); //< объяснение предоставить print(state), вызванный ниже.
            else
            if (setError) {
                statesManager.errorOn ();
                state.setErrCode(errCode);
                if (taskFuture != null && !taskFuture.isDone())
                    taskFuture.cancel(true);
            }
            else {
                statesManager.errorOff();
                state.setErrCode(null);
            }
            lnprintln (state.toString());
        }
    }

    @Override public void crExecuteTask (String taskname)   //+
    {
        if (DEBUG)
        synchronized (consoleMonitor)
        {
            if (!state.getOpCode().lesserThan (CMD_BUSY))
                // расчёт на то, что коды в интервале (BUSY; WAKEUP] являются командами, а не состояниями, т.е.
                // выполняются почти мгновенно, а устанавливаются в потоке кдиента на короткий период (из конслои
                // их застать нельзя).
                println("Невозможно запустить задачу сейчас.");
            else {
                //Повторяем поведение обработчика команды CMD_TASK, но без информирования хэндлера.
                Task t = getTaskToStart(taskname);
                if (t == errorTask)
                    println("Не удалось запустить задачу: " + errorTask);
                else {
                    statesManager.taskStarts();
                    state.setCurrentTask (t);
            }   }
            lnprintln (state.toString());
        }
    }

//-------------------------- про задачи -------------------------------------

    private void onCmdTask (Object data) throws Exception
    {
        Task newTask = getTaskToStart (data);
        if (newTask == errorTask) {
            sendData (CMD_TASK, newTask.safeCopy());
        }
        else {
            TaskExecutor te = launchTask (newTask);
            taskFuture = taskExecutorService.submit (te);
            //Если задача создана и запущена, то изменяем state и отвечаем хэндлеру только что созданной Task:
            statesManager.taskStarts();
            state.setCurrentTask (newTask);
            sendData (CMD_TASK, newTask);
        }
        if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_TASK");
    }

    protected TaskExecutor launchTask (Task t) {
        return new TaskExecutor (t);
    }

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
    private @NotNull Task getTaskToStart (Object data)
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
            else if (currentTask != null && !currentTask.getTstate().canReplace) {
        //если текущая задача находится в состоянии, НЕ допускающем запуск параллельной задачи:
                taskErrorMessage = format("Не завершена задача: «%s».", currentTask.getName());
            }
            else if (taskFuture != null) {
        //если почему-либо предыдущая задача не завершена (такого не может быть):
                taskErrorMessage = "Ошибка запуска.";
            }
            else {
        //если задачу можно создать и запустить:
                newTask = t.safeCopy();
            }
        /*  Тут не учитывается, что текущая задача может быть прервана для запуска другой задачи.
        Такую проверку делать здесь не стоит, например, потому, что нарушается «зона
        ответственности».  */
        }
        if (newTask == null)
            //Если не удалось запустиь задачу, то заполняем errTask, специально используеый для
            // информирования об ошибках.
            newTask = errorTask.setName (taskName).setTstate (errorTState).setMessage (taskErrorMessage);
        return newTask;
    }

/** Пытаемся остановить текущую задачу.
 @return TRUE, если задачу удалост остановить.<br>
         FALSE, если задачу не удалост остановить, или нет текущей задачи. */
    @SuppressWarnings("all")
    private boolean interruptCurrentTask () throws InterruptedException
    {
        boolean ok = false;
        Task tcurrent = state.getCurrentTask();
        if (tcurrent != null)
        {
            TaskStates tstate = tcurrent.getTstate();
            if (tstate.canReplace)
                ok = true;
            else if (tstate.runningState && tcurrent.isInterruptible())
            {
                if (taskFuture != null) {
                    if (!taskFuture.isDone()) {
                        taskFuture.cancel (true);
                        while (!taskFuture.isDone()); //taskFuture.get() даст исключение CancellationException
                    }
                    onTaskEndOrInterrupted();
                    //taskFuture = null;
                    //statesManager.taskEnds();

                    TimeUnit.MILLISECONDS.sleep(500); //< даём параллельному потоку возможность
                    // обработать завершение задачи, чтобы его результаты попали в state. (Вызова
                    // yield() оказалось недостаточно.)
                    tcurrent.setTstate (TS_INTERRUPTED);
                    ok = true;
                }
                else if (DEBUG) throw new RuntimeException("Несовместимые условия: tstate.runningState + taskFuture == null");
            }
        }
        return ok;
    }

//-------------------------- про датчики ------------------------------------

/** Вычисляет новое допустимое состояние и, если вычисленное состояние отличается от текущего,
 то применяет его к датчику — записывает в state.sensors. Такая проверка нужна для того, чтобы,
 например, не запустить тревожное состояние датчика, который уже находится в тревожном состоянии.
 @param senUuid UUID датчика, состояние которого нужно изменить.
 @param to Состояние, которое нужно установить у датчика. (Датчик может уже находиться в этом состоянии.)
 @return SensorStates, если состояние было изменено, или NULL, если состояние осталось прежним.
*/
    private SensorStates changeSensorState (UUID senUuid, SensorStates to)
    {
        Map<UUID, SensorStates> sensors = state.getSensors();
        SensorStates from    = sensors.get (senUuid),
                     newStat = SensorStates.calcState (from, to);

        if (!from.equals (newStat))
            sensors.put (senUuid, newStat);
        else
            newStat = null;
        return newStat;
    }

/** Обработчик команды CMD_SENSOR. Эта команда приходит с фронта для проверки работоспособности
 датчика.
 @param data должен являться экземпляром Sensor, по образцу которого нужно изменить
 соответствующий датчик УУ. Сейчас у этого Sensor рассматриваются только поля uuid и sstate.
 @return Sensor датчика, где поле sstate соответствует текущему состоянию датчика. Также можно
 вернуть NULL, чтобы вызывающему методу было проще определить, что состояние датчика не
 было изменено. */
    protected Sensor onCmdSensor (Object data)
    {
        try {
            Sensor sensor = sensorFromObject (data);
            UUID senUuid = sensor.getUuid();

            SensorStates newStat = changeSensorState (senUuid, sensor.getSstate());
            if (newStat != null)
            {
                sensor.setSstate (newStat);
                if (newStat.alarm)
                    alarmForAWhile (senUuid);
            }
//lnprintln ("onCmdSensor() - "+ sensor.toString());
            return sensor;
        }
        catch (Exception e) {  return null;  }
    }

/** Выключаем тревожное состояние датчика спустя указаный промежуток времени.
 @param uuid UUID датчика, который нужно вернуть из тревожного состояния. */
    protected void alarmForAWhile (UUID uuid)
    {
        int millis = Math.max (7000, pollInterval +(pollInterval/2));
        Thread tr = new Thread (()->{
            try {
//lnprintln("alarmForAWhile() - начат отчсёт для датчика: "+ uuid +"; state.sensors: "+ state.getSensors());
                //statesManager.sensorAlarmIsOn();
                TimeUnit.MILLISECONDS.sleep (millis);
            }
            catch (InterruptedException e) { lnerrprintln (e.getMessage()); }
            finally {
                if (state.getSensors().get(uuid).on)
                    state.getSensors().put(uuid, SST_ON);
                //statesManager.sensorAlarmOff();
//lnprintln("alarmForAWhile() - закончен отчсёт для датчика: "+ uuid+ "; state.sensors: "+ state.getSensors());
//println (state.toString());
            }});
        tr.setDaemon (true);
        tr.start();
    }

/** В state создаём список, посредством которого мы сможем передавать хэндлеру актуальную информацию
 о состоянии датчиков. */
    protected void initSensors ()
    {
        state.setSensors (new HashMap<>(sensorsNumber));
        Map<UUID, SensorStates> sensors = state.getSensors();

        for (Sensor s : abilities.getSensors())
            sensors.put (s.getUuid(), s.getSstate());
    }

//---------------------------------------------------------------------------

//TODO: все вызовы этого метода, выполняемые вне класса StatesManager, нужно заменить на вызовы
// методов StatesManager.
    private void overideCurrentState (@NotNull OperationCodes opCode)
    {
        state.setOpCode (opCode);
    }

/** Класс заботится о правильном перелючении состояний УУ.<p>
 Состояния УУ сменяют друг друга, и
 иногда, выходя из состояния с высоким приоритетом, бывает желательно или необходимо вернуться
 к состоянию с низким приоритетом, в кором УУ находилось прежде. StatesManager делает это
 возможным независимо от того, сколько состояний наложились друг на друга. */
    class StatesManager
    {
/** Стек (список) запомненных кодов состояний. Какое-либо временное событие имеющее приотет, в
 момент своего начала должно поместить свой код OperationCodes в начало этого списка (на вершину стека),
 а по завершении забрать код из списка (стека).<p>
 В обоих случаях событие должно использовать соответствущие методы класса StatesManager, т.к.
 statesStack не совсем стек. */
        private final LinkedList<OperationCodes> statesStack = new LinkedList<>();

//TODO: некоторые методы похожи.

/** Запоминаем состояние УУ, в котором задача запускается, и устанавливаем состояние CMD_BUSY. */
        void taskStarts () {
            statesStack.addFirst (state.getOpCode());
            overideCurrentState (CMD_BUSY);
//lnprintf("StatesManager.taskStarts() сделала: стек = %s, state.opCode = %s.\n", statesStack.toString(), state.getOpCode());
        }

/** Обновляем состояние УУ по завершении задачи.<p>
 Извлекаем из стека самый ближний к вершине код, не превышающий CMD_BUSY. Если приоритет кода
 состояния УУ также не превышает CMD_BUSY, то меняем его на код, извлечённый из стека. */
        void taskEnds ()
        {
            OperationCodes opCode = popOpCodeLike (CMD_BUSY);
            if (opCode != null && !state.getOpCode().greaterThan (CMD_BUSY))
            {
                overideCurrentState(opCode);
            }
//lnprintf("StatesManager.taskEnds() сделала: стек = %s, state.opCode = %s.\n", statesStack.toString(), state.getOpCode());
        }

/** Запоминаем состояние УУ, в котором сработал датчик, и устанавливаем состояние CMD_ERROR. */
        void errorOn () {
            statesStack.addFirst (state.getOpCode());
            overideCurrentState (CMD_ERROR);
//lnprintf("StatesManager.errorOn() сделала: стек = %s, state.opCode = %s.\n", statesStack.toString(), state.getOpCode());
        }

        void sleepOn () {}
        void sleepOff () {}

/** Восстанавливаем состояние УУ после состояния ошибки.<p>
 Извлекаем из стека самый ближний к вершине код, не превышающий CMD_ERROR. Если приоритет кода
 состояния УУ также не превышает CMD_ERROR, то меняем его на код, извлечённый из стека. */
        void errorOff () {
            OperationCodes opCode = popOpCodeLike (CMD_ERROR);
            if (opCode != null && !state.getOpCode().greaterThan (CMD_ERROR))
            {
                overideCurrentState(opCode);
            }
//lnprintf("StatesManager.errorOff() сделала: стек = %s, state.opCode = %s.\n\n", statesStack.toString(), state.getOpCode());
        }

/** Идём от вершины стека (от начала списка {@code statesStack}) и ищем код, приоритет которого не
 превышает приоритет образца, извлекаем найденый код из стека и отдаём его в вызывающую ф-цию.
 @param example Образец, код состояния, на приоритет которого нам нужно ориентироваться во
 время поисков.
 @return Извлечённый из стека код, или NULL, если в стеке не нашёлся код, подходящий под example. */
        private OperationCodes popOpCodeLike (OperationCodes example)
        {
            for (OperationCodes opCode : statesStack)
                if (!opCode.greaterThan (example))
                {
                    statesStack.remove (opCode);
//lnprintf("\nStatesManager.popOpCodeLike(%s) удаляет из стека opCode = %s.", example, opCode);
//lnprintf("StatesManager.popOpCodeLike(%s) сделала: стек = %s, state.opCode = %s.\n", example, statesStack.toString(), state.getOpCode());
                    return opCode;
                }
            if (DEBUG) throw new RuntimeException ("В стеке отсутствует код уровня "+ example.name());
            return null;
        }
    }//class StatesManager
}
