# CI/CD Avanzado

## Ubicación de los pipelines

Los pipelines están en la carpeta `pipelines/` del repositorio:

| Archivo | Propósito |
|---|---|
| `pipelines/dev/Jenkinsfile` | Build, pruebas, escaneo y push de imágenes a Docker Hub |
| `pipelines/stage/Jenkinsfile` | Deploy a stage, pruebas E2E, estrés y seguridad |
| `pipelines/master/Jenkinsfile` | Aprobación manual, deploy a master y generación de release notes |
| `pipelines/scripts/release_notes.sh` | Script standalone para generar release notes fuera del pipeline |

---

## Agentes: pods efímeros en AKS

Ninguno de los pipelines corre directamente en la VM de Jenkins. Todos usan
`agent { kubernetes { namespace 'jenkins-agents' } }`, lo que hace que Jenkins
lance un **pod efímero dentro del cluster AKS** por cada ejecución. Cada pod
tiene los containers necesarios para las fases de ese pipeline:

| Pipeline | Containers del pod |
|---|---|
| **dev** | `jdk21` (Eclipse Temurin 21 JDK) + `kubectl` (line/kubectl-kustomize) |
| **stage** | `jdk21` + `kubectl` + `newman` (postman/newman) + `locust` (locustio/locust:2.29.0) + `zap` (zaproxy/zap-stable) |
| **master** | `kubectl` (line/kubectl-kustomize) + `git` (alpine/git) |

Esto significa que los pipelines escalan bajo demanda: cuando se ejecuta un
build, Jenkins crea el pod en Kubernetes, corre las fases y lo destruye al
terminar. No ocupan recursos de la VM de Jenkins mientras no están corriendo.

---

## Fases principales y pruebas por pipeline

### Dev pipeline (`pipelines/dev/Jenkinsfile`)

| Fase | Container | Qué hace |
|---|---|---|
| **Checkout** | jdk21 | Clona el repositorio |
| **Build & Unit Tests** | jdk21 | Compila cada servicio con Gradle y ejecuta las pruebas unitarias de cada módulo |
| **Integration Tests** | jdk21 | Levanta las dependencias necesarias con `docker-compose.dev.yml` (PostgreSQL, Kafka, Redis, Neo4j, etc.) y ejecuta pruebas de integración contra esos servicios |
| **SonarQube** | jdk21 | Ejecuta el escáner de SonarQube sobre el código fuente para medir calidad, cobertura y code smells. Los resultados se envían al servidor de SonarQube configurado en Jenkins. Los reportes de cobertura de pruebas (unitarias e integración) se envían junto con el análisis y están disponibles en el dashboard del proyecto dentro de SonarQube (`http://<PUBLIC_IP>:9000/dashboard?id=cgp`) |
| **Trivy** | jdk21 | Escanea vulnerabilidades en las dependencias del proyecto (archivos `build.gradle.kts`) |
| **Build Docker** | jdk21 | Construye las 8 imágenes Docker usando los `Dockerfile` de cada servicio. El tag de cada imagen es el commit hash de 7 caracteres (`env.GIT_COMMIT.take(7)`) |
| **Push Docker** | jdk21 | Sube las 8 imágenes a Docker Hub (`prapoju/circleguard-*-service:<hash>`) usando las credenciales `dockerhub-credentials` |
| **Save Artifacts** | jdk21 | Guarda un archivo `commit-hash-{svc}.txt` por cada servicio como artefacto del build. Estos archivos contienen el commit hash exacto de la imagen que se subió |

**Artefactos generados:**
- `commit-hash-auth.txt`, `commit-hash-identity.txt`, etc. (8 archivos, uno por servicio)
- Reportes de SonarQube (en el servidor de SonarQube)
- Reporte de Trivy

### Stage pipeline (`pipelines/stage/Jenkinsfile`)

| Fase | Container | Qué hace |
|---|---|---|
| **Checkout** | jdk21 | Clona el repositorio |
| **Load Dev Hashes** | jdk21 | Usa `copyArtifacts` para recuperar los 8 archivos `commit-hash-{svc}.txt` del último build exitoso del pipeline **dev**. Esto asegura que stage despliega exactamente las mismas imágenes que se probaron en dev |
| **Deploy to Stage** | kubectl | Ejecuta `kustomize edit set image` para cambiar el tag de cada imagen al hash que viene de dev, luego `kustomize build \| kubectl apply -f -` en el namespace `circleguard-stage` |
| **Verify Rollout** | kubectl | Corre `kubectl rollout status deployment/{svc} -n circleguard-stage --timeout=180s` para cada uno de los 8 servicios. Si algún rollout no termina en 3 minutos, la fase falla |
| **E2E Tests (Newman)** | newman | Ejecuta la colección de Postman (`tests/e2e/circleguard.postman_collection.json`) contra el ambiente stage (`tests/e2e/stage.postman_environment.json`) usando Newman. Genera reporte JUnit |
| **Stress Tests (Locust)** | locust | Ejecuta pruebas de carga con Locust usando `tests/stress/locustfile.py` (15 usuarios concurrentes, ramp rate de 3/s, duración 60s). Genera reporte CSV y HTML |
| **ZAP Scan** | zap | Ejecuta `zap-baseline.py` contra `auth-service.circleguard-stage.svc.cluster.local:8180` para detectar vulnerabilidades de seguridad. Genera reporte HTML |
| **Save Artifacts** | jdk21 | Vuelve a guardar los `commit-hash-{svc}.txt` como artefactos para que el pipeline **master** los consuma |

**Artefactos generados:**
- `commit-hash-*.txt` (8 archivos, re-empaquetados para master)
- `newman-report.xml` (reporte JUnit de E2E)
- `locust-report_stats.csv` y `locust-report.html` (reporte de estrés)
- `zap-baseline-*.html` (reporte de seguridad)

### Master pipeline (`pipelines/master/Jenkinsfile`)

| Fase | Container | Qué hace |
|---|---|---|
| **Checkout** | git | Clona el repositorio |
| **Load Stage Hashes** | git | Usa `copyArtifacts` para recuperar los `commit-hash-{svc}.txt` del pipeline **stage** |
| **Approval** | — | Pausa el pipeline con `input` y espera aprobación manual (timeout 24h). Muestra un resumen con: número de build, commit de master, y tabla de imágenes con sus hashes. Solo un usuario con permisos puede aprobar |
| **Deploy to Master** | kubectl | Ejecuta `kustomize edit set image` y `kustomize build \| kubectl apply -f -` en el namespace `circleguard-master`, igual que stage pero con las imágenes aprobadas |
| **Verify Rollout** | kubectl | Verifica `rollout status` de los 8 servicios en `circleguard-master` |
| **Save Artifacts** | git | Guarda los `commit-hash-*.txt` como artefactos del build |
| **Generate Release Notes** | git | Genera el tag de release, construye las release notes clasificando commits por Conventional Commits, y las publica en GitHub Releases vía API. Usa la credencial `github-app` para autenticarse |

**Artefactos generados:**
- `commit-hash-*.txt` (8 archivos)
- Release en GitHub (tag + release notes)

---

## Comunicación entre pipelines

Los pipelines se comunican entre sí mediante **artefactos de Jenkins** y
**Docker Hub**:

```
Dev pipeline                    Stage pipeline                   Master pipeline
─────────────────               ─────────────────               ──────────────────
Build + Push images             copyArtifacts(dev)               copyArtifacts(stage)
  ↓                              ↓                                ↓
Docker Hub:                      Tiene los hashes                 Tiene los hashes
prapoju/circleguard-{svc}:<hash>  de las imágenes en dev          de las imágenes en stage
  ↓                              ↓                                ↓
Guarda commit-hash-{svc}.txt    kustomize set image               kustomize set image
  → artefacto                     → deploy con ese hash            → deploy con ese hash
                                  ↓                                ↓
                                Guarda commit-hash-{svc}.txt      Aprobación manual
                                  → artefacto para master          → release + tag
```

El flujo completo:
1. **Dev** compila, prueba y sube imágenes a Docker Hub con tag = commit hash
2. **Dev** guarda los hashes como artefactos
3. **Stage** recupera los hashes de dev, despliega exactamente esas imágenes,
   corre pruebas E2E, estrés y ZAP
4. **Stage** vuelve a guardar los mismos hashes como artefactos
5. **Master** recupera los hashes de stage, muestra el resumen para aprobación
   humana, despliega las mismas imágenes y genera la release

Las imágenes Docker son el **contrato** entre pipelines: el hash garantiza que
stage y master despliegan exactamente el mismo binario que se probó en dev.

---

## GitHub App

En `pipelines/master/Jenkinsfile:119` se usa la credencial `github-app` para
autenticar la creación de releases en GitHub:

```groovy
withCredentials([usernamePassword(credentialsId: 'github-app',
                                  usernameVariable: 'GITHUB_APP',
                                  passwordVariable: 'GITHUB_ACCESS_TOKEN')]) {
    // POST a https://api.github.com/repos/${REPO}/releases
}
```

Es una credencial de tipo **Username with password** en Jenkins:
- **Username**: el nombre de la GitHub App o el usuario asociado
- **Password**: un Personal Access Token (PAT) con permisos para crear releases
  vía API

Se usa exclusivamente en la fase `Generate Release Notes` para autenticar el
POST a la API de GitHub.

---

## Secrets necesarios en Jenkins

Para que los pipelines funcionen correctamente, deben existir las siguientes
credenciales en Jenkins:

| ID | Tipo | Propósito | Pipeline |
|---|---|---|---|
| `github-app` | Username with password | Token de GitHub con permisos para crear releases | master (Generate Release Notes) |
| `dockerhub-credentials` | Username with password | Autenticación para subir imágenes a Docker Hub | dev (Push Docker) |
| `kubernetes-token` (o el nombre que se le asigne) | Secret text | Token del ServiceAccount `jenkins` en el namespace `jenkins-agents` para que Jenkins se conecte al API de AKS | Configuración de Kubernetes Cloud en Jenkins |
| `sonarqube-token` | Secret text | Token de autenticación para que Jenkins envíe los resultados del análisis a SonarQube | Configuración de SonarQube Server en Jenkins |

### Dónde se configuran

**Kubernetes token:**
1. Se obtiene del Secret creado por `k8s/token-secret.yaml`:
   ```bash
   kubectl get secret -n jenkins-agents jenkins-secret -o jsonpath='{.data.token}' | base64 -d
   ```
2. Se agrega en **Manage Jenkins → Credentials → Global** como tipo **Secret text**
3. Se usa en **Manage Jenkins → Clouds → aks → Credentials**

**SonarQube token:**
1. Se genera desde SonarQube: **Account → Security → Generate Token**
2. Se agrega en **Manage Jenkins → Credentials → Global** como tipo **Secret text**
3. Se configura en **Manage Jenkins → System → SonarQube servers → Server authentication token**
4. El **Server URL** apunta a `http://<PUBLIC_IP>:9000`
5. Se referencia en el dev pipeline para que el escáner sepa a dónde enviar los resultados

**Webhook de SonarQube a Jenkins:**
1. Dentro de SonarQube, crea un proyecto (ej. `cgp`)
2. Ve a **Project Settings → Webhooks → Create**
3. **Name:** `jenkins-webhook`
4. **URL:** `http://<PUBLIC_IP>:8080/sonarqube-webhook/`
5. Este webhook notifica a Jenkins cuando el analysis completo y el quality gate terminan, permitiendo que el pipeline reaccione al resultado (pass/fail) sin necesidad de polling

**Docker Hub:**
1. Se genera un Personal Access Token en Docker Hub con permisos de Read & Write
2. Se agrega en Jenkins como **Username with password**, con id `dockerhub-credentials`

**GitHub App:**
1. Se genera un Personal Access Token en GitHub con permisos para crear releases
2. Se agrega en Jenkins como **Username with password**, con id `github-app`
