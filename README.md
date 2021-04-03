# From source code to production using kpack and Knative(serving and eventing)

## Prerequisites
- A Kubernetes v1.17+ cluster
- [kpack](https://github.com/pivotal/kpack) or [VMware Tanzu Build Service](https://docs.pivotal.io/build-service/1-1/installing.html) installed installed in the cluster
- [Knative](https://knative.dev/docs/install/) or [VMware Cloud Native Runtimes](https://docs.vmware.com/en/Cloud-Native-Runtimes-for-VMware-Tanzu/0.2/tanzu-cloud-native-runtimes-02/GUID-install.html) installed in the cluster



kp secret create registry-secret --registry demo.goharbor.io --registry-user tsalm

 kp image create deploy-on-knative-sink --tag demo.goharbor.io/tsalm/deploy-on-knative-sink --local-path .
kp image patch deploy-on-knative-sink  --local-path .

kp image create sample-java-app-tsalm --tag demo.goharbor.io/tsalm/sample-java-app-tsalm --git https://github.com/tsalm-pivotal/sample-java-app.git --git-revision master

watch kp build list sample-java-app-tsalm


kn service create deploy-on-knative-sink --image demo.goharbor.io/tsalm/deploy-on-knative-sink


kn service create deploy-on-knative-sink --image demo.goharbor.io/tsalm/knative-kpack-demo 
kn service update deploy-on-knative-sink --image demo.goharbor.io/tsalm/deploy-on-knative-sink:b4.20210402.181939
http://deploy-on-knative-sink.default.example.com
export EXTERNAL_ADDRESS=$(kubectl get service envoy -n contour-external \
  --output 'jsonpath={.status.loadBalancer.ingress[0].hostname}')

  curl -H "Host: deploy-on-knative-sink.default.example.com" $EXTERNAL_ADDRESS

