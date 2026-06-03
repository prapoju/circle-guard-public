#!/bin/bash
set -e
cd k8s/overlays/stage

# Hashes
AUTH_HASH="67b3f13"
IDENTITY_HASH="67b3f13"
FORM_HASH="67b3f13"
FILE_HASH="67b3f13"
DASHBOARD_HASH="67b3f13"
NOTIFICATION_HASH="67b3f13"
GATEWAY_HASH="67b3f13"
PROMOTION_HASH="67b3f13"

kustomize edit set image circleguard-auth-service=prapoju/circleguard-auth-service:${AUTH_HASH}
kustomize edit set image circleguard-identity-service=prapoju/circleguard-identity-service:${IDENTITY_HASH}
kustomize edit set image circleguard-form-service=prapoju/circleguard-form-service:${FORM_HASH}
kustomize edit set image circleguard-file-service=prapoju/circleguard-file-service:${FILE_HASH}
kustomize edit set image circleguard-dashboard-service=prapoju/circleguard-dashboard-service:${DASHBOARD_HASH}
kustomize edit set image circleguard-notification-service=prapoju/circleguard-notification-service:${NOTIFICATION_HASH}
kustomize edit set image circleguard-gateway-service=prapoju/circleguard-gateway-service:${GATEWAY_HASH}
kustomize edit set image circleguard-promotion-service=prapoju/circleguard-promotion-service:${PROMOTION_HASH}

kustomize build | kubectl apply -f -
