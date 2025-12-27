# Docker App 1
This is a sample java spring boot application with dockerfile. This is to demonstrate how to containerize java applications using docker and kubernetes and deploy the same in **GKE**.

Also, this app demonstrates how we can connect to **google cloud storage** and **cloud sql** from GKE application using **cloud storage FUSE CSI driver** and **cloud sql auth proxy** respectively.

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
To run the application in GKE first we have to create the cluster(autopilot mode) by running the following command(this is applicable for windows):
```
gcloud container clusters create-auto <CLUSTER_NAME> --location=asia-south1
```
To run docker images built and pushed from macOS arm64 architecture, create a standard cluster with arm64 architecture:
```
gcloud container clusters create <CLUSTER_NAME> --zone=us-central1-a --machine-type=t2a-standard-1 --num-nodes=2
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
Do the same for **app2**(except for configMap). 

After the deployments are done for app1 and app2, test the connectivity from app1 to app2 from postman.

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

### [Additional Info] Create cloud sql instance in GCP and connect from local PC via MySQL Workbench tool:
* First create a cloud sql instance(say MySQL) from google cloud console. Make sure that public IP is selected.
* Add your PC's IPv4 address into the authorized network option of the cloud sql instance. This can be done during or after the creation of the instance. 
Kindly note that **ipconfig** command would not give the correct IP address of your PC(maybe because it is behind a router). Try to find actual IPv4 ip from online tools by searching **what is my ip** in google. 
Also, the IPv4 address may change from time to time, so recheck if the connection fails.
* Download and install the MySQL workbench tool. In the new connection dialog provide the public IP of the cloud sql instance alongwith username(maybe **root**) and password. Establish connection.
* Alternatively you can use **Cloud SQL Studio** option in google cloud sql console to connect to cloud sql instance from browser itself.

**References:**
* [About Cloud SQL connections](https://cloud.google.com/sql/docs/mysql/connect-overview#public_ip)
* [Connect from other MySQL tools](https://cloud.google.com/sql/docs/mysql/admin-tools)

### [Additional Info] Create cloud sql instance in GCP and connect from local PC via cloud-sql-auth-proxy connector:
* Create cloud sql instance as described in the previous section.
* Download cloud sql auth proxy (the steps to download the proxy is given in the official google docs in the reference link here)
* Set the application default credential using your google CLI login account. This is required by cloud auth proxy to connect to cloud sql with required permissions (the alternate is to create a service account with necessary privileges and create a credential JSON file from that and use the same with **--credentials-file** flag)
```
gcloud auth application-default login
```
* Get the instance connection name of the cloud sql instance:
```
gcloud sql instances describe <INSTANCE_NAME> --format='value(connectionName)'
```
* Connect to the cloud sql instance using cloud sql auth proxy. This will open a TCP socket connection at localhost on the specified PORT_NUMBER.
```
.\cloud-sql-proxy.x64.exe --port <PORT_NUMBER> <INSTANCE_CONNECTION_NAME>
```
* Try to connect to the cloud sql proxy connection from MySQL workbench using host=127.0.0.1 and port=PORT_NUMBER as given above. It should be able to connect.

**References:**
* [Connect using the Cloud SQL Auth Proxy](https://cloud.google.com/sql/docs/mysql/connect-auth-proxy)
* [About the Cloud SQL Auth Proxy](https://cloud.google.com/sql/docs/mysql/sql-proxy)

### Create cloud sql instance in GCP and connect from GKE via cloud-sql-auth-proxy connector:
#### Using Auto IAM Authentication
* Create cloud sql instance as described in the previous sections.
* Patch the cloud sql instance to enable auto IAM authentication:
```
gcloud sql instances patch <INSTANCE_NAME> --database-flags=cloudsql_iam_authentication=on
```
* Create kubernetes namespace and service account:
```
kubectl create namespace <KUBERNETES_NAMESPACE_NAME>
kubectl create serviceaccount <KUBERNETES_SERVICE_ACCOUNT> --namespace <NAMESPACE_NAME>
```
* Create kubernetes secret for DB credentials:
```
kubectl create secret generic cloud-sql-secret --from-literal=username=root --from-literal=password=password --from-literal=database=TEST_DB -n my-space
```
* Create a GCP service account in GCP console IAM page.
* Grant the GCP service account with cloud sql client and logs writer roles:
```
gcloud projects add-iam-policy-binding <GCP_PROJECT_ID> --member="serviceAccount:<GCP_SERVICE_ACCOUNT_NAME>@<GCP_PROJECT_ID>.iam.gserviceaccount.com" --role="roles/cloudsql.instanceUser"
gcloud projects add-iam-policy-binding <GCP_PROJECT_ID> --member="serviceAccount:<GCP_SERVICE_ACCOUNT_NAME>@<GCP_PROJECT_ID>.iam.gserviceaccount.com" --role="roles/cloudsql.client"
gcloud projects add-iam-policy-binding <GCP_PROJECT_ID> --member="serviceAccount:<GCP_SERVICE_ACCOUNT_NAME>@<GCP_PROJECT_ID>.iam.gserviceaccount.com" --role="roles/logging.logWriter"
```
* Create workload identity federation by linking kubernetes service account and GCP service account:
```
gcloud iam service-accounts add-iam-policy-binding <GCP_SERVICE_ACCOUNT_NAME>@<GCP_PROJECT_ID>.iam.gserviceaccount.com --role="roles/iam.workloadIdentityUser" --member="serviceAccount:<GCP_PROJECT_ID>.svc.id.goog[<KUBERNETES_NAMESPACE_NAME>/<KUBERNETES_SERVICE_ACCOUNT>]"
```
* Annotate the kubernetes service account:
```
kubectl annotate serviceaccount <KUBERNETES_SERVICE_ACCOUNT> iam.gke.io/gcp-service-account=<GCP_SERVICE_ACCOUNT_NAME>@<GCP_PROJECT_ID>.iam.gserviceaccount.com --namespace=<KUBERNETES_NAMESPACE_NAME>
```
* Create a cloud sql user for the corresponding GCP service account.
This is required as we are leveraging auto IAM authentication to authenticate against cloud sql.
```
gcloud sql users create <GCP_SERVICE_ACCOUNT_NAME>@<GCP_PROJECT_ID>.iam.gserviceaccount.com --instance=<INSTANCE_NAME> --type=cloud_iam_service_account
```
* Additionally, grant privileges to the user in the mysql database side by running the following command. We can run the command by connecting using MYSQL workbench.
```
GRANT ALL PRIVILEGES ON <DB_NAME>.* TO '<USER>'@'%';
```
* Finally, deploy the application in GKE by creating configmap, deployment and service:
```
kubectl apply -f .\k8s\configMap.yaml -n <KUBERNETES_NAMESPACE_NAME>
kubectl apply -f .\k8s\deployment-cloud-sql-proxy-auto-iam-auth.yaml
kubectl apply -f .\k8s\service-cloud-sql.yaml
```
Note that in the **deployment-cloud-sql-proxy-auto-iam-auth.yaml** file we are using the same kubernetes service account as we created earlier. 
This service account will leverage the workload identity federation to connect to cloud sql via the GCP service account(which already has the required privileges).

We have provided **--auto-iam-authn** as an argument to the cloud-sql-proxy sidecar for enabling auto IAM authentication to authenticate against cloud sql.

Also, we have set the spring profile as **auto-iam-auth** as that will be required to pick the database credentials from configmap for connecting via "auto IAM authentication".

**References:**
* [Connect to Cloud SQL from Google Kubernetes Engine](https://cloud.google.com/sql/docs/mysql/connect-kubernetes-engine)
* [Add an individual IAM user or service account to a Cloud SQL instance](https://cloud.google.com/sql/docs/mysql/add-manage-iam-users#creating-a-database-user)
* [Setup GCP service account](https://cloud.google.com/sql/docs/postgres/connect-instance-kubernetes#set_up_a_service_account)
* [Configure existing instances for IAM database authentication](https://cloud.google.com/sql/docs/postgres/create-edit-iam-instances#configure-existing)

#### Using Database Builtin Authentication:
* Create cloud sql instance as described in the previous sections.
* Create kubernetes namespace and service account:
```
kubectl create namespace <KUBERNETES_NAMESPACE_NAME>
kubectl create serviceaccount <KUBERNETES_SERVICE_ACCOUNT> --namespace <NAMESPACE_NAME>
```
* Create kubernetes secret for DB credentials:
```
kubectl create secret generic cloud-sql-secret --from-literal=username=root --from-literal=password=password --from-literal=database=TEST_DB -n my-space
```
* Create a GCP service account in GCP console IAM page.
* Grant the GCP service account with cloud sql client and logs writer roles:
```
gcloud projects add-iam-policy-binding <GCP_PROJECT_ID> --member="serviceAccount:<GCP_SERVICE_ACCOUNT_NAME>@<GCP_PROJECT_ID>.iam.gserviceaccount.com" --role="roles/cloudsql.instanceUser"
gcloud projects add-iam-policy-binding <GCP_PROJECT_ID> --member="serviceAccount:<GCP_SERVICE_ACCOUNT_NAME>@<GCP_PROJECT_ID>.iam.gserviceaccount.com" --role="roles/cloudsql.client"
gcloud projects add-iam-policy-binding <GCP_PROJECT_ID> --member="serviceAccount:<GCP_SERVICE_ACCOUNT_NAME>@<GCP_PROJECT_ID>.iam.gserviceaccount.com" --role="roles/logging.logWriter"
```
* Create workload identity federation by linking kubernetes service account and GCP service account:
```
gcloud iam service-accounts add-iam-policy-binding <GCP_SERVICE_ACCOUNT_NAME>@<GCP_PROJECT_ID>.iam.gserviceaccount.com --role="roles/iam.workloadIdentityUser" --member="serviceAccount:<GCP_PROJECT_ID>.svc.id.goog[<KUBERNETES_NAMESPACE_NAME>/<KUBERNETES_SERVICE_ACCOUNT>]"
```
* Annotate the kubernetes service account:
```
kubectl annotate serviceaccount <KUBERNETES_SERVICE_ACCOUNT> iam.gke.io/gcp-service-account=<GCP_SERVICE_ACCOUNT_NAME>@<GCP_PROJECT_ID>.iam.gserviceaccount.com --namespace=<KUBERNETES_NAMESPACE_NAME>
```
* Create a cloud sql user.
```
gcloud sql users create prat --host=% --instance=<INSTANCE_NAME> --password=password
```
* Finally, deploy the application in GKE by creating configmap, deployment and service:
```
kubectl apply -f .\k8s\configMap.yaml -n <KUBERNETES_NAMESPACE_NAME>
kubectl apply -f .\k8s\deployment-cloud-sql-proxy-builtin-auth.yaml
kubectl apply -f .\k8s\service-cloud-sql.yaml
```
Note that in the deployment-cloud-sql-proxy-builtin-auth.yaml file there is no **--auto-iam-authn** flag as we are using database builtin authentication here.

Also, we have set the spring profile as **database-auth** as that will be required to pick the database credentials from configmap for connecting via "database builtin authentication".

**References:**
* [Manage users with built-in authentication](https://cloud.google.com/sql/docs/mysql/create-manage-users)

### Useful debugging tips:
* To check logs of a pod in rolling manner:
```
kubectl logs <POD_NAME>  -n <NAMESPACE_NAME> -f
```
* To check logs of a particular container inside pod:
```
kubectl logs <POD_NAME> -c <CONTAINER_NAME> -n <NAMESPACE_NAME> -f
```
* To initiate an interactive session inside a pod:
```
kubectl exec --stdin --tty <POD_NAME> -n <NAMESPACE> -- /bin/sh
```
* Sometimes when you update the application, build the image using docker locally and push it to docker hub and afterwards redeploy the application in GKE using deployment yaml files, the updated image may not get pulled from docker hub.
To check whether the GKE deployment is using the latest image of the application or not we can compare the unique image ID of the latest image in the docker hub and the image ID of the application container being used by the deployment pod.
The latest image ID of the application can be checked from the docker hub portal under **Image Management** tab.
To check the image ID of the application container we can check the output of the below command:
```
kubectl describe pod <POD_NAME> -n <NAMESPACE>
```
* Another way to ensure that the deployment uses the latest image is that we can tag the image with unique version while building the image and then use the same image tag in our deployment file.
```
docker build -t prat21/app1:<UNIQUE_TAG> .
docker push prat21/app1:<UNIQUE_TAG>
```

### TODO for later:
#### Using environment variables in configMaps:
* Kubernetes does not support using env variables in configMaps out of the box. Because configMaps are static key-value pairs which needs to be created before any of the deployment which consumes the configMap.
* Hence, injecting sensitive data in configMaps (example DB credentials) using env variables(obtained from kubernetes secrets maybe) is tricky.
* In spring applications though, the env variables gets substituted with their values in the configuration files (example application.yml) during bootstrap process. This is also possible using spring cloud config server.
* So, for using env vars in configMaps some abstraction is needed which will do the job of substituting the env variable values. **Spring Cloud Kubernetes Config Server** can be used for the same. For this a deployment has to be created (with a ClusterIP service maybe so that services can access the config server internally within the cluster) for config server. The docker image for this is available in public docker repo.
* Or the alternative will be to use spring cloud config server with git repo as backend. Also since kubernetes secrets do not encrypt data at rest, hence we can use some alternate secret management service like GCP secret manager.