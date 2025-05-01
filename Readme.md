# Docker App 1
This is a sample java spring boot application with dockerfile. This is to demonstrate how to containerize java applications using docker and kubernetes.

## Supporting App
This app tries to make connection with **app2** and hence running app2 parallely is also crucial.
Kindly clone the same from
[docker-app2](https://github.com/prat21/docker-app2).

## Command to build and push images using docker
First login.
```
docker login
```
To build image.
```
docker build -t prat21/app1 .
```
To push image.
```
docker push
```

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

## Command to build and push images using docker compose
This is to push the images using docker compose tool, so that it can be run using kubernetes, as kubernetes pulls image from image repository to create deployment resource.
```
docker-compose build --push
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

### Running application in kubernetes locally using minikube:
We can test the application in kubernetes locally with the help of minikube. For that we have to first install minikube. References are given below:
* [Udemy](https://www.udemy.com/course/docker-kubernetes-the-practical-guide/learn/lecture/22627611#overview)

Start minikube:
```
minikube start --driver=docker
```
Check minikube status:
```
minikube status
```
Stop minikube:
```
minikube stop
```
Open minikube dashboard:
```
minikube dashboard
```
Also, as part of this process, create **.kube** folder in your home directory and inside that create **config** file, before installing minikube. This file holds all the kubernetes connection details to be used by **kubectl**.
While installing minikube, this file automatically gets configured by minikube's cluster information.

Once minikube is installed and kubectl is also configured, we can run the kubernetes manifest files using normal **kubectl** commands.

For example:

#### To create a deployment:
```
kubectl apply -f .\k8s\deployment.yaml
```
#### To create a service:
```
kubectl apply -f .\k8s\service.yaml
```
Here we are running the **app1** application with **spring.profiles.active=minikube** (set via env variable in deployment.yaml), so that app1 can communicate with **app2** using kubernetes internal DNS, which by default registers all the created service, in this case **app2-service**.

Please note that **app2-service** is configured as clusterIP which is an internal IP of the cluster, but since **app1-service** and **app2-service** both are inside the same cluster, hence app1 can communicate internally with app2 using internal DNS name (which is app2-service as configured in **configMap.yaml**)

#### To create configMap:
```
kubectl apply -f .\k8s\configMap.yaml
```
Before this we have to create clusterRole and clusterRoleBinding so that the default service account has the permission to access the configMap. Otherwise the pods will error out while starting.
```
kubectl apply -f .\k8s\configMapRole.yaml
```
#### Mount configMap as volume in a test pod for testing:
Create a test pod with alpine base image using **pod-alpine.yaml** file. The options **tty** and **stdin** in the container spec of the pod manifest file correspond to the **-it** option while running a container using docker or kubectl.
These options keep the session(/bin/sh) inside the pod alive, otherwise the pod will transition to **complete** status(try by removing the tty and stdin option).
```
kubectl apply -f .\k8s\pod-alpine.yaml
```
As the **configMap** has been mounted inside **pod-alpine** as a volume at the path **/config**, hence this path should be accessible from inside the pod.
To test the same, we attach with the session inside the pod using the following command
```
kubectl attach -it alpine-pod
```
This will open a session inside the pod and from there we can check the path **/config**.
This path should contain the file **application.yml**. This proves that the configMap has been successfully mounted inside the pod.

References:
* [Docker exec command](https://docs.docker.com/reference/cli/docker/container/exec/)
* [Kubectl exec commmand](https://kubernetes.io/docs/tasks/debug/debug-application/get-shell-running-container/)
* [Stack Overflow](https://stackoverflow.com/questions/59965032/docker-run-with-interactive-and-tty-flag)