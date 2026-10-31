# Где мы остановились (заметка для следующей сессии)

Обновлено 30.10.2026. Репозиторий: `ghostoftheeleven/mpei-next`, работа идёт прямо в `main` (правило владельца: без веток и PR).
План iOS и статус по этапам — задача [#5](https://github.com/ghostoftheeleven/mpei-next/issues/5) (её чекбоксы устарели, ориентироваться на этот файл).

## Состояние

- **M0** конвейер iOS `.ipa` — готов. **M6** БАРС на iOS — готов в коде, **на устройстве не проверен** (Sideloadly + вход с настоящим 2FA).
- **M1 (фундамент KMP)** готов, кроме системной кнопки «назад» и свайпа назад на iOS. Новые KMP-модули (конвенция `mpeix.lib.kmp`, цели jvm + iosArm64 + iosSimulatorArm64):
  `ext_kotlin`, `ext_koin`, `lib_coroutines`, `lib_elm`, `ext_datetime` (kotlinx-datetime), `lib_settings` (KeyValueStore,
  NSUserDefaults на iOS, адаптер SharedPreferences в `ext_shared_preferences`), `lib_http` (клиент Ktor + каталог
  `BackendServiceUrl`), `lib_mpei_api` (API расписания ts.mpei.ru), `lib_navigator` (стек экранов с командами Router) и
  `lib_navigator_compose` (`NavHost`, `LocalBackStackEntry`) и `lib_elm_ui` (`rememberElmStore`, `ElmContent`: ELM-стор
  экрана лежит в `BackStackEntry.scope` и останавливается, когда запись уходит из стека).
- Эти модули пока **только добавлены**: Android-приложение по-прежнему на Retrofit, `java.time`, `SharedPreferences` и Fragment.
  Потребители переходят на них вместе с переводом своих модулей на KMP (M3 расписание, M4 заметки и настройки).
- `mock_server` переведён на Ktor 3.3.3 (в debug-сборке сервер и клиент на одном classpath); в рантайме на устройстве не проверялся.
- **M2 начат:** `ui_theme` теперь KMP с Android-таргетом (конвенция `mpeix.lib.kmp.android`): палитра, типографика,
  `MpeixTheme` в `commonMain`, шрифты Roboto в `composeResources/font` (`rememberRobotoFontFamily()`), прозрачные системные
  панели через `expect/actual`, превью в `androidMain`. `RobotoFontFamily` как обычная переменная остался в `androidMain`
  для трёх Android-экранов. Остаётся: `ui_kit_*`, иконки, строки RU/EN, `ui_shimmer`, `ui_splash`.
- **M3 начат:** `feature_schedule_shared` (KMP): модели расписания (`@Serializable`, kotlinx-datetime), `MpeiScheduleMapper`,
  `ScheduleService` (поиск и неделя через `MpeiScheduleApi`), `ScheduleCache` (выбранное расписание и последние 12 недель
  в `KeyValueStore`, JSON) и `ScheduleController` (состояния `Search` / `Week`, офлайн-копия при ошибке сети). 32 теста на JVM
  (в тестах контроллера `MockEngine` отвечает на реальном диспетчере, поэтому ждём через `settle`, а не `advanceUntilIdle`).
  Не сделано: экран расписания в `:shared` (Compose, iOS), подключение к `App`, `KeyValueStore` для iOS в `MainViewController`,
  Room KMP вместо `KeyValueStore`-кэша (если понадобится), перевод Android-потребителей на эти классы.
- **Побочный эффект Compose Multiplatform 1.9.3:** он жёстко (`strictly`) задаёт `androidx.compose.material3:1.4.0-beta01`,
  поэтому всё Android-приложение теперь на этой бете вместо 1.3.2 из BOM. В 1.4 `material-icons-core` не подтягивается сам:
  его нужно добавлять явно (`libs.compose.material.icons.core`) в модули, которые импортируют `androidx.compose.material.icons`.
  Проверено на эмуляторе: онбординг, поиск, шрифт, цвета, сеть работают; экран расписания не открывали.

## Следующие шаги

1. **M1, остаток:** системная кнопка «назад» на Android и свайп назад на iOS для `NavHost`. Старый `lib_elm_compose`
   (ViewModel, Bundle) остаётся для Fragment-экранов Android и уйдёт, когда они станут Compose-экранами на `lib_elm_ui`.
2. **M2** дизайн-система (`ui_theme`, `ui_kit_*`, иконки, шрифты, строки RU/EN → Compose Resources).
3. **M3** расписание: самый большой этап, там же заменяются Retrofit → `MpeiScheduleApi`, `java.time` → `ext_datetime`,
   `SharedPreferences` → `lib_settings`, Fragment/XML → Compose.
4. Потом M4 (заметки, настройки, дашборд), M5 (карта, `osmdroid` на iOS не работает), M7 (уведомления, виджет, TestFlight).
   Грубо ещё 30–35 срезов вида «модуль + тесты + CI».

## Как работать

- **Проверка локально работает.** Проект лежит на пути без кириллицы (`C:\dev\MPEI Hub\mpei-next`).
  `./gradlew :app:assembleDebug` (около 3 минут с нуля, собирает debug APK) и `./gradlew :<модуль>:jvmTest` проходят.
  Android SDK: `C:\Users\rrust\AppData\Local\Android\Sdk` (платформа 36, build-tools 35.0.0, эмулятор и образы).
- **Эмулятор:** AVD `Medium_Phone_API_37.0` и `Arca_API_30`. Запуск без окна:
  `emulator.exe -avd Medium_Phone_API_37.0 -no-window -no-audio -no-snapshot -gpu swiftshader_indirect` (загрузка около
  40 секунд), потом `adb install -r modules/app/build/outputs/apk/debug/MPEI-Hub-v0.5.2-debug.apk`, пакет `ru.mpei.hub.dev`,
  скриншот `adb exec-out screencap -p > shot.png`. Холодный старт приложения около 10 секунд (сначала чёрный экран).
  `adb input text` не умеет кириллицу.
- **`local.properties`** лежит в `.gitignore`, в новой папке его нужно создать **с прямыми слэшами**:
  `sdk.dir=C:/Users/rrust/AppData/Local/Android/Sdk`. Одинарные обратные слэши в `.properties` это экранирование,
  путь превращается в мусор и сборка падает с `Invalid file path`.
- **Кириллица в пути ломает JVM-тесты** (`ClassNotFoundException`, компиляция при этом проходит): держать проект на ASCII-пути.
- **iOS компилируется и тестируется только в CI** (workflow `iOS`, macOS-раннер). Раннер иногда не находится 15 минут и
  прогон отменяется без шагов: это не ошибка кода, достаточно `gh run rerun`.
- **Kotlin/Native: в именах тестов нельзя `( ) , . ; : / < >`** (в обратных кавычках). Тесты `lib_bars_*` с запятыми проходят
  только потому, что CI гоняет их на JVM.
- `Dispatchers.IO` на Native — расширение (`kotlinx.coroutines.IO`), в общем коде через `expect/actual`.
- Коммиты заканчиваются строкой `Co-Authored-By: Syberia <noreply@syberia.invalid>`. Пуш сразу в `main`.
- **Issues:** на каждую текущую и оставшуюся задачу заводить GitHub Issue (русский текст: цель, объём с чекбоксами,
  критерий готовности), чтобы другие разработчики видели, что осталось. Зонтичная задача: [#5](https://github.com/ghostoftheeleven/mpei-next/issues/5)
  (статус в её начале), дочерние: #11 (кнопка «назад»), #12 (шиммер), #13 (проверка ресурсов), #14 (решение по Material3 beta),
  #15 (M3 расписание), #16 (M4), #17 (M5 карта), #18 (M7), #19 (проверка iOS), #20 (единый сетевой клиент).
  Закрывайте и обновляйте чекбоксы по мере готовности.

## Ждём от владельца

- Проверка `.ipa` на iPhone/iPad (iOS 15+, режим разработчика, отдельный Apple ID, Sideloadly, iTunes/iCloud с сайта Apple).
- Позиция ИВЦ МЭИ по доступу к БАРС из приложения (влияет на публикацию в сторах).
- Apple Developer Program для TestFlight (оплата из России проблематична).
- Вне плана iOS: задачи #8 (подготовка Android к сторам) и #9 (ветка `master` нужна установленным v0.5.x).
