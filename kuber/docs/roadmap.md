# Implementation Roadmap

## Phase 1 rollout order

1. Prepare nodes
   - verify node health
   - apply node labels
   - verify Android USB passthrough path

2. Create namespaces
   - `argocd`
   - `mongodb`
   - `openldap`
   - `devicehub`
   - `appium`
   - `mitmproxy`
   - `observability`

3. Prepare storage on `k3s-worker-2`
   - `mongodb` PVC `5Gi`
   - `openldap` PVC `1Gi`
   - `devicehub-storage-temp` PVC `5Gi`

4. Deploy `Argo CD`

5. Deploy `MongoDB`
   - `mongodb`
   - `mongodb-init`
   - `devicehub-migrate`

6. Deploy `OpenLDAP`
   - `openldap`
   - `phpldapadmin`

7. Deploy DeviceHub core
   - singleton control services
   - `devicehub-storage-temp`
   - initial replicas for scalable services

8. Deploy Android execution pair
   - `adbd`
   - `devicehub-provider`

9. Deploy `Appium Grid`
   - control plane
   - Android Appium nodes

10. Deploy `mitmproxy`
    - `mitmproxy`
    - `mitmweb`

11. Deploy observability
    - `prometheus`
    - `grafana`
    - `loki`
    - `promtail`
    - `alertmanager`

12. Configure LAN ingress
    - DeviceHub
    - `phpldapadmin`
    - Appium Grid
    - `mitmweb`

13. Later public exposure
    - public DNS
    - `cert-manager`
    - Let's Encrypt
    - HTTPS

## Validation checkpoints

- MongoDB is healthy and persistent
- OpenLDAP and phpLDAPadmin are working
- DeviceHub authenticates against LDAP
- Android devices are visible through the provider
- browser-based manual testing works
- Android automation works through Appium Grid
- `mitmproxy` works for supported traffic interception flows
- observability stack is collecting metrics and logs

## Sequencing rules

- do not deploy DeviceHub before MongoDB is ready
- do not wire LDAP auth before OpenLDAP is ready
- do not deploy `adbd` or `devicehub-provider` before Android node labels are applied
- do not publish public ingress before LAN validation is complete
