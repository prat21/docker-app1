# Docker App 1
This is a sample java spring boot application with dockerfile. This is to demonstrate how to containerize java applications using docker and kubernetes.

## Supporting App
This app tries to make connection with **app2** and hence running app2 parallely is also crucial.
Kindly clone the same from
[docker-app2](https://github.com/prat21/docker-app2).

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

## Modes of running the application
### Running application in standalone mode:
Run the **app1** and **app2** applications as plain spring boot application without any containerization. Run **app1** using **--spring.profiles.active=local** configuration so that **app1** communicates with **app2** using **localhost**.

### Running application in containers:
Run the **docker-compose up --build** commands on both the apps to run the applications inside containers. 
The docker-compose file of **app1** points to the **env/app1.env** file and hence **app1** communicates with **app2** using host name, 
ie host name which is resolved by docker internally for services running in the same network. 
We have first created a custom network **app1-network** for **app1** and then connected **app2** with that network. 
Check the below references:
* [Udemy](https://www.udemy.com/course/docker-kubernetes-the-practical-guide/learn/lecture/22166972#overview)
* [Docker Docs](https://docs.docker.com/compose/how-tos/networking/)
* [Stack Overflow](https://stackoverflow.com/questions/38088279/communication-between-multiple-docker-compose-projects)
* https://medium.com/@caysever/docker-compose-network-b86e424fad82
