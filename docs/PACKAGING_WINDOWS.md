# Сборка CashStore для Windows

## Требования

Для создания EXE необходимы:

- Windows 10/11 x64;
- JDK 21 с утилитами `jpackage` и `jlink` (именно 21: рантайм установщика собирается из того JDK, на котором запущен Maven);
- WiX Toolset 3.x (`candle.exe` и `light.exe`), каталог `bin` WiX должен быть в `PATH`;
- Maven Wrapper из проекта.

Проверка:

```powershell
$env:JAVA_HOME
.\mvnw.cmd -version
where.exe jpackage
where.exe candle.exe
where.exe light.exe
```

Если Maven использует не Java 21, установите `JAVA_HOME` перед сборкой:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;C:\Program Files (x86)\WiX Toolset v3.11\bin;$env:Path"
```

Вторая часть `Path` нужна, если WiX установлен, но его `bin` не добавлен в системный `PATH`
(`jpackage` ищет `candle.exe` именно там). Чтобы не делать этого каждый раз, добавьте каталог WiX в
системный `PATH` и перезапустите консоль.

## Версия установщика

Откройте `pom.xml` и измените значение перед новым релизом:

```xml
<installer.version>0.0.1</installer.version>
```

Версия должна состоять только из чисел и точек в формате, который принимает Windows Installer.

## Рекомендуемая команда

Запускайте из корня проекта:

```powershell
.\mvnw.cmd clean verify jpackage:jpackage
```

`verify` выполняет все Maven-фазы до проверки включительно. Unit-тесты запускаются до упаковки. При падении теста JAR и EXE не должны рассматриваться как готовый релиз, а выполнение Maven останавливается до вызова `jpackage`.

Допустим и эквивалентный вариант:

```powershell
.\mvnw.cmd clean package jpackage:jpackage
```

## Раздельное выполнение

Если нужно явно проверить каждый этап:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd package
.\mvnw.cmd jpackage:jpackage
```

После `clean test` команда `package` повторно проходит Maven lifecycle и снова запускает тесты. Для обычной работы предпочтительнее единая команда `clean verify jpackage:jpackage`.

## Результаты сборки

- `target/app/cash-0.0.1-SNAPSHOT.jar` — Spring Boot fat-JAR;
- `target/dist/CashStore-SQLite-<installer.version>.exe` — установщик Windows (имя файла = `app.name` + `installer.version` из `pom.xml`).

Настройки берутся из `pom.xml`:

- имя приложения: `CashStore-SQLite` (свойство `app.name`; отдельное имя и `winUpgradeUuid` нужны, чтобы эта сборка не перезаписывала установленную версию на Postgres);
- главный JAR: `cash-0.0.1-SNAPSHOT.jar`;
- главный класс упакованного Boot-приложения: `org.springframework.boot.loader.launch.JarLauncher`;
- тип пакета: `EXE`;
- каталог результата: `target/dist`;
- иконка: `src/main/resources/icons/app.ico`.

Параметр `removeDestination=true` очищает каталог `target/dist` перед созданием нового установщика. Дополнительно `clean` удаляет весь старый `target`.

## Java-рантайм внутри установщика (jlink)

В установщик попадает не копия всего JDK (около 315 МБ, из них `jmods` и `src.zip` приложению не
нужны), а рантайм из нужных модулей, который `jpackage` собирает через `jlink`. В `pom.xml` для этого
заданы `addModules` и `jLinkOptions` (`--strip-debug`, `--no-header-files`, `--no-man-pages`,
`--compress=zip-6`); параметр `runtimeImage` не используется. Такой рантайм весит около 59 МБ, а
установщик — около 110 МБ вместо около 138 МБ.

Список модулей получен командой `jdeps` по fat-JAR плюс модули, которых `jdeps` не видит, потому что
они подключаются через `ServiceLoader`:

| Модуль | Зачем нужен |
|---|---|
| `jdk.localedata` | русская локаль: без неё даты и числа форматируются по-английски (`27 Oct 2026`, `1,234,567.50`) |
| `jdk.crypto.ec` | эллиптические шифры TLS (ECDSA, X25519): без них HTTPS к 1С и книге задолженности может не работать |
| `jdk.unsupported.desktop`, `jdk.xml.dom` | зависимости JavaFX |

Кодировки `CP1251`/`IBM866`, которыми печатаются чеки, входят в `java.base` и отдельного модуля не
требуют.

### Когда список нужно дополнить

Признак — установленное приложение падает при запуске или в конкретном действии с
`NoClassDefFoundError`, `ClassNotFoundException` или `ServiceConfigurationError`, хотя из JAR
(`java -jar`) то же самое работает. Это означает, что новой зависимости нужен модуль JDK, которого
нет в `addModules`. Чтобы найти его:

```powershell
$w = "$env:TEMP\cash-jar"; Remove-Item $w -Recurse -Force -ErrorAction SilentlyContinue
Expand-Archive target\app\cash-0.0.1-SNAPSHOT.jar $w
$libs = (Get-ChildItem "$w\BOOT-INF\lib\*.jar" | ForEach-Object FullName) -join ';'
& "$env:JAVA_HOME\bin\jdeps.exe" --multi-release 21 --ignore-missing-deps --print-module-deps `
  --class-path $libs "$w\BOOT-INF\classes"
```

Добавьте недостающие модули в `addModules` и пересоберите. `jdeps` не находит модули, подключаемые по
имени или через `ServiceLoader` (локали, криптопровайдеры, кодировки, драйверы), — их нужно
проверять отдельно.

### Проверка рантайма без установки

Можно собрать такой же рантайм вручную и запустить на нём JAR из пустой папки (не из каталога с
рабочей БД, чтобы не создать там `cash_store.db` и `cash-store.lock`):

```powershell
$mods = "java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.sql,java.sql.rowset,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.jfr,jdk.localedata,jdk.unsupported,jdk.unsupported.desktop,jdk.xml.dom"
& "$env:JAVA_HOME\bin\jlink.exe" --add-modules $mods --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output "$env:TEMP\cash-rt"
New-Item -ItemType Directory "$env:TEMP\cash-run" -Force | Out-Null; cd "$env:TEMP\cash-run"
& "$env:TEMP\cash-rt\bin\java.exe" "-Djavafx.preloader=kz.store.cash.config.AppPreloader" -jar D:\dev-spring-boot\cash\target\app\cash-0.0.1-SNAPSHOT.jar
```

В логе должно появиться `Successfully applied 2 migrations` и `Started application`. Список модулей в
команде должен совпадать с `addModules` в `pom.xml`.

## Размер установщика

Установщик состоит из Java-рантайма (около 59 МБ) и fat-JAR приложения (около 66 МБ до упаковки в
EXE). Самые тяжёлые библиотеки в JAR (в сжатом виде):

| Библиотека | МБ |
|---|---|
| `hibernate-core` | 11,5 |
| `sqlite-jdbc` | 11,5 (нативные библиотеки SQLite для всех ОС; драйвер Postgres, который он заменил, весил около 1) |
| `byte-buddy` | 8,6 |
| `javafx-graphics` | 5,7 |
| `javafx-controls` | 2,5 |
| `aspectjweaver` | 2,1 |

Посмотреть состав JAR можно командой `jar tf target\app\cash-0.0.1-SNAPSHOT.jar | findstr BOOT-INF/lib`.

## Запуск только тестов

```powershell
.\mvnw.cmd test
```

Ожидаемый итог:

```text
Tests run: ..., Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Запуск собранного JAR

До создания EXE JAR можно проверить отдельно:

```powershell
java -jar target\app\cash-0.0.1-SNAPSHOT.jar
```

Отдельная БД не нужна — SQLite-файл `cash_store.db` создастся автоматически рядом с JAR/EXE при
первом запуске (см. [настройка БД](DB_SETUP.md)).

## Частые ошибки

### WiX tools not found

Установите WiX Toolset 3.x и добавьте каталог `bin` в `PATH`. Затем перезапустите PowerShell и проверьте `where.exe candle.exe` и `where.exe light.exe`.

### jpackage не найден

Проверьте, что установлен полноценный JDK 21, а не только JRE, и что Maven Wrapper использует этот JDK.

### Tests have failures

Откройте отчёты в `target/surefire-reports`, исправьте тест или production-код и повторите полную команду. Не обходите ошибку флагом `-DskipTests` в релизной сборке.

### Ошибка при первом запуске EXE (не удаётся создать/открыть базу)

Это не ошибка упаковки. Проверьте, что у приложения есть права на запись в свой рабочий каталог
(там создаётся `cash_store.db`) и что файл не заблокирован другим уже запущенным экземпляром
приложения.

### Старый EXE остался в каталоге

Используйте `clean` и убедитесь, что никакой процесс не удерживает файл из `target/dist`.

### Установленное приложение падает с `NoClassDefFoundError` или `ServiceConfigurationError`

Из JAR (`java -jar`) то же самое работает, а из установленного EXE — нет: в урезанный рантайм не
вошёл нужный модуль JDK. Найдите его по разделу «Когда список нужно дополнить» выше и добавьте в
`addModules` в `pom.xml`.

### Русские даты и числа показываются по-английски или не работает HTTPS

В рантайме нет `jdk.localedata` (локали) или `jdk.crypto.ec` (эллиптические шифры TLS). Проверьте,
что оба модуля остались в `addModules`.

### Сборка прошла на другом JDK

Maven не откажется собирать на JDK 22 и т. п. (код компилируется с `release 21`), но Java-рантайм в
установщик попадёт из того JDK, на котором запущен Maven. Проверьте, что `.\mvnw.cmd -version`
показывает Java 21.
