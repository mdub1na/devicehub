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
- `mitmproxy`: traffic interception services
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
- Appium Grid control plane
- observability stack

Fixed placement requirements:

- `MongoDB` must be fixed to this node
- `OpenLDAP` must be fixed to this node
- `devicehub-storage-temp` must be fixed to this node
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
- `MongoDB`, `OpenLDAP`, and `devicehub-storage-temp` must be pinned to `k3s-worker-2`
- other DeviceHub control services should remain movable and not be pinned to a single worker by default
- singleton coordination services should avoid unnecessary relocation where practical, but do not need hard pinning unless they own local state

## Required Affinity

Phase 1 workloads with hard node requirements:

| Workload | Required node label | Why it is required |
| --- | --- | --- |
| `adbd` | `devicehub.role=android` | needs USB-attached Android devices on the dedicated Android worker |
| `devicehub-provider` | `devicehub.role=android` | must run next to `adbd` and the physically attached Android devices |
| `mongodb` | `devicehub.role=storage` | stores database data on the fixed storage node |
| `openldap` | `devicehub.role=storage` | stores LDAP directory data on the fixed storage node |
| `devicehub-storage-temp` | `devicehub.role=storage` | uses node disk for temporary files |

Workloads intentionally not using required affinity in phase 1:

- `devicehub-app`
- `devicehub-auth`
- `devicehub-api`
- `devicehub-websocket`
- `devicehub-api-groups-engine`
- `devicehub-processor`
- `devicehub-reaper`
- `devicehub-triproxy-app`
- `devicehub-triproxy-dev`
- `devicehub-storage-plugin-apk`
- `devicehub-storage-plugin-image`
- Appium Grid control plane services

Rationale:

- these services do not depend on local USB ownership
- these services do not need fixed node-local product storage in phase 1
- keeping them movable gives the cluster more scheduling flexibility

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
- separate namespaces for `devicehub`, `mongodb`, `appium`, `openldap`, `mitmproxy`, `argocd`
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
| `devicehub-storage-plugin-apk` | `Deployment` | no hard pin | scalable later | uses `devicehub-storage-temp` as backend storage service |
| `devicehub-storage-plugin-image` | `Deployment` | no hard pin | scalable later | uses `devicehub-storage-temp` as backend storage service |
| `adbd` | pinned `Deployment` | pin to `k3s-worker-1` | singleton | USB + ADB access |
| `devicehub-provider` | `Deployment` | pin to `k3s-worker-1` | singleton initially | device owner workload |
| Android device workers | spawned by provider | run on `k3s-worker-1` | dynamic | not managed as regular static manifests |

Phase 1 intent:

- keep DeviceHub control services on the general worker
- isolate Android USB-bound services on the dedicated Android worker

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

### `mitmproxy` namespace

| Workload | Kubernetes kind | Placement | Scaling | Notes |
| --- | --- | --- | --- | --- |
| `mitmproxy` | `Deployment` | no hard pin | singleton initially | interception backend |
| `mitmweb` | `Deployment` or sidecar with `mitmproxy` | no hard pin | singleton | web UI for interception |

Phase 1 intent:

- isolate traffic interception from the main DeviceHub runtime
- make release and access policy management simpler

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

Additional note:

- if `Prometheus`, `Loki`, or `Grafana` are configured with persistent local storage, they also become disk-sensitive workloads and should then be pinned deliberately
- if observability storage is backed by portable persistent volumes, they do not need hard node pinning for architectural reasons

## Phase 1 Storage Strategy

### General decision

Three disk-bound platform services use persistent storage in phase 1.

Services covered by this rule:

- `mongodb`
- `openldap`
- `devicehub-storage-temp`

### Why this is the chosen model

- it keeps pod restarts safe
- it avoids accidental data loss during rollout or node-level restarts
- it matches the fixed placement of the storage node
- it keeps the first implementation predictable

### Cleanup assumption

- `devicehub-storage-temp`

are still treated as persistent services, but temporary file cleanup is expected to be handled by the services themselves.

Phase 1 assumption:

- disk growth should not rely on pod restarts for cleanup
- cleanup logic belongs to the application services, not to Kubernetes volume churn

### Volume model

Use separate persistent volumes / claims for each service.

Do not share one common application data volume across these workloads.

Planned mapping:

- `mongodb` -> dedicated persistent volume
- `openldap` -> dedicated persistent volume
- `devicehub-storage-temp` -> dedicated persistent volume

### Storage implementation choice for phase 1

Chosen option:

- `local-path` storage on `k3s-worker-2`

Practical meaning:

- persistent volumes for the five disk-bound services are backed by storage on the fixed storage node
- workloads remain pinned to `k3s-worker-2`
- this keeps storage simple and aligned with the single-node storage strategy chosen for phase 1

Services using this storage model:

- `mongodb`
- `openldap`
- `devicehub-storage-temp`

Operational implication:

- this is a simple and predictable phase 1 storage model
- it is not designed for multi-node failover
- if later we need portable or replicated storage, this can be revisited as a separate architecture step
### Initial PVC sizes

- `mongodb` -> `5Gi`
- `openldap` -> `1Gi`
- `devicehub-storage-temp` -> `5Gi`

### Storage plugin note

- `devicehub-storage-plugin-apk`
- `devicehub-storage-plugin-image`

do not get their own required affinity or dedicated persistent volumes in phase 1.

Reason:

- they use `devicehub-storage-temp` as the backend storage service for writes and reads
- persistence responsibility stays with `devicehub-storage-temp`

## Phase 1 Scaling Policy

### `devicehub` namespace

Singleton workloads:

- `devicehub-app`
- `devicehub-auth`
- `devicehub-api`
- `devicehub-websocket`
- `devicehub-api-groups-engine`
- `devicehub-reaper`
- `devicehub-triproxy-app`
- `devicehub-triproxy-dev`
- `devicehub-storage-temp`

Scalable workloads:

- `devicehub-processor`
- `devicehub-storage-plugin-apk`
- `devicehub-storage-plugin-image`

Paired scaling workloads:

- `adbd`
- `devicehub-provider`

Paired scaling rule:

- `adbd` and `devicehub-provider` do not scale independently
- they scale together as one Android execution pair
- a new pair only makes sense when a new Android execution node or a new USB ownership boundary is added

### `mongodb` namespace

Singleton workloads:

- `mongodb`
- `mongodb-init`
- `devicehub-migrate`

### `openldap` namespace

Singleton workloads:

- `openldap`
- `phpldapadmin`

### `argocd` namespace

Singleton workloads:

- `argocd-server`
- `argocd-repo-server`
- `argocd-application-controller`
- `argocd-redis`

Phase 1 note:

- Argo CD is intentionally kept simple and non-HA in the first implementation

### `appium` namespace

Singleton workloads:

- `appium-grid-router`
- `appium-grid-distributor`
- `appium-grid-session-queue`
- `appium-grid-sessions`

Scalable workloads:

- Android Appium nodes

Phase 1 note:

- Grid control plane stays singleton
- execution nodes scale with Android capacity

### `mitmproxy` namespace

Singleton workloads:

- `mitmproxy`
- `mitmweb`

### `observability` namespace

Singleton workloads:

- `prometheus`
- `grafana`
- `loki`
- `alertmanager`

Scalable workloads:

- `promtail`

Scaling note:

- `promtail` is expected to run per node, typically as a `DaemonSet`

## Phase 1 Scaling Summary

### Singleton by default

- all `argocd` workloads
- all `mongodb` workloads
- all `openldap` workloads
- most `devicehub` control services
- Appium Grid control plane
- `prometheus`
- `grafana`
- `loki`
- `alertmanager`

### Scalable in phase 1

- `devicehub-processor`
- `devicehub-storage-plugin-apk`
- `devicehub-storage-plugin-image`
- Android Appium nodes
- `promtail`

### Scalable only as an execution pair

- `adbd`
- `devicehub-provider`

## Phase 1 Kubernetes Resource Types

### `argocd` namespace

| Workload | Resource type |
| --- | --- |
| `argocd-server` | `Deployment` |
| `argocd-repo-server` | `Deployment` |
| `argocd-application-controller` | `StatefulSet` |
| `argocd-redis` | `Deployment` |

### `mongodb` namespace

| Workload | Resource type |
| --- | --- |
| `mongodb` | `StatefulSet` |
| `mongodb-init` | `Job` |
| `devicehub-migrate` | `Job` |

### `openldap` namespace

| Workload | Resource type |
| --- | --- |
| `openldap` | `StatefulSet` |
| `phpldapadmin` | `Deployment` |

### `devicehub` namespace

| Workload | Resource type |
| --- | --- |
| `devicehub-app` | `Deployment` |
| `devicehub-auth` | `Deployment` |
| `devicehub-api` | `Deployment` |
| `devicehub-websocket` | `Deployment` |
| `devicehub-api-groups-engine` | `Deployment` |
| `devicehub-processor` | `Deployment` |
| `devicehub-reaper` | `Deployment` |
| `devicehub-triproxy-app` | `Deployment` |
| `devicehub-triproxy-dev` | `Deployment` |
| `devicehub-storage-temp` | `Deployment` |
| `devicehub-storage-plugin-apk` | `Deployment` |
| `devicehub-storage-plugin-image` | `Deployment` |
| `adbd` | `Deployment` |
| `devicehub-provider` | `Deployment` |

### `appium` namespace

| Workload | Resource type |
| --- | --- |
| `appium-grid-router` | `Deployment` |
| `appium-grid-distributor` | `Deployment` |
| `appium-grid-session-queue` | `Deployment` |
| `appium-grid-sessions` | `Deployment` |
| Android Appium nodes | `Deployment` |

### `mitmproxy` namespace

| Workload | Resource type |
| --- | --- |
| `mitmproxy` | `Deployment` |
| `mitmweb` | `Deployment` |

### `observability` namespace

| Workload | Resource type |
| --- | --- |
| `prometheus` | `StatefulSet` |
| `grafana` | `Deployment` |
| `loki` | `StatefulSet` |
| `promtail` | `DaemonSet` |
| `alertmanager` | `Deployment` |

## Resource Type Summary

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

- all remaining workloads in phase 1

## GitOps Layout

The proposed GitOps repository layout is documented separately in [gitops-layout.md](./gitops-layout.md).

## AppProject Model

Phase 1 decision:

- use one shared `Argo CD AppProject` for the whole platform

### AppProject name

- `devicehub-platform`

### Allowed source repository

- `git@github.com:mdub1na/devicehub.git`

### Allowed destination cluster

- `https://kubernetes.default.svc`

### Allowed namespaces

- `argocd`
- `mongodb`
- `openldap`
- `devicehub`
- `appium`
- `mitmproxy`
- `observability`

### Allowed cluster-scoped resources in phase 1

- `Namespace`

### Applications using this AppProject

- `argocd`
- `mongodb`
- `openldap`
- `devicehub`
- `appium`
- `mitmproxy`
- `observability`

### Why this model was chosen

- it keeps phase 1 simple
- it is strict enough for one cluster and one platform repo
- it avoids premature complexity from splitting projects too early
- it can be split into multiple AppProjects later if the platform grows
