# Jobflow

**Jobflow** — backend-система для асинхронной обработки задач на Java и Spring Boot.

Проект демонстрирует практическое применение Kafka, Transactional Outbox Pattern, повторной обработки задач, идемпотентности и управления состоянием задач.

### Основные компоненты

- **API Service** — принимает HTTP-запросы и управляет созданием задач.
- **PostgreSQL** — хранит задачи, историю событий и события Outbox.
- **Transactional Outbox** — гарантирует атомарное сохранение задачи и события для последующей публикации.
- **Kafka** — обеспечивает асинхронную передачу событий между сервисами.
- **Worker Service** — получает события из Kafka и выполняет задачи.

## Используемые технологии

- Java 21
- Spring Boot 3.5
- Maven
- PostgreSQL 17
- Apache Kafka 4.1 (KRaft)
- Spring Data JPA
- Flyway
- Docker Compose
- JUnit 5, Mockito
- GitHub Actions

## Поддерживаемые типы задач

| Тип | Описание |
|---|---|
| `CHECK_URL` | Выполняет HTTP-запрос и сохраняет код ответа |
| `GENERATE_REPORT` | Имитирует генерацию отчёта и сохраняет результат |

## Запуск проекта

### Требования

- JDK 21
- Maven 3.9+
- Docker Desktop с поддержкой Docker Compose

### 1. Запуск инфраструктуры

Из корневой директории проекта выполни:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Команда запускает PostgreSQL и Kafka.

### 2. Запуск API Service

Запусти класс `JobflowApiApplication` через IntelliJ IDEA.

API будет доступен по адресу:

```text
http://localhost:8080
```

### 3. Запуск Worker Service

Запусти класс `JobflowWorkerApplication` через IntelliJ IDEA.

Worker подключится к Kafka и PostgreSQL и начнёт получать события о новых задачах.

## REST API

Базовый путь: `/api/v1/jobs`

| Метод | Endpoint | Описание |
|---|---|---|
| POST | `/api/v1/jobs` | Создание задачи |
| GET | `/api/v1/jobs` | Получение списка задач |
| GET | `/api/v1/jobs/{id}` | Получение информации о задаче |
| GET | `/api/v1/jobs/{id}/events` | Получение истории событий задачи |

### Пример создания задачи CHECK_URL

```http
POST /api/v1/jobs
Content-Type: application/json
```

```json
{
  "type": "CHECK_URL",
  "payload": {
    "url": "https://example.com"
  }
}
```

API возвращает `202 Accepted` с информацией о созданной задаче.

### Пример создания задачи GENERATE_REPORT

```json
{
  "type": "GENERATE_REPORT",
  "payload": {
    "reportName": "Monthly Sales",
    "delaySeconds": 2
  }
}
```

## Надёжность обработки

В проекте реализованы:

- Transactional Outbox Pattern.
- Асинхронная обработка сообщений через Kafka.
- Повторная обработка задач при ошибках.
- Ограничение количества попыток обработки.
- Сохранение состояния задач в PostgreSQL.
- История изменения статусов задач.
- Идемпотентная обработка сообщений с использованием блокировок базы данных.

## Тестирование

Для сборки проекта и запуска тестов выполни:

```bash
mvn clean verify
```

В проекте есть интеграционные тесты репозиториев API Service и модульные тесты обработки задач Worker Service.

GitHub Actions автоматически собирает проект и запускает тесты при отправке изменений в `main` и создании Pull Request в `main`.

## Проверка состояния приложения

API предоставляет endpoint Spring Boot Actuator:

```http
GET http://localhost:8080/actuator/health
```

## Безопасность

Реализация `CHECK_URL`:

- Разрешает только HTTP и HTTPS.
- Разрешает только стандартные порты 80 и 443.
- Запрещает указание логина и пароля в URL.
- Блокирует локальные и приватные IP-адреса.
- Отключает автоматическое следование редиректам.

## Структура проекта

```text
jobflow/
├── common/
├── api-service/
├── worker-service/
├── docker/
│   └── docker-compose.yml
├── .github/
│   └── workflows/
│       └── ci.yml
└── pom.xml
```