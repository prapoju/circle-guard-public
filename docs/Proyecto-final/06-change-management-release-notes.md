# 6.1 Change Management y Release Notes

## 1. Change Management Process

El proceso de gestión de cambios sigue el flujo GitFlow definido en
[02-branching.md](./02-branching.md). Cada cambio empieza como feature branch
desde `develop` y pasa por tres pipelines antes de llegar a producción:

```
feature branch → dev pipeline → stage pipeline → approval gate → master pipeline → producción
```

### Fases por pipeline

#### Dev pipeline (`pipelines/dev/Jenkinsfile`)

| Fase | Descripción |
|---|---|
| **Checkout** | Clona el repositorio |
| **Build & Unit Tests** | Compila cada servicio con Gradle y ejecuta pruebas unitarias |
| **Integration Tests** | Ejecuta pruebas de integración contra dependencias |
| **SonarQube** | Escanea calidad de código con SonarQube |
| **Trivy** | Escanea vulnerabilidades en dependencias |
| **Build Docker** | Construye imágenes Docker para cada servicio |
| **Push Docker** | Sube las imágenes a Docker Hub con tag = commit hash |
| **Save Artifacts** | Guarda los commit hashes como artefactos para el stage pipeline |

#### Stage pipeline (`pipelines/stage/Jenkinsfile`)

| Fase | Descripción |
|---|---|
| **Checkout** | Clona el repositorio |
| **Load Dev Hashes** | Recupera los commit hashes desde los artefactos del dev pipeline |
| **Deploy to Stage** | Aplica las imágenes al namespace `circleguard-stage` vía Kustomize |
| **Verify Rollout** | Verifica que todos los despliegues hayan terminado (`rollout status`) |
| **E2E Tests (Newman)** | Ejecuta pruebas end-to-end con Postman/Newman |
| **Stress Tests (Locust)** | Ejecuta pruebas de carga con Locust (15 usuarios, 60s) |
| **ZAP Scan** | Ejecuta baseline de seguridad con OWASP ZAP |
| **Save Artifacts** | Pasa los commit hashes al master pipeline |

#### Master pipeline (`pipelines/master/Jenkinsfile`)

| Fase | Descripción |
|---|---|
| **Checkout** | Clona el repositorio |
| **Load Stage Hashes** | Recupera los commit hashes desde los artefactos del stage pipeline |
| **Approval** | Pausa el pipeline y espera aprobación manual (timeout 24h). Muestra las imágenes y commits a desplegar |
| **Deploy to Master** | Aplica las imágenes al namespace `circleguard-master` vía Kustomize |
| **Verify Rollout** | Verifica que todos los despliegues hayan terminado |
| **Save Artifacts** | Guarda los commit hashes como artefactos |
| **Generate Release Notes** | Genera el tag, las release notes y las publica en GitHub |

### Quality gates

Cada pipeline funciona como puerta de calidad:
- **Dev**: pruebas, SonarQube, Trivy — si falla, el cambio no pasa a stage
- **Stage**: E2E, estrés, ZAP — si falla, no se puede promover a master
- **Master**: aprobación manual — un humano revisa qué imágenes y commits van a
  producción antes de autorizar

---

## 2. Release Tagging System

Las releases se etiquetan automáticamente al final del master pipeline.

### Formato del tag

```
v<YYYY.MM.DD>-<sha_repo>-<sha_imagen>
```

Ejemplo: `v2026.06.03-6e6cf0d-9779b87`

| Componente | Fuente | Propósito |
|---|---|---|
| `YYYY.MM.DD` | Fecha del build | Orden cronológico y trazabilidad del deploy |
| `sha_repo` | `git rev-parse --short HEAD` | Estado del código fuente (Jenkinsfile, k8s, configs) |
| `sha_imagen` | Commit hash de la imagen Docker | Código que corren los contenedores en producción |

*Referencia: `pipelines/master/Jenkinsfile:132-146`*

### Generación automática

El master pipeline crea el tag y la release en GitHub en un solo paso vía API:

```
POST https://api.github.com/repos/${REPO}/releases
```

El tag se crea automáticamente si no existe. El release se publica como
`latest` (no draft, no prerelease).

*Referencia: `pipelines/master/Jenkinsfile:222-229`*

### Tags existentes en el repositorio

| Tag | Fecha |
|---|---|
| `v2026.05.10-d43ee5c` | 2026-04-20 |
| `v2026.06.02-2354ba4-47ce7b8` | 2026-05-15 |
| `v2026.06.03-6e6cf0d-9779b87` | 2026-06-03 (último) |

---

## 3. Automatic Release Notes

Las release notes se generan automáticamente en el master pipeline clasificando
los commits desde el tag anterior.

### Clasificación por tipo de commit (Conventional Commits)

```
feat:  → New Features
fix:   → Bug Fixes
docs:  → Documentation
test:  → Tests
resto  → Other Changes (chore, refactor, style, ci, etc.)
```

*Referencia: `pipelines/master/Jenkinsfile:156-182`*

### Estructura del release

```
## CircleGuard Release v<version>

**Commit repo:**   `<sha_repo>`
**Commit imagen:** `<sha_imagen>`

### New Features
- descripción - autor

### Bug Fixes
- descripción - autor

### Documentation
- descripción - autor

### Tests
- descripción - autor

### Other Changes
- descripción - autor

---
_Jenkins build #<num> - <fecha>_
```

### Script alternativo

Existe un script standalone en `pipelines/scripts/release_notes.sh` que hace lo
mismo por fuera del pipeline, usando la variable de entorno `GITHUB_TOKEN` en
vez de `GITHUB_ACCESS_TOKEN`.

---

## 4. Rollback en AKS (master y stage)

### Rollback de aplicación (cambiar tag de imagen)

Cada despliegue en Kustomize referencia imágenes con un tag específico (commit
hash). Para revertir a una versión anterior:

```bash
# 1. Ir al overlay del ambiente a revertir
cd k8s/overlays/master    # o stage/

# 2. Cambiar todas las imágenes al hash anterior
#    (por ejemplo, volver a 67b3f13)
kustomize edit set image \
  prapoju/circleguard-auth-service:stage=prapoju/circleguard-auth-service:67b3f13
kustomize edit set image \
  prapoju/circleguard-identity-service:stage=prapoju/circleguard-identity-service:67b3f13
# ... repetir para los 8 servicios

# 3. Aplicar el rollback
kustomize build | kubectl apply -f -

# 4. Verificar que los pods se hayan reiniciado correctamente
kubectl rollout status deployment/auth-service -n circleguard-master --timeout=180s
kubectl rollout status deployment/identity-service -n circleguard-master --timeout=180s
# ... repetir para los 8 servicios
```

*Los manifiestos de overlay están en `k8s/overlays/master/kustomization.yml` y
`k8s/overlays/stage/kustomization.yml`.*

### Rollback completo del cluster AKS

Si el problema está en el cluster mismo (no en las imágenes), se puede destruir
y reconstruir solo el módulo AKS sin tocar la VM de Jenkins ni la red:

```bash
# 1. Destruir solo el cluster
terraform destroy -target="module.aks-cluster"

# 2. Reconstruir
terraform plan -target=module.aks-cluster -out aks
terraform apply "aks"

# 3. Limpiar kubeconfig y reconectar
rm -f ~/.kube/config
az aks get-credentials --resource-group cgp-rc --name cgp-cluster

# 4. Re-aplicar manifiestos K8s
cd k8s && ./script.sh
```

*Referencia completa en `docs/Proyecto-final/9.4.1-manual-operaciones-aks-y-el-infra.md`.*

---

## Referencias

| Archivo | Líneas relevantes |
|---|---|
| `pipelines/dev/Jenkinsfile` | Pipeline completo de dev |
| `pipelines/stage/Jenkinsfile` | Pipeline completo de stage |
| `pipelines/master/Jenkinsfile` | Pipeline completo de master (approval + release) |
| `pipelines/scripts/release_notes.sh` | Script standalone de release notes |
| `docs/Proyecto-final/02-branching.md` | Estrategia GitFlow |
| `k8s/overlays/master/kustomization.yml` | Overlay de master con tags de imágenes |
| `k8s/overlays/stage/kustomization.yml` | Overlay de stage con tags de imágenes |
| `docs/Proyecto-final/9.4.1-manual-operaciones-aks-y-el-infra.md` | Rebuild completo del cluster |
