# datamessie

datamessie is a solution for scraping online news articles via RSS feeds and also processes articles automatically to facilitate content analysis.
It consists of a web app and a database.


## Prerequisites

- Java 8
- Maven 3
- Docker


## Build

Build web app:
```bash
mvn clean package
docker-compose build
```


## Run

Start database container:
```bash
docker-compose up db
```

Start web app container:
```bash
docker-compose up webapp
```

Start all containers:
```bash
docker-compose up
```

Stop all running containers:
```bash
docker-compose down
```
