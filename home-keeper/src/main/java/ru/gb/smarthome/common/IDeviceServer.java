package ru.gb.smarthome.common;

import ru.gb.smarthome.common.smart.ISmartHandler;
import ru.gb.smarthome.common.smart.structures.Message;

public interface IDeviceServer extends Runnable {

/** Вызывается хэндлером, когда он завершает работу. */
    void goodBy (ISmartHandler device);

/** Хэндлер отчитывается о выполнении какого-то запроса. Этот метод хэндлер вызывает, если ему
 удалось отдать запрос в УУ и в ответ получить из УУ внятный результат (даже если этот результат
  — отчёт о невозможности выполнить запрошенную операцию).
 @param device УУ, вызвавшее этот метод.
 @param message сообщение, которое ранее было поставлено в очередь на обработку и которое УУ как-то обработало.
*/
    //void requestCompleted (ISmartHandler device, Message message, Object result);

/** Хэндлер сообщает, что не смог выполненить запрос. Этот метод хэндлер вызывает, если ему
 не удалось связаться с УУ, или данные, полученные от УУ, непригодны для использования в качестве
 результата (хэндлер не получил необходимые данные или не смог их интерпретировать).
 @param device УУ, вызвавшее этот метод.
 @param message сообщение, которое ранее было поставлено в очередь на обработку и которое не удалось обработать.
*/
    //void requestError (ISmartHandler device, Message message);
}
