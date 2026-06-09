# Documentación del Proyecto Final

Carpeta de gestión y proceso del Proyecto Final sobre `circle-guard-public`. El alcance técnico (servicios, infraestructura, pipelines, pruebas) se documenta tanto aquí como en sus propias carpetas (`terraform/`, `k8s/`, `pipelines/`).

## Contenido

| Archivo | Tema |
|---|---|
| [01-metodologia.md](./01-metodologia.md) | Kanban |
| [02-branching.md](./02-branching.md) | GitFlow |
| [03-gestion-proyectos.md](./03-gestion-proyectos.md) | GitHub Projects |
| [04-iteraciones.md](./04-iteraciones.md) | Plan de las dos iteraciones |
| [05-definiciones.md](./05-definiciones.md) | DoR y DoD |
| [patrones.md](./patrones.md) | Patrones de diseño (Circuit Breaker, External Config, Health Checks) |
| [multi-cloud-comparison.md](./multi-cloud-comparison.md) | Comparativa Azure AKS vs DigitalOcean DOKS |
| [9.4.1-manual-operaciones-aks-y-el-infra.md](./9.4.1-manual-operaciones-aks-y-el-infra.md) | Manual de operaciones Azure (AKS + Platform) |
| [06-change-management-release-notes.md](./06-change-management-release-notes.md) | Change Management, release tagging, release notes y rollback |
| [ci-cd.md](./ci-cd.md) | Pipelines CI/CD, fases, agentes en AKS, secrets |

## Otras carpetas importantes del proyecto

### `terraform/` — Infraestructura como Código (IaC)

Toda la infraestructura en Azure se define con Terraform. Se compone de:

| Carpeta | Contenido |
|---|---|
| `modules/aks/` | Cluster AKS con 3 pools de nodos (default/sistema, master, stage). Kubernetes 1.34, RBAC, OIDC, service principal auth |
| `modules/platform/` | VM de Jenkins (Ubuntu 22.04, Standard_B2s) con VNet, subred, NSG (puertos 22/8080/9000), IP pública estática y storage account para diagnósticos |
| `k8s/` | Manifiestos de Kubernetes que se aplican post-despliegue: namespaces (`jenkins-agents`, `circleguard-stage`, `circleguard-master`), ServiceAccount `jenkins` con ClusterRole y tokens |
| `sonarqube/` | Docker Compose para levantar SonarQube Community en la VM de Jenkins |
| `terraform.tfvars.example` | Ejemplo de variables para configurar el despliegue |
| `cloud-init-jenkins.txt` | Script cloud-init que instala OpenJDK 21, Jenkins, Docker y docker-compose en la VM |

### `k8s/` — Manifiestos de Kubernetes

Los manifiestos que se despliegan en el cluster AKS para los diferentes ambientes:

| Carpeta | Contenido |
|---|---|
| `base/` | Recursos base: ConfigMap compartido (`circleguard-config`), Secrets, infraestructura (PostgreSQL, Kafka, Redis, Neo4j, OpenLDAP, Mailhog), deployments de los 8 microservicios (auth, identity, dashboard, notification, form, file, promotion, gateway), stack de observabilidad (Prometheus, Grafana, Jaeger, ELK), y backup/restore de BD |
| `overlays/` | Configuraciones por ambiente: `stage/`, `master/`, `do-dev/`, `do-stage/`, `do-master/`. Cada overlay tiene su namespace, kustomization con tags de imágenes y patches de ConfigMap |
| `security/` | Cluster issuer de cert-manager, ingress HTTPS para el gateway |
| `jenkins/` | Manifiestos para desplegar Jenkins en Kubernetes (volumen, service account, valores Helm) |
| `components/` | Componentes Kustomize reutilizables: `no-observability` (quita ELK, Jaeger, Prometheus, Grafana), `no-logging` (quita solo ELK) |

### `pipelines/` — Pipelines CI/CD (Jenkinsfiles)

Los pipelines corren como pods efímeros dentro del namespace `jenkins-agents` del cluster AKS:

| Archivo | Propósito |
|---|---|
| `dev/Jenkinsfile` | Build, unit tests, integration tests, SonarQube, Trivy, build Docker y push a Docker Hub |
| `stage/Jenkinsfile` | Deploy a stage, verify rollout, E2E (Newman), stress (Locust), ZAP scan |
| `master/Jenkinsfile` | Aprobación manual, deploy a master, verify rollout, generación de release notes vía GitHub API |
| `scripts/release_notes.sh` | Script standalone para generar release notes fuera del pipeline |

La comunicación entre pipelines se hace mediante artefactos de Jenkins (`commit-hash-{svc}.txt`) y Docker Hub (imágenes taggeadas con el commit hash).

## Convenciones

Cualquier cambio sobre estos documentos pasa por PR a `develop`, igual que el código. Los diagramas se hacen en Mermaid cuando se puede, ya que GitHub los renderiza nativo. Las decisiones de arquitectura técnica viven en `docs/adr/`, no acá.

## Enlaces útiles

- Project Kanban: https://github.com/users/ItsJuanda17/projects/3
- Issues: https://github.com/prapoju/circle-guard-public/issues
- Milestones: https://github.com/prapoju/circle-guard-public/milestones
- PDF del proyecto: [`../Proyecto Final IngeSoft V.pdf`](../Proyecto%20Final%20IngeSoft%20V.pdf)
