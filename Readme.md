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
Expose a loadbalancer service in minikube:
```
minikube service <service-name>
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

#### Mount configMap in the app1 deployment:
The configMap can be mounted in the **app1** application using volumes.
The container spec is updated accordingly in the deployment.yaml. The configMap has been mounted in the **/config** path of the container.
Also, the spring boot application **app1** can import the configuration from the mounted folder using the **spring.config.import: file:/config/application.yml** property in the bootstrap.yml file.
This will import the configuration from the mounted application.yml and bootstrap the application.

In addition to this, to facilitate hot reloading of properties we have used **@RefreshScope** annotation(on TestController.java), which refreshes the environment of the spring context and hence properties imported using **@Value** and **@ConfigurationProperties** are refreshed without restarting the entire application.
For this **spring-boot-actuator** is required in the classpath. Also the **refresh** endpoint has to be enabled in the application.yml file.

After a configMap is updated, we can just send a POST request to the **/actuator/refresh** endpoint and the context will be refreshed automatically.
We can verify this using the **/test** endpoint of app1.

Please note that after the configMap has been updated, it takes some time for the pod/container to sync the latest changes in the mounted volume. Hence keep hitting **/actuator/refresh** endpoint until you get a valid response(It may take 30-60 seconds).

References:
* [Mounted configmaps are updated automatically](https://kubernetes.io/docs/concepts/configuration/configmap/#mounted-configmaps-are-updated-automatically)
* [Spring Config Import](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.files.configtree)
* [ConfigMaps and Pods](https://kubernetes.io/docs/concepts/configuration/configmap/#configmaps-and-pods)

### Running application in google kubernetes engine:
First we have to install the gcloud CLI. 

Also install **gke-gcloud-auth-plugin**. This is necessary for **kubectl** to interact with GKE.
```
gcloud components install gke-gcloud-auth-plugin
```
Initialize gcloud CLI:
```
gcloud init
```
To run the application in GKE first we have to create the cluster(autopilot mode) by running the following command:
```
gcloud container clusters create-auto <CLUSTER_NAME> --location=asia-south1
```
This will create a regional cluster(asia-south1 is MUMBAI).

First create the configMap
```
kubectl apply -f .\k8s\configMap.yaml
```
Then create the deployment
```
kubectl apply -f .\k8s\deployment.yaml
```
Then create service
```
kubectl apply -f .\k8s\service.yaml
```
Do the same for **app2**(except for configMap)

**Note:** There maybe initial error in pod scheduling since the cluster runs in autopilot mode and may take some time to provision resources.

### Running test pod in google kubernetes engine with cloud storage FUSE CSI driver:
Reference:
* [Quickstart: Access Cloud Storage buckets with the FUSE CSI driver](https://cloud.google.com/kubernetes-engine/docs/how-to/persistent-volumes/cloud-storage-fuse-csi-driver)

First create a cloud storage bucket and upload some files for testing from cloud console. Make sure **Uniform bucket level access** is configured instead of fine-grained ACLs.

#### Configure access to cloud storage bucket using gke workload identity:
```
kubectl create namespace <NAMESPACE_NAME>
kubectl create serviceaccount <KUBERNETES_SERVICE_ACCOUNT_NAME> --namespace <NAMESPACE_NAME>
gcloud storage buckets add-iam-policy-binding gs://BUCKET_NAME --member "principal://iam.googleapis.com/projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/PROJECT_ID.svc.id.goog/subject/ns/NAMESPACE_NAME/sa/KUBERNETES_SERVICE_ACCOUNT_NAME" --role "roles/storage.objectAdmin"
```
Reference:
* [Configure access to Cloud Storage buckets](https://cloud.google.com/kubernetes-engine/docs/how-to/cloud-storage-fuse-csi-driver-setup#authentication)

#### Create a test pod and mount the cloud storage bucket as ephemeral volume using cloud FUSE driver:
Apply the pod manifest to create the test pod
```
kubectl apply -f .\k8s\pod-csi-fuse-test.yaml
```
This will create a pod and mount the **pratbucket** bucket as ephemeral volume at the path **fusevol**. Now we can access the volume from within the container.
First initiate an interactive session in the pod.
```
kubectl exec -it fuse-pod -n my-space -- busybox bash
```
Then check the contents of the **fusevol** folder(from within the interactive bash session).
```
cd fusevol
cat <ANY_FILE_OF_THE_BUCKET>
```
The file should be accessible.

**Note**: Check [Mount Cloud Storage buckets as persistent volumes](https://cloud.google.com/kubernetes-engine/docs/how-to/cloud-storage-fuse-csi-driver-pv) to see how to mount cloud storage buckets as persistent volumes.

### Create multiple test pods with the same cloud storage bucket as ephemeral volume to test parallel read writes:
Apply the pod manifest to create two pods with the same cloud storage bucket as ephemeral volume:
```
kubectl apply -f .\k8s\multi-pod-csi-fuse-test.yaml
```
This will create two pods **fuse-pod1** and **fuse-pod2** with the same cloud storage bucket **pratbucket** as volume.

Initiate interactive sessions from both the pods(in separate terminals):
```
kubectl exec -it fuse-pod1 -n my-space -- busybox bash
kubectl exec -it fuse-pod2 -n my-space -- busybox bash
```
Try creating a new file in **fuse-pod1** session inside the **fusevol** volume and check whether the same reflects on the **fusevol** volume of **fuse-pod2** session. Do the same for deletion of a file. 

We can see that the creation and deletion gets synchronized across all the volumes linked to the bucket, thus demonstrating that cloud storage FUSE csi driver supports ReadWriteMany mode.

### Create a test pod of "app1" with cloud storage bucket as ephemeral volume:
Apply the pod manifest to create a pod of **app1** with **pratbucket** as mounted volume.
```
kubectl apply -f .\k8s\pod-app-csi-fuse-test.yaml
```
While the pod starts, check the logs of the pod:
```
kubectl logs app1-fuse-pod -n my-space
```
You will notice that the contents of the **fusevol** volume is printed in the startup logs of the application(due to the commandLineRunner in the DockerApp1 class).
This shows that the cloud storage bucket was successfully mounted in the **fusevol** volume inside the pod.

**Reference**:
* [Quickstart: Access Cloud Storage buckets with the FUSE CSI driver](https://cloud.google.com/kubernetes-engine/docs/how-to/persistent-volumes/cloud-storage-fuse-csi-driver)
* [Mount Cloud Storage buckets as CSI ephemeral volumes](https://cloud.google.com/kubernetes-engine/docs/how-to/cloud-storage-fuse-csi-driver-ephemeral)

### Create a deployment and service of "app1" with cloud storage bucket as ephemeral volume:
Apply the deployment and service manifest to create exposed deployment of **app1** application with cloud storage bucket mounted as ephemeral volume:
```
kubectl apply -f .\k8s\deployment-csi-fuse.yaml
kubectl apply -f .\k8s\service-csi-fuse.yaml
```
Once the service is ready with an external IP, call the get endpoint **/docker/app1/bucket/files** with request parameter **volume=/fusevol.**
The endpoint returns the list of files of the directory/volume given as input by the request param **volume.**

The returned list of files proves that the bucket was mounted successfully(at **fusevol** mount path) inside the pods of the deployment.

To upload a file into the volume/bucket, use the POST endpoint **/docker/app1/upload** and attach the file as form-data request body. After the upload is successful, check whether the file got uploaded or not in the cloud storage bucket.

### Create a deployment and service of "app1" with cloud storage bucket as persistent volume:
First we have to create persistent volume and persistent volume claim for this:
```
kubectl apply -f .\k8s\persistentVolume-csi-fuse.yaml
kubectl apply -f .\k8s\persistentVolumeClaim-csi-fuse.yaml
```
Next we will create the deployment and a corresponding loadbalancer service:
```
kubectl apply -f .\k8s\deployment-pv-csi-fuse.yaml
kubectl apply -f .\k8s\service-pv-csi-fuse.yaml
```
This will create a deployment with the cloud storage bucket mounted as persistent volume claims in the pods.
Now we can do **list files** and **upload files** operation same as we did with ephemeral volumes in the previous task.
The difference is that here we are using persistent volumes instead of ephemeral volumes.

**Reference**:
* [Mount Cloud Storage buckets as persistent volumes](https://cloud.google.com/kubernetes-engine/docs/how-to/cloud-storage-fuse-csi-driver-pv)