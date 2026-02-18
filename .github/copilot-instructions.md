# Copilot instructions for ms-account-service

## Architecture & flow
- Spring Boot monolith with layered flow: controller → service → repository → JPA entities.
- REST controllers live in [src/main/java/com/fintech/account/controller](src/main/java/com/fintech/account/controller) and delegate to services like `AccountService` and `TransactionService`.
- Core business rules are enforced in services, e.g. KYC must be VERIFIED before account creation and account status must be ACTIVE for credit/debit ([AccountService](src/main/java/com/fintech/account/service/AccountService.java), [TransactionService](src/main/java/com/fintech/account/service/TransactionService.java)).
- Transaction logic is atomic via `@Transactional`, and balance updates are persisted alongside `Transaction` records ([TransactionService](src/main/java/com/fintech/account/service/TransactionService.java)).
- DTOs use Jakarta validation annotations; controller endpoints expect `@Valid` payloads ([CustomerRequestDTO](src/main/java/com/fintech/account/dto/CustomerRequestDTO.java), [TransactionRequestDTO](src/main/java/com/fintech/account/dto/TransactionRequestDTO.java)).
- Centralized error mapping is handled in `GlobalExceptionHandler` with structured error payloads ([GlobalExceptionHandler](src/main/java/com/fintech/account/exception/GlobalExceptionHandler.java)).

## Data model
- JPA entities in [src/main/java/com/fintech/account/model](src/main/java/com/fintech/account/model) with timestamps via `@CreationTimestamp`/`@UpdateTimestamp`.
- Repositories extend `JpaRepository` and use Spring Data derived queries ([AccountRepository](src/main/java/com/fintech/account/repository/AccountRepository.java), [TransactionRepository](src/main/java/com/fintech/account/repository/TransactionRepository.java)).

## Conventions & quirks
- Package declarations are authoritative even if file paths are odd. Example: `AccountRequestDTO` is stored under model but declared in package `com.fintech.account.dto` ([AccountRequestDTO](src/main/java/com/fintech/account/model/AccountRequestDTO.java)).
- There is a duplicate `AccountRepository` file under DTOs with the same package name; treat it as an accidental duplicate and avoid creating more copies ([src/main/java/com/fintech/account/dto/AccountRepository.java](src/main/java/com/fintech/account/dto/AccountRepository.java)).
- OpenAPI config file path doesn’t match its package; use the package name when searching by type ([OpenApiConfig](src/main/java/com/fintech/config/OpenApiConfig.java)).

## Developer workflows
- Build: `mvn clean install` (uses Java 17; see [pom.xml](pom.xml)).
- Run: `mvn spring-boot:run` (default port 8080; config in [src/main/resources/application.properties](src/main/resources/application.properties)).
- Dev profile uses a different DB and `ddl-auto=create-drop` in [src/main/resources/application-dev.properties](src/main/resources/application-dev.properties). Use `-Dspring-boot.run.profiles=dev` if needed.
- API docs via SpringDoc at `/swagger-ui.html` and `/api-docs` (see [application.properties](src/main/resources/application.properties)).

## External dependencies & integration points
- PostgreSQL is the primary runtime database; H2 is test-only (see [pom.xml](pom.xml) and [application.properties](src/main/resources/application.properties)).
- Account numbers are generated via `AccountNumberGenerator` (timestamp + random suffix) in [src/main/java/com/fintech/account/util/AccountNumberGenerator.java](src/main/java/com/fintech/account/util/AccountNumberGenerator.java).