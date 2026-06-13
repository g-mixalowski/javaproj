# Library Service

## Краткое описание

Учебный REST-сервис на Java + Spring Boot для управления небольшой
библиотекой. Хранит:

- **каталог** книг и их авторов (метаданные: название, описание, год, кол-во страниц),
- **читателей**,
- **выдачи** — кто и когда взял книгу, когда вернул,
- **закладки чтения** — текущая страница каждого читателя по каждой книге;
  закладка переживает возврат книги, поэтому при следующей выдаче читатель
  продолжит с того же места (модель Goodreads / трекер чтения).

Сервис хранит метаданные книги и её FB2-текст для встроенной читалки. Полное описание
функциональности, API и доменной модели см. в [DESIGN.md](DESIGN.md).

## Требования

- JDK 21+
- Maven 3.9+

## Запуск приложения

```bash
mvn spring-boot:run
```

После запуска:

- REST API: `http://localhost:8080/api/...`
- H2-консоль: `http://localhost:8080/h2-console`
  (JDBC URL `jdbc:h2:mem:librarydb`, user `sa`, пустой пароль)

База — H2 in-memory, поэтому при перезапуске данные сбрасываются.

## Запуск тестов

```bash
mvn test
```

Всего 47 тестов:

- **39 unit-тестов сервисов** (JUnit 5 + Mockito, моки репозиториев) — проверяют
  бизнес-инварианты и happy-path каждого сервиса;
- **8 интеграционных тестов** (`@SpringBootTest` + `MockMvc` + реальный H2) —
  гоняют сквозные сценарии (создание → выдача → закладка → возврат) и проверяют
  коды ошибок (400/404/409).

## Структура проекта

```
java_project/
├── pom.xml                       — Maven, Spring Boot 3.4, Java 21
├── DESIGN.md                     — диздок (доменная модель, API, инварианты)
├── README.md                     — этот файл
└── src/
    ├── main/
    │   ├── java/com/example/library/
    │   │   ├── LibraryApplication.java     — точка входа
    │   │   ├── controller/                 — REST-контроллеры (5 шт.)
    │   │   │   ├── AuthorController.java
    │   │   │   ├── BookController.java
    │   │   │   ├── ReaderController.java
    │   │   │   ├── LoanController.java
    │   │   │   └── ReadingProgressController.java
    │   │   ├── service/                    — бизнес-логика, инварианты (5 шт.)
    │   │   ├── repository/                 — Spring Data JPA-репозитории (5 шт.)
    │   │   ├── model/                      — JPA-сущности (5 шт.)
    │   │   ├── dto/                        — Request/Response (Java records)
    │   │   └── exception/                  — NotFound/Conflict +
    │   │                                     GlobalExceptionHandler
    │   └── resources/
    │       └── application.properties      — настройки H2 и JPA
    └── test/
        └── java/com/example/library/
            ├── service/                    — unit-тесты сервисов
            └── controller/                 — интеграционные тесты MockMvc
```

Архитектура — классический трёхслойный Spring:
`Controller` (HTTP) → `Service` (бизнес-логика, валидация инвариантов) →
`Repository` (Spring Data JPA) → БД.

## Стек

- Java 21
- Spring Boot 3.4 — Web, Data JPA, Validation
- H2 (in-memory)
- JUnit 5, Mockito, AssertJ, MockMvc

## Краткая шпаргалка по API

Базовый префикс — `/api`. Полная таблица эндпоинтов — в [DESIGN.md](DESIGN.md),
раздел 5.

```bash
# Создать автора
curl -X POST http://localhost:8080/api/authors \
  -H 'Content-Type: application/json' \
  -d '{"firstName": "Лев", "lastName": "Толстой"}'

# Создать книгу
curl -X POST http://localhost:8080/api/books \
  -H 'Content-Type: application/json' \
  -d '{"title": "Война и мир", "description": "Роман-эпопея",
       "year": 1869, "pageCount": 1300, "authorId": 1}'

# Зарегистрировать читателя
curl -X POST http://localhost:8080/api/readers \
  -H 'Content-Type: application/json' \
  -d '{"firstName": "Иван", "lastName": "Иванов", "email": "ivan@example.com"}'

# Выдать книгу
curl -X POST http://localhost:8080/api/loans \
  -H 'Content-Type: application/json' \
  -d '{"bookId": 1, "readerId": 1}'

# Сохранить закладку: остановился на 9 стр.
curl -X PUT http://localhost:8080/api/readers/1/progress/1 \
  -H 'Content-Type: application/json' \
  -d '{"currentPage": 9}'

# Вернуть книгу (закладка сохраняется)
curl -X POST http://localhost:8080/api/loans/1/return

# Прочитать закладку
curl http://localhost:8080/api/readers/1/progress/1
```

## Формат ошибок

```json
{ "status": 404, "error": "Not Found", "message": "Author 5 not found" }
```

| Код | Когда                                                         |
|-----|---------------------------------------------------------------|
| 400 | Невалидный JSON или нарушение `@Valid` (пустое поле, плохой email) |
| 404 | Сущность не найдена                                           |
| 409 | Нарушение бизнес-инварианта (см. диздок, раздел 4)            |
