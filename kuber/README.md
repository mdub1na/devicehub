# Kubernetes GitOps bootstrap

This directory contains Kubernetes manifests for the DeviceHub farm.

## Current layout

- `bootstrap/argocd`: ArgoCD installation overlay (already applied in cluster).
- `gitops/bootstrap/root-app.yaml`: root Application for App-of-Apps.
- `gitops/root`: child Applications (`infra` and `apps`).
- `gitops/infra`: infrastructure-level resources (currently `AppProject`).
- `gitops/apps`: app-level resources (currently `devicehub` namespace).

## Bootstrap steps

1. Update `repoURL` in:
   - `kuber/gitops/bootstrap/root-app.yaml`
   - `kuber/gitops/root/infra-app.yaml`
   - `kuber/gitops/root/apps-app.yaml`
2. Ensure `targetRevision` points to your active branch (`main` by default).
3. Apply root app:

```bash
kubectl apply -f kuber/gitops/bootstrap/root-app.yaml
```

4. Verify:

```bash
kubectl -n argocd get applications
kubectl -n argocd get appprojects
kubectl get ns devicehub
```

