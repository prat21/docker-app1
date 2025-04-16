# Docker App 1
This is a sample java spring boot application with dockerfile. This is to demonstrate how to containerize java applications using docker and kubernetes.

## Command to run container using docker
```
docker run --name=<CONTAINER_NAME> --rm -p 8081:8081 <IMAGE_NAME>
```

## Commands to run container using docker compose
* Run containers in attached mode. Put **--build** argument to make sure that the image is built every time.
```
docker-compose up --build
```
* Run containers in detached mode
```
docker-compose up -d --build
```