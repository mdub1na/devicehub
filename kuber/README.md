# DeviceHub K8s Notes

This directory contains architecture notes for deploying the DeviceHub mobile farm on the current Proxmox, k3s, and Mac mini infrastructure.

## Documents

- [Requirements and analysis](./docs/requirements.md)
- [Target architecture](./docs/architecture.md)

## Current decisions

- Kubernetes cluster:
  - `k3s-control` - `192.168.0.121`
  - `k3s-worker-1` - `192.168.0.122`
  - `k3s-worker-2` - `192.168.0.123`
- Android devices will be connected to the Proxmox host and passed through into one dedicated Android worker VM.
- iOS execution stays on the external `Mac mini`.
- Main Kubernetes namespaces:
  - `devicehub`
  - `mongodb`
  - `appium`
  - `openldap`
- Selected platform stack:
  - `Traefik`
  - `Prometheus + Grafana`
  - `Loki + Promtail`
  - `Alertmanager`
  - later `cert-manager + Let's Encrypt`
- `Argo CD` will be used as the GitOps deployment layer for Kubernetes workloads.

## Next focus

- finalize phase 1 deployment map by namespace and node placement
- define `Argo CD` application boundaries
- prepare implementation layout for `5.3 codex`
