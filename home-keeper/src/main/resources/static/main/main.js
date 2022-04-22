
 angular.module('smarthome-front')
		.controller('mainController', function ($scope, $rootScope, $http, $localStorage)
{
	const contextMainPath 	  = 'http://localhost:15550/home/v1/main';
	const contextSchedulePath = 'http://localhost:15550/home/v1/schedule';
	const contextAuthoPath	  = 'http://localhost:15550/home/v1/auth';
	//http://localhost:15550/home/index.html	- главная страница

	$rootScope.pollInterval = 3;


	$scope.getDevicesList = function ()
	{
		$http.get (contextMainPath + '/home_dto')
		.then (
		function successCallback (response)
		{
			$scope.home_dto = response.data;
			$rootScope.pollInterval = $scope.home_dto.pollInterval;

			console.log ('$scope.home_dto загружен.');
			console.log (response.data);
		},
		function failureCallback (response) {
			alert ('ОШИБКА: Не удалось загрузить список устройств.');
			console.log ('Error @ getDevicesList().');
		});
	}

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

	$scope.toggleDeviceActiveState = function (device)
	{
		if (device != null) {
			var uuid = device.abilities.uuid;
			var active = device.state.active;

			$http.get (contextMainPath + '/activate/'+ uuid)
			.then (
			function successCallback (response) {
				device.state.active = !active;
			},
			function failureCallback (response)	{  console.log ('toggleActiveState() resut: '+ response.data);  });
		}
	}

	$scope.getActiveString = function (active)	{
		if (active)	return "Активно";
		else		return "Неактивно";
	}

	$scope.getButtonNameActivate = function (active)	{
		if (active)	return "Деактивировать";
		else		return "Активировать";
	}

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

	$scope.getButtonNameShowPanel = function (uuid) {
		let show = $scope.isPanelOpened (uuid);
		if (show) return "Скрыть";
		else      return "Показать";
	}

	$scope.isPanelOpened = function (uuid)
	{
		let opened = true;	//< на случай, если что-то пойдёт не так (все панели будут открыты без возможности их закрыть.
							//	Это заметно лучше, чем если они будут закрыты без возможности их открыть).
		if ($localStorage.openedPanels) {
			opened = $localStorage.openedPanels.includes(uuid);
		}
		return opened;
	}

/*	$scope.updatePanelStates = function ()
	{
		$scope.home_dto.groups.forEach(walkThroughGroup);

		function walkThroughGroup(group) {
			group.devices.forEach(extractUuids);
		}
		function extractUuids(device) {
			console.log (device.abilities.uuid);
		}
	}*/
//-------------------------------------------------------------------------------- вызовы
	$scope.getDevicesList();
});
