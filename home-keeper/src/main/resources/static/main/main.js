
 angular.module('smarthome-front')
		.controller('mainController', function ($scope, $http)
{
	const contextMainPath 	  = 'http://localhost:15550/home/v1/main';
	const contextSchedulePath = 'http://localhost:15550/home/v1/schedule';
	const contextAuthoPath	  = 'http://localhost:15550/home/v1/auth';
	//http://localhost:15550/home/index.html	- главная страница

	$scope.getDevicesList = function ()
	{
		$http.get (contextMainPath + '/home_dto')
		.then (
		function successCallback (response) {
			$scope.home_dto = response.data;
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

	$scope.getActiveButtonName = function (active)	{
		if (active)	return "Деактивировать";
		else		return "Активировать";
	}

	$scope.getShowOrHideButtonName = function (show) {
		if (show) return "Скрыть";
		else      return "Показать";
	}

	$scope.togglePanel = function (device, isopened)
	{
		var uuid = device.abilities.uuid;
		$http.get (contextMainPath + '/panel/'+ uuid +'/'+ !isopened);
		device.htmlPanelOpened = !isopened;
	}

//-------------------------------------------------------------------------------- вызовы
	$scope.getDevicesList();
});
/*
	@scope.togglePanel = function (param)
	{
		$http.get (contextMainPath + '/panel/'+ param).then
		(function successCallback (response)
		{
		},
		function errorCallback (response)	{  console.log ('Error: '+ response.data);  });
	}
*/