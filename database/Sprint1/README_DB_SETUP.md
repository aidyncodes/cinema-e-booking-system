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

The database is automatically initialized from:

```text
db/init/01_movies.sql
```

It creates the `movies` table and inserts 15 seeded movies.

## Spring Boot connection

Use these settings in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:33306/ces_db
spring.datasource.username=root
spring.datasource.password=mysqlpass
spring.sql.init.mode=never
spring.jpa.hibernate.ddl-auto=none
```