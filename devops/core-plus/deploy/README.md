<!--- Deploy -->

# Deploy helm chart

## Introduction

This chart bootstraps a deployment on a [Kubernetes](https://kubernetes.io) cluster using [Helm](https://helm.sh) package manager.

## Prerequisites

The code was tested on **Kubernetes cluster** (v1.21.11) with **Istio** (1.12.6)
> It is possible to use other versions, but it hasn't been tested

### Operation system

The code works in Debian-based Linux (Debian 10 and Ubuntu 20.04) and Windows WSL 2. Also, it works but is not guaranteed in Google Cloud Shell. All other operating systems, including macOS, are not verified and supported.

### Packages

Packages are only needed for installation from a local computer.

- **HELM** (version: v3.7.1 or higher) [helm](https://helm.sh/docs/intro/install/)
- **Kubectl** (version: v1.21.0 or higher) [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl)

## Installation

You need to set variables in **values.yaml** file using any code editor. Some of the values are prefilled, but you need to specify some values as well. You can find more information about them below.

### Global variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**global.domain** | your domain for the external endpoint, ex `example.com` | string | - | yes
**global.onPremEnabled** | whether on-prem is enabled | boolean | false | yes
**global.limitsEnabled** | whether CPU and memory limits are enabled | boolean | true | yes

### rosa flag

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**rosa** | This flag enables configuration specific to ROSA environments | boolean | - | yes

### Configmap variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**data.logLevel** | logging level | string | `ERROR` | yes
**data.entitlementsHost** | Entitlements service host | string | `http://entitlements` | yes
**data.indexerHost** | Indexer service host | string | `http://indexer` | yes
**data.policyHost** | Policy service host | string | `http://policy` | yes
**data.partitionHost** | Partition service host | string | `http://partition` | yes
**data.policyId** | policy id from ex `${POLICY_HOST}/api/policy/v1/policies` | string | `search` | yes
**data.securityHttpsCertificateTrust** | Elastic client connection uses TrustSelfSignedStrategy(), if it is `true` | bool | `true` | yes
**data.servicePolicyEnabled** | Enables Search service integration with Policy service | bool | `false` | yes
**data.redisSearchHost** | The host for redis instance. If empty (by default), helm installs an internal redis instance | string | - | yes
**data.redisSearchPort** | The port for redis instance | digit | 6379 | yes

### Deployment variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**data.requestsCpu** | amount of requested CPU | string | `20m` | yes
**data.requestsMemory** | amount of requested memory| string | `550Mi` | yes
**data.limitsCpu** | CPU limit | string | `1` | only if `global.limitsEnabled` is true
**data.limitsMemory** | memory limit | string | `1G` | only if `global.limitsEnabled` is true
**data.serviceAccountName** | name of your service account | string | `search` | yes
**data.imagePullPolicy** | when to pull image | string | `IfNotPresent` | yes
**data.image** | service image | string | - | yes
**data.redisImage** | service image | string | `redis:7` | yes

### Configuration variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**conf.appName** | Service name | string | `search` | yes
**conf.elasticSecretName** | secret for elasticsearch | string | `search-elastic-secret` | yes
**conf.searchRedisSecretName** | search Redis secret that contains redis password with REDIS_PASSWORD key | string | `search-redis-secret` | yes

### Istio variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**istio.proxyCPU** | CPU request for Envoy sidecars | string | 10m | yes
**istio.proxyCPULimit** | CPU limit for Envoy sidecars | string | 200m | yes
**istio.proxyMemory** | memory request for Envoy sidecars | string | 100Mi | yes
**istio.proxyMemoryLimit** | memory limit for Envoy sidecars | string | 256Mi | yes

## Install the Helm chart

Run this command from within this directory:

```console
helm install core-plus-search-deploy .
```

## Uninstall the Helm chart

To uninstall the helm deployment:

```console
helm uninstall core-plus-search-deploy
```

> Do not forget to delete all k8s secrets and PVCs accociated with the Service.

[Move-to-Top](#deploy-helm-chart)
