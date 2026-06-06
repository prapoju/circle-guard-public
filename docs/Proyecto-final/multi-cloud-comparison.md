# Comparativa Multi-Cloud: Azure (AKS) vs DigitalOcean (DOKS)

Cubre el Bonus 1 "Implementación Multi-Cloud" desplegando CircleGuard en dos
proveedores: Azure (AKS, primario, ya existente) y DigitalOcean (DOKS,
secundario). Ambos clusters corren el **mismo binario** (imágenes
`prapoju/circleguard-*` fijadas al tag `67b3f13`, arquitectura amd64) para que la
comparativa sea justa.

> Las tablas de rendimiento traen placeholders (`TBD`). Se completan tras correr
> Locust contra cada cluster siguiendo la metodología de abajo.

## Por qué DigitalOcean y no GCP

GCP fue la primera opción, pero su free trial **exige un prepago en Colombia**
(requisito regional). Se pivoteó a DigitalOcean: el crédito de **$200 del GitHub
Student Developer Pack** (gratis con el correo `@u.icesi.edu.co`) cubre el gasto,
y DOKS usa nodos amd64, así que las imágenes existentes corren sin rebuild.

## Arquitectura

| Aspecto | Azure (AKS) | DigitalOcean (DOKS) |
|---|---|---|
| IaC | `terraform/` (módulo `modules/aks`) | `terraform/digitalocean/` (módulo `modules/doks`) |
| Backend de estado | Azure Blob (`cgpaccount/tfstate`) | DO Spaces S3-compat (`circleguard-tfstate-do`) |
| Overlay K8s | `k8s/overlays/stage` (`circleguard-stage`) | `k8s/overlays/do-stage` (`circleguard-do-stage`) |
| Región | (según tfvars de AKS) | `nyc3` |
| Nodos | pools `default`/`stage`/`master` | pool `stage`, 2× `s-2vcpu-4gb` |
| Plano de control | Gratis (tier Free) | Gratis (no-HA) |

## Metodología de medición

Las pruebas usan el `locustfile.py` existente en `tests/stress/`. **Importante:**
ese archivo apunta a DNS interno del cluster con el namespace `circleguard-stage`
hardcodeado (p. ej. `auth-service.circleguard-stage.svc.cluster.local`), por lo
que debe ejecutarse **dentro de cada cluster** (como Pod/Job en el namespace
correspondiente), no por port-forward externo. Para DO, el namespace es
`circleguard-do-stage`, así que hay dos opciones:

1. **Recomendada** — parametrizar el namespace en `locustfile.py` vía variable de
   entorno (`NAMESPACE`, default `circleguard-stage`) y correr Locust como Job
   en cada cluster.
2. Mantener un duplicado del locustfile con el namespace de DO.

Comando de referencia (5 minutos, 50 usuarios, ramp 5/s), ejecutado en un pod
con Locust dentro de cada namespace:

```bash
# Azure (namespace circleguard-stage)
locust -f locustfile.py --headless -u 50 -r 5 -t 5m --csv=azure-run

# DigitalOcean (namespace circleguard-do-stage, con el locustfile apuntando a ese ns)
locust -f locustfile.py --headless -u 50 -r 5 -t 5m --csv=do-run
```

Los CSV de cada corrida pueden archivarse en `tests/stress/results/`.

## Resultados de rendimiento

> Mismo perfil de carga (50 VUs, 5 min) en ambos clouds.

| Métrica | Azure (AKS) | DigitalOcean (DOKS) |
|---|---|---|
| Latencia p50 (ms) | TBD | TBD |
| Latencia p95 (ms) | TBD | TBD |
| Latencia p99 (ms) | TBD | TBD |
| Throughput (req/s) | TBD | TBD |
| Tasa de error (%) | TBD | TBD |
| Requests totales | TBD | TBD |

Umbrales de aceptación del proyecto: error rate ≤ 5 %, p95 ≤ 3000 ms.

## Costo

Estimaciones de cómputo 24/7 (sin egress ni balanceadores). Verificar contra la
factura real de cada proveedor.

| Concepto | Azure (AKS) | DigitalOcean (DOKS) |
|---|---|---|
| Plano de control | Gratis (tier Free) | Gratis (cluster no-HA) |
| Nodos | Depende del VM size del pool (ver `terraform.tfvars` de AKS) — TBD | 2× `s-2vcpu-4gb` ≈ $48/mes |
| Almacenamiento de estado/backup | Azure Blob | DO Spaces ≈ $5/mes (250 GB incl.) |
| **Aprox. por mes** | TBD | **≈ $53/mes** |

El crédito de **$200 del GitHub Student Pack** cubre ~3–4 meses del cluster DOKS
corriendo continuo. Para ahorrar: bajar `doks_node_count`, escalar el pool a 0
desde el panel/`doctl`, o `terraform destroy` en `terraform/digitalocean/`.

## Estrategia de respaldo entre clouds

- **Backup** (`k8s/base/backup/postgres-backup-cronjob.yml`): CronJob horario que
  hace `pg_dumpall` del Postgres del cluster primario y sube el dump a una DO
  Space S3-compatible (`circleguard-backups-do/<env>/`, timestamped + `latest.sql`).
- **Restore** (`k8s/base/backup/restore/`): CronJob cada 6 h, solo en el overlay de
  DO, que descarga `latest.sql` del entorno origen y lo restaura en el Postgres de
  DOKS, manteniéndolo caliente para failover.
- Autenticación a Spaces por Secret de access keys S3 (`spaces-credentials`),
  portable entre Azure y DO (vía `aws-cli` con `--endpoint-url`). El Secret **no**
  se commitea.

| Objetivo | Valor |
|---|---|
| RPO (Recovery Point Objective) | ~1 h (cadencia del backup) |
| RTO (Recovery Time Objective) | ~15 min (tiempo de restore en cluster nuevo) |

## Balanceo de carga entre clouds

- **Recomendado:** DNS de Cloudflare (gratis) con dos registros A (ingress de Azure
  + ingress de DO), weighted DNS y TTL bajo para failover. Las credenciales de
  Cloudflare **no** se commitean.
- **Fallback local:** `scripts/multicloud-loadbalance.sh` — health-check de ambos
  ingress (`/actuator/health`) y round-robin con failover automático sobre los
  endpoints sanos.

## Tradeoffs

| Dimensión | Azure (AKS) | DigitalOcean (DOKS) |
|---|---|---|
| Madurez del equipo en la plataforma | Alta (entorno primario) | Nueva, pero curva suave |
| Plano de control | Gratis | Gratis (no-HA) |
| Simplicidad de aprovisionamiento | Media (RG, SP, pools) | Alta (un recurso, slugs simples) |
| Free tier para el proyecto | Crédito de estudiante | $200 Student Pack |
| Compatibilidad de imágenes | amd64 | amd64 (sin rebuild) |
| Ecosistema / servicios gestionados | Muy amplio | Acotado pero suficiente |
| Riesgo de recursos | Pools dimensionados | `s-2vcpu-4gb` (4 GB) puede quedar corto para todo el stack (ver nota) |

> **Nota de recursos:** 2× `s-2vcpu-4gb` = 8 GB RAM totales. El stack completo
> (Postgres, Kafka, Zookeeper, Redis, Neo4j, OpenLDAP, Mailhog, ELK, Jaeger,
> Prometheus, Grafana + 8 microservicios) puede no caber. Si hay `Pending` por
> recursos: subir a `s-4vcpu-8gb` en `terraform/digitalocean/terraform.tfvars`,
> aumentar `doks_node_count`, o recortar el stack de observabilidad en el overlay de DO.
