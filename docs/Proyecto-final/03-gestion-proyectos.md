# Gestión del proyecto: GitHub Projects


## Por qué GitHub Projects

Toda la trazabilidad HU ↔ código vive en GitHub: issues, PRs, commits, labels, milestones. Mover la gestión a Jira o Trello obligaría a duplicar referencias o mantener sincronización manual, sin un beneficio claro para un proyecto académico de tres semanas. GitHub Projects v2 es gratis y no tiene curva de aprendizaje para quienes ya usan GitHub.

## El board

El project [CircleGuard - Proyecto Final IngeSoft V](https://github.com/users/ItsJuanda17/projects/3) lo crea `ItsJuanda17` como owner, con `prapoju` invitado como admin para co-mantener. Es privado, el equipo se invita por colaboración.

Tiene cuatro columnas en el campo Status: `Backlog`, `In Progress`, `In Review`, `Done`. Solo `In Progress` tiene WIP limit duro (dos por persona); en `In Review` aplica un límite suave: si pasa de cinco, paramos de tomar trabajo nuevo. Los detalles del flujo están en [01-metodologia.md](./01-metodologia.md).

Las vistas configuradas son cuatro: el board por defecto, una agrupada por épica (para ver el avance por área), otra por milestone (para comparar M1 vs M2), y "My work" filtrada por assignee. Hay una vista extra "Bonus" filtrada por `bonus:*` para no perder de vista esos ítems mientras se trabaja el alcance base.

## Labels

Se agrupan en tres familias.

Épicas, una por cada uno de los nueve requisitos del taller: `epic:metodologia`, `epic:terraform`, `epic:patrones`, `epic:cicd`, `epic:pruebas`, `epic:releases`, `epic:observabilidad`, `epic:seguridad`, `epic:documentacion`.

Bonificaciones (5% c/u): `bonus:multicloud`, `bonus:servicemesh`, `bonus:chaos`, `bonus:finops`.

Soporte: `type:story`, `type:task`, `type:bug` y `priority:high|medium|low`.

## Milestones

Dos, uno por iteración. `Milestone 1 - Fundamentos` cierra el 2026-05-26 y empuja la base lista (branching, Terraform base, patrones, CI inicial, observabilidad y seguridad base). `Milestone 2 - Completitud + Bonus` cierra el 2026-06-08 con la entrega completa más las cuatro bonificaciones. El detalle de qué HU caen en cada uno está en [04-iteraciones.md](./04-iteraciones.md).

## El flujo de un issue

Si surge algo nuevo durante el trabajo, se abre como issue con la misma estructura.

Cuando una HU del Backlog cumple DoR, alguien se asigna, mueve la tarjeta a `In Progress` y crea una rama `feature/<scope>-<descripcion>` desde `develop`. Trabaja con Conventional Commits referenciando el issue (`Closes #N` en el último commit) y abre PR a `develop`. La tarjeta pasa a `In Review` por workflow del project. Tras la revisión y los checks, el squash & merge cierra el issue y mueve la tarjeta a `Done`, también por workflow.

## Workflows automáticos

Cuatro reglas en Project Settings → Workflows: ítem agregado al project pasa a `Backlog`, PR mergeado pasa a `Done`, issue cerrado pasa a `Done`, y los issues nuevos en `prapoju/circle-guard-public` se agregan al project automáticamente.

## Visibilidad

Hay un detalle de GitHub Projects v2 que vale la pena registrar: como el repo es de `prapoju` y el project de `ItsJuanda17`, el backend de GitHub no permite linkear el project a la pestaña *Projects* del repo. La restricción exacta es `Only projects owned by the same owner as the repository can be linked.`. El project sigue siendo accesible por link directo desde este README y desde el sidebar de cada issue, donde el campo "Projects" lo muestra normalmente.
