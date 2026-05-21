# Planificación de iteraciones

Cubre el punto 1 del PDF y el criterio de realizar al menos dos iteraciones completas.

Dos iteraciones marcadas con los milestones de GitHub. La primera cubre la base; la segunda completa el alcance y mete las bonificaciones.

| Iteración | Milestone | Inicio | Cierre | HU |
|---|---|---|---|---|
| 1 | Milestone 1 - Fundamentos | 2026-05-16 | 2026-05-26 | 18 |
| 2 | Milestone 2 - Completitud + Bonus | 2026-05-27 | 2026-06-08 | 28 + 4 bonus |

## Iteración 1: Fundamentos (cierra 26-may)

El objetivo es dejar listas las bases para que la iteración 2 pueda enfocarse en completar el alcance y agregar bonificaciones sin descubrir blockers de infra o metodología.

Al cierre tenemos: repo configurado con GitFlow y branch protection en `master` y `develop`, tablero Kanban en GitHub Projects con las 46 HU y workflows automáticos, esta documentación de metodología completa, Terraform modular para red y cluster K8s en los tres ambientes con backend remoto, diagrama C4 y de infraestructura publicados, los tres patrones nuevos (Circuit Breaker, External Configuration y un tercero), pipeline de CI sobre PRs con build, tests, lint, SonarQube y Trivy, health/readiness/liveness probes en todos los servicios, Prometheus y Grafana desplegados con dashboards iniciales por servicio, secretos gestionados fuera del código y RBAC inicial en el cluster.

Las 18 HU asignadas:

| Título | Epic |
|---|---|
| Configurar GitFlow como estrategia de branching | metodología |
| Configurar GitHub Projects con tablero Kanban | metodología |
| Documentar metodología ágil y planificación de iteraciones | metodología |
| Crear módulos Terraform para red e infraestructura base | terraform |
| Configurar backend remoto para estado de Terraform | terraform |
| Configurar Terraform para dev/stage/prod | terraform |
| Documentar arquitectura de infraestructura con diagramas | terraform |
| Identificar y documentar patrones existentes | patrones |
| Implementar Circuit Breaker | patrones |
| Implementar External Configuration / Feature Toggle | patrones |
| Implementar un tercer patrón adicional | patrones |
| Pipeline de CI para validación de PRs | cicd |
| Integrar SonarQube | cicd |
| Integrar Trivy | cicd |
| Stack Prometheus + Grafana | observabilidad |
| Health checks y readiness/liveness probes | observabilidad |
| Gestión segura de secretos | seguridad |
| Configurar RBAC en Kubernetes | seguridad |

La iteración se cierra cuando todas estas HU están en `Done`, la branch protection está activa en ambas ramas y queda al menos un PR ejemplar que recorrió el pipeline completo.

## Iteración 2: Completitud + Bonus (cierra 8-jun)

Se completa el 100% del alcance base y se incorporan las cuatro bonificaciones. Al cierre tenemos pipeline de CD con promoción dev → stage → prod y aprobación manual, semver automático con notificaciones de fallo y release notes automáticas, suite de pruebas completa (unitarias con cobertura mínima del 70%, integración, E2E, Locust y OWASP ZAP), ELK más tracing distribuido y métricas de negocio con alertas configuradas, TLS en endpoints públicos, escaneo continuo de vulnerabilidades, documentación restante (ADRs, manual de operaciones, análisis de costos, planes de rollback), video demostrativo, presentación lista y las cuatro bonificaciones implementadas.

Las HU agrupadas por área:

CI/CD avanzado: semver, notificaciones, aprobaciones a prod, pipeline CD.

Pruebas: unitarias, integración, E2E, Locust, OWASP ZAP, reportes de cobertura.

Releases: release notes, planes de rollback, sistema de tagging.

Observabilidad avanzada: ELK, tracing, métricas de negocio, alertas.

Seguridad avanzada: escaneo continuo, TLS.

Documentación y presentación: arquitectura completa, manual de ops, costos, video, presentación.

Bonificaciones: Multi-Cloud, Service Mesh, Chaos, FinOps.

La iteración cierra cuando todas las HU están en `Done`, el release `v1.0.0` está tageado en `master` con sus release notes publicadas, el video está enlazado en el README y la presentación está ensayada.

## Riesgos

El más probable es el sobre-alcance por querer hacer las cuatro bonificaciones; la mitigación es no abrir ningún bonus hasta que el alcance base esté en `Done`. El cluster en cloud puede generar costos altos si se queda corriendo, así que usamos minikube/kind para dev y solo provisionamos cloud para validar stage/prod en sesiones cortas. Las pruebas E2E pueden volverse flaky si comparten estado entre tests, por eso se aíslan con Testcontainers. OWASP ZAP puede generar ruido en el pipeline; empezamos con baseline scan en vez de full scan. La aprobación manual a prod requiere coordinación humana, definimos dos aprobadores por adelantado.

