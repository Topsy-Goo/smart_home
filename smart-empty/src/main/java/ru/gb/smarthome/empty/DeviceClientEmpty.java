package ru.gb.smarthome.empty;

import org.jetbrains.annotations.NotNull;
import ru.gb.smarthome.common.PropertyManager;
import ru.gb.smarthome.common.exceptions.OutOfServiceException;
import ru.gb.smarthome.common.smart.IConsolReader;
import ru.gb.smarthome.common.smart.SmartDevice;
import ru.gb.smarthome.common.smart.enums.OperationCodes;
import ru.gb.smarthome.common.smart.enums.TaskStates;
import ru.gb.smarthome.common.smart.structures.Abilities;
import ru.gb.smarthome.common.smart.structures.DeviceState;
import ru.gb.smarthome.common.smart.structures.Message;
import ru.gb.smarthome.common.smart.structures.Task;

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
//import static ru.gb.smarthome.empty.FactoryEmpty.check;

public class DeviceClientEmpty extends SmartDevice implements IConsolReader
{
    private final PropertyManager propMan;
    private       OperationCodes  mode = OPCODE_INITIAL;   //< временно будет изображать состояние УУ
    private final ExecutorService exeService;
    private final Thread          threadConsole;
    private       boolean safeTurnOff;
    private       Future<Boolean> taskFuture;
    private final Set<Task>       availableTasks;
    //private final boolean CAN_ABORT_UNINTERRUPTIBLE_TASK = true;
    //private final Map<String, Task> mapTasks;

    public DeviceClientEmpty (PropertyManager pm)
    {
        Random rnd  = new Random();
        propMan = pm;
        availableTasks = pm.getAvailableTasks_Fridge(); //pm.getTaskList_Empty(); //
        //mapTasks = new HashMap<> (listTasks.size() +1, 1.0F);
        abilities = new Abilities(
                SMART, "Учебное УУ №" + rnd.nextInt(100500),
                UUID.randomUUID(),
                new ArrayList<>(availableTasks),
                CAN_SLEEP);

        state = new DeviceState().setCode (CMD_READY).setActive(NOT_ACTIVE);

        //if (DEBUG) {
            threadConsole = new Thread (()->IConsolReader.runConsole (this));
            threadConsole.setDaemon(true);
            threadConsole.start();
        //}

        exeService = Executors.newSingleThreadExecutor (r->{
                            Thread t = new Thread (r);
                            t.setDaemon (true);
                            return t;
                        });
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
    private void disconnect() {}

/** Основной цикл клиента. */
    private void mainLoop () //throws InterruptedException
    {
        Message mR;
        try
        {   while (!threadRun.isInterrupted())
            {
                if ((mR = readMessage (ois)) == null) { //< блокирующая операция
                    if (DEBUG)
                        throw new RuntimeException ("Неправильный тип считанного объекта.");
                    sendState(); //< (Метод умеет обрабатывать m == null.)
                    continue;
                }

                final OperationCodes opCode = mR.getOpCode();

            //Если код текущего состояния УУ имеет более высокий приоритет по отношению к коду запроса,
            // то не обрабатываем запрос, а лишь посылаем в ответ state, который покажет вызывающему
            // положение дел:
                if (state.getCode().greaterThan (opCode)) {     //     opCode < state
                    sendState();
                    errprintf("\n\nEmpty.mainLoop(): несвоевременный запрос из УД: state:%s, Msg:%s\n\n",
                              state.getCode().name(), opCode.name());
                    continue;
                }

                checkSpecialStates();                           //     state < opCode

            //(Запросы в switch для удобства выстроены в порядке увеличения их приоритета, хотя приоритет
            // здесь не обрабатывается.)
                switch (opCode)
                {
            //------ Запросы, требующие обновления статуса ------------------------------

                    case CMD_READY:
                        if (CMD_READY.greaterThan (state.getCode()))
                            state.setCode (CMD_READY);
                        sendState();
                        break;

                    case CMD_SLEEP:
                        if (canSleepNow())
                            state.setCode (CMD_SLEEP);
                        sendState();
                        break;

                    case CMD_WAKEUP:
                        state.setCode (CMD_READY);
                        sendState();
                        break;

                    case CMD_TASK:
                    //запускаем задачу и составляем state для информирования вызывающего:
                        state.setCode (CMD_TASK)    //< временный условный код для информирования.
                             .setCurrentTask (startTask (mR.getData()));

                    //информируем и приводим state в порядок:
                        sendState();
                        if (taskFuture != null && !taskFuture.isDone()) //< если выполнение задачи займёт какое-то время, …
                            state.setCode (CMD_BUSY);                   //  …включим соотв.режим.
                        else {
                            state.setCurrentTask (null);  //< если задача не запустилась или уже закончилась, …
                        }                                 //  …обновим state соотв.образом. (Код state не меняем, каким бы он не был!)
                        break;

            //----- Необрабатываемые запросы. Отвечаем на них формально -----------------

                    //(Неподдерживаемые запросы рекомендуется именно игнорировать, чтобы не усложнять
                    // механизм вычисления приортетов для текущей реализации УУ. Лучшее решение —
                    // обрабатывать такие запросы как запрос CMD_STATE.)

                    case CMD_PAIR:  //< если УУ это не поддерживает, то игнорируем и просто отдаём статус.

                    case CMD_BUSY:  //< Этот запрос вообще не должен приходить. Игнорируем запрос
                        sendState();//  (вместо возни с обработкой этого недоразумения).
                        break;

            //------ Простые запросы (можно не вычислять приоритет) ---------------------

            //У этих запросов не только высокий приоритет, но и очень простое выполнение: мы отправляем
            // вызывающему заранее заготовленные данные. Поэтому их легко выполнять параллельно с другими
            // операциями.

                    case CMD_STATE:     //< приходит очень часто. Первый запрос является частью
                        sendState();    //  инициализации хэндлера нашего УУ (после него хэндлер добавляется
                        break;          //  в список обнаруженых УУ).

                    case CMD_ABILITIES:   //< обычно приходит 1 — раз сразу после подключения. Первый запрос
                        sendAbilities();  //  является частью инициализации хэндлера нашего УУ.
                        break;

            //------ Не требуют ответа --------------------------------------------------

                    case CMD_CONNECTED: println ("\nПодключен."); //< приходит из УД при подключении, когда
                        // соединение можно считать состоявшимся. В этот момент хэндлер нашего УУ ещё не
                        // полностью инициализирован (см. case CMD_ABILITIES и case CMD_STATE).
                        //...
                        break;

                    case CMD_NOPORTS:   //< приходит из УД при подкючении, когда все порты оказались заняты.
                        throw new OutOfServiceException("!!! Отказано в подключении, — нет свободных портов. !!!");

                    case CMD_ERROR: //< считаем, что это сообщение не может придти извне, и устанавливается
                        break;      //  по усмотрению самого УУ. Однако его можно установить из консоли.
                                    //  Установку статуса CMD_ERROR планируется сопровождать заданием спец.
                                    //  кода ошибки и описания/расшифровки этого спец.кода.

                    case CMD_EXIT:      //< вызывается только из консоли.
                        //threadRun.interrupt();
                        break;

                    default: if (DEBUG)
                                 throw new UnsupportedOperationException ("Неизвестный код операции: "+ opCode.name());
                             else sendState();
                }
            }//while
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
            state.setCode(CMD_SLEEP);
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
        if (DEBUG) printf ("\nОтправили %s\n", m);
        if (!ok) throw new OutOfServiceException (format ("\nНе удалось отправить сообщение : %s.\n", m));
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
        Message m = new Message().setOpCode (CMD_STATE)
                                 .setData (state.copy())
                                 .setDeviceUUID (abilities.getUuid());
        boolean ok = writeMessage (oos, m);
        if (DEBUG) printf ("\nОтправили %s\n", m);
        if (!ok)
            throw new OutOfServiceException (format ("\nНе удалось отправить сообщение : %s.\n", m));

        clearTemporaryStates();
    }

/** Сбрасываем некоторые особые сотсояния на что-нибудь попроще.
Эти особые состояния устанавливаются на период до ближайшей отправки
state в УД, а потом должны сбрасываться (на что-то), т.к. не являются
полноценными состояними. */
    private void clearTemporaryStates () {
        if (state.getCode().equals(CMD_TASK)) {
            state.setCode (CMD_READY).setCurrentTask (null);
        }
        else if (state.getCode().equals(CMD_PAIR)) {
            //...
        }
    }

/** Проверяем состояние некоторых особых состояний.
Сейчас метод реагирует на state.code == CMD_BUSY — проверяет, не завершилась ли задача. */
    private void checkSpecialStates () throws ExecutionException, InterruptedException
    {
        OperationCodes statecode = state.getCode();
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
                state.setCode (CMD_TASK); //< это уйдёт при первой же отаправке state. Подробности
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
    private @NotNull Task startTask (Object data) throws Exception
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

            if (state.getCode().equals (CMD_ERROR)) {
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
                TaskExecutor te = new TaskExecutor (newTask = t.copy());
                taskFuture = exeService.submit (te);
            }
        }
        if (newTask == null) //< не удалось запустиь задачу (составляем Task, пригодный только для информирования)
            newTask = new Task (taskName, tstate, taskMessage);
        return newTask;
    }

/** Этот класс изображает запущенную на исполнение задачу. Всё, что он делает, — это
отсчёт времени до окончания задачи. По истечении этого времени call() "возвращает true.
Если во время «операции» произошла одибка, то call() возвращает false.
<p>
    Во время выполнения задачи в структуру state должны вноситься минимальные изменения,
чтобы было легче синхронизировать их с передачей state в УД. Сейчас эти изменения
следующие:<br>
• конструктор класса изменяет state.code на CMD_BUSY;<br>
• в state.currentTask поля remained и elapsed обновляются так, чтобы они отображали
прогресс операции.<br>
<p>
Если задача завершилась ошибкой (Task.tstate == TS_ERROR), то мы НЕ считаем это
ошибкой всего УУ и не устанавливаем state.code == CMD_ERROR.
 */
    class TaskExecutor implements Callable<Boolean>
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

/*    private void applyTaskResult (*//*Future<> *//*) {

    }*/

    //TODO: Если УУ продолжает работать даже если связь с УД прервалась, то нужно это
    // как-то оформить (сделать консольный поток НЕдемоном, сделать поток executor-а недемоном,
    // применить св-о автономности и иначе реагировать на консольную команду /exit).
}
