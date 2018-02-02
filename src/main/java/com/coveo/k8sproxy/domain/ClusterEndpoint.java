package com.coveo.k8sproxy.domain;

public class ClusterEndpoint
{
    String k8sClusterEndpoint;

    public ClusterEndpoint()
    {
    }

    public ClusterEndpoint(String k8sClusterEndpoint)
    {
        this.k8sClusterEndpoint = k8sClusterEndpoint;
    }

    public String getK8sClusterEndpoint()
    {
        return k8sClusterEndpoint;
    }

    public void setK8sClusterEndpoint(String k8sClusterEndpoint)
    {
        this.k8sClusterEndpoint = k8sClusterEndpoint;
    }
}
