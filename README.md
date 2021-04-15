# Sample for a "From source code to production" supply chain using kpack and Knative(serving and eventing)

Sample for a "From source code to production" supply chain that uses the Knative Eventing [ApiServerSource](https://knative.dev/docs/eventing/sources/apiserversource/), that brings Kubernetes API server events into Knative. In this case it's listening for [kpack Image](https://github.com/pivotal/kpack/blob/master/docs/image.md) events, translates them to [CloudEvents](https://cloudevents.io) and sends these CloudEvents to a custom [deploy-on-knative-sink](deploy-on-knative-sink) that checks whether there is a related Knative service available. If that is not the case it creates one with the container image tag available in the CloudEvent payload and if there is already a Knative service available it checks whether the container image matches the one from the CloudEvent payload. If not it will update the service and create a new revision. 

## Prerequisites
For the sample you need:

- A Kubernetes v1.17+ cluster
- [kpack](https://github.com/pivotal/kpack) or [VMware Tanzu Build Service(TBS)](https://docs.pivotal.io/build-service/1-1/installing.html) installed in the cluster
- [Knative](https://knative.dev/docs/install/) or [VMware Cloud Native Runtimes](https://docs.vmware.com/en/Cloud-Native-Runtimes-for-VMware-Tanzu/0.2/tanzu-cloud-native-runtimes-02/GUID-install.html) installed in the cluster
- Have access to a container registry to store the application images that will be created

## Deploy the supply chain

### Create the Kubernetes namespace for the sample
```
kubectl create ns knative-kpack-demo
```
### Create a Kubernetes secret with container registry credentials in the target namespace "". 
See documentation for kpack [here](https://github.com/pivotal/kpack/blob/master/docs/secrets.md) and TBS here (https://docs.pivotal.io/build-service/1-0/managing-secrets.html)

Example using the VMware Tanzu Build Service **kp cli** and a Harbor registry:
```
kp secret create registry-secret --registry REGISTRY-URL --registry-user REGISTRY-USER-ID -n knative-kpack-demo
```

### Apply the [ApiServerSource](https://knative.dev/docs/eventing/sources/apiserversource/) definition including RBAC
```
kubectl apply -f k8s/api-server-source.yaml
```
### Deploy the deploy-on-knative-sink Spring Boot app
1. Apply the RBAC definition
   ```
   kubectl apply -f k8s/deploy-on-knative-sink-rbac.yaml
   ```
2. Apply the kpack/TBS container image definition
   - Using the VMware Tanzu Build Service **kp cli** and a Harbor registry:
	   ```
	   kp image create deploy-on-knative-sink --tag REGISTRY-URL/MY-PROJECT/deploy-on-knative-sink --git https://github.com/tsalm-pivotal/knative-kpack-demo.git --git-revision v1.0 --sub-path deploy-on-knative-sink -n knative-kpack-demo
	   ```
   - For **kpack** customize the sample in [k8s/kpack/deploy-on-knative-sink-image.yaml](k8s/kpack/deploy-on-knative-sink-image.yaml) and see more information [here](https://github.com/pivotal/kpack/blob/master/docs/image.md)

   Wait until the image is avialable:
   ```
   watch kp build list deploy-on-knative-sink -n knative-kpack-demo
   ```
   or 
   ```
   watch kubectl get builds -n knative-kpack-demo
   ```
 3. Deploy the sink with Knative
 	```
 	kn service create deploy-on-knative-sink --image REGISTRY-URL/MY-PROJECT/deploy-on-knative-sink --service-account deploy-on-knative-sink-sa -n knative-kpack-demo
 	```

## Testing the "From source code to production" with a sample app
1. Choose any sample app that can be containerized with your kpack/TBS buildpacks, e.g. https://github.com/tsalm-pivotal/spring-boot-hello-world. If you want to see that an update of the source code triggers a new container image build and deployment, you should fork the sample app.
2. Apply the kpack/TBS container image definition
   - Using the VMware Tanzu Build Service **kp cli** and a Harbor registry:
	   ```
	   kp image create spring-boot-hello-world --tag REGISTRY-URL/MY-PROJECT/spring-boot-hello-world --git https://github.com/MY-GITHUB-USER/spring-boot-hello-world.git --git-revision main -n knative-kpack-demo
	   ```
	 Hint: You can also use --local-path instead of a git repository
   - For **kpack** customize the sample in [k8s/kpack/spring-boot-hello-world-image.yaml](k8s/kpack/spring-boot-hello-world-image.yaml) and see more information [here](https://github.com/pivotal/kpack/blob/master/docs/image.md)
3. Verify that it's working
- Watch the image builds
   ```
   watch kp build list deploy-on-knative-sink -n knative-kpack-demo
   ```
   or 
   ```
   watch kubectl get builds -n knative-kpack-demo
   ```
- View the logs of the sink (there is only an instance available if there is load)
  ```
  kubectl logs -l "app=$(kn revision list -s deploy-on-knative-sink -n knative-kpack-demo -o jsonpath='{.items[0].metadata.name}')" -f -c user-container -n knative-kpack-demo
  ```
- Inspect the deployed services and revisions
  ```
  watch kn service list -n knative-kpack-demo
  watch kn revision list -s deploy-on-knative-sink -n knative-kpack-demo
  ```
- Call the application - It can take some time until the app is reachable from outside!
  ```
  curl -H "Host: spring-boot-hello-world.knative-kpack-demo.example.com" $EXTERNAL_ADDRESS
  ```
  The value of the $EXTERNAL_ADDRESS really depends on how your Knative is setup.
4. Update your source code in the Git repository or patch the image if you used local source code to verify that a new container image build and deployment will be triggered.

## Delete the supply chain
```
kubectl delete ns knative-kpack-demo
```