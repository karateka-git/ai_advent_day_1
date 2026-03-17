# AI Advent Day 1

CLI-приложение на Kotlin для общения с моделью через Timeweb Cloud AI API.

## Возможности

- интерактивный чат в консоли;
- отправка всей истории сообщений в API;
- индикатор ожидания, пока модель формирует ответ;
- сохранение текущего диалога в `output.txt` в кодировке UTF-8;
- чтение `AGENT_ID` и `USER_TOKEN` из локального конфигурационного файла, который не попадает в git.

## Технологии

- Kotlin
- Gradle
- kotlinx.serialization
- Java `HttpClient`

## Требования

- Java 21
- доступ в интернет для запросов к API

## Настройка

1. Склонируйте репозиторий.
2. Создайте файл `config/app.properties` на основе `config/app.properties.example`.
3. Заполните его своими значениями:

```properties
AGENT_ID=your-agent-id
USER_TOKEN=your-user-token
```

Файл `config/app.properties` исключён из git, поэтому секреты не попадут в репозиторий.

## Сборка

```powershell
.\gradlew.bat build
```

## Рекомендуемый запуск

Для интерактивного чата на Windows лучше использовать сгенерированный launcher:

```powershell
.\gradlew.bat installDist
.\build\install\ai_advent_day_1\bin\ai_advent_day_1.bat
```

После запуска:

- введите сообщение и нажмите Enter;
- для выхода введите `exit` или `quit`.

## Альтернативный запуск

```powershell
.\gradlew.bat run
```

Но для интерактивного режима на Windows launcher из `installDist` обычно работает надёжнее.

## Где смотреть результат

- ответ модели выводится в консоль;
- полный текущий диалог сохраняется в `output.txt`.

Если терминал Windows отображает кириллицу некорректно, откройте `output.txt`: файл сохраняется в UTF-8 и подходит как надёжный способ просмотра ответа.

## Структура проекта

- `src/main/kotlin/Main.kt` — точка входа и логика консольного чата
- `src/main/kotlin/models/` — модели запроса и ответа API
- `config/app.properties.example` — пример локальной конфигурации
- `output.txt` — сохранённый диалог
