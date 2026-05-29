# GitOps Layout

## Rules

- `root/` contains only top-level `Argo CD Application` resources.
- each child directory maps to one platform slice.
- each child directory owns its namespace resources when it needs a dedicated namespace.
- ingress resources live next to the applications they expose.
- `mitmproxy/` and `observability/` are reserved slices for later implementation and currently contain namespace placeholders only.
- real secret management is deferred.

## Current structure

```text
kuber/
  README.md
  appium-tests/
  bugs/
  docs/
  gitops/
  scripts/
```

## Bootstrap

```text
gitops/bootstrap/
  argocd/
    kustomization.yaml
    namespace.yaml
  root-app.yaml
```

`bootstrap/argocd` creates the Argo CD namespace before the upstream Argo CD install is applied.
`bootstrap/root-app.yaml` creates the app-of-apps entrypoint.

## Root Applications

```text
gitops/root/
  kustomization.yaml
  argocd-app.yaml
  traefik-app.yaml
  cert-manager-app.yaml
  mongodb-app.yaml
  openldap-app.yaml
  devicehub-app.yaml
  appium-app.yaml
  mitmproxy-app.yaml
  observability-app.yaml
```

`root/` points all child applications at `targetRevision: kuber`.

## Argo CD Config

```text
gitops/argocd/
  namespace.yaml
  kustomization.yaml
  project.yaml
  argocd-cmd-params-cm.yaml
  argocd-issuer.yaml
  argocd-ingress.yaml
```

This slice owns the shared `devicehub-platform` AppProject, Argo CD server ingress, and HTTPS issuer for the Argo CD namespace.

## Traefik

```text
gitops/traefik/
  kustomization.yaml
  traefik-helmchart.yaml
  traefik-helmchartconfig.yaml
```

Traefik is installed through k3s `HelmChart` resources in `kube-system` and is pinned to the control node.

## cert-manager

```text
gitops/cert-manager/
  kustomization.yaml
  cert-manager-helmchart.yaml
```

cert-manager is installed through k3s `HelmChart` resources and provides Let's Encrypt HTTP-01 issuers for the public ingress endpoints.

## MongoDB

```text
gitops/mongodb/
  namespace.yaml
  kustomization.yaml
  mongodb-pvc.yaml
  mongodb-service.yaml
  mongodb-service-external.yaml
  mongodb-statefulset.yaml
  mongodb-init-job.yaml
  devicehub-migrate-job.yaml
```

MongoDB is a single-node replica set with persistent local-path storage and a NodePort used for external operational access.

## OpenLDAP

```text
gitops/openldap/
  namespace.yaml
  kustomization.yaml
  openldap-pvc.yaml
  openldap-service.yaml
  openldap-statefulset.yaml
  phpldapadmin-deployment.yaml
  phpldapadmin-service.yaml
  phpldapadmin-issuer.yaml
  phpldapadmin-ingress.yaml
```

OpenLDAP and phpLDAPadmin run on the storage node. Credentials are still plain manifest values until the later secret-management pass.

## DeviceHub

```text
gitops/devicehub/
  namespace.yaml
  kustomization.yaml
  devicehub-configmap.yaml
  devicehub-core-deployments.yaml
  devicehub-core-services.yaml
  devicehub-storage-temp-pvc.yaml
  devicehub-android-deployments.yaml
  devicehub-android-services.yaml
  devicehub-ios-bridge-services.yaml
  devicehub-ios-provider-bridge.yaml
  devicehub-dynamic-proxy.yaml
  devicehub-issuer.yaml
  devicehub-ingress.yaml
```

This slice owns DeviceHub core services, two Android ADB/provider pairs, external iOS bridge endpoints, storage, and public HTTPS ingress.

## Appium

```text
gitops/appium/
  namespace.yaml
  kustomization.yaml
  appium-grid-services.yaml
  appium-grid-deployments.yaml
  android-appium-node-config.yaml
  android-appium-nodes-deployment.yaml
  appium-grid-issuer.yaml
  appium-grid-ingress.yaml
```

Appium Grid control-plane components run on the storage node. Android Appium node replicas run on the Android node and connect to the DeviceHub ADB service.

## Reserved Slices

```text
gitops/mitmproxy/
  namespace.yaml
  kustomization.yaml

gitops/observability/
  namespace.yaml
  kustomization.yaml
```

These directories intentionally reserve namespace and Argo CD application boundaries. Their workloads will be added later.
