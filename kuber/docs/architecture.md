# Current Architecture

## Runtime zones

### Kubernetes

- DeviceHub services
- `MongoDB`
- `OpenLDAP`
- `phpLDAPadmin`
- Appium Grid control plane
- Android Appium nodes
- `mitmproxy` / `mitmweb`
- observability
- `Argo CD`

### External Mac mini

- WebDriverAgent
- iOS tooling
- iOS Appium nodes
- future iOS-side integration processes

## Namespaces

- `argocd`
- `mongodb`
- `openldap`
- `devicehub`
- `appium`
- `mitmproxy`
- `observability`

## Node labels

| Node | Label |
| --- | --- |
| `k3s-control` | `devicehub.role=control` |
| `k3s-worker-1` | `devicehub.role=android` |
| `k3s-worker-2` | `devicehub.role=storage` |

## Node roles

| Node | Role | Main workloads |
| --- | --- | --- |
| `k3s-control` | control / GitOps | `argocd`, light control-plane infra |
| `k3s-worker-1` | Android execution | `adbd`, `devicehub-provider`, Android device workers, Android Appium nodes |
| `k3s-worker-2` | storage / stateful | `mongodb`, `openldap`, `devicehub-storage-temp`, Appium Grid control plane, observability |
| `Mac mini` | iOS execution | WebDriverAgent, iOS Appium nodes, Apple tooling |

## Required affinity

| Workload | Required label | Reason |
| --- | --- | --- |
| `adbd` | `devicehub.role=android` | owns USB-attached Android devices |
| `devicehub-provider` | `devicehub.role=android` | must run next to `adbd` |
| `mongodb` | `devicehub.role=storage` | fixed persistent data |
| `openldap` | `devicehub.role=storage` | fixed persistent data |
| `devicehub-storage-temp` | `devicehub.role=storage` | fixed persistent temp storage backend |

Everything else stays movable in phase 1.

## Storage

### Persistent workloads

| Workload | Storage | Size | Placement |
| --- | --- | --- | --- |
| `mongodb` | PVC via `local-path` | `5Gi` | `k3s-worker-2` |
| `openldap` | PVC via `local-path` | `1Gi` | `k3s-worker-2` |
| `devicehub-storage-temp` | PVC via `local-path` | `5Gi` | `k3s-worker-2` |

### Storage rules

- `local-path` on `k3s-worker-2` is the phase 1 storage model.
- `devicehub-storage-plugin-apk` and `devicehub-storage-plugin-image` use `devicehub-storage-temp` as backend storage.
- temp cleanup is expected to be handled by the application services.

## Scaling policy

### Singleton

- all `argocd` workloads
- all `mongodb` workloads
- all `openldap` workloads
- `devicehub-app`
- `devicehub-auth`
- `devicehub-api`
- `devicehub-websocket`
- `devicehub-api-groups-engine`
- `devicehub-reaper`
- `devicehub-triproxy-app`
- `devicehub-triproxy-dev`
- `devicehub-storage-temp`
- Appium Grid control plane
- `mitmproxy`
- `mitmweb`
- `prometheus`
- `grafana`
- `loki`
- `alertmanager`

### Scalable

- `devicehub-processor`
- `devicehub-storage-plugin-apk`
- `devicehub-storage-plugin-image`
- Android Appium nodes
- `promtail`

### Paired scaling

- `adbd`
- `devicehub-provider`

These two scale together as one Android execution pair.

## Kubernetes resource types

### `StatefulSet`

- `argocd-application-controller`
- `mongodb`
- `openldap`
- `prometheus`
- `loki`

### `Job`

- `mongodb-init`
- `devicehub-migrate`

### `DaemonSet`

- `promtail`

### `Deployment`

- all remaining phase 1 workloads

## Argo CD model

### Root model

- use a root app / app-of-apps pattern
- `root/` contains only top-level `Application` resources

### Shared AppProject

| Setting | Value |
| --- | --- |
| name | `devicehub-platform` |
| repo | `git@github.com:mdub1na/devicehub.git` |
| cluster | `https://kubernetes.default.svc` |
| namespaces | `argocd`, `mongodb`, `openldap`, `devicehub`, `appium`, `mitmproxy`, `observability` |
| allowed cluster-scoped resources | `Namespace` |

### Applications using the shared AppProject

- `argocd`
- `mongodb`
- `openldap`
- `devicehub`
- `appium`
- `mitmproxy`
- `observability`
