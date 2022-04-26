package ru.gb.smarthome.empty;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.gb.smarthome.common.exceptions.OutOfServiceException;
import ru.gb.smarthome.common.exceptions.RWCounterException;
import ru.gb.smarthome.common.smart.IConsolReader;
import ru.gb.smarthome.common.smart.SmartDevice;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.enums.TaskStates;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.DeviceState;
import ru.gb.smarthome.common.smart.structures.Message;
import ru.gb.smarthome.common.smart.structures.Task;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static ru.gb.smarthome.common.FactoryCommon.*;
import static ru.gb.smarthome.common.smart.enums.DeviceTypes.SMART;
import static ru.gb.smarthome.common.smart.enums.OperationCodes.*;
import static ru.gb.smarthome.common.smart.enums.TaskStates.*;
import static ru.gb.smarthome.empty.EmptyApp.DEBUG;

@Component
@Scope ("prototype")
public class DeviceClientEmpty extends SmartDevice implements IConsolReader
{
    protected final PropertyManagerEmpty propMan;
    //private         boolean safeTurnOff;

    /** Спонсор потока текущей задачи. */
    protected       ExecutorService exeService;
    /** Фьючерс окончания текущей задачи. */
    private         Future<Boolean> taskFuture;
    /** состояние, к которому нужно вернуться по окончании текущей задачи. */
    private         OperationCodes opCodeToReturnTo;


    @Autowired
    public DeviceClientEmpty (PropertyManagerEmpty pm) {
        propMan = pm;
        state = new DeviceState().setOpCode(CMD_NOT_CONNECTED).setActive(NOT_ACTIVE);
    }

    @PostConstruct public void init ()
    {
        abilities = new Abilities(
                SMART,
                propMan.getName(),
                propMan.getUuid(),
                new ArrayList<>(propMan.getAvailableTasks()),
                CAN_SLEEP);
        exeService = Executors.newSingleThreadExecutor (r->{
                            Thread t = new Thread (r);
                            t.setDaemon (true);
                            return t;
                        });
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
            {   //блок readMessage
                if ((mR = readMessage (ois)) == null) { //< блокирующая операция
                    if (DEBUG)
                        throw new RuntimeException ("Неправильный тип считанного объекта.");
                    sendState(); //< (Метод умеет обрабатывать m == null.)

                    check (rwCounter.get() == 0L, RWCounterException.class, "блок readMessage");
                    continue;
                }

                final OperationCodes opCode = mR.getOpCode();

            //Если код текущего состояния УУ имеет более высокий приоритет по отношению к коду запроса,
            // то не обрабатываем запрос, а лишь посылаем в ответ state, который покажет вызывающему
            // положение дел:
                //блок state greaterThan opCode
                if (state.getOpCode().greaterThan (opCode)) {     //     opCode < state
                    sendState();
                    errprintf("\n\n%s.mainLoop(): несвоевременный запрос из УД: state:%s, Msg:%s\n\n",
                              getClass().getSimpleName(), state.getOpCode().name(), opCode.name());

                    check (rwCounter.get() == 0L, RWCounterException.class, "блок state greaterThan opCode");
                    continue;
                }

                checkSpecialStates();                           //     state < opCode

            //(Запросы в switch для удобства выстроены в порядке увеличения их приоритета, хотя приоритет
            // здесь не обрабатывается.)
            //На необрабатываемые запросы игнорируем — обрабатываем их как запрос CMD_STATE.

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

                    case CMD_PAIR:  //< не поддерживается.
                        sendState();
                        if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_PAIR");
                        break;

                    case CMD_TASK:
                    //пробуем запустить задачу:
                        Task t = startTask (mR.getData());
                        if (t == errTask) {
                            sendData (CMD_TASK, t.safeCopy());
                        }
                        else {
                            opCodeToReturnTo = state.getOpCode();
                    //Если задача создана и запущена, то изменяем state и отвечаем хэндлеру только что созданной Task:
                            state.setOpCode (CMD_TASK)  //< временный условный код для информирования.
                                 .setCurrentTask (t);
                            sendData (CMD_TASK, t);
                            state.setOpCode (CMD_BUSY); //< теперь включим соотв. состояние до завершения задачи.
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

                    case CMD_EXIT:      //< вызывается только из консоли.
                        //threadRun.interrupt();
                        state.setOpCode(CMD_EXIT);
                        if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_EXIT");
                        break;

                    default: {
                            if (DEBUG)
                                 throw new UnsupportedOperationException ("Неизвестный код операции: "+ opCode.name());
                            else sendState();
                        }
                        if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок default");
                }
                check (rwCounter.get() == 0L, RWCounterException.class, "блок switch"); //< общая проверка остаётся в чистовой версии.
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
        catch (Exception e)    {  e.printStackTrace();  }
        finally {
            cleanup();
            println ("\nВыход из mainLoop().");
        }
    }

/** Очистка в конце работы метода mainLoop(). */
    private void cleanup ()
    {
        //«… previously submitted tasks are executed, but no new tasks will be accepted.…»
        if (exeService != null) exeService.shutdown();
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
    private void sendAbilities () throws OutOfServiceException
    {
        Message m = new Message().setOpCode (CMD_ABILITIES)
                                 .setData (abilities.copy()) //< см.комментарий в sendState().
                                 .setDeviceUUID (abilities.getUuid());
        boolean ok = writeMessage (oos, m);
//print("_wMa ");
//if (DEBUG && ok) printf ("\nОтправили %s\n", m);
        if (!ok)
            throw new OutOfServiceException (format ("\nНе удалось отправить сообщение : %s.\n", m));
    }

/** Отправлем в УД наш {@code state}.<p>
 При изменении этого метода следует помнить, что он используется как умолчальное, ни к чему
 не обязывающее действие для некоторых операций. */
    private void sendState () throws OutOfServiceException
    {
    /*  Делаем копию state и отдаём её в УД. Отсылать оригинал state нельзя,
        т.к. в УД приходит старое состояние. Наверное, что-то кэширует ссылку на него
        и, если ссылка не меняется, то в УД уходит старая версия. Стоит только начать
        отправлять копии, как в УД начинают приходить актуальные данные.
            Похожая ситуация происходит в УД: когда для отправки использовался один и
        тот же экземпляр Message, но с обновлёнными полями, — в УУ приходили устаревшие
        данные. Стоило начать перед отправкой создавать новый экземпляр, как данные,
        приходящие в УУ, стали актуальными.
    */
        sendData (CMD_STATE, state.safeCopy());
/*        Message mS = new Message().setOpCode (CMD_STATE)
                                  .setData (state.safeCopy())
                                  .setDeviceUUID (abilities.getUuid());
        boolean ok = writeMessage (oos, mS);
        if (!ok)
            throw new OutOfServiceException (format ("\nНе удалось отправить сообщение : %s.\n", mS));*/
    }

    private void sendData (OperationCodes opCode, Object data) throws OutOfServiceException
    {
        Message mS = new Message().setOpCode (opCode)
                                  .setData (data)
                                  .setDeviceUUID (abilities.getUuid());
        boolean ok = writeMessage (oos, mS);
//print("_wMs ");
//if (DEBUG && ok) printf ("\nОтправили %s\n", mS);
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
//print("_wMc ");
//if (DEBUG && ok) printf ("\nОтправили %s\n", mS);
        if (!ok)
            throw new OutOfServiceException (format ("\nНе удалось отправить код : %s.\n", opCode.name()));
    }

/** Проверяем состояние некоторых особых состояний.
Сейчас метод реагирует на state.code == CMD_BUSY — проверяет, не завершилась ли задача. */
    private void checkSpecialStates () throws ExecutionException, InterruptedException
    {
        OperationCodes statecode = state.getOpCode();
        if ( ! statecode.greaterThan (CMD_BUSY))
        {
        //Если мы здесь, то sate.opCode <= BUSY.

        //Если приоритет текущего состояния выше CMD_BUSY, то завершение задачи не
        // обрабатываем, даже если задача уже давно завершена, — ждём, когда приоритет понизится
        // хотя бы до CMD_BUSY. Это упрощение позволит передать в УД результат выполнение задачи,
        // когда это никому не будет мешать.
        //    В качестве побочного эффекта, это решение запрещает нам обрабатывать завершение
        // задачи в аварийной ситуации, т.е. когда state.code == CMD_ERROR.

            if (taskFuture != null && taskFuture.isDone())  // задача звершилась сама или была отменена (canceled)
            {
//boolean success = taskFuture.get(); //TODO: бросается исключениями, если задача завершилсь ненормально.
                //boolean success = !taskFuture.isCancelled() < если taskFuture имеет тип Future<?>.
                taskFuture = null;
                OperationCodes c = (opCodeToReturnTo == null) ? CMD_READY : opCodeToReturnTo;
                opCodeToReturnTo = null;
                state.setOpCode (c); //< это уйдёт при первой же отаправке state. Выполненная
                                     //  задача останется в state.currentTask.
println("");
                //Task t = state.getCurrentTask();
                //if (t != null)
                //    t.setMessage (format ("Задача «%s» завершена.", t.getName()));
            }
        }
    }

//------------ Методы, используемые в IConsoleReader.runConsole -------------

    @Override public Socket getSocket () { return socket; }
    @Override public DeviceState getState () { return state; }
    @Override public Abilities getAbilities () { return abilities; }
    @Override public boolean isDebugMode () { return DEBUG; }
    @Override public void enterErrorState (String errCode)
    {
        boolean setError = errCode != null;
        if (setError) {
            state.setOpCode(CMD_ERROR).setErrCode(errCode);

            if (taskFuture != null) //< пусть при ошибке задача прерывается назависимо от флага Task.interruptible
                taskFuture.cancel(true);
        }
        else {
            state.setOpCode(CMD_READY).setErrCode(null);
/*            if (taskFuture != null) {
                println("\n* "+ taskFuture);
                println("\n* "+ opCodeToReturnTo);
                println("\n* "+ state);
            }*/
        }

    }

//---------------------------------------------------------------------------

/** Используется для ошибок. */
    static final Task errTask = new Task (DEF_TASK_NAME, DEF_TASK_STATE, DEF_TASK_MESSAGE);

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
        Task t = abilities.getTasks()
                          .stream()
                          .filter ((tsk)->(tsk.equals (data)))
                          .findFirst()
                          .orElse (null);
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
                taskFuture = exeService.submit (te);
            }
        }
        if (newTask == null)
            //Если не удалось запустиь задачу, то заполняем errTask, специально используеый для информирования об ошибках.
            newTask = errTask.setName (taskName).setTstate (errorTState).setMessage (taskErrorMessage);
        return newTask;
    }

/** Этот класс изображает запущенную на исполнение задачу. Всё, что он делает, — это
отсчёт времени до окончания задачи. По истечении этого времени call() возвращает true.
Если во время «операции» произошла одибка, то call() возвращает false.
<p>
    Во время выполнения задачи в структуру state должны вноситься минимальные изменения,
чтобы было легче синхронизировать их с передачей state в УД. Нельзя заменять структуры, например,
state, state.currentTask, но можно изменять их Atomic-поля. Сейчас эти изменения следующие:<br>
• в state.currentTask меняем Atomic-поля remained и elapsed — они обновляются так, чтобы отображать
 прогресс операции.<br>
 • ;<br>
 • ;<br>
 <p>
Если задача завершилась ошибкой (Task.tstate == TS_ERROR), то мы НЕ считаем это
ошибкой всего УУ и не устанавливаем state.code == CMD_ERROR.
 */
    static class TaskExecutor implements Callable<Boolean>
    {
        private final Task theTask;
        private final String taskName;

        public TaskExecutor (@NotNull Task newTask) {
            theTask = newTask.setTstate (TS_LAUNCHING)
                             .setMessage (format ("Запускается задача «%s».", newTask.getName()));
            taskName = newTask.getName();
        }

        @Override public Boolean call ()
        {
            if (DEBUG) printf("\nНачинает работать задача: %s.", theTask);

            theTask.setTstate(TS_RUNNING).setMessage(format ("Выполняется задача «%s»", taskName));
            boolean ok = false;
            try {
                AtomicLong reminder = theTask.getRemained();
                while (reminder.get() > 0L) {
                    TimeUnit.SECONDS.sleep(1);
                    theTask.tick (1);
                }
                ok = reminder.get() == 0L;
                theTask.setTstate (ok ? TS_DONE : TS_ERROR);
            }
            catch (Exception e) {
                ok = (e instanceof InterruptedException  &&  theTask.isInterruptible());
                if (ok)
                    theTask.setTstate (TS_INTERRUPTED).setMessage (format("Задача «%s» прервана.", taskName));
                else
                    theTask.setTstate (TS_ERROR).setMessage (format("Задача «%s» прервана из-за ошибки «%s».",
                                                             taskName, e.getMessage()));
            }
            finally {
                if (DEBUG) printf("\nЗавершилась задача: %s.", theTask);
            }
            return ok;
        }
    }//class TaskExecutor

    //TODO: Если УУ продолжает работать даже если связь с УД прервалась, то нужно это
    // как-то оформить (сделать консольный поток НЕдемоном, сделать поток executor-а недемоном,
    // применить св-о автономности и иначе реагировать на консольную команду /exit).
}
