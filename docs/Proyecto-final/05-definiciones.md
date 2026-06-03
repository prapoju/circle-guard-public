# Definición de Ready y Definición de Done

## Definición de Ready

Una HU está lista para tomarse (moverse de Backlog a In Progress) cuando el título sigue el formato acordado (`US-XX: <verbo> <objeto>` o `BONUS-XX: ...`), tiene la descripción tipo HU (`Como <rol>, quiero <funcionalidad> para <beneficio>`), incluye al menos dos criterios de aceptación verificables (sin ambigüedades del tipo "que funcione bien"), tiene asignados label de épica y milestone, y no tiene dependencias bloqueadas. Si depende de otra HU, la dependencia ya está en `Done` o al menos en `In Review`. Si encima la HU se ve demasiado grande para un solo PR, se divide antes de tomarse. La estimación rough (S/M/L) se discute como comentario, no es obligatoria.

Una HU del Backlog que no cumple DoR no se toma. Se refina en el issue hasta que cumpla.

## Definición de Done

Una HU pasa a `Done` cuando todos sus criterios de aceptación están marcados, el código sigue las convenciones del proyecto y no quedan TODOs sin issue asociado ni secretos hardcodeados. Las pruebas unitarias correspondientes están agregadas o actualizadas, la cobertura del servicio no bajó respecto al baseline, y si la HU toca integración entre servicios hay al menos una prueba de integración cubriéndola. El pipeline de CI corre verde (build, tests, lint, y SonarQube y Trivy cuando aplican), sin nuevos blockers ni nuevas vulnerabilidades HIGH/CRITICAL introducidas por el cambio.

La documentación se actualiza si la HU cambió la forma de usar un servicio (su README), si introdujo una decisión de arquitectura significativa (un ADR en `docs/adr/`), si cambió la topología (`docs/architecture.md`), o si tocó pipelines o infra (`docs/operations.md`).

El PR pasó al menos por una revisión de alguien distinto del autor, los comentarios están resueltos o respondidos, el squash & merge a `develop` ya ocurrió, y el cambio aterrizó en `dev` automáticamente con un smoke test manual pasando.

## DoD específico por épica

Algunas épicas suman criterios extra:

En `terraform`, después del apply el `terraform plan` queda limpio, el state remoto se actualizó correctamente y las variables del módulo quedan documentadas en su README.

En `cicd`, el pipeline ejecuta sin fallar en una corrida completa de prueba y los artefactos generados (reportes, imágenes) quedan publicados como artifacts del run.

En `pruebas`, el reporte (cobertura, performance, ZAP, etc.) queda archivado como artifact y un análisis breve de los resultados va en el PR o en `docs/`.

En `observabilidad`, hay un dashboard funcional con datos reales en al menos un ambiente y al menos una alerta probada (se disparó intencionalmente para verificar).

En `seguridad`, el control implementado pasa por validación activa: bloquea efectivamente lo que tiene que bloquear, no basta con que esté configurado.

En `documentacion`, el documento es accesible desde el índice principal y no tiene enlaces rotos.

## Excepciones

Si una HU no puede cumplir parte del DoD por razón válida (ambiente externo no disponible, dependencia bloqueada por algo fuera del equipo), se documenta en el PR y se abre un issue de seguimiento con label `chore` para cerrar el gap. Solo así puede moverse a `Done` sin cumplir el DoD completo.
