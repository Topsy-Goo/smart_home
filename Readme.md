# Описание приложения Умный дом

## Уровень абстракции
Умный дом это — итоговый учебный проект-приложение, которое изображает вёб-интерфейс некоего электронного устройства, которое работает непрерывно в режиме 24/7 и контролирует и координирует работу подключенных к нему разнообразных умных устройств (УУ). 

Также предположим, что:
* не уточняется, каким образом реализовано подключение УУ к УД. Известно лишь, что УД может подключать ограниченное количество устройств и предоставляет пользователю возможность централизованно управлять подключенными УУ из вёб-интерфейса. УД поддерживает «горячее подключение» устройств;
* УУ могут управляться независимо от УД, т.с. автономно, используя собственные элементы управления, недоступные из УД;
* УД и подключаемые УУ реализуют некий стандарт, позволяющий им «понимать» друг друга;
* настройки, которые пользователь делает в УД, сохраняются в энергонезависимой памяти УД, роль которой выполняет база данных.

> ### Следует иметь в виду, что
> приложение не эмулирует работу умных устройств, — модули УУ лишь следуют протоколу работы с УД. (В отладочном режиме модулям устройств дана возможность получать несколько простых команд из консоли, чтобы немного компенсировать отсутствие управления со стороны.)

Для большей правдоподобности УД и умные устройства реализованы в различных модулях. Обязательным является запуск модуля УД. Остальные модули — модули УУ — запускаются на выполнение исключительно по желанию.

## Авторизация и регистрация
Работа пользователя с Умным домом начинается с регистрации пользователя. Приложение использует только одну пару логин — пароль; их умолчальные значения — admin / admin. Для смены логина и пароля пользователь должен указать ключ активации. После авторизации пользователь может переходить на другие страницы интерфейса.

![Страница регистрации и авторизации](screenshots\registration-and-authorization.png)

## Главная страница
На главной странице находятся панели устройств, сгруппированные по типам устройств. Панели содержат почти всю информацию об устройствах, которая доступна УД. Именно в панели устройства пользователь может активировать или деактивировать выбранное УУ. Там же он может дать устройству имя, что полезно, например, при использовании одинаковых устройств.

УУ, подключенное к УД, имеет статус обнаруженного устройства. Пользователь должен перевести его в статус активного УУ, чтобы получить возможность управлять устройством из вёб-интерфейса.

![Главная страница](screenshots\main-page.png)

## Запуск задач
Панель позволяет запускать задачи, которые УУ способно выполнять. Ход выполнения запущенной задачи отображается в заголовке панели. Если тип задачи допускает её преждевременное прекращение, то пользователь может воспользоваться кнопкой «Остановить».

![Запуск задачи](screenshots\running-task.png)

## Планирование и расписание
Запуск задачи можно запланировать. Нажав кнопку «Запланировать», пользователь переходит на страницу расписания, где, указав время старта задачи, помещает её в список запланированных задач. После этого запланированная задача выполнится в указанное время, если соответствующее ей УУ будет активно. Время запуска можно изменить. Запланированную задачу можно удалить из расписания. Задачи с истекшим сроком запуска удаляются из расписания автоматически.

![Планирование запуска задачи](screenshots\schedule-page.png)

## Датчики устройств
Некоторые устройства обладают датчиками. Назначение датчиков — сигнализировать о наступлении какого-либо состояния УУ или иного события. Датчики условно делятся по типу: 
* датчик движения, 
* датчик открывания,
* датчик протечки, и др.

Все они имеют три состояния:
* выключен,
* включен,
* тревожное состояние (состояние срабатывания).

![Управление датчиками](screenshots\sensors-testing.png)

Тревожное состояние длится несколько секунд. Продолжительность других состояний неограничена.

## Связывание устройств
Кроме расписания запуска задач, УД предоставляет пользователю ещё одну полезную возможность — связывание устройств в пары «ведущий — ведомый». Это, например, позволяет ведущему УУ использовать датчики ведомого УУ. Связывание двух устройств выполняется пользователем в панели ведущего УУ. Там же отображается информация об уже существующих связях этого ведущего УУ. Созданную ранее связь можно «разорвать» тут же в панели ведущего УУ.

![Связывание устройств](screenshots\devices-binding.png)

## Используемые технологии
Приложение написано на Java 11 с использованием Spring Boot v2.6.6. Для вёб-страниц кроме HTML использовались JavaScript, AngularJS и Bootstrap. Трансляция с USB-камеры стала возможна  благодаря SarXos Webcam Capture API. Эмуляция энергонезависимой памяти реализована при помощи MySQL data base.
