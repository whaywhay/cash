# Сборка CashStore для Windows

## Требования

Для создания EXE необходимы:

- Windows 10/11 x64;
- JDK 21 с утилитой `jpackage`;
- WiX Toolset 3.x (`candle.exe` и `light.exe`);
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
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## Версия установщика

Откройте `pom.xml` и измените значение перед новым релизом:

```xml
<installer.version>0.0.8</installer.version>
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
- `target/dist/CashStore-<installer.version>.exe` — установщик Windows.

Настройки берутся из `pom.xml`:

- имя приложения: `CashStore`;
- главный JAR: `cash-0.0.1-SNAPSHOT.jar`;
- главный класс упакованного Boot-приложения: `org.springframework.boot.loader.launch.JarLauncher`;
- тип пакета: `EXE`;
- каталог результата: `target/dist`;
- иконка: `src/main/resources/icons/app.ico`.

Параметр `removeDestination=true` очищает каталог `target/dist` перед созданием нового установщика. Дополнительно `clean` удаляет весь старый `target`.

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

Для запуска должны быть доступны PostgreSQL и корректные настройки `application.yml`.

## Частые ошибки

### WiX tools not found

Установите WiX Toolset 3.x и добавьте каталог `bin` в `PATH`. Затем перезапустите PowerShell и проверьте `where.exe candle.exe` и `where.exe light.exe`.

### jpackage не найден

Проверьте, что установлен полноценный JDK 21, а не только JRE, и что Maven Wrapper использует этот JDK.

### Tests have failures

Откройте отчёты в `target/surefire-reports`, исправьте тест или production-код и повторите полную команду. Не обходите ошибку флагом `-DskipTests` в релизной сборке.

### Ошибка подключения к PostgreSQL при запуске EXE

Это не ошибка упаковки. Проверьте, что PostgreSQL запущен, база создана, реквизиты верны и Flyway может применить миграции.

### Старый EXE остался в каталоге

Используйте `clean` и убедитесь, что никакой процесс не удерживает файл из `target/dist`.
