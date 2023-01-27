# datamessie

datamessie is a solution for scraping online news articles via RSS feeds. It also processes articles automatically to facilitate content analysis.

## Build

### Web App

Prerequisites: Java 8, Maven, Docker
```bash
mvn clean package
docker build -t datamessie-webapp ./datamessie-core
```

## Run

Prerequisites: Docker
```bash
docker-compose up
```
