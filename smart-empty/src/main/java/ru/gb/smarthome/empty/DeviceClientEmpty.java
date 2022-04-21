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
    private final PropertyManagerEmpty propMan;
    private final ExecutorService exeService;
    private       boolean safeTurnOff;
    private       Future<Boolean> taskFuture;
    private final Set<Task>       availableTasks;

    @Autowired
    public DeviceClientEmpty (PropertyManagerEmpty pm)
    {
        propMan = pm;
        exeService = Executors.newSingleThreadExecutor (r->{
                            Thread t = new Thread (r);
                            t.setDaemon (true);
                            return t;
                        });
        availableTasks = new HashSet<>();
    }

    @PostConstruct public void init ()
    {
        Random rnd  = new Random();
        abilities = new Abilities(
                SMART, "Учебное УУ №" + rnd.nextInt(100500),
                UUID.randomUUID(),
                new ArrayList<>(availableTasks),
                CAN_SLEEP);
        state = new DeviceState().setOpCode(CMD_NOT_CONNECTED).setActive(NOT_ACTIVE);

        //if (DEBUG) {
            Thread threadConsole = new Thread (()->IConsolReader.runConsole (this));
            threadConsole.setDaemon(true);
            threadConsole.start();
        //}
        availableTasks.addAll(propMan.getAvailableTasks_Empty());
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
                    "\noos : %s\n", abilities.getVendorName(), socket, !socket.isClosed(), ois, oos);
            mainLoop();
        }
        catch (Exception e) {
            code = e.getMessage();
            if (DEBUG) e.printStackTrace();
        }
        finally {
            disconnect();
            Thread.yield(); //< возможно, это позволит вовремя вывести сообщение об ошибке.
            if (DEBUG) printf ("\nПоток %s завершился. Код завершения: %s.\n", threadRun, code);
        }
    }

    private Socket connect () throws IOException
    {
        String address = propMan.getServerAddress();
        int port       = propMan.getServerSocketPort();

        socket = new Socket (address, port);
        //(Стримы нельзя здесь получать, — если они взбрыкнут, то метод не вернёт socket, и мы не
        // сможем его закрыть в try(…){}.)
        if (DEBUG)
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
                    errprintf("\n\nEmpty.mainLoop(): несвоевременный запрос из УД: state:%s, Msg:%s\n\n",
                              state.getOpCode().name(), opCode.name());

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

                    case CMD_TASK:
                    //запускаем задачу и составляем state для информирования вызывающего:
                        state.setOpCode(CMD_TASK)    //< временный условный код для информирования.
                             .setCurrentTask (startTask (mR.getData()));

                    //информируем и приводим state в порядок:
                        sendState();
                        if (taskFuture != null && !taskFuture.isDone()) //< если выполнение задачи займёт какое-то время, …
                            state.setOpCode(CMD_BUSY);                   //  …включим соотв.режим.
                        else {
                            state.setCurrentTask (null);  //< если задача не запустилась или уже закончилась, …
                        }                                 //  …обновим state соотв.образом. (Код state не меняем, каким бы он не был!)
                        if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_TASK");
                        break;

                    case CMD_PAIR:  //< не поддерживается.
                        sendState();
                        if (DEBUG) check (rwCounter.get() == 0L, RWCounterException.class, "блок case CMD_PAIR");
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

                    case CMD_NOT_CONNECTED:
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
print("_wMa ");
//if (DEBUG && ok) printf ("\nОтправили %s\n", m);
        if (!ok)
            throw new OutOfServiceException (format ("\nНе удалось отправить сообщение : %s.\n", m));
    }

/** Отправлем в УД наш {@code state}.<p>
При изменении этого метода следует помнить, что он используется как умолчальное, ни к чему не обязывающее действие для некоторых операций. */
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
        Message mS = new Message().setOpCode (CMD_STATE)
                                 .setData (state.safeCopy())
                                 .setDeviceUUID (abilities.getUuid());
        boolean ok = writeMessage (oos, mS);
print("_wMs ");
//if (DEBUG && ok) printf ("\nОтправили %s\n", mS);
        if (!ok)
            throw new OutOfServiceException (format ("\nНе удалось отправить сообщение : %s.\n", mS));
        clearTemporaryStates();
    }

/** Отправляем в УД сообщение, которое содержит только указанный OperationCodes. Другая полезная
нагрузка в сообщении отсутствует.
@param opCode код, который нужно отправить в УД. */
    private void sendCode (OperationCodes opCode) throws OutOfServiceException
    {
        Message mS = new Message().setOpCode (opCode);
        boolean ok = writeMessage (oos, mS);
print("_wMc ");
//if (DEBUG && ok) printf ("\nОтправили %s\n", mS);
        if (!ok)
            throw new OutOfServiceException (format ("\nНе удалось отправить код : %s.\n", opCode.name()));
    }

/** Сбрасываем некоторые особые сотсояния на что-нибудь попроще.
Эти особые состояния устанавливаются на период до ближайшей отправки
state в УД, а потом должны сбрасываться (на что-то), т.к. не являются
полноценными состояними. */
    private void clearTemporaryStates ()
    {
        if (state.getOpCode().equals(CMD_TASK)) {
            state.setOpCode(CMD_READY).setCurrentTask (null);
        }
        //else if (state.getCode().equals(CMD_PAIR)) {/*...*/}
    }

/** Проверяем состояние некоторых особых состояний.
Сейчас метод реагирует на state.code == CMD_BUSY — проверяет, не завершилась ли задача. */
    private void checkSpecialStates () throws ExecutionException, InterruptedException
    {
        OperationCodes statecode = state.getOpCode();
        if ( ! statecode.greaterThan (CMD_BUSY))
        {
        //Если мы здесь, то sate == BUSY, а Message.opCode > CMD_BUSY (если УД работает правильно)

            //Если приоритет текущего состояния выше CMD_BUSY, то завершение задачи не
            // обрабатываем, даже если задача уже давно завершена, — ждём, когда приоритет понизится
            // хотя бы до CMD_BUSY. Это упрощение позволит передать в УД результат выполнение задачи,
            // когда это никому не будет мешать.
            //    В качестве побочного эффекта, это решение запрещает нам обрабатывать завершение
            // задачи в аварийной ситуации, т.е. когда state.code == CMD_ERROR.

            if (taskFuture != null && taskFuture.isDone()) { // задача звершилась сама или была отменена (canceled)
                boolean success = taskFuture.get();
                //boolean success = !taskFuture.isCancelled() < если taskFuture имеет тип Future<?>.
                taskFuture = null;
                state.setOpCode(CMD_TASK); //< это уйдёт при первой же отаправке state. Подробности
                    // выполнения задачи будут лежать в state.currentTask.
            }
        }
    }

//---------------------------------------------------------------------------

    @Override public Socket getSocket () { return socket; }
    @Override public DeviceState getState () { return state; }
    @Override public Abilities getAbilities () { return abilities; }

//---------------------------------------------------------------------------

/** Запуск указанной задачи. Запрос на запуск задачи будет проигнориован, если:<br>
• state.code == CMD_ERROR (эта проверка нужна на случай, если УД ещё не знает об ошике в нашем УУ. Если бы он знал, то не прислал бы CMD_TASK);<br>
• выполняется другая задача ();<br>
• УУ не нашло указанную задачу в своём списке (нет такой задачи, или передана пустая строка);<br>
• передан некорректный параметр (null или не String).
@param data строка-идентификатор задачи, для поиска её в списке доступных задач устройства.
@return экземпляр Task, который является запущенной задачей или, в случае ошибки, сгодится для информирования о результатах запроса.
*/
    private @NotNull Task startTask (Object data) //throws Exception
    {
        String taskName = "?",
               taskMessage = "";
        TaskStates tstate = TS_REJECTED;

        Task newTask = null;
        Task t = availableTasks.stream()
                               .filter ((tsk)->(tsk.getName().equals (data)))
                               .findFirst()
                               .orElse (null);
        if (t == null) {
            tstate = TS_NOT_SUPPORTED;      taskMessage = "Задача не найдена.";
        }
        else {
            Task currentTask = state.getCurrentTask();
            taskName = t.getName();

            if (state.getOpCode().equals (CMD_ERROR)) {
                taskMessage = "УУ неисправно.";
            }
            else if (currentTask != null && !currentTask.getTstate().get().canReplace) {
                taskMessage = format ("Выполняется задача «%s»", currentTask.getName());
            }
            else if (taskFuture != null) {
                taskMessage = "Ошибка запуска.";
            }
            else {
                tstate = TS_LAUNCHING;
                TaskExecutor te = new TaskExecutor(newTask = t.safeCopy());
                taskFuture = exeService.submit (te);
            }
        }
        if (newTask == null) //< не удалось запустиь задачу (составляем Task, пригодный только для информирования)
            newTask = new Task (taskName, tstate, taskMessage);
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

        public TaskExecutor (@NotNull Task t) {
            theTask = t.setTstate (TS_LAUNCHING);
            //state.setCurrentTask (theTask); < это делает обработчик CMD_TASK
        }

        @Override public Boolean call ()
        {
            theTask.setTstate(TS_RUNNING);
            boolean result = false;
            try {
                AtomicLong reminder = theTask.getRemained();
                while (reminder.get() > 0L) {
                    TimeUnit.SECONDS.sleep(1);
                    theTask.tick (1);
                }
                result = reminder.get() == 0L;
            }
            catch (InterruptedException e) {
                result = theTask.isInterruptible();
                theTask.setMessage ("Задача прервана");
            }
            catch (Exception e) {
                theTask.setMessage (e.getMessage());
            }
            finally {
                theTask.setTstate (result ? TS_DONE : TS_ERROR);
            }
            return result;
        }
    }//class TaskExecutor

/*    private void applyTaskResult (Future<> f) {    }*/

    //TODO: Если УУ продолжает работать даже если связь с УД прервалась, то нужно это
    // как-то оформить (сделать консольный поток НЕдемоном, сделать поток executor-а недемоном,
    // применить св-о автономности и иначе реагировать на консольную команду /exit).
}
