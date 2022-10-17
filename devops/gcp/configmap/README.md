<!--- Configmap -->

# Configmap helm chart

## Introduction

This chart bootstraps a configmap deployment on a [Kubernetes](https://kubernetes.io) cluster using [Helm](https://helm.sh) package manager.

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
This Helm chart should be installed before [deploy Helm Chart](../deploy)
First you need to set variables in **values.yaml** file using any code editor. Some of the values are prefilled, but you need to specify some values as well. You can find more information about them below.

### Common variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**logLevel** | logging level | string | `INFO` | yes
**springProfilesActive** | active spring profile | string | `gcp` | yes

### GCP variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**entitlementsHost** | entitlements service host address | string | `http://entitlements` | yes
**indexerHost** | indexer service host address | string | `http://register` | yes
**policyHost** | policy service host address | string | `http://policy` | yes
**partitionHost** | partition service host address | string | `http://partition` | yes
**redisGroupHost** | redis group host address | string | `redis-group-master` | yes
**redisSearchHost** | redis search host address | string | `redis-search-master` | yes
**policyId** | policeId from ex `${POLICY_HOST}/api/policy/v1/policies` | string | `search` | yes
**securityHttpsCertificateTrust** | Elastic client connection uses TrustSelfSignedStrategy(), if it is `true` | bool | `true` | yes
**servicePolicyEnabled** | Enables search service integration with policy service | bool | `false` | yes
**googleAudiences** | your GCP client ID | string | - | yes

> googleAudiences: If you are connected to GCP console with `gcloud auth application-default login --no-browser` from your terminal, you can get your client_id using the command:

```console
cat ~/.config/gcloud/application_default_credentials.json | grep client_id
```

### Config variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**configmap** | configmap name | string | notification-config | yes
**appName** | name of the app | string | notification | yes
**onPremEnabled** | whether on-prem is enabled | boolean | false | yes

### Install the helm chart

Run this command from within this directory:

```bash
helm install gcp-search-configmap .
```

## Uninstalling the Chart

To uninstall the helm deployment:

```bash
helm uninstall gcp-search-configmap
```

[Move-to-Top](#configmap-helm-chart)
