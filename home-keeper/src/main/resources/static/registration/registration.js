
 angular.module('smarthome-front')
		.controller('registrationController', function ($rootScope, $scope, $http, $location, $localStorage)
{
/*	$routeParams -	позволяет при маршрутизации передавать парметры в адресной строке (маршрутизация
					описывается в index10.js. >> function config)
	$location - позволяет переходить на др.страницу.
	$http - позволяет посылать из приложения http-запросы
	$scope - некий обменник между этим js-файлом и html-файлом.
	$rootScope - глобальный контекст (позволяет обращаться к ф-циям (и переменным?) откуда угодно)
	$localStorage - локальное хранилище браузера (требуется подкл. скрипт ngStorage.min.js.)
*/
	const contextMainPath 	  = 'http://localhost:15550/home/v1/main';
	const contextSchedulePath = 'http://localhost:15550/home/v1/schedule';
	const contextAuthoPath	  = 'http://localhost:15550/home/v1/auth';

	$scope.contextPrompt = "";
	const contextPrompt_Unathorized = "Введите логин и пароль.";
	const contextPrompt_LogedIn = "Вы авторизованы.";
	const contextPrompt_Registered = "Вы успешно зарегистрированы.";
	const contextPrompt_Error = "Ошибка регистрации.";

//-------------------------------------------------------------------------------- запуск
	prepareToRegistration = function()
	{
console.log ('prepareToRegistration(): $localStorage.smartHomeUser: ',$localStorage.smartHomeUser)
		clearNewUserFields();

		if (isUserLoggedIn())
			$scope.contextPrompt = contextPrompt_LogedIn;
		else
			$scope.contextPrompt = contextPrompt_Unathorized;
	}

	isUserLoggedIn = function () {
		if ($localStorage.smartHomeUser)	{	return true;	}	else	{	return false;	}
	}

//-------------------------------------------------------------------------------- регистрация

	$scope.authorize = function ()
	{
console.log ('$scope.authorize() - вызван для $scope.new_user = ',$scope.new_user);
		if ($scope.new_user)
		{
			let autho = {"login":$scope.new_user.login, "password":$scope.new_user.password};
console.log ('$scope.authorize() - на бэк отправляется autho = ', autho);
			$http.post (contextAuthoPath + '/login', $scope.new_user)
			.then(
			function successCallback (response)
			{
				if (onSuccessfullLogin (response.data))
					$scope.contextPrompt = contextPrompt_LogedIn;
			},
			function failureCallback (response) {
				onLoginError (response.data);
			});
		}
	}

	onSuccessfullLogin = function (data)
	{
		if (data.s)
		{
			$http.defaults.headers.common.Authorization = 'Bearer ' + data.s;
			$localStorage.smartHomeUser = {login: $scope.new_user.login, token: data.s};
			clearNewUserFields();
			return true;
		}
		return false;
	}

	onLoginError = function (data)
	{
		$scope.contextPrompt = contextPrompt_Error;
		alert ('ОШИБКА:\r'+ data.s);
console.log ('$scope.authorize() - бэк вернул ошибку: ', data.s);
	}

	$scope.register = function ()
	{
console.log ('$scope.register() - вызван для $scope.new_user = ',$scope.new_user);
		if ($scope.new_user != null)
		{
			$http.post (contextAuthoPath + '/register', $scope.new_user)
			.then(
			function successCallback (response)
			{
				if (onSuccessfullLogin (response.data))
					$scope.contextPrompt = contextPrompt_Registered;
			},
			function failureCallback (response) {
				onLoginError (response.data);
			});
		}
	}

	clearNewUserFields = function()	{	$scope.new_user = null;	}





/*	$scope.cancelRegistration = function()
	{
		clearNewUserFields();
//		$location.path('/main');
		location.reload(true); *//* перезагружает страницу (false=из кэша, true=из сервера);
				 место вызова в коде имеет значение, т.к. при перезагрузке, например, могут потеряться
				 данные о регистрации, если они не были записаны в хранилище браузера или не были
				 сохранены иным способом *//*
	}*/

/*	$scope.tryMergeCarts = function ()
	{
		if ($localStorage.gbj7MarketGuestCartId)
		{
			$http.get (contextCartPath + '/merge/' + $localStorage.gbj7MarketGuestCartId)
			.then (
			function successCallback (response)
			{
				console.log ('registration - $scope.tryMergeCarts - OK');
//////////////////////////				$scope.loadCart();
			},
			function failureCallback (response)
			{
				console.log ('Ой! @ registration - $scope.tryMergeCarts');
				alert (response.data);
			});
		}
	}*/
//-------------------------------------------------------------------------------- разрешения
/*	$scope.canShowRegistrationElements = function() {
		if ($localStorage.smartHomeUser)
			return false;
		return true;
	}*/

//-------------------------------------------------------------------------------- вызовы
	prepareToRegistration();	//< вызов описанной выше функции
});
