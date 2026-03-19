# GitOps Layout

## Rules

- `root/` contains only top-level `Argo CD Application` resources
- each child directory maps to one platform slice
- each child directory owns its own namespace resources
- ingress resources live next to the applications they expose
- example secrets may exist in git, but real secret management is deferred

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

</details>

<details>
<summary><code>kuber/gitops/argocd/</code></summary>

```text
argocd/
  namespace.yaml
  kustomization.yaml
  project.yaml
```

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

</details>
