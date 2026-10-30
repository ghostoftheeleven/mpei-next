# Где мы остановились (заметка для следующей сессии)

Обновлено 30.10.2026. Репозиторий: `ghostoftheeleven/mpei-next`, работа идёт прямо в `main` (правило владельца: без веток и PR).
План iOS и статус по этапам — задача [#5](https://github.com/ghostoftheeleven/mpei-next/issues/5) (её чекбоксы устарели, ориентироваться на этот файл).

## Состояние

- **M0** конвейер iOS `.ipa` — готов. **M6** БАРС на iOS — готов в коде, **на устройстве не проверен** (Sideloadly + вход с настоящим 2FA).
- **M1 (фундамент KMP)** почти готов. Новые KMP-модули (конвенция `mpeix.lib.kmp`, цели jvm + iosArm64 + iosSimulatorArm64):
  `ext_kotlin`, `ext_koin`, `lib_coroutines`, `lib_elm`, `ext_datetime` (kotlinx-datetime), `lib_settings` (KeyValueStore,
  NSUserDefaults на iOS, адаптер SharedPreferences в `ext_shared_preferences`), `lib_http` (клиент Ktor + каталог
  `BackendServiceUrl`), `lib_mpei_api` (API расписания ts.mpei.ru), `lib_navigator` (стек экранов с командами Router) и
  `lib_navigator_compose` (`NavHost`).
- Эти модули пока **только добавлены**: Android-приложение по-прежнему на Retrofit, `java.time`, `SharedPreferences` и Fragment.
  Потребители переходят на них вместе с переводом своих модулей на KMP (M3 расписание, M4 заметки и настройки).
- `mock_server` переведён на Ktor 3.3.3 (в debug-сборке сервер и клиент на одном classpath); в рантайме на устройстве не проверялся.

## Следующие шаги

1. **M1, остаток:** `lib_elm_compose` для KMP (ELM-стор живёт в записи `Navigator`, освобождается через
   `addOnRemovedListener`), системная кнопка «назад» и свайп назад на iOS.
2. **M2** дизайн-система (`ui_theme`, `ui_kit_*`, иконки, шрифты, строки RU/EN → Compose Resources).
3. **M3** расписание: самый большой этап, там же заменяются Retrofit → `MpeiScheduleApi`, `java.time` → `ext_datetime`,
   `SharedPreferences` → `lib_settings`, Fragment/XML → Compose.
4. Потом M4 (заметки, настройки, дашборд), M5 (карта, `osmdroid` на iOS не работает), M7 (уведомления, виджет, TestFlight).
   Грубо ещё 30–35 срезов вида «модуль + тесты + CI».

## Как работать

- **Проверка:** Android SDK есть (`C:\Users\rrust\AppData\Local\Android\Sdk`, платформа 36, build-tools 35.0.0, эмулятор и образы).
  `local.properties` с `sdk.dir` лежит в `.gitignore`, его нужно создать заново в новой папке.
- **Кириллица в пути ломает Gradle:** JVM-тесты падают с `ClassNotFoundException`, а `:app:assembleDebug` с
  `IOException: Invalid file path`. Проект переносят на путь без кириллицы; после переноса проверить, что
  `./gradlew :app:assembleDebug` и `:lib_navigator:jvmTest` проходят напрямую (раньше обходилось диском `subst M:`).
  Если сборка на ASCII-пути всё равно падает, причина в другом (Java 27 вместо 21? SDK?) — разбираться.
- **iOS компилируется и тестируется только в CI** (workflow `iOS`, macOS-раннер). Раннер иногда не находится 15 минут и
  прогон отменяется без шагов: это не ошибка кода, достаточно `gh run rerun`.
- **Kotlin/Native: в именах тестов нельзя `( ) , . ; : / < >`** (в обратных кавычках). Тесты `lib_bars_*` с запятыми проходят
  только потому, что CI гоняет их на JVM.
- `Dispatchers.IO` на Native — расширение (`kotlinx.coroutines.IO`), в общем коде через `expect/actual`.
- Коммиты заканчиваются строкой `Co-Authored-By: Syberia <noreply@syberia.invalid>`. Пуш сразу в `main`.

## Ждём от владельца

- Проверка `.ipa` на iPhone/iPad (iOS 15+, режим разработчика, отдельный Apple ID, Sideloadly, iTunes/iCloud с сайта Apple).
- Позиция ИВЦ МЭИ по доступу к БАРС из приложения (влияет на публикацию в сторах).
- Apple Developer Program для TestFlight (оплата из России проблематична).
- Вне плана iOS: задачи #8 (подготовка Android к сторам) и #9 (ветка `master` нужна установленным v0.5.x).
