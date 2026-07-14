# CES Sprint 1 Database Setup

This package gives everyone the same MySQL database using Docker.

## Requirement

Install Docker first.

## Start the database

From this folder, run:

```bash
docker compose up -d
```

This starts MySQL with:

- host: `localhost`
- port: `33306`
- database: `ces_db`
- username: `root`
- password: `mysqlpass`

# CES Sprint 2 Database Setup

The database is automatically initialized from:

```text
db/init/00_reset_schema.sql
db/init/01_movies.sql
db/init/02_users.sql
```

It creates the `movies`, `users`, `addresses`, `payment_cards`, `favorites`,
`email_confirmation_tokens`, and `password_reset_tokens` tables.
The movie script also inserts seeded movies.

Note: Docker only runs files in `db/init` when the MySQL volume is first created.
If you already started the Sprint 1 database, apply new scripts manually or recreate
the Docker volume before testing the new tables.

## Rebuild the database

If your local Docker database already has old tables, rebuild it from scratch:

```bash
docker compose down -v
docker compose up -d
```

This deletes the old MySQL volume and reruns all init scripts in order:

1. `00_reset_schema.sql` drops existing project tables
2. `01_movies.sql` recreates and seeds movies
3. `02_users.sql` recreates and seeds users, addresses, cards, favorites, and tokens

Demo accounts seeded by `02_users.sql`:

| Email | Password | Role | Status | Purpose |
| --- | --- | --- | --- | --- |
| `admin@cinema.com` | `Password123!` | `ADMIN` | `ACTIVE` | Admin login and admin redirect testing |
| `customer@cinema.com` | `Password123!` | `CUSTOMER` | `ACTIVE` | Customer login, profile with 3 payment cards, and favorites testing |
| `inactive@cinema.com` | `Password123!` | `CUSTOMER` | `INACTIVE` | Inactive account login validation testing |

The password is stored in the database as a BCrypt hash, not as plaintext.

## Spring Boot connection

Use these settings in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:33306/ces_db
spring.datasource.username=root
spring.datasource.password=mysqlpass
spring.sql.init.mode=never
spring.jpa.hibernate.ddl-auto=none
```
