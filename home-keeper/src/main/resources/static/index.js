(function ()	//< Описание основной ф-ции
{
	 angular.module('smarthome-front', ['ngRoute','ngStorage'])
			.config(config)
			.run(run);
/*
	anguler.module - создание (основного или дополнительного) модуля приложения.

	('smarthome-front', […]) - название приложения и список модулей-зависимостей, разделённых запятыми;

	наличие []-скобок означает создание основного модуля, а в скобках можно указать список подключаемых модулей (возможно подключение сторонних модулей);

	отсутствие []-скобок означает создание доп.модуля. При его создании будет выполнен поиск родительского приложения с указанным именем (будет выполнен поиск осн.модуля указанного приложения).

	ngRoute - имя доп.модуля (из библиотеки angular), подключенного в html-файле при пом.тэга <script src="…/angular-route.min.js">. (используется тут же, ниже, в function config()). Этот модуль позволяет делать переходы между страницами.

	ngStorage - имя доп.модуля (из библиотеки angular), подключенного в html-файле при пом.тэга <script src="…/ngStorage.min.js">. (используется ниже, в function run() и в контроллере)

	config(func_name) - указывает на ф-цию, которая будет конфигурировать приложение.

	run(func_name) - указывает на ф-цию, которая будет запускаться при старте приложения.
*/

	function config ($routeProvider)
	{
	/*	$routeProvider - модуль, который позволяет переходить между страницами
	*/
		$routeProvider
			.when('/main',		//< задаём постфикс для перехода на главную страницу
			{
				templateUrl: 'main/main.html',		//< адрес главной страницы и…
				controller:	 'mainController'		//	…имя её контроллера
			})
			.when('/schedule',	//< задаём адрес страницы с расписанием
			{
				templateUrl: 'schedule/schedule.html',	//<	адрес страницы с расписанием и…
				controller:	 'scheduleController'		//	…имя её контроллера
			})
			.when('/registration',
			{
				templateUrl: 'registration/registration.html',
				controller:	 'registrationController'
			})
/*			.when('/product_page/:pid',
			{
				templateUrl: 'product_page/product_page.html',
				controller:	 'product_pageController'
			})
			.when('/edit_product/:pid',	//< для возможности передавать параметр требуется указать $routeParams в объявлении edit_productController'а.
			{
				templateUrl: 'edit_product/edit_product.html',
				controller:	 'edit_productController'
			})
			.when('/edit_product',	//< пусть переход на страницу через главное меню означает намерение создать новый товар, а не редактировать существующий
			{
				templateUrl: 'edit_product/edit_product.html',
				controller:	 'edit_productController'
			})
			.when('/cart',
			{
				templateUrl: 'cart/cart.html',
				controller:	 'cartController'
			})
			.when('/order',
			{
				templateUrl: 'order/order.html',
				controller:	 'orderController'
			})
			.when('/user_profile',
			{
				templateUrl: 'user_profile/user_profile.html',
				controller:	 'user_profileController'
			})*/
			.otherwise(
			{
				redirectTo:	'/main'
//				redirectTo:	'/registration'
			});
	}

	function run ($rootScope, $http, $localStorage)
	{
	/*	При запуске приложения во фронте неразлогиненный юзер будет считан (из локального хранилища
	браузера) и в соотв-ии с ним будет добавлен и настроен умолчальный заголовок Authorization, как
	при авторизации и регистрации.
	(В нашем учебном проекте это не заработает, т.к. при старте
	приложения бэк считывает БД из sql-файла, а при регистрации нового юзера он не записывается в
	упомянутый файл).
	*/
		const contextAuthoPath	= 'http://localhost:15550/home/v1/auth';

		//В $localStorage.openedPanels будем запоминать uuid-ы устройств, чьи панели нужно открыть при
		//обновлении страницы. (Необязательная, но удобная ф-ция.)
		if ($localStorage.openedPanels == null) {
			$localStorage.openedPanels = [];
		}

        if ($localStorage.smartHomeUser) {
            $http.defaults.headers.common.Authorization = 'Bearer ' + $localStorage.smartHomeUser.token;
        }

		if ($localStorage.smartHomeUser) {
			//...
		}



/*		if (!$localStorage.gbj11SmartHomeActivationCode)
		{
			$http.get(contextAuthoPath + '/activation')
			.then(
			function successCallback(response)
			{
				$localStorage.gbj11SmartHomeActivationCode = response.data.value;
				console.log ('Activated successfully:'+ response.data.value);
			});
		}*/
	}
})();

 angular.module('smarthome-front')
		.controller('indexController', function ($rootScope, $scope, $http, $localStorage, $location)
{
/*	function ($scope, $http, ...) - по мере необходимости инжектим модули, которые входят в стандартную поставку ангуляра:
	$http - позволяет посылать из приложения http-запросы
	$scope - некий обменник между этим js-файлом и html-файлом.
	$rootScope - глобальный контекст (позволяет обращаться к ф-циям (и переменным?) откуда угодно (?в рамках приложения?))
	$localStorage - локальное хранилище браузера (требуется подкл. скрипт ngStorage.min.js.)
*/
	const contextMainPath 	  = 'http://localhost:15550/home/v1/main';
	const contextSchedulePath = 'http://localhost:15550/home/v1/schedule';
	const contextAuthoPath	  = 'http://localhost:15550/home/v1/auth';

	$scope.appTitle = 'Умный дом';
	$scope.mainPageTitle = 'Главная страница';
	$scope.schedulePageTitle = 'Расписание';


	$scope.tryToRegister = function ()
	{
		console.log ('$scope.tryToRegister call.');
		$scope.clearUserFields();		 //< это очистит поля формы авторизации (в шапке index.html)
		$location.path('/registration'); //< выполняем переход на страницу регистрации
	}

	$scope.clearUserFields = function () { $scope.user = null; }

	$scope.tryToLogin = function ()
	{
		if ($scope.user != null)
		{
			$http.post (contextAuthoPath + '/login',
						$scope.user)
			.then(
			function successCallback (response)
			{
				if (response.data.token)	//< проверка, что в ответе именно токен
				{
					$http.defaults.headers.common.Authorization = 'Bearer ' + response.data.token;
					$localStorage.smartHomeUser = {login: $scope.user.login, token: response.data.token};
					$scope.clearUserFields();
				}
				location.reload(false); /* перезагружает страницу (false=из кэша, true=из сервера);
				 место вызова в коде имеет значение, т.к. при перезагрузке, например, могут потеряться
				 данные о регистрации, если они не были записаны в хранилище браузера или не были
				 сохранены иным способом */
			},
			function failureCallback (response)
			{
				alert ('ОШИБКА: '+ response.data.messages);
			});
		}
	}

	$scope.tryToLogout = function ()
	{
		$scope.removeUserFromLocalStorage();
		$scope.clearUserFields();
		$location.path('/registration');
	}

	$scope.removeUserFromLocalStorage = function ()
	{
		delete $localStorage.smartHomeUser;
		$http.defaults.headers.common.Authorization = '';
	}

//----------------------------------------------------------------------- разрешения
	$rootScope.isUserLoggedIn = function ()
	{
		if ($localStorage.smartHomeUser)	{	return true;	}	else	{	return false;	}
	}
});
