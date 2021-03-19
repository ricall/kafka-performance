# kafka-performance project
Simple project that is used to test spring boot kafka against spring cloud streams kafka 

# Getting started
To start development start the dockers containers
```bash
make start-dev
```

## Accessing the broker terminal
```bash
make broker-terminal
```
Will respond with `[appuser@broker ~]$` - you are now in the broker terminal

### Useful commands
```bash
kafka-topics -bootstrap-server localhost:9092 --list
```

## Accesing the database terminal
In a separate terminal you can access the database to make sure it is working:
```bash
make db-terminal
```
You can open a SQL terminal using:
```bash
psql --host=database --username=admin --dbname=messages
```
The password when prompted is `password`
```sql
SELECT * FROM KAFKA_MESSAGE;
```

## Sending messages to the MessageProducer
```http request
POST http://localhost:8200/bridge
Content-Type: text/plain

3;Testing
```


# Testing Methodology
## Delete all the messages in the Database
```sql
DELETE FROM kafka_message;
```
## Build the applications
```bash
./gradlew build
```
## Start the kafka-listeners (3)
If you want to test multiple instances listening to the `dev.message` topic using spring boot kafka
```bash
java -jar kafka-consumer/build/libs/kafka-consumer-0.0.1-SNAPSHOT.jar --server.port=8201  
java -jar kafka-consumer/build/libs/kafka-consumer-0.0.1-SNAPSHOT.jar --server.port=8202  
java -jar kafka-consumer/build/libs/kafka-consumer-0.0.1-SNAPSHOT.jar --server.port=8203  
```

## or Start the kafka-cloud-consumers (3)
If you want to test multiple instances listening to the `dev.message` topic using spring cloud streams
```bash
java -jar spring-cloud-consumer/build/libs/spring-cloud-consumer-0.0.1-SNAPSHOT.jar --server.port=8201
java -jar spring-cloud-consumer/build/libs/spring-cloud-consumer-0.0.1-SNAPSHOT.jar --server.port=8202
java -jar spring-cloud-consumer/build/libs/spring-cloud-consumer-0.0.1-SNAPSHOT.jar --server.port=8203
```

## Start the producer (1)
```bash
java -jar build/libs/kafka-producer-0.0.1-SNAPSHOT.jar
```

## Killing random servers
```bash
ps aux | fgrep consumer
```
lists the java consumers running, use kill -9 `pid` to kill the application, then restart it

## Reviewing Run
The listeners should be running in `at-least-once` delivery mode so we would expect to get some messages multiple
times, but no messages should be dropped.

```sql
SELECT COUNT(1) FROM KAFKA_MESSAGE;
```
This will be 10,000 if the servers ae gracefully shutdown - performing kill -9 we would expect this to be > 10,000

```sql
SELECT COUNT(DISTINCT key) FROM kafka_message;
```
This must always be 10,000 any lower and messages have been dropped.

```sql
SELECT DISTINCT(key), COUNT(key)
FROM kafka_message
GROUP BY key
HAVING COUNT(key) != 1;
```
Useful when you are running multiple times - it should list out any messages that don't have the expected number
of sends.

```sql
DELETE FROM kafka_message;
```
Clean up after testing.
