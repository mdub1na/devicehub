# Target Architecture

## Runtime zones

### Kubernetes zone

Runs almost the entire platform:

- DeviceHub services
- MongoDB
- OpenLDAP
- phpLDAPadmin
- Appium Grid control plane
- Android Appium nodes
- mitmproxy / mitmweb
- ingress
- observability
- GitOps control plane

### External Mac mini zone

Runs iOS execution:

- WebDriverAgent
- Xcode / idb / pymobiledevice3 tooling
- iOS Appium nodes
- iOS-side integration back into DeviceHub

## Layers

### Access layer

- `Traefik`
- public and LAN entrypoints for:
  - DeviceHub
  - phpLDAPadmin
  - mitmweb
  - Appium Grid

Phase plan:

- phase 1: LAN validation
- phase 2: public DNS, HTTPS, Let's Encrypt

### Identity layer

- `OpenLDAP`
- `phpLDAPadmin`
- `devicehub-auth` in LDAP mode

### DeviceHub control layer

- `devicehub-app`
- `devicehub-auth`
- `devicehub-api`
- `devicehub-websocket`
- `devicehub-api-groups-engine`
- `devicehub-triproxy-app`
- `devicehub-triproxy-dev`
- `devicehub-processor`
- `devicehub-reaper`

### Data layer

- `MongoDB`
- OpenLDAP persistent data
- DeviceHub storage services

### Android execution layer

- `adbd`
- `devicehub-provider`
- dynamic Android device workers
- Android Appium nodes

### iOS execution layer

- Mac mini based iOS stack

### Traffic interception layer

- `mitmproxy`
- `mitmweb`

### Automation layer

- Appium Grid control plane in Kubernetes
- Android Appium nodes in Kubernetes
- iOS Appium nodes on Mac mini

### Observability layer

- `Prometheus`
- `Grafana`
- `Loki`
- `Promtail`
- `Alertmanager`

## Namespaces

- `devicehub`: main farm services except MongoDB
- `mongodb`: MongoDB workloads
- `appium`: Appium Grid and Kubernetes-side automation services
- `openldap`: OpenLDAP and phpLDAPadmin
- `argocd`: GitOps control plane
- later optional infra namespaces for observability and cert-manager if we want clearer separation

## Node placement

### `k3s-control` (`192.168.0.121`)

Use for:

- Kubernetes control plane
- lightweight shared infra if needed
- `argocd` is a good fit here

Avoid:

- USB-bound Android workloads
- heavier stateful workloads when possible

### `k3s-worker-1` (`192.168.0.122`)

Role:

- dedicated Android worker

Use for:

- `adbd`
- `devicehub-provider`
- Android device workers
- Android Appium nodes

Fixed placement requirements:

- `adbd` must be fixed to this node
- `devicehub-provider` must be fixed to this node
- this node is the physical Android USB owner in phase 1

### `k3s-worker-2` (`192.168.0.123`)

Role:

- stateful platform worker

Use for:

- DeviceHub control services
- `MongoDB`
- `OpenLDAP`
- `phpLDAPadmin`
- `devicehub-storage-temp`
- `devicehub-storage-plugin-apk`
- `devicehub-storage-plugin-image`
- `mitmproxy` / `mitmweb`
- Appium Grid control plane
- observability stack

Fixed placement requirements:

- `MongoDB` must be fixed to this node
- `OpenLDAP` must be fixed to this node
- `devicehub-storage-temp` must be fixed to this node
- `devicehub-storage-plugin-apk` must be fixed to this node
- `devicehub-storage-plugin-image` must be fixed to this node
- these workloads depend on persistent or node-local disk usage in phase 1

### Mac mini

Use for:

- iOS tooling
- WebDriverAgent
- iOS Appium nodes
- iOS-side provider integration

## Scheduling model

Suggested labels:

- `devicehub.role=control`
- `devicehub.role=android`
- `devicehub.role=general`

Phase 1 fixed labels:

- `k3s-control` -> `devicehub.role=control`
- `k3s-worker-1` -> `devicehub.role=android`
- `k3s-worker-2` -> `devicehub.role=storage`

Placement rules:

- `adbd`, `devicehub-provider`, and Android Appium nodes must be pinned to the Android worker
- `MongoDB`, `OpenLDAP`, `devicehub-storage-temp`, `devicehub-storage-plugin-apk`, and `devicehub-storage-plugin-image` must be pinned to `k3s-worker-2`
- other DeviceHub control services should remain movable and not be pinned to a single worker by default
- singleton coordination services should avoid unnecessary relocation where practical, but do not need hard pinning unless they own local state

## Argo CD

### Should we use it

Yes. This platform is large enough that GitOps is worth it.

### Why it belongs here

- multiple namespaces
- multiple layers
- state we want reproducible from git
- easier disaster recovery and environment rebuilds
- visible drift detection

### What it should manage

- namespace creation manifests
- DeviceHub applications
- MongoDB application
- OpenLDAP application
- Appium application
- observability application
- ingress-related application
- later cert-manager application

### What it should not manage

- Mac mini host internals
- ad-hoc manual operations on iOS tooling
- anything outside Kubernetes desired state

### Placement

- namespace: `argocd`
- preferred node: `k3s-control`

### Recommended model

Use `Argo CD` as a deployment layer after architecture is fixed.

Suggested structure:

- root application
- child applications:
  - `devicehub`
  - `mongodb`
  - `openldap`
  - `appium`
  - `observability`
  - `ingress`

## Phase 1 scope

Included:

- core DeviceHub in Kubernetes
- separate namespaces for `devicehub`, `mongodb`, `appium`, `openldap`, `argocd`
- one dedicated Android worker
- LAN-first access model
- observability stack
- Appium Grid foundation

Deferred:

- public HTTPS with Let's Encrypt
- fully productionized iOS integration
- advanced device temperature alerting

## Phase 1 Deployment Map

### `argocd` namespace

| Workload | Kubernetes kind | Placement | Scaling | Notes |
| --- | --- | --- | --- | --- |
| `argocd-server` | `Deployment` | prefer `k3s-control` | singleton | UI and API for GitOps control |
| `argocd-repo-server` | `Deployment` | prefer `k3s-control` | singleton | renders manifests from git |
| `argocd-application-controller` | `StatefulSet` or `Deployment` depending on chart | prefer `k3s-control` | singleton | reconciliation engine |
| `argocd-redis` | `Deployment` | prefer `k3s-control` | singleton | internal Argo CD dependency |

Phase 1 intent:

- keep Argo CD lightweight
- keep it close to the control-plane node
- use it to deploy the remaining platform namespaces

### `mongodb` namespace

| Workload | Kubernetes kind | Placement | Scaling | Notes |
| --- | --- | --- | --- | --- |
| `mongodb` | `StatefulSet` | prefer `k3s-worker-2` | singleton | primary phase 1 database |
| `mongodb-init` | `Job` | pin to `k3s-worker-2` | one-shot | bootstrap replica set or init logic if required |
| `devicehub-migrate` | `Job` | pin to `k3s-worker-2` | one-shot per rollout | can stay here or move to `devicehub`; phase 1 keeps DB bootstrap close to DB |

Phase 1 intent:

- one MongoDB instance with persistent volume
- no multi-node MongoDB topology yet
- keep stateful DB concerns isolated in one namespace

### `openldap` namespace

| Workload | Kubernetes kind | Placement | Scaling | Notes |
| --- | --- | --- | --- | --- |
| `openldap` | `StatefulSet` | pin to `k3s-worker-2` | singleton | persistent directory data |
| `phpldapadmin` | `Deployment` | no hard pin | singleton | always available admin UI |

Phase 1 intent:

- isolate identity infrastructure from product runtime
- expose `phpldapadmin` through ingress
- keep LDAP data persistent

### `devicehub` namespace

| Workload | Kubernetes kind | Placement | Scaling | Notes |
| --- | --- | --- | --- | --- |
| `devicehub-app` | `Deployment` | no hard pin | scalable later | UI service |
| `devicehub-auth` | `Deployment` | no hard pin | singleton initially | run in LDAP mode |
| `devicehub-api` | `Deployment` | no hard pin | scalable later | REST API |
| `devicehub-websocket` | `Deployment` | no hard pin | scalable later | realtime bridge |
| `devicehub-api-groups-engine` | `Deployment` | no hard pin | singleton | coordination workload |
| `devicehub-processor` | `Deployment` | no hard pin | scalable later | ZeroMQ processing |
| `devicehub-reaper` | `Deployment` | no hard pin | singleton | device heartbeat cleanup |
| `devicehub-triproxy-app` | `Deployment` | no hard pin | singleton | app-side ZeroMQ proxy |
| `devicehub-triproxy-dev` | `Deployment` | no hard pin | singleton | device-side ZeroMQ proxy |
| `devicehub-storage-temp` | `Deployment` | pin to `k3s-worker-2` | singleton initially | temp storage and local files |
| `devicehub-storage-plugin-apk` | `Deployment` | pin to `k3s-worker-2` | scalable later | APK storage and local files |
| `devicehub-storage-plugin-image` | `Deployment` | pin to `k3s-worker-2` | scalable later | screenshots and image files |
| `adbd` | pinned `Deployment` | pin to `k3s-worker-1` | singleton | USB + ADB access |
| `devicehub-provider` | `Deployment` | pin to `k3s-worker-1` | singleton initially | device owner workload |
| Android device workers | spawned by provider | run on `k3s-worker-1` | dynamic | not managed as regular static manifests |
| `mitmproxy` | `Deployment` | no hard pin | singleton initially | interception backend |
| `mitmweb` | `Deployment` or sidecar with `mitmproxy` | no hard pin | singleton | web UI for interception |

Phase 1 intent:

- keep DeviceHub control services on the general worker
- isolate Android USB-bound services on the dedicated Android worker
- keep traffic interception in the product namespace for now

### `appium` namespace

| Workload | Kubernetes kind | Placement | Scaling | Notes |
| --- | --- | --- | --- | --- |
| `appium-grid-router` | `Deployment` | no hard pin | singleton initially | front entrypoint for grid |
| `appium-grid-distributor` | `Deployment` | no hard pin | singleton | session distribution |
| `appium-grid-session-queue` | `Deployment` | no hard pin | singleton | queueing |
| `appium-grid-sessions` | `Deployment` | no hard pin | singleton | session map/state |
| Android Appium nodes | `Deployment` | pin to `k3s-worker-1` | scalable by device capacity | Android automation executors |

Phase 1 intent:

- host Grid control plane in Kubernetes
- keep Android Appium execution physically near Android devices
- allow later registration of external iOS Appium nodes from the Mac mini

### Observability namespace

Recommended extra namespace:

- `observability`

| Workload | Kubernetes kind | Placement | Scaling | Notes |
| --- | --- | --- | --- | --- |
| `prometheus` | `StatefulSet` | prefer `k3s-worker-2` | singleton | metrics storage and scraping |
| `grafana` | `Deployment` | prefer `k3s-worker-2` | singleton | dashboards |
| `loki` | `StatefulSet` or `Deployment` | prefer `k3s-worker-2` | singleton | centralized logs |
| `promtail` | `DaemonSet` | all k3s nodes | per node | log shipping |
| `alertmanager` | `Deployment` | prefer `k3s-worker-2` | singleton | alerts |

Phase 1 intent:

- keep observability isolated from the product namespace
- collect logs from all nodes
- provide a baseline for future device health alerts

## Phase 1 Placement Summary

### `k3s-control`

- `argocd`
- optionally light ingress/system add-ons
- node label: `devicehub.role=control`

### `k3s-worker-1`

- `adbd`
- `devicehub-provider`
- Android device workers
- Android Appium nodes
- node label: `devicehub.role=android`

### `k3s-worker-2`

- `mongodb`
- `openldap`
- `devicehub-storage-temp`
- `devicehub-storage-plugin-apk`
- `devicehub-storage-plugin-image`
- `mitmproxy` / `mitmweb`
- Appium Grid control plane
- observability stack
- node label: `devicehub.role=storage`

### `Mac mini`

- iOS Appium nodes
- WebDriverAgent
- iOS tooling
- future iOS-side DeviceHub integration processes

## Disk-sensitive workloads

Confirmed as node-fixed in phase 1:

- `mongodb`
- `openldap`
- `devicehub-storage-temp`
- `devicehub-storage-plugin-apk`
- `devicehub-storage-plugin-image`

Additional note:

- if `Prometheus`, `Loki`, or `Grafana` are configured with persistent local storage, they also become disk-sensitive workloads and should then be pinned deliberately
- if observability storage is backed by portable persistent volumes, they do not need hard node pinning for architectural reasons
