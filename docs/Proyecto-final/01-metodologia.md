# Metodología: Kanban


## Por qué Kanban

Considerado el tamaño del equipo y la duración del proyecto (alrededor de tres semanas), Scrum exige más overhead del que se justifica: roles dedicados, sprints con compromiso fijo, ceremonias recurrentes. Kanban encaja mejor porque permite flujo continuo, no congela el alcance y deja al equipo ajustar prioridades sin esperar al cierre de un sprint.

En Kanban las iteraciones no son sprints rígidos sino cadencias de entrega; usamos los milestones de GitHub como marcadores temporales (ver [04-iteraciones.md](./04-iteraciones.md)).

## Cómo lo aplicamos

El tablero (descrito en [03-gestion-proyectos.md](./03-gestion-proyectos.md)) hace visible el trabajo. Limitamos el WIP a dos ítems por persona en `In Progress` para evitar acumulación y reducir cycle time. Las políticas de entrada y salida están en [05-definiciones.md](./05-definiciones.md).

## Tablero Kanban

Las cuatro columnas:

- `Backlog`: HU priorizada y lista para tomarse (cumple DoR). Sin límite.
- `In Progress`: alguien la está trabajando en una rama `feature/*`. Máximo dos por persona.
- `In Review`: PR abierto con checks corriendo. Si pasa de cinco ítems acumulados, paramos de tomar trabajo nuevo y priorizamos revisar.
- `Done`: mergeada a `develop`, DoD cumplida.

## Ceremonias

Mínimas. Standup async diario por el canal del equipo (ayer, hoy, bloqueos), refinamiento bajo demanda cuando una HU del Backlog no cumple DoR, y revisión obligatoria por PR (al menos un reviewer distinto del autor).

## Métricas

Cycle time por HU (objetivo bajo cinco días), throughput semanal (al menos cinco en M1 y ocho en M2), WIP promedio y antigüedad del Backlog. Si una HU lleva más de dos semanas sin mover, se revisa para entender si está bloqueada.

## Priorización

Tres reglas. Primero, bloqueantes: si una HU bloquea a otras (por ejemplo US-04 Terraform bloquea US-28 Prometheus), va antes. Segundo, peso en el PDF: las épicas grandes (Terraform 20%, CI/CD 15%) entran temprano. Tercero, las bonificaciones solo se abren cuando el 100% del alcance base está en `Done`.
