# Requirements And Analysis

## Goal

Build a Kubernetes-based mobile device farm for DeviceHub on the existing infrastructure.

## Infrastructure

### Proxmox host

- `192.168.0.110`

### k3s virtual machines

- `k3s-control` - `192.168.0.121` - `4 CPU`, `6 GB RAM`, `128 GB disk`
- `k3s-worker-1` - `192.168.0.122` - `6 CPU`, `16 GB RAM`, `128 GB disk`
- `k3s-worker-2` - `192.168.0.123` - `4 CPU`, `8 GB RAM`, `128 GB disk`

### Apple hardware

- `Mac mini M4` - `16 GB RAM`, `256 GB disk`

## Functional requirements

- DeviceHub already implements browser-based manual testing.
- The browser UI already provides:
  - device screen streaming
  - remote control
  - device file access
  - device logs
  - remote app installation
- DeviceHub must also act as a hub for UI automation on Android and iOS.

## Device connectivity

- Android devices are physically connected to the Proxmox host.
- Android devices are manually passed through from Proxmox into the selected VM.
- iOS devices are handled through the external Mac mini.

## Access requirements

- DeviceHub must ultimately be available from the Internet.
- Initial verification can happen over LAN first.
- External users must be able to connect to Android devices over ADB.
- `phpLDAPadmin` must be available continuously.

## Scale targets

- `8` Android devices
- `2` iOS devices
- parallel automated test execution is required

## Repository analysis

### Main findings

- The clearest runtime topology source is `docker-compose-prod.yaml`.
- DeviceHub is split into multiple services:
  - `devicehub-app`
  - `devicehub-auth`
  - `devicehub-api`
  - `devicehub-websocket`
  - `devicehub-api-groups-engine`
  - `devicehub-triproxy-app`
  - `devicehub-triproxy-dev`
  - `devicehub-processor`
  - `devicehub-reaper`
  - `devicehub-provider`
  - `adbd`
  - storage services
  - `MongoDB`
- Runtime processes are exposed as independent `stf` / `devicehub` CLI units.
- Internal communication heavily relies on `ZeroMQ`.
- The repo already supports `LDAP` auth via `stf auth-ldap`.
- The repo already contains iOS integration, but it depends on macOS tooling and should stay on the Mac mini.

### Deployment conclusion

Everything related to DeviceHub except iOS execution can be deployed in Kubernetes.

This includes:

- UI
- API
- websocket
- auth
- groups engine
- processor
- reaper
- triproxy services
- provider
- Android ADB workloads
- storage services
- MongoDB

Additional required platform services:

- `OpenLDAP`
- `phpLDAPadmin`
- observability stack
- `mitmproxy` / `mitmweb`
- Appium Grid

## Technology decisions already agreed

### MongoDB

- MongoDB should run in Kubernetes.
- For phase 1, use a simple Kubernetes-hosted MongoDB with persistent storage.
- Do not start with a complex multi-node MongoDB topology.

### mitmproxy

- `mitmproxy` can run in Kubernetes.
- `mitmweb` can be exposed through ingress.
- Android traffic interception is realistic if proxy and CA trust are configured.
- iOS traffic interception is possible, but requires careful routing and certificate trust handling.
- Certificate pinning is a known limitation.

### Appium Grid

- Appium Grid control plane can run in Kubernetes.
- Android Appium nodes can run in Kubernetes on the Android worker.
- iOS Appium nodes should run on the Mac mini and register into the shared grid.

### Observability stack

Selected stack:

- `Traefik`
- `Prometheus + Grafana`
- `Loki + Promtail`
- `Alertmanager`
- later `cert-manager + Let's Encrypt`

### Monitoring scope

- full monitoring is required from day one:
  - metrics
  - logs
  - alerting foundation
- device temperature alerting is planned later via a DeviceHub API method
