# Estrategia de branching: GitFlow


## Por qué GitFlow

Tenemos tres ambientes (`dev`, `stage`, `prod`) y la entrega final exige aprobación manual a producción y release notes automáticas por tag. Eso se mapea naturalmente a `release/*` y tags sobre `master`. GitHub Flow puro deja muy poco espacio para preparar releases sin congelar el día a día; GitFlow se ajusta mejor. El contenido del Taller 2 ya estaba en `master`, así que conservamos esa convención histórica.

## Modelo

```
master (producción, recibe solo de release/* o hotfix/*)
  │
  └─ develop (integración del día a día)
       │
       ├─ feat/<descripcion>
       ├─ docs/<descripcion>
       ├─ fix/<descripcion>
       ├─ release/<version>   (sale de develop, merge a master + develop, genera tag)
       └─ hotfix/<version>    (sale de master, merge a master + develop, genera tag)
```


## Nombres de rama

Prefijos válidos: `feat/`, `docs/`, `fix/`, `chore/`, `test/`, `release/`, `hotfix/`. Todos salen de `develop` salvo `release/` y `hotfix/` (ver arriba). Algunos ejemplos del repo: `feat/circuit-breaker-promotion-service`, `docs/proyecto-final-metodologia`, `release/v1.0.0`, `hotfix/v1.0.1-cve-fix`.

## Pull Requests

Se mergea por PR, nunca push directo a `master` ni `develop`. Tamaño objetivo bajo 400 líneas cambiadas; si se pasa, conviene dividir. Hace falta al menos un reviewer distinto del autor y que pasen los checks del CI. Para ramas normales usamos squash & merge; en `release/*` y `hotfix/*` preferimos merge commit para conservar la trazabilidad de la preparación de release.

## Convención de commits

Conventional Commits, alineado con el versionado semántico automático que se generará a partir del histórico:

```
<tipo>: <descripción>
```

Tipos en uso: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`, `perf`, `ci`, `build`, `revert`. Algunos ejemplos:

```
feat: add circuit breaker between promotion and identity
fix: retry token exchange on 503
docs: initial methodology docs
```

## Protección de ramas

En `master` exigimos PR, una aprobación, todos los status checks (build, tests, Sonar, Trivy), historia lineal, y bloqueo de force-push y deletes. En `develop` aplica lo mismo pero con checks más livianos (build + tests). Ambas reglas se configuran en Settings → Branches del repo.


