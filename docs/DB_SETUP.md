# PostgreSQL: Database Setup

## Option A — SQL directly
```sql
CREATE USER cash WITH PASSWORD 'StrongPass123!';
CREATE DATABASE cash_store OWNER cash;
GRANT ALL PRIVILEGES ON DATABASE cash_store TO cash;

## Ensure PostgreSQL is listening on localhost:5432
## Change in application.yml username and password 
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cash_store
    username: cash
    password: StrongPass123!
  jpa:
    hibernate:
      ddl-auto: none
  flyway:
    enabled: true