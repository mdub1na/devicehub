# Implementation Roadmap

## Goal

This roadmap defines the recommended implementation order for phase 1 of the DeviceHub Kubernetes platform.

The order is chosen to:

- reduce hidden dependency failures
- keep stateful services stable from the start
- avoid rebuilding manifests after initial rollout
- let us validate the platform layer by layer

## Phase 1 rollout order

### 1. Prepare the cluster nodes

Tasks:

- verify hostnames, static IPs, DNS resolution, and time sync
- verify `k3s-control`, `k3s-worker-1`, and `k3s-worker-2` are healthy
- apply node labels:
  - `k3s-control` -> `devicehub.role=control`
  - `k3s-worker-1` -> `devicehub.role=android`
  - `k3s-worker-2` -> `devicehub.role=storage`
- verify the Android worker can later receive USB passthrough from Proxmox

Why first:

- every later scheduling rule depends on correct node labeling

### 2. Create the base namespaces

Namespaces:

- `argocd`
- `mongodb`
- `openldap`
- `devicehub`
- `appium`
- `mitmproxy`
- `observability`

Why early:

- it gives the platform a clean logical layout before workloads are deployed

### 3. Prepare persistent storage on the storage node

Tasks:

- confirm `local-path` storage behavior on `k3s-worker-2`
- prepare persistent volume claims for:
  - `mongodb` -> `5Gi`
  - `openldap` -> `1Gi`
  - `devicehub-storage-temp` -> `5Gi`
- validate that pinned workloads can mount storage on `k3s-worker-2`

Why before applications:

- MongoDB, OpenLDAP, and `devicehub-storage-temp` must not be introduced before their storage is defined

### 4. Deploy Argo CD

Tasks:

- deploy `Argo CD` into namespace `argocd`
- keep it pinned toward `k3s-control`
- validate UI, repo access, and application reconciliation

Why here:

- once the basic cluster and storage foundations exist, Argo CD can take over deployment of the rest of the platform

### 5. Deploy MongoDB

Tasks:

- deploy `mongodb` as a `StatefulSet`
- deploy `mongodb-init` as a `Job`
- deploy `devicehub-migrate` as a `Job`
- validate connectivity and schema/bootstrap readiness

Why before DeviceHub:

- DeviceHub depends on MongoDB as its primary state store

### 6. Deploy OpenLDAP

Tasks:

- deploy `openldap` as a `StatefulSet`
- deploy `phpldapadmin`
- validate LDAP login, search, and admin access

Why before DeviceHub auth:

- the target auth model depends on LDAP from the start

### 7. Deploy the DeviceHub core services

Tasks:

- deploy singleton control services:
  - `devicehub-app`
  - `devicehub-auth`
  - `devicehub-api`
  - `devicehub-websocket`
  - `devicehub-api-groups-engine`
  - `devicehub-reaper`
  - `devicehub-triproxy-app`
  - `devicehub-triproxy-dev`
  - `devicehub-storage-temp`
- deploy scalable services with initial single replica:
  - `devicehub-processor`
  - `devicehub-storage-plugin-apk`
  - `devicehub-storage-plugin-image`
- validate internal service connectivity
- validate LDAP-backed auth wiring

Why now:

- at this point the product can start working against real platform dependencies instead of temporary substitutes

### 8. Deploy the Android execution pair

Tasks:

- deploy `adbd` pinned to `k3s-worker-1`
- deploy `devicehub-provider` pinned to `k3s-worker-1`
- validate ADB connectivity and provider registration
- validate device discovery after USB passthrough is configured

Why after core:

- Android execution should attach to an already running DeviceHub control plane

### 9. Deploy Appium Grid

Tasks:

- deploy Grid control plane in namespace `appium`
- deploy Android Appium nodes pinned to `k3s-worker-1`
- validate Android automation flow
- leave room for later Mac mini based iOS node registration

Why after DeviceHub and Android execution:

- automation is easier to validate once the device farm itself is already alive

### 10. Deploy mitmproxy services

Tasks:

- deploy `mitmproxy`
- deploy `mitmweb`
- publish controlled access through ingress
- validate proxy routing for compatible Android flows

Why after core services:

- traffic interception is an important feature, but not a prerequisite for bringing up the farm itself

### 11. Deploy observability

Tasks:

- deploy `prometheus`
- deploy `grafana`
- deploy `loki`
- deploy `promtail`
- deploy `alertmanager`
- validate cluster metrics, application metrics, and centralized logs

Why here:

- observability is most useful once the main workloads are already present and producing real signals

### 12. Configure ingress for LAN-first access

Tasks:

- expose DeviceHub entrypoints
- expose `phpldapadmin`
- expose Appium Grid entrypoints
- expose `mitmweb`
- validate routing over LAN first

Why before public HTTPS:

- internal validation should happen before Internet exposure and certificate setup

### 13. Prepare public exposure later

Deferred from first rollout:

- public DNS
- `cert-manager`
- Let's Encrypt
- public HTTPS
- external hardening and access policy refinement

## Validation checkpoints

After phase 1 implementation, we should be able to confirm:

- MongoDB is persistent and healthy
- OpenLDAP and phpLDAPadmin work
- DeviceHub authenticates against LDAP
- Android devices are visible through the provider
- Android manual testing works in the browser
- Android automation works through Appium Grid
- mitmproxy is reachable and usable for supported traffic interception scenarios
- Grafana, Loki, Prometheus, and Alertmanager are functioning

## Important sequencing rules

- do not deploy DeviceHub before MongoDB is ready
- do not wire LDAP auth before OpenLDAP is ready
- do not deploy `adbd` or `devicehub-provider` before Android node labeling is done
- do not publish public ingress before LAN validation is complete
- do not treat Appium or mitmproxy validation as blockers for the initial DeviceHub core rollout
