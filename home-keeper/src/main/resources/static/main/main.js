
 angular.module('smarthome-front')
		.controller('mainController', function ($scope, $http)
{
	const contextMainPath 	  = 'http://localhost:15550/home/v1/main';
	const contextSchedulePath = 'http://localhost:15550/home/v1/schedule';
	const contextAuthoPath	  = 'http://localhost:15550/home/v1/auth';
	//http://localhost:15550/home/index.html	- главная страница

	$scope.getDevicesList = function ()
	{
		$http.get (contextMainPath + '/device_list')
		.then (
		function successCallback (response)
		{
			$scope.device_list = response.data;
			console.log ('$scope.device_list = '+ response.data);
		},
		function failureCallback (response)
		{
//			alert (response.data.messages);
//			console.log ('Error @ getDevicesList(): '+ response.data.messages);
			console.log ('Error @ getDevicesList().');
		});
	}

//-------------------------------------------------------------------------------- вызовы
	$scope.getDevicesList();
});
