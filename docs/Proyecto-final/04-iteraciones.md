# Planificación de iteraciones

Cubre el punto 1 del PDF y el criterio "realizar al menos dos iteraciones completas" de la HU US-03.

Dos iteraciones marcadas con los milestones de GitHub. La primera cubre la base; la segunda completa el alcance y mete las bonificaciones.

| Iteración | Milestone | Inicio | Cierre | HU |
|---|---|---|---|---|
| 1 | Milestone 1 - Fundamentos | 2026-05-16 | 2026-05-26 | 18 |
| 2 | Milestone 2 - Completitud + Bonus | 2026-05-27 | 2026-06-08 | 28 + 4 bonus |

## Iteración 1: Fundamentos (cierra 26-may)

El objetivo es dejar listas las bases para que la iteración 2 pueda enfocarse en completar el alcance y agregar bonificaciones sin descubrir blockers de infra o metodología.

Al cierre tenemos: repo configurado con GitFlow y branch protection en `master` y `develop`, tablero Kanban en GitHub Projects con las 46 HU y workflows automáticos, esta documentación de metodología completa, Terraform modular para red y cluster K8s en los tres ambientes con backend remoto, diagrama C4 y de infraestructura publicados, los tres patrones nuevos (Circuit Breaker, External Configuration y un tercero), pipeline de CI sobre PRs con build, tests, lint, SonarQube y Trivy, health/readiness/liveness probes en todos los servicios, Prometheus y Grafana desplegados con dashboards iniciales por servicio, secretos gestionados fuera del código y RBAC inicial en el cluster.

Las 18 HU asignadas:

| ID | Título | Epic |
|---|---|---|
| US-01 | Configurar GitFlow como estrategia de branching | metodología |
| US-02 | Configurar GitHub Projects con tablero Kanban | metodología |
| US-03 | Documentar metodología ágil y planificación de iteraciones | metodología |
| US-04 | Crear módulos Terraform para red e infraestructura base | terraform |
| US-05 | Configurar backend remoto para estado de Terraform | terraform |
| US-06 | Configurar Terraform para dev/stage/prod | terraform |
| US-07 | Documentar arquitectura de infraestructura con diagramas | terraform |
| US-08 | Identificar y documentar patrones existentes | patrones |
| US-09 | Implementar Circuit Breaker | patrones |
| US-10 | Implementar External Configuration / Feature Toggle | patrones |
| US-11 | Implementar un tercer patrón adicional | patrones |
| US-12 | Pipeline de CI para validación de PRs | cicd |
| US-13 | Integrar SonarQube | cicd |
| US-14 | Integrar Trivy | cicd |
| US-28 | Stack Prometheus + Grafana | observabilidad |
| US-31 | Health checks y readiness/liveness probes | observabilidad |
| US-35 | Gestión segura de secretos | seguridad |
| US-36 | Configurar RBAC en Kubernetes | seguridad |

La iteración se cierra cuando todas estas HU están en `Done`, la branch protection está activa en ambas ramas y queda al menos un PR ejemplar que recorrió el pipeline completo. La retro se documenta en `retros/2026-05-26-iteracion-1.md`.

## Iteración 2: Completitud + Bonus (cierra 8-jun)

Se completa el 100% del alcance base y se incorporan las cuatro bonificaciones. Al cierre tenemos pipeline de CD con promoción dev → stage → prod y aprobación manual, semver automático con notificaciones de fallo y release notes automáticas, suite de pruebas completa (unitarias con cobertura mínima del 70%, integración, E2E, Locust y OWASP ZAP), ELK más tracing distribuido y métricas de negocio con alertas configuradas, TLS en endpoints públicos, escaneo continuo de vulnerabilidades, documentación restante (ADRs, manual de operaciones, análisis de costos, planes de rollback), video demostrativo, presentación lista y las cuatro bonificaciones implementadas.

Las HU agrupadas por área:

CI/CD avanzado: US-15 (semver), US-16 (notificaciones), US-17 (aprobaciones a prod), US-18 (pipeline CD).

Pruebas: US-19 (unitarias), US-20 (integración), US-21 (E2E), US-22 (Locust), US-23 (OWASP ZAP), US-24 (reportes de cobertura).

Releases: US-25 (release notes), US-26 (planes de rollback), US-27 (sistema de tagging).

Observabilidad avanzada: US-29 (ELK), US-30 (tracing), US-32 (métricas de negocio), US-33 (alertas).

Seguridad avanzada: US-34 (escaneo continuo), US-37 (TLS).

Documentación y presentación: US-38 (arquitectura completa), US-39 (manual de ops), US-40 (costos), US-41 (video), US-42 (presentación).

Bonificaciones: BONUS-01 (Multi-Cloud), BONUS-02 (Service Mesh), BONUS-03 (Chaos), BONUS-04 (FinOps).

La iteración cierra cuando todas las HU están en `Done`, el release `v1.0.0` está tageado en `master` con sus release notes publicadas, el video está enlazado en el README y la presentación está ensayada. Retro en `retros/2026-06-08-iteracion-2.md`.

## Riesgos

El más probable es el sobre-alcance por querer hacer las cuatro bonificaciones; la mitigación es no abrir ningún bonus hasta que el alcance base esté en `Done`. El cluster en cloud puede generar costos altos si se queda corriendo, así que usamos minikube/kind para dev y solo provisionamos cloud para validar stage/prod en sesiones cortas. Las pruebas E2E pueden volverse flaky si comparten estado entre tests, por eso se aíslan con Testcontainers. OWASP ZAP puede generar ruido en el pipeline; empezamos con baseline scan en vez de full scan. La aprobación manual a prod (US-17) requiere coordinación humana, definimos dos aprobadores por adelantado.

## Formato de retrospectiva

Cada acta sigue este esqueleto:

```markdown
# Retrospectiva iteración N — YYYY-MM-DD

## Métricas
HU completadas, cycle time promedio, throughput semanal, WIP promedio.

## Qué salió bien

## Qué se puede mejorar

## Acciones para la siguiente iteración
- (owner, fecha objetivo)
```
