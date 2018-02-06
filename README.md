# k8s-proxy
Simple reverse proxy to serve the [Kubernetes dashboard](https://github.com/kubernetes/dashboard) with Google OIDC as the identity provider. Technically, any OIDC provider should work but this has been tested only with Google for the moment. 

This project will allow you to access the dashboard without having to paste the JWT token in the UI and paste a new one once it expires. It handles the refresh of the token and the injection of the JWE token on each request. This makes a transparent solution until [this PR](https://github.com/kubernetes/kubernetes/pull/29714) is reopened and merged.


## Disclaimer
This project was made as part of a hackathon so it's rough around the edges, lacks validation, might contains bugs and have a couple of things hardcoded. Feel free to open a PR or an issue if you find anything.

## How to use
The proxy needs a couple of parameters before it can start. You can inject those properties in a yaml file or with command line argument.

### YAML configuration
Create a `application.yml` file next to the .jar file : 
```
server:
  port: 8888

google: 
  clientId: insert client id from Google here
  clientSecret: insert client secret from Google here
  authorizeUrl: https://accounts.google.com/o/oauth2/auth
  tokenUrl: https://www.googleapis.com/oauth2/v4/token
  
k8s:
  clusterEndpoint: https://your.k8scluster.com
```

### Command line argument
You can inject the required parameter on the command line as such : 
```
java -jar -Dgoogle.clientId=something -Dgoogle.clientSecret=secret -Dgoogle.authorizeUrl=https://accounts.google.com/o/oauth2/auth -Dgoogle.tokenUrl=https://www.googleapis.com/oauth2/v4/token -Dk8s.clusterEndpoint=https://your.k8scluster.com k8s-proxy-0.0.1.jar
```

### Run the jar file

1. Download the latest [release version](https://github.com/coveo/k8s-proxy/releases). 
1. Run the jar file with `java -jar k8s-proxy-0.0.1.jar`
1. Access the proxy at [http://localhost:8888/ui](http://localhost:8888/ui)

## Using multiple kubernetes clusters
If you have multiple kubernetes clusters that use the same login, it is possible to switch between clusters at runtime without relogging or rebooting the proxy.

You can see the endpoint of the active cluster at [http://localhost:8888/k8s_cluster_endpoint](http://localhost:8888/k8s_cluster_endpoint). The response is of the following format:
```
{
    "k8sClusterEndpoint":"https://my.awesome.cluster.k8s.com"
}
```

To set the active endpoint, you can do a request on 
```
GET http://localhost:8888/k8s_cluster_endpoint/set?endpoint=<YOUR_K8S_ENDPOINT>
```

or
```
PUT http://localhost:8888/k8s_cluster_endpoint?endpoint=<YOUR_K8S_ENDPOINT>
```


If successful, the response will have the same format as previously described for the get endpoint method.

Once the endpoint has been changed, all calls to the proxy will be routed to the new endpoint. It is not possible to use multiple clusters at this time (if you wish to do so, you need to spawn multiple instances of the proxy on different ports).

## Print the current token
If you need to see the current token used by the proxy, you can do so by using this endpoint : 
```
GET http://localhost:8888/get_token
```