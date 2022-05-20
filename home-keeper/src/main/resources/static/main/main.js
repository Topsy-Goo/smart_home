
 angular.module('smarthome-front') //< приложение
		.controller('mainController', function ($scope, $rootScope, $http, $routeParams, $localStorage, $location)
{
	const contextMainPath 	  = 'http://localhost:15550/home/v1/main';
	const contextSchedulePath = 'http://localhost:15550/home/v1/schedule';
	const contextAuthoPath	  = 'http://localhost:15550/home/v1/auth';
	//http://localhost:15550/home/index.html	- главная страница

	$rootScope.stateTimer; //< Таймер для одновления (?динамических элементов?) страницы.
	$scope.pollInterval = 3000; //< Интервал в миллисекундах для таймера $rootScope.stateTimer.

	$scope.states = [];	/*	< Этот массив содержит объекты {uuid, StateDto}, которые позволяют обновлять не всю
	стопку панелей, а отдельные участки панелей. Необходимость это делать возникла, когда к фронту прикрутили
	таймер, обновляющий всю страницу. Эти обновления стали мешать вводить текст в поля форм и выбирать элементы
	ниспадающих списков. Пришлось обновления делать только для динамически меняющихся элементов.	*/

	$scope.uuids = []; //массив UUID-строк всех подключенных устройств. Нужен для обнаружения подключения или
	//отключения устройств.

//-------------------------------------------------------------------------------- запуск
	$scope.startMain = function ()
	{
		if (!isUserLoggedIn()) {
			$location.path ('/registration'); //< выполняем переход на страницу регистрации
			return;
		}
console.log ('******** главнвая-авторизован ********');
		getDevicesList();
	}

//Загрузка с бэка всего, что нужно для отобажения страницы. Сейчас это всё умещается в $scope.home_dto.
	getDevicesList = function ()
	{
		cleanUpMainPage();

		$http.get (contextMainPath + '/home_dto')
		.then (
		function successCallback (response)
		{
			$scope.home_dto = response.data;
//console.log (response.data);
			$scope.pollInterval = $scope.home_dto.pollInterval;
			$scope.fillStatesArray();
			$scope.getHomeNews();

			$rootScope.stateTimer = setInterval (updateStates, $scope.pollInterval);
		},
		function failureCallback (response) {
			alert ('ОШИБКА: Не удалось загрузить список устройств.');
			console.log ('Error @ getDevicesList().');
		});
	}
//-------------------------------------------------------------------------------- обновление состояний

//При получении $scope.home_dto вызываем этот метод, чтобы заполнить массив $scope.states.
	$scope.fillStatesArray = function ()
	{
		$scope.states = [];
		$scope.uuids = [];
		$scope.home_dto.groups.forEach(walkThroughGroup);

		function walkThroughGroup(group) {
			group.devices.forEach(extractAndStoreDeviceState);
		}
		function extractAndStoreDeviceState(device) {
			let uu = device.abilities.uuid;

		/*	Тонкий момент: мы сохраняем в массиве ссылку на state. Её же ангуляр использует для
			обновления данных на странице. Если при работе с массивом мы ссылку не профукаем, то
			можем рассчитывать на правильное обновление полей state на странице при изменении
			полей state в массиве.
		*/
			$scope.states.push({uuid: uu, state: device.state});
			$scope.uuids.push(uu);
		}
		console.log ('$scope.states = ', $scope.states);
	}

/*	Запрашиваем из бэка структуры StateDto для каждого элемента массива $scope.states, и обновляем
 у них поля (именно поля, а не целые StateDto, чтобы не потерять связи этих StateDto с таковыми
 в $scope.home_dto).
*/
	function updateStates()
	{
/*	Сперва запрашиваем у бэка массив UUID-строк, чтобы определить, не изменился ли набор
 обнаруженых устройств с момента последнего запроса $scope.home_dto. Если набор изменился,
 то перезагружаем $scope.home_dto.
*/
		$http.get (contextMainPath + '/all-uuids')
		.then (
		function successCallback (response)
		{
			if (!$scope.compareStringArrays ($scope.uuids, response.data)) {
				getDevicesList();
				return;
			}
		},
		function failureCallback (response) {
			console.log ('ОШИБКА в updateStates(): Не удалось получить UUID[].');
		});

	//Если набор устройств остался прежним, то запросим из бэка только состояния устройств.
		$scope.states.forEach (update);

		function update(element)
		{
			$http.get (contextMainPath + '/state/'+ element.uuid)
			.then (
			function successCallback (response) {
			/*	если обновить целиком структуру StateDto, то в $scope.home_dto и в $scope.states ссылки на
				state будут указывать на разные экземпляры, и ангуляр не сможет обновлять данные на странице.
			*/
				element.state.active 	  		= response.data.active;
				element.state.opCode 	  		= response.data.opCode;
				element.state.errCode     		= response.data.errCode;
				element.state.currentTask 		= response.data.currentTask;
				element.state.videoImageSource	= response.data.videoImageSource;
			/*	Состояния датчиков копируем не всюструктуру, а только те поля которые могут измениться.
				Это связано с тем, что копирование всей структуры «дёргает» всю инфорацию о датчике на
				странице, не давая его переименовывать.	*/
				copySensorState (element.state.sensors, response.data.sensors);
				$scope.getHomeNews();
			},
	/*	Эту часть, наверное, можно удалить, а то яндекс-браузер, чтоб его… перестали дебилы делать,
	сколько раз ошибку встретит, столько раз её и напечатает. Пока отлаживаешь бэк, может
	накопиться не одна тысяча одинаковых ошибок.
	*/
			function failureCallback (response) {
				cleanUpMainPage();
				console.log ('ОШИБКА в updateStates(): Не удалось обновить статус устройства ', element.uuid);
			});
		}
	}

	function copySensorState (snTo, snFrom)
	{
		if (snTo != null && snFrom != null)
		{
			let i=0;
			let len = snTo.length;
			for (i; i<len; i++)
			{
				snTo[i].on = snFrom[i].on;
				snTo[i].alarm = snFrom[i].alarm;
		}	}
	}

//Сравниваем строковые массивы. Порядок элементов не важен, главное — чтобы их наборы были идентичны.
	$scope.compareStringArrays = function (array1, array2)
	{
		let len = array1.length;
		if (len == array2.length)
		{
			while (len > 0) {
				if (!array2.includes(array1[--len]))
					break;
			}
			return len == 0;
		}
		return false;
	}


//-------------------------------------------------------------------------------- переименование

//Отправлвем на бэк сообщение, что о необходимости изменить поле deviceFriendlyName на указанное значение.
	$scope.applyNewFriendlyName = function (device, newFriendlyName)
	{
		if (device != null && newFriendlyName != null) {
			var uuid = device.abilities.uuid;

			$http.get (contextMainPath + '/device_friendly_name/'+ uuid +'/'+ newFriendlyName.string)
			.then (
			function successCallback (response) {
				device.friendlyName = newFriendlyName.string;
			},
			function failureCallback (response)	{
				let text = "Не удалось изменить имя устройства:\r" + device.friendlyName;
				alert (text);
			});
		}
	}

	$scope.applyNewSensorName = function (sn, newSensorName)
	{
		if (sn != null && newSensorName != null)
		{
			var uuid = sn.uuid;

			$http.get (contextMainPath + '/sensor_friendly_name/'+ uuid +'/'+ newSensorName.string)
			.then (
			function successCallback (response) {
				sn.name = newSensorName.string;
			},
			function failureCallback (response)	{
				let text = "Не удалось изменить имя датчика:\r«"+ sn.name +'» ('+ sn.uuid +').';
				alert (text);
			});
//			resumeStateTimer();
		}
	}

/*	$scope.pauseStateTimer = function ()
	{
		clearInterval ($rootScope.stateTimer);
	}

	resumeStateTimer = function ()
	{
		$rootScope.stateTimer = setInterval (updateStates, $scope.pollInterval);
	}*/
//-------------------------------------------------------------------------------- активация УУ

//Отправлвем на бэк сообщение, что о необходимости изменить флаг active для УУ с указаным UUID.
	$scope.toggleDeviceActiveState = function (uuid)
	{
		$http.get (contextMainPath + '/activate/'+ uuid)
		.then (
		function successCallback (response) {
			if (response.data == false)
				alert ('Не удалось (де)активировать устройство.');
		},
		function failureCallback (response)	{
			console.log ('toggleActiveState() resut: '+ response.data);
		});
	}

	$scope.getActiveString = function (active)	{
		if (active)	return "Активно";
		else		return "Неактивно";
	}

	$scope.getButtonNameActivate = function (active)	{
		if (active)	return "Деактивировать";
		else		return "Активировать";
	}
//-------------------------------------------------------------------------------- открывание панелей

	$scope.getButtonNameShowPanel = function (uuid) {
		let show = $scope.isPanelOpened (uuid);
		if (show) return "Скрыть";
		else      return "Показать";
	}
/*	Определяем, открыта ли панель устройства, UUID-стрку которого нам передали в параметре.
 Если UUID есть в $localStorage.openedPanels, то это означает, что панель открыта. Или
 была открыта перед разрывом соединения.
*/
	$scope.isPanelOpened = function (uuid)
	{
		let opened = true;	//< на случай, если что-то пойдёт не так (все панели будут открыты без возможности их закрыть.
							//	Это заметно лучше, чем если они будут закрыты без возможности их открыть).
		if ($localStorage.openedPanels) {
			opened = $localStorage.openedPanels.includes(uuid);
		}
		return opened;
	}

//Когда юзер открывает/закрывает панель устройства, мы запоминаем это в хранилище браузера.
	$scope.togglePanelOpenedState = function (uuid)
	{
		if ($localStorage.openedPanels)
		{
			let index = $localStorage.openedPanels.indexOf(uuid);
			if (index >= 0) {
				$localStorage.openedPanels.splice(index,1);
			}
			else {
				$localStorage.openedPanels.push(uuid);
			}
			console.log ('togglePanelOpenedState() : $localStorage.openedPanels = ', $localStorage.openedPanels);
		}
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
//-------------------------------------------------------------------------------- задачи
	$scope.launchTask = function (device, taskName)
	{
		$http.get (contextMainPath + '/launch_task/'+ device.abilities.uuid +'/'+ taskName)
		.then (
		function successCallback (response) {
			if (!response.data)
				$scope.getHomeNews();
//			alert (response.data.s);
		},
		function failureCallback (response)	{
			alert ('ОШИБКА: не удалось обработать запрос:\r'+ response.data);
		});
	}

	$scope.interruptCurrentTask = function (device)
	{
		$http.get (contextMainPath + '/interrupt_task/'+ device.abilities.uuid)
		.then (
		function successCallback (response) {
//console.log ('$scope.interruptCurrentTask() получила в ответ:', response.data);
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в interruptCurrentTask() бэк вернул: ', response.data);
		});
	}
//-------------------------------------------------------------------------------- видео
/*	$scope.startVideoStreaming = function (device)
	{
		$http.get (contextMainPath + '/video_on/'+ device.abilities.uuid)
		.then (
		function successCallback (response) {
//console.log ('$scope.startVideoStreaming() получила в ответ:', response.data);
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в startVideoStreaming() бэк вернул: ', response.data);
		});
	}*/

/*	$scope.stopVideoStreaming = function (device)
	{
		$http.get (contextMainPath + '/video_off/'+ device.abilities.uuid)
		.then (
		function successCallback (response) {
//console.log ('$scope.stopVideoStreaming() получила в ответ:', response.data);
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в stopVideoStreaming() бэк вернул: ', response.data);
		});
	}*/

//-------------------------------------------------------------------------------- планирование
	$scope.scheduleTask = function (device, taskName)
	{
//console.log ('$scope.scheduleTask(): uuid = ', device.abilities.uuid);
//console.log ('$scope.scheduleTask(): taskName = ', taskName);
		$location.path ('/schedule/'+ device.abilities.uuid +'/'+ taskName +'/'+ device.friendlyName);
	}
//-------------------------------------------------------------------------------- связывание
	$scope.loadDeviceSlaveList = function (device, taskName)
	{
//console.log ('Вызван $scope.loadDeviceSlaveList() с параметрами: device = ', device, ', и taskName = ', taskName);
		if (!taskName) {
			device.slaveList = null;
			return;
		}
		$http.get (contextMainPath + '/slave-list/'+ device.abilities.uuid)
		.then (
		function successCallback (response) {
			device.slaveList = response.data;
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в loadDeviceSlaveList() бэк вернул: ', response.data);
		});
	}

	$scope.requestSlaveBindableFunctions = function (device, uuid)
	{
//console.log ('$scope.requestSlaveBindableFunctions() получила параметры: ', device, ', и ', uuid);
		if (!uuid) {
			device.bindableFunctions = null;
			return;
		}

		$http.get (contextMainPath + '/bindable-functions/'+ uuid)
		.then (
		function successCallback (response) {
			device.bindableFunctions = response.data;
//console.log ('$scope.requestSlaveBindableFunctions() получила в ответ:', response.data);
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в requestSlaveBindableFunctions() бэк вернул: ', response.data);
		});
	}

	$scope.bindSlave = function (device, object)
	{
//console.log ('bindSlave() получила object: ', object, '\rи device: ', device);
		let dto =  {"masterTaskName":	object.masterTaskName,
					"masterUUID":		device.abilities.uuid,
					"slaveUUID":		object.slaveUUID,
					"slaveFuctionUUID":	object.slaveFuctionUUID };
//console.log ('bindSlave() отправляет dto: ', dto);

		$http.post (contextMainPath + '/bind', dto)
		.then (
		function successCallback (response) {
			console.log ('bindSlave() получила ответ: ', response.data);
			if (response.data)
				$scope.getContracts(device);
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в bindSlave() получила ответ: ', response.data);
		});
	}

	$scope.getContracts = function (device)
	{
		$http.get (contextMainPath + '/contracts/'+ device.abilities.uuid)
		.then (
		function successCallback (response) {
			device.contracts = response.data;
//console.log ('$scope.getContracts() получила в ответ:', response.data);
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в getContracts() бэк вернул: ', response.data);
		});
	}

	$scope.deleteContract = function (device, contractToRemove)
	{
//console.log ('$scope.deleteContract() получила contractToRemove: ', contractToRemove);
		let obj = JSON.parse (contractToRemove.data);

//console.log ('$scope.deleteContract() obj = JSON.parse(contractToRemove) >>', obj);
		let dto =  {"masterTaskName":	obj.taskName,
					"masterUUID":		device.abilities.uuid,
					"slaveUUID":		obj.mateUuid,
					"slaveFuctionUUID":	obj.functionUuid };
//console.log ('$scope.deleteContract() - dto-шка: ', dto);

		$http.post (contextMainPath + '/unbind', dto)
		.then (
		function successCallback (response) {
			console.log ('deleteContract() получила ответ: ', response.data);
			if (response.data)
				$scope.getContracts(device);
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в deleteContract() получила ответ: ', response.data);
		});
	}

//-------------------------------------------------------------------------------- датчики

	$scope.turnSensor = function (sn)
	{
		$http.post (contextMainPath + '/sensor-turn', sn)
		.then (
		function successCallback (response) {
			sn = response.data;
//console.log ('$scope.turnSensor() получила в ответ:', response.data);
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в turnSensor() бэк вернул: ', response.data);
		});
	}

	$scope.alarmSensor = function (sn)
	{
		$http.post (contextMainPath + '/sensor-alarm', sn)
		.then (
		function successCallback (response) {
			sn = response.data;
//console.log ('$scope.alarmSensor() получила в ответ:', response.data);
		},
		function failureCallback (response)	{
			console.log ('ОШИБКА: в alarmSensor() бэк вернул: ', response.data);
		});
	}

	$scope.sensorLightStateImage = function (on, alarm)
	{
		if (!on)
			return "./images/grey-light-ring.png";
		if (on && !alarm)
			return "./images/green-light-ring.png";
		return "./images/red-light-ring.png";
	}
//-------------------------------------------------------------------------------- разрешения
	$scope.showSlaveBindingForm = function (device) {
		return device.abilities.master;
	}

	$scope.showTasksForm = function (tasklist) {
		 return tasklist != null;
	}

	isUserLoggedIn = function () {
		if ($localStorage.smartHomeUser) { return true; } else { return false; }
	}

	$scope.showInterruptTaskButton = function (currentTask)
	{
		if (currentTask) {
			return currentTask.interruptible && currentTask.running;
		}
		return false;
	}

	$scope.panelHeaderColor1 = function (isActive) {
		return isActive ? '#dffffa' : '#fefefe';
	}

	$scope.panelHeaderColor2 = function (isActive) {
		return isActive ? '#e8fffa' : '#f8f8f8';	//dbfee9
	}

	$scope.panelBodyColor = function (isActive) {
		return isActive ? '#f8fff0' : '#f8f8f8';	//dbfee9
	}
//-------------------------------------------------------------------------------- очистка
	cleanUpMainPage = function () {
		clearInterval ($rootScope.stateTimer);
	}
//-------------------------------------------------------------------------------- вызовы
	$scope.startMain();
});
