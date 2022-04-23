
 angular.module('smarthome-front')
		.controller('mainController', function ($scope, $rootScope, $http, $localStorage)
{
	const contextMainPath 	  = 'http://localhost:15550/home/v1/main';
	const contextSchedulePath = 'http://localhost:15550/home/v1/schedule';
	const contextAuthoPath	  = 'http://localhost:15550/home/v1/auth';
	//http://localhost:15550/home/index.html	- главная страница

	$scope.stateTimer; //< Таймер для одновления (?динамических элементов?) страницы.
	$rootScope.pollInterval = 3000; //< Интервал в миллисекундах для таймера $scope.stateTimer.

	$scope.states = [];	/*	< Этот массив содержит объекты {uuid, StateDto}, которые позволяют обновлять не всю
	стопку панелей, а отдельные участки панелей. Необходимость это делать возникла, когда к фронту прикрутили
	таймер, обновляющий всю страницу. Эти обновления стали мешать вводить текст в поля форм и выбирать элементы
	ниспадающих списков. Пришлось обновления делать только для динамически меняющихся элементов.	*/

	$scope.uuids = []; //массив UUID-строк всех подключенных устройств. Нужен для обнаружения подключения или
	//отключения устройств.


	$scope.startMain = function () {
		$scope.getDevicesList();
		$scope.stateTimer = setInterval ($scope.updateStates, $rootScope.pollInterval);
	}

//Загрузка с бэка всего, что нужно для отобажения страницы. Сейчас это всё умещается в $scope.home_dto.
	$scope.getDevicesList = function ()
	{
		$http.get (contextMainPath + '/home_dto')
		.then (
		function successCallback (response)
		{
			$scope.home_dto = response.data;
			$rootScope.pollInterval = $scope.home_dto.pollInterval * 1000;
			$scope.fillStatesArray();
		},
		function failureCallback (response) {
			$scope.cleanUp();
			alert ('ОШИБКА: Не удалось загрузить список устройств.');
			console.log ('Error @ getDevicesList().');
		});
	}

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
			полей state в массиве.	*/
			$scope.states.push({uuid: uu, state: device.state});
			$scope.uuids.push(uu);
		}
		console.log ('$scope.states = '+ $scope.states);
	}

//Запрашиваем из бэка структуры StateDto для каждого элемента массива $scope.states, и обновляем
// у них поля (именно поля, а не целые StateDto, чтобы не потерять связи этих StateDto с таковыми
// в $scope.home_dto).
	$scope.updateStates = function ()
	{
	//Сперва запрашиваем у бэка массив UUID-строк, чтобы определить, не изменился ли набор
	//обнаруженых устройств с момента последнего запроса $scope.home_dto. Если набор изменился,
	//то перезагружаем $scope.home_dto.

		$http.get (contextMainPath + '/uuids')
		.then (
		function successCallback (response)
		{
			if (!$scope.compareStringArrays ($scope.uuids, response.data)) {
				$scope.getDevicesList();
				return;
			}
		},
		function failureCallback (response) {
			console.log ('ОШИБКА в updateStates(): Не удалось получить UUID[].');
		});//*/

	//Если набор устройств остался прежним, то запросим из бэка только состояния устройств.

		$scope.states.forEach(update);
		function update(element)
		{
			$http.get (contextMainPath + '/state/'+ element.uuid)
			.then (
			function successCallback (response)
			{	/*	если обновить целиком структуру StateDto, то в $scope.home_dto и в $scope.states ссылки на
					state будут указывать на разные экземпляры, и ангуляр не сможет обновлять данные на странице.
				*/
				element.state.active 	  = response.data.active;
				element.state.opCode 	  = response.data.opCode;
				element.state.errCode     = response.data.errCode;
				element.state.currentTask = response.data.currentTask;
			},
			function failureCallback (response) {
				$scope.cleanUp();
				console.log ('ОШИБКА в getDevicesList(): Не удалось обновить статус устройства '+ element.uuid);
			});
		}
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

//Отправлвем на бэк сообщение, что о необходимости изменить поле deviceFriendlyName на указанное значение.
	$scope.tryNewFriendlyName = function (device, newFriendlyName)
	{
		if (device != null && newFriendlyName != null) {
			var uuid = device.abilities.uuid;

			$http.get (contextMainPath + '/friendly_name/'+ uuid +'/'+ newFriendlyName)
			.then (
			function successCallback (response) {
				device.friendlyName = newFriendlyName;
			},
			function failureCallback (response)	{  console.log ('tryNewFriendlyName() resut: '+ response.data);  });
		}
	}

//Отправлвем на бэк сообщение, что о необходимости изменить флаг active для УУ с указаным UUID.
	$scope.toggleDeviceActiveState = function (uuid)
	{
		$http.get (contextMainPath + '/activate/'+ uuid)
		.then (
		function successCallback (response) {
//			device.state.active = response.data;
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

	$scope.getButtonNameShowPanel = function (uuid) {
		let show = $scope.isPanelOpened (uuid);
		if (show) return "Скрыть";
		else      return "Показать";
	}

//Определяем, открыта ли панель устройства, UUID-стрку которого нам передали в параметре.
//Если UUID есть в $localStorage.openedPanels, то это означает, что панель открыта. Или
//была открыта перед разрывом соединения.
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
			console.log ('togglePanelOpenedState() : $localStorage.openedPanels = '+ $localStorage.openedPanels);
		}
	}

	$scope.launchTask = function (uuid, task)
	{
		console.log ('$scope.launchTask: uuid = '+ uuid);
		console.log ('$scope.launchTask: task = '+ task);
		//...
	}

	$scope.scheduleTask = function (uuid, task)
	{
		//...
	}

/*	$scope.runStateTimer = function(interval) {
		$scope.stateTimer = setInterval ($scope.updateStates, interval);
	}*/

	$scope.cleanUp = function () {
		clearInterval($scope.stateTimer);
	}
//-------------------------------------------------------------------------------- для отладки
	$scope.timerStop = function() {
		clearInterval($scope.stateTimer);
	}
//-------------------------------------------------------------------------------- вызовы
	$scope.startMain();
});

