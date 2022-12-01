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

### Configmap variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**logLevel** | logging level | string | `ERROR` | yes
**springProfilesActive** | active spring profile | string | `gcp` | yes
**entitlementsHost** | Entitlements service host | string | `http://entitlements` | yes
**indexerHost** | Indexer service host | string | `http://indexer` | yes
**policyHost** | Policy service host | string | `http://policy` | yes
**partitionHost** | Partition service host | string | `http://partition` | yes
**redisGroupHost** | Redis group host | string | `redis-group-master` | yes
**redisSearchHost** | Redis search host | string | `redis-search-master` | yes
**policyId** | policy id from ex `${POLICY_HOST}/api/policy/v1/policies` | string | `search` | yes
**securityHttpsCertificateTrust** | Elastic client connection uses TrustSelfSignedStrategy(), if it is `true` | bool | `true` | yes
**servicePolicyEnabled** | Enables Search service integration with Policy service | bool | `false` | yes
**googleAudiences** | Client ID of Google Cloud Credentials, ex `123-abc123.apps.googleusercontent.com` | string | - | yes

### Deployment variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**requestsCpu** | amount of requested CPU | string | `0.1` | yes
**requestsMemory** | amount of requested memory| string | `448M` | yes
**limitsCpu** | CPU limit | string | `1` | yes
**limitsMemory** | memory limit | string | `1G` | yes
**serviceAccountName** | name of your service account | string | `search` | yes
**imagePullPolicy** | when to pull image | string | `IfNotPresent` | yes
**image** | service image | string | - | yes

### Configuration variables

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|----------|
**appName** | Service name | string | `search` | yes
**configmap** | configmap to be used | string | `search-config` | yes
**elasticSecretName** | secret for elasticsearch | string | `search-elastic-secret` | yes
**onPremEnabled** | whether on-prem is enabled | boolean | false | yes
**domain** | your domain, ex `example.com` | string | - | yes

## Install the Helm chart

Run this command from within this directory:

```console
helm install gcp-search-deploy .
```

## Uninstall the Helm chart

To uninstall the helm deployment:

```console
helm uninstall gcp-search-deploy
```

> Do not forget to delete all k8s secrets and PVCs accociated with the Service.

[Move-to-Top](#deploy-helm-chart)
