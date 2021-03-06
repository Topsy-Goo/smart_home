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
	//	$routeProvider - модуль, который позволяет переходить между страницами.
		$routeProvider.when('/main',		//< задаём постфикс для перехода на главную страницу
					  {
					  	templateUrl: 'main/main.html',		//< адрес главной страницы и…
					  	controller:	 'mainController'		//	…имя её контроллера
					  })
					  .when('/schedule',	//< задаём адрес страницы с расписанием
					  {
					  	templateUrl: 'schedule/schedule.html',	//<	адрес страницы с расписанием и…
					  	controller:	 'scheduleController'		//	…имя её контроллера
					  })
					  .when('/schedule/:uuid/:taskname/:friendlyname',
					  {
					  	templateUrl: 'schedule/schedule.html',	//< для возможности передавать параметр требуется указать $routeParams в объявлении scheduleController'а.
					  	controller:	 'scheduleController'
					  })
					  .when('/registration',
					  {
					  	templateUrl: 'registration/registration.html',
					  	controller:	 'registrationController'
					  })
					  .otherwise(
					  {
					  	redirectTo:	'/main' //'/registration' //redirectTo:
					  });
	}

	function run ($rootScope, $http, $localStorage)
	{
	/*	При запуске приложения во фронте неразлогиненный юзер будет считан из локального хранилища
	браузера и в соотв-ии с ним будет добавлен и настроен умолчальный заголовок Authorization, как
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
	}
})();

 angular.module('smarthome-front')
		.controller('indexController', function ($scope, $rootScope, $http, $routeParams, $localStorage, $location)
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

//----------------------------------------------------------------------- выход
	$scope.logout = function ()
	{
console.log ('$scope.logout(): вызван.');
		removeUserFromLocalStorage();
		$localStorage.openedPanels = [];
//console.log ('logout(): $localStorage.openedPanels: ',$localStorage.openedPanels)
		$location.path('/registration');
	}

	removeUserFromLocalStorage = function ()
	{
//console.log ('removeUserFromLocalStorage(): $localStorage.smartHomeUser: ',$localStorage.smartHomeUser)
		delete $localStorage.smartHomeUser;
//console.log ('removeUserFromLocalStorage(): $localStorage.smartHomeUser: ',$localStorage.smartHomeUser)
//console.log ('removeUserFromLocalStorage(): $http.defaults.headers.common.Authorization: ',$http.defaults.headers.common.Authorization)
		$http.defaults.headers.common.Authorization = '';
//console.log ('removeUserFromLocalStorage(): $http.defaults.headers.common.Authorization: ',$http.defaults.headers.common.Authorization)
	}
//----------------------------------------------------------------------- разрешения
});
