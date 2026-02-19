# Fintech Account Management Microservice

A production-ready Spring Boot microservice for managing fintech accounts, transactions, and customer information.

## Features

- ✅ Account Management (Create, Read, Update, Status Management)
- ✅ Balance Management (Credit, Debit operations)
- ✅ Customer Management
- ✅ Transaction History & Audit Trail
- ✅ Multiple Account Types (Savings, Checking, Business)
- ✅ Multi-currency Support
- ✅ RESTful API with OpenAPI/Swagger Documentation
- ✅ PostgreSQL Database Integration
- ✅ Comprehensive Exception Handling
- ✅ Input Validation
- ✅ Transaction Atomicity

## Tech Stack

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- PostgreSQL
- Lombok
- SpringDoc OpenAPI
- Maven

## Prerequisites

- JDK 17 or higher
- Maven 3.6+
- PostgreSQL 12+

## Setup Instructions

### 1. Clone the repository

```bash
git clone https://github.com/peterkimeli/ms-account-service.git
cd ms-account-service
```

### 2. Configure Database

Create a PostgreSQL database:

```sql
CREATE DATABASE fintech_accounts;
```

Update `src/main/resources/application.properties` with your database credentials.

### 3. Build the project

```bash
mvn clean install
```

### 4. Run the application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Documentation

Once the application is running, access the Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

## API Endpoints

### Account Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/accounts` | Create new account |
| GET | `/api/v1/accounts/{accountNumber}` | Get account details |
| GET | `/api/v1/accounts/customer/{customerId}` | Get accounts by customer |
| PUT | `/api/v1/accounts/{accountNumber}` | Update account |
| PATCH | `/api/v1/accounts/{accountNumber}/status` | Update account status |
| GET | `/api/v1/accounts/{accountNumber}/balance` | Get account balance |

### Transaction Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/transactions/credit` | Credit account |
| POST | `/api/v1/transactions/debit` | Debit account |
| GET | `/api/v1/transactions/{accountNumber}` | Get transaction history |
| GET | `/api/v1/transactions/details/{transactionId}` | Get transaction details |

### Customer Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/customers` | Create customer |
| GET | `/api/v1/customers/{customerId}` | Get customer details |
| PUT | `/api/v1/customers/{customerId}` | Update customer |

## Example Requests

### Create a Customer

```bash
curl -X POST http://localhost:8080/api/v1/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "+1234567890",
    "kycStatus": "VERIFIED"
  }'
```

### Create an Account

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "accountType": "SAVINGS",
    "currency": "USD",
    "initialDeposit": 1000.00
  }'
```

### Credit Account

```bash
curl -X POST http://localhost:8080/api/v1/transactions/credit \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "ACC1234567890",
    "amount": 500.00,
    "description": "Salary deposit"
  }'
```

### Debit Account

```bash
curl -X POST http://localhost:8080/api/v1/transactions/debit \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "ACC1234567890",
    "amount": 100.00,
    "description": "ATM withdrawal"
  }'
```

## Database Schema

### Account Table
- `id` (BIGINT, PK)
- `account_number` (VARCHAR, UNIQUE)
- `customer_id` (BIGINT, FK)
- `account_type` (VARCHAR)
- `balance` (DECIMAL)
- `currency` (VARCHAR)
- `status` (VARCHAR)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

### Customer Table
- `id` (BIGINT, PK)
- `first_name` (VARCHAR)
- `last_name` (VARCHAR)
- `email` (VARCHAR, UNIQUE)
- `phone` (VARCHAR)
- `kyc_status` (VARCHAR)
- `created_at` (TIMESTAMP)

### Transaction Table
- `id` (BIGINT, PK)
- `account_id` (BIGINT, FK)
- `transaction_type` (VARCHAR)
- `amount` (DECIMAL)
- `balance_before` (DECIMAL)
- `balance_after` (DECIMAL)
- `description` (VARCHAR)
- `status` (VARCHAR)
- `transaction_date` (TIMESTAMP)

## Error Handling

The API uses standard HTTP status codes:

- `200 OK` - Successful GET/PUT requests
- `201 Created` - Successful POST requests
- `400 Bad Request` - Invalid input
- `404 Not Found` - Resource not found
- `409 Conflict` - Business rule violation
- `500 Internal Server Error` - Server errors

## Security Considerations

- Input validation on all endpoints
- Transaction atomicity for financial operations
- Thread-safe balance updates
- Audit trail for all transactions
- KYC status verification

## Testing

Run unit tests:

```bash
mvn test
```

Run integration tests:

```bash
mvn verify
```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Contact

Peter Kimeli - [@peterkimeli](https://github.com/peterkimeli)

Project Link: [https://github.com/peterkimeli/ms-account-service](https://github.com/peterkimeli/ms-account-service)
```

Build & Test → Code Quality → Trivy FS Scan → ┬─ AI Code Quality   ─┬→ Docker Build → Trivy Image Scan → Push to ACR
                                               └─ AI Test Automation ┘