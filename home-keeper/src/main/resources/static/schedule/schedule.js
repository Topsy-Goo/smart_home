
 angular.module('smarthome-front') //< приложение
		.controller('scheduleController', function ($scope, $rootScope, $http, $routeParams, $localStorage, $location)
{
	const contextMainPath 	  = 'http://localhost:15550/home/v1/main';
	const contextSchedulePath = 'http://localhost:15550/home/v1/schedule';
	const contextAuthoPath	  = 'http://localhost:15550/home/v1/auth';

	$scope.newRecord;
	$scope.schedule;
//-------------------------------------------------------------------------------- запуск
	$scope.startSchedulePage = function ()
	{
		if (!isUserLoggedIn()) {
			$location.path ('/registration'); //< выполняем переход на страницу регистрации
			return;
		}
console.log ('******** авторизован ********');
		resetNewRecord();
		clearInterval ($rootScope.stateTimer);
		$scope.loadSchedule();

		if ($routeParams.uuid == null) {
		}
		else {
console.log ('$scope.startSchedulePage() вызван. $routeParams: ', $routeParams);
			initNewRecord();	// $routeParams сохраняются после обновления страницы.
		}
	}

//Инициализируем $scope.newRecord для отображения в форме.
	initNewRecord = function ()
	{
		$scope.newRecord.id			= null;
		$scope.newRecord.deviceName = $routeParams.friendlyname;
		$scope.newRecord.taskName	= $routeParams.taskname;
		$scope.newRecord.uuid		= $routeParams.uuid

		let dateTime = new Date();
		dateTime.setMilliseconds(0);
		dateTime.setSeconds(0);
		$scope.newRecord.dateTime	= dateTime;
//		$scope.newRecord.dateTime	= dateTime.valueOf();
//		$scope.newRecord.state		= "";
//		$scope.newRecord.available	= ;

		//секунды и доли нужно обнулить:	setMilliseconds(0), setSeconds(0).
		//parse()   даёт значение для long. https://www.w3schools.com/jsref/jsref_parse.asp		//
		//getTime() даёт значение для long. https://www.w3schools.com/jsref/jsref_gettime.asp	//
		//valueOf() даёт значение для long. https://www.w3schools.com/jsref/jsref_valueof_date.asp	//
	}

//сбрасываем все поля $scope.newRecord в умолчальное состояние.
	resetNewRecord = function ()
	{
		$scope.newRecord = {
			"id"			: null,
			"deviceName"	: "",
			"uuid"			: "",
			"taskName"		: "",
			"dateTime"		: null,
			"dateTimeLong"	: 0,
			"available"		: false,	//< Указывает, подключено ли УУ. (На случай, когда в расписании
	//остались записи для УУ, которое сейчас отключено от УД — отсутствует среди обнаруженых устройств.)
			"state"			: ""
		};
	}

//Вызывается из формы при нажатии на кнопку Запомнить.
	$scope.createRecord = function (newRecord) //< TODO:параметр не нужен?
	{
		//$scope.newRecord.id - не трогаем, т.к. форма может быть заполнена данными из строки таблицы.
		//	(Если id корректный, то фронт расценит запрос как запрос на изменение данных.)

		$scope.newRecord.dateTimeLong = $scope.newRecord.dateTime.valueOf();
console.log ('$scope.createRecord() отправляет на бэк $scope.newRecord = ', $scope.newRecord);

		$http.post (contextSchedulePath + '/new_schedule_record', $scope.newRecord)
		.then (
		function successCallback (response)
		{
			console.log ('$scope.createRecord() получила ответ: ', response.data);
			if (response.data)
			{
				$scope.loadSchedule();
			}
			else $scope.getHomeNews(); //< расчитываем получить текст сообщения об ошибке.
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в $scope.createRecord(). Получен ответ: ', response.data);
		});
	}

//Обработчик нажатия на кнопку Сброс.
	$scope.resetNewRecordForm = function () {
		resetNewRecord();
	}

//Вызывается при старте и после добавления новой записи.
	$scope.loadSchedule = function ()
	{
console.log ('$scope.loadSchedule() вызван для загрузки расписания из бэка.');
		$http.get (contextSchedulePath + '/schedule')
		.then (
		function successCallback (response) {
			$scope.schedule = response.data;
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в $scope.loadSchedule() бэк вернул: ', response.data);
		});
	}

//-------------------------------------------------------------------------------- редактирование

//Копируем данные из выбранной записи расписания в $scope.newRecord. Это отобразит их в форме.
	scheduleRecordToNewRecord = function (record)
	{
		$scope.newRecord.id			= record.id;
		$scope.newRecord.deviceName	= record.deviceName;
		$scope.newRecord.uuid		= record.uuid;
		$scope.newRecord.taskName	= record.taskName;
		$scope.newRecord.dateTime	= new Date (record.dateTimeLong)/*new Date(record.dateTime)*/;
		$scope.newRecord.dateTimeLong = record.dateTimeLong;
		$scope.newRecord.available	= record.available;
		$scope.newRecord.state		= record.state;
console.log ('$scope.scheduleRecordToNewRecord() создал $scope.newRecord: ', $scope.newRecord);
	}

//Подготавливаем форму к редактированию записи.
	$scope.startEditRecord = function (record) {
		scheduleRecordToNewRecord (record);
	}

//Отправляем на бэк запрос на удаление записи.
	$scope.deleteRecord = function (record)
	{
console.log ('$scope.deleteRecord() вызван. record = ', record);
		scheduleRecordToNewRecord (record);

		$http.post (contextSchedulePath + '/schedule_delete_record', $scope.newRecord)
		.then (
		function successCallback (response) {
console.log ('$scope.deleteRecord() - бэк вернул: ', response.data);
			if (response.data)
			{
				$scope.newRecord.id = null; /* Поскольку (ре)инициализация $scope.newRecord данными
		из строки таблицы поместила в поле id ненулевое значение, то после удаления записи его нужно
		потереть. Форму не очищаем, — пусть попавшие в неё данные дадут юзеру шанс восстановить
		удалённую запись (id равный null поспособствует корректному созданию новой записи).	*/
				$scope.loadSchedule();
			}
			else $scope.getHomeNews(); //< расчитываем получить текст сообщения об ошибке.

		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в $scope.deleteRecord() бэк вернул: ', response.data);
		});
	}

//-------------------------------------------------------------------------------- сообщения
	$scope.showDeviceNews = function (lastNews)
	{
		if (lastNews)
		{
			let newsPaper = '';
			let length = lastNews.length;
			if (length > 0) {
				for (let i=0; i<length; i++)
				{
					newsPaper += lastNews[i];
					if (i < length-1) {
						newsPaper += '\r\r';
					}
				}
				alert (newsPaper);
			}
		}
	}

	$scope.getHomeNews = function ()
	{
		$http.get (contextMainPath + '/home_news')
		.then (
		function successCallback (response) {
			if (response.data)
				$scope.showDeviceNews (response.data);
		},
		function failureCallback (response)	{
			let text = 'ОШИБКА в $scope.getHomeNews(): не удалось обработать запрос:\r'+ response.data;
			alert (text);
		});
	}
//-------------------------------------------------------------------------------- планирование
//-------------------------------------------------------------------------------- разрешения

/*	$scope.getBgColor = function (rec)
	{
		$http.get (contextMainPath + '/is_task_name/'+ rec.uuid + '/' + rec.taskName)
		.then (
		function successCallback (response) {
			if (response.data)
				return 'black';
			else
				return 'silver';
		},
		function failureCallback (response)	{
			return 'silver';
			console.log ('ОШИБКА в $scope.getBgColor(): не удалось обработать запрос: ', response.data);
		});
	}*/
	/*$rootScope.*/isUserLoggedIn = function () //< TODO: $rootScope ?
	{
		if ($localStorage.smartHomeUser)	{	return true;	}	else	{	return false;	}
	}
//-------------------------------------------------------------------------------- для отладки

	$scope.clickButton = function () {
console.log ('$scope.clickButton() вызван.');
//		var myModal = new bootstrap.Modal(document.getElementById('staticBackdrop'));
//		var myModal = new bootstrap.Modal(document.getElementById('myDialog'), {
//		  backdrop: true,//'static',
//		  keyboard: true,
//		  focus: true
//		});
//		myModal.show();
	}

//-------------------------------------------------------------------------------- вызовы
	$scope.startSchedulePage();
});
