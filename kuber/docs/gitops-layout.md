# GitOps Layout

## Purpose

This document describes the proposed GitOps directory structure for `Argo CD`.

The layout is split by platform slice so each part of the system can be deployed, debugged, and evolved independently.

## Top-level idea

- `root/` contains only top-level `Argo CD Application` resources
- each child directory represents one platform slice
- each child directory is self-contained and owns its namespace resources
- ingress resources live next to the applications they expose
- example secrets can be stored in git, but real secrets should use a dedicated secret-management approach later

## Proposed structure

<details>
<summary><code>kuber/</code></summary>

```text
kuber/
  README.md
  docs/
  gitops/
```

</details>

<details>
<summary><code>kuber/docs/</code></summary>

```text
docs/
  requirements.md
  architecture.md
  roadmap.md
  gitops-layout.md
```

</details>

<details>
<summary><code>kuber/gitops/root/</code></summary>

```text
root/
  kustomization.yaml
  root-app.yaml
  argocd-app.yaml
  mongodb-app.yaml
  openldap-app.yaml
  devicehub-app.yaml
  appium-app.yaml
  mitmproxy-app.yaml
  observability-app.yaml
```

Purpose:

- defines the root app / app-of-apps entrypoint
- wires all child platform applications together

</details>

<details>
<summary><code>kuber/gitops/argocd/</code></summary>

```text
argocd/
  namespace.yaml
  kustomization.yaml
  project.yaml
```

Purpose:

- stores Argo CD namespace and project-level manifests

</details>

<details>
<summary><code>kuber/gitops/mongodb/</code></summary>

```text
mongodb/
  namespace.yaml
  kustomization.yaml
  mongodb-statefulset.yaml
  mongodb-service.yaml
  mongodb-pvc.yaml
  mongodb-init-job.yaml
  devicehub-migrate-job.yaml
```

Purpose:

- isolated MongoDB deployment slice
- keeps database state and bootstrap logic separate from app runtime

</details>

<details>
<summary><code>kuber/gitops/openldap/</code></summary>

```text
openldap/
  namespace.yaml
  kustomization.yaml
  openldap-statefulset.yaml
  openldap-service.yaml
  openldap-pvc.yaml
  phpldapadmin-deployment.yaml
  phpldapadmin-service.yaml
  phpldapadmin-ingress.yaml
```

Purpose:

- isolated identity slice
- keeps LDAP and phpLDAPadmin changes separate from DeviceHub changes

</details>

<details>
<summary><code>kuber/gitops/devicehub/</code></summary>

```text
devicehub/
  namespace.yaml
  kustomization.yaml
  configmap.yaml
  secrets-example.yaml

  devicehub-app-deployment.yaml
  devicehub-app-service.yaml

  devicehub-auth-deployment.yaml
  devicehub-auth-service.yaml

  devicehub-api-deployment.yaml
  devicehub-api-service.yaml

  devicehub-websocket-deployment.yaml
  devicehub-websocket-service.yaml

  devicehub-api-groups-engine-deployment.yaml
  devicehub-processor-deployment.yaml
  devicehub-reaper-deployment.yaml

  devicehub-triproxy-app-deployment.yaml
  devicehub-triproxy-app-service.yaml

  devicehub-triproxy-dev-deployment.yaml
  devicehub-triproxy-dev-service.yaml

  devicehub-storage-temp-deployment.yaml
  devicehub-storage-temp-service.yaml
  devicehub-storage-temp-pvc.yaml

  devicehub-storage-plugin-apk-deployment.yaml
  devicehub-storage-plugin-apk-service.yaml

  devicehub-storage-plugin-image-deployment.yaml
  devicehub-storage-plugin-image-service.yaml

  adbd-deployment.yaml
  devicehub-provider-deployment.yaml

  ingress.yaml
```

Purpose:

- main DeviceHub runtime slice
- contains only DeviceHub services, not LDAP, Appium, or mitmproxy

</details>

<details>
<summary><code>kuber/gitops/appium/</code></summary>

```text
appium/
  namespace.yaml
  kustomization.yaml
  appium-grid-router-deployment.yaml
  appium-grid-router-service.yaml
  appium-grid-distributor-deployment.yaml
  appium-grid-session-queue-deployment.yaml
  appium-grid-sessions-deployment.yaml
  android-appium-nodes-deployment.yaml
  ingress.yaml
```

Purpose:

- automation slice
- separates Appium rollout from DeviceHub core rollout

</details>

<details>
<summary><code>kuber/gitops/mitmproxy/</code></summary>

```text
mitmproxy/
  namespace.yaml
  kustomization.yaml
  mitmproxy-deployment.yaml
  mitmproxy-service.yaml
  mitmweb-deployment.yaml
  mitmweb-service.yaml
  ingress.yaml
```

Purpose:

- traffic interception slice
- independent from the DeviceHub release cycle

</details>

<details>
<summary><code>kuber/gitops/observability/</code></summary>

```text
observability/
  namespace.yaml
  kustomization.yaml
  prometheus-statefulset.yaml
  prometheus-service.yaml
  grafana-deployment.yaml
  grafana-service.yaml
  loki-statefulset.yaml
  loki-service.yaml
  promtail-daemonset.yaml
  alertmanager-deployment.yaml
  alertmanager-service.yaml
  ingress.yaml
```

Purpose:

- monitoring, logging, and alerting slice
- independent operational layer

</details>

## Why this layout was chosen

- it matches the agreed namespace model
- it matches the agreed platform layers
- it works naturally with the root app / app-of-apps model
- it keeps ownership boundaries clear
- it makes later Argo CD troubleshooting much easier
