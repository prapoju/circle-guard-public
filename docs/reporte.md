# Pruebas

Los microservicios seleccionados fueron:
Auth, Identity, Form, File, dashboard y notification

## Tests Unitarios para Auth Service (QrTokenService)

### 1. testQrToken_ContainsCorrectSubject

**Archivo:** `services/circleguard-auth-service/src/test/java/com/circleguard/auth/service/QrTokenServiceTest.java` 

**Metodo:** `QrTokenService.generateQrToken(UUID anonymousId)`

**Recibe:** `UUID anonymousId` - el identificador anónimo del usuario

**Que se testea:** Se genera un token JWT y se verifica que el "sub" (subject) del payload contiene exactamente el UUID que se pasó. Se decodifica el token y se extrae el claim "sub".

**Por que es pertinente:** El token QR es usado por la app móvil para registrar presencia. Si el subject no contiene el ID correcto, el sistema no podrá vincular el check-in al usuario anónimo correcto. Esto rompería el rastreo de contactos y la generación de estadísticas de proximidad. Este test garantiza que el token transporta la identidad correcta.

---

### 2. testQrToken_ExpiresCorrectly

**Archivo:** `QrTokenServiceTest.java` 

**Metodo:** `QrTokenService.generateQrToken(UUID anonymousId)`

**Recibe:** `UUID anonymousId` - el identificador anónimo del usuario

**Que se testea:** Se genera un token y se verifica que el claim "exp" (expiration) sea igual a la hora actual + el tiempo de expiración configurado (por defecto 60000ms). Se permite una tolerancia de ±5 segundos por posibles delays en la ejecución del test.

**Por que es pertinente:** Los tokens QR tienen una vida útil corta (60 segundos) por seguridad. Si el token no expirara o expirara en un tiempo incorrecto, un atacante podría reutilizar tokens viejos. Además, si expira muy rápido, los usuarios no tendrían tiempo de escanear el código. Este test asegura que la configuración de expiración se aplica correctamente.

---

## Tests Unitarios para Identity Service

### 1. testGetOrCreateAnonymousId_ExistingUser_ReturnsExisting

**Archivo:** `services/circleguard-identity-service/src/test/java/com/circleguard/identity/service/IdentityVaultServiceTest.java` (NUEVO)

**Metodo:** `IdentityVaultService.getOrCreateAnonymousId(String realIdentity)`

**Recibe:** `String realIdentity` - el email o identificador real del usuario (ej: "juan@university.edu")

**Que se testea:** Cuando un usuario ya existe en la base de datos, el servicio debe retornar el UUID anonimo existente en lugar de crear uno nuevo. Se verifica que el repository se llama con el hash correcto y que retorna el dato ya guardado.

**Por que es pertinente:** Este comportamiento es critico para cumplir con FERPA. Cada estudiante debe mantener su mismo ID anonimo a lo largo de todo su historial en el sistema. Si por error se creara un nuevo ID cada vez que el usuario accede, se perderia el rastreo de su historial de salud. Ademas, crear mapeos duplicados consumiria espacio innecesario en la base de datos y romperia la integridad referencial. Este test asegura que la deduplicacion funciona correctamente.

---

### 2. testResolveRealIdentity_NotFound_Throws404

**Archivo:** `IdentityVaultServiceTest.java` 

**Metodo:** `IdentityVaultService.resolveRealIdentity(UUID anonymousId)`

**Recibe:** `UUID anonymousId` - el identificador anonimo que no existe en la base de datos

**Que se testea:** Cuando se busca un ID anonimo que no existe, el servicio debe lanzar una excepcion `ResponseStatusException` con codigo HTTP 404 (Not Found). Se verifica que el mensaje de error sea apropiado.

**Por que es pertinente:** Este escenario ocurre cuando: (a) el ID fue eliminado de la base de datos, o (b) alguien intenta acceder con un ID invalido. Sin este manejo, el sistema retornaria null o vacio, causando errores 500 en el caller. El test garantiza que la API retorna un error 404 claro, permitiendo que el frontend muestre un mensaje apropiado al usuario o administrador.

---

### 3. testComputeHash_IsDeterministic

**Archivo:** `IdentityVaultServiceTest.java` (mismo archivo)

**Metodo:** `IdentityVaultService.computeHash(String input)` (metodo private dentro del servicio)

**Recibe:** `String input` - cualquier string para hashear

**Que se testea:** Se llama al metodo dos veces con el mismo input y se verifica que el hash resultante sea identico. Esto valida que SHA-256 es deterministico para el mismo contenido.

**Por que es pertinente:** El hash es la clave primaria para buscar mappings en la base de datos. Si SHA-256 produjera resultados diferentes para el mismo input, el sistema no podria encontrar mappings existentes - cada lookup crearia un duplicado. Este es el fundamento de toda la operacion del identity vault. Ademas, testear un metodo privado requiere reflexion o usar tecnicas de testing como mocks con Whitebox.

---

## Tests Unitarios para Form Service 

### 1. testActivateQuestionnaire_DeactivatesOthers

**Archivo:** `services/circleguard-form-service/src/test/java/com/circleguard/form/service/QuestionnaireServiceTest.java`

**Metodo:** `QuestionnaireService.activateQuestionnaire(UUID id)`

**Recibe:** `UUID id` - el ID del cuestionario a activar

**Que se testea:** Al activar un cuestionario, todos los demas cuestionarios deben marcarse como inactivos. Se verifica que el repository.save() se llama para cada cuestionario que estaba activo previamente, marcandolos con `setIsActive(false)`, y que solo el objetivo recibe `setIsActive(true)`.

**Por que es pertinente:** Solo puede haber un cuestionario activo a la vez. Esta restriccion es critica porque el sistema necesita consistencia en las preguntas que hace a los estudiantes. Si por un bug dos cuestionarios quedan activos simultaneamente, el sistema podria evaluar sintomas de manera inconsistente - algunos estudiantes harian preguntas diferentes a otros. Este test asegura que la logica de exclusividad mutua funciona correctamente y que no hay race conditions donde dos cuestionarios quedan activos al mismo tiempo.

---

### 2. testGetActiveQuestionnaire_ReturnsActiveQuestionnaire 

**Archivo:** `services/circleguard-form-service/src/test/java/com/circleguard/form/service/QuestionnaireServiceTest.java` 

**Metodo:** `QuestionnaireService.getActiveQuestionnaire()`

**Recibe:** Nada (no tiene parametros)

**Que se testea:** Cuando existe un cuestionario activo en la base de datos, debe retornarlo correctamente. Se verifica que el repository llama a `findFirstByIsActiveTrueOrderByVersionDesc()` y retorna el Optional con el cuestionario activo.

**Por que es pertinente:** Este metodo es el punto de entrada para todos los envios de encuestas de salud. Cuando un estudiante envia su survey diario, el sistema necesita saber cual es el cuestionario activo para evaluar sus respuestas. Si este metodo falla o retorna null incorrectamente, ningun estudiante podra enviar surveys. Ademas, un cuestionario activo es quello que define que preguntas se hacen y como se evalua la presencia de sintomas. Es fundamental para el flujo de salud del campus.

---

## Tests Unitarios para File Service

### 1. testSaveFile_ReturnsFilename

**Archivo:** `services/circleguard-file-service/src/test/java/com/circleguard/file/service/FileStorageServiceTest.java`

**Metodo:** `FileStorageService.saveFile(MultipartFile file)`

**Recibe:** `MultipartFile file` - archivo a guardar

**Que se testea:** Cuando se guarda un archivo, debe retornar un filename que incluye UUID y el nombre original. Se verifica que el nombre tiene el formato correcto.

**Por que es pertinente:** Este es el unico metodo implementado del file-storage. Garantiza que cada archivo guardado tiene un identificador unico (UUID) que evita colisiones de nombres. Si dos usuarios suben archivos con el mismo nombre, el UUID diferencia los archivos. Ademas, el UUID previene que usuarios malicious adivinen URLs de archivos de otros usuarios.

---

## Tests Unitarios para Dashboard Service

### 1. testGetTimeSeries_FallsBackToMockData

**Archivo:** `services/circleguard-dashboard-service/src/test/java/com/circleguard/dashboard/service/AnalyticsServiceTest.java`

**Metodo:** `AnalyticsService.getTimeSeries(String period, int limit)`

**Recibe:** `String period` ("hourly" o "daily"), `int limit`

**Que se testea:** Cuando la tabla status_events no existe (por exception), el metodo debe retornar datos mock generados en lugar de fallar. Se verifica que se retorna una lista con datos y no se lanza exception.

**Por que es pertinente:** El dashboard necesita funcionar incluso cuando la base de datos no esta completamente configurada (ej: durante desarrollo inicial o migraciones). Si el metodo falla cuando la tabla no existe, el dashboard mostraria errores en produccion durante deployments. Este test asegura resiliencia - el sistema continua funcionando con datos mock cuando hay problemas de infraestructura, y el equipo recibe aviso visual de que los datos no son reales.



# Tests de Integración

## Auth Service - Login Flow

### 1. testLogin_ReturnsJwtWithAnonymousId

**Archivo:** `services/circleguard-auth-service/src/integrationTest/java/com/circleguard/auth/AuthLoginIntegrationTest.java`

**Metodo:** `LoginController.login()` (HTTP POST `/api/v1/auth/login`)

**Que se testea:** El flujo completo de login: usuario envía credenciales LDAP → auth valida contra LDAP → auth obtiene anonymousId de identity-service → auth retorna JWT con anonymousId.

**Por que es pertinente:** Este es el punto de entrada principal de la app. Si el login falla, ningún usuario puede autenticarse. El test valida la comunicación entre auth-service e identity-service.

## Dashboard Service - PromotionClient

**Archivo:** `services/circleguard-dashboard-service/src/integrationTest/java/com/circleguard/dashboard/client/PromotionClientIntegrationTest.java`

**Clase:** `PromotionClientIntegrationTest`

**Servicios involucrados:**
- `dashboard-service` (el que corre el test)
- `promotion-service` (el que se consume)

**Tests:**

**`testGetHealthStats_ReturnsMapWithTotalUsers`**
- Metodo: `PromotionClient.getHealthStats()`
- Que se testea: El cliente hace GET a `/api/v1/health-status/stats` del promotion-service y verifica que retorna un mapa con las claves `totalUsers` y `timestamp`, sin errores.
- Por que es pertinente: El dashboard muestra estadísticas de salud de usuarios. Este test valida que el `PromotionClient` puede comunicarse con el promotion-service y obtener datos reales para el dashboard.

**`testGetHealthStatsByDepartment_ReturnsDepartmentStats`**
- Metodo: `PromotionClient.getHealthStatsByDepartment(String department)`
- Que se testea: El cliente hace GET a `/api/v1/health-status/stats/department/{department}` con `department=INGENIERIA` y verifica que retorna un mapa con `totalUsers`, `department=INGENIERIA` y sin errores.
- Por que es pertinente: El dashboard filtra estadísticas por departamento. Este test valida que los filtros funcionan correctamente y que el promotion-service retorna datos segmentados.

**Configuración:**
- Puerto del dashboard: `8080`
- Puerto del promotion-service (docker): `8088`
- Archivo env: `services/circleguard-dashboard-service/promotion-service.env`

**Comandos:**

```bash
# 1. Compilar el JAR de promotion-service
./gradlew :services:circleguard-promotion-service:bootJar

# 2. Levantar infraestructura
docker compose -f services/circleguard-dashboard-service/docker-compose.integration.yml up -d --build

# 3. Correr integration tests
./gradlew :services:circleguard-dashboard-service:integrationTest

# 4. Test específico
./gradlew :services:circleguard-dashboard-service:integrationTest --tests '*PromotionClientIntegrationTest*'
```

---

## Notification Service - LmsService

**Archivo:** `services/circleguard-notification-service/src/integrationTest/java/com/circleguard/notification/service/LmsServiceIntegrationTest.java`

**Clase:** `LmsServiceIntegrationTest`

**Servicios involucrados:**
- `notification-service` (el que corre el test)
- `identity-service` (consumido por LmsServiceImpl para resolver anonymousId)

**Tests:**

**`syncRemoteAttendance_completesWithoutError`**
- Metodo: `LmsService.syncRemoteAttendance(String userId, String status)`
- Que se testea: Llama al servicio con un `anonymousId` (UUID) y status `"SUSPECT"`. Verifica que retorna un `CompletableFuture` que completa sin excepciones en 5 segundos.
- Por que es pertinente: El `LmsServiceImpl` usa `${identity.service.url}` para resolver el anonymousId en una identidad real antes de sincronizar con el LMS externo. Este test valida que la cadena de resolución funciona end-to-end. Ademas, el metodo es `@Async`, asi que el test verifica que la ejecucion asincrona no falla.

**Configuracion:**
- Puerto del notification-service: `8082`
- Puerto de identity-service (docker): `8083`
- Kafka: `localhost:9092`

**Comandos:**
```bash
# 1. Compilar el JAR de identity-service
./gradlew :services:circleguard-identity-service:bootJar

# 2. Levantar kafka
docker compose -f services/circleguard-notification-service/docker-compose.integration.yml up -d

# 3. Correr integration tests
./gradlew :services:circleguard-notification-service:integrationTest

# 4. Test especifico
./gradlew :services:circleguard-notification-service:integrationTest --tests '*LmsServiceIntegrationTest*'
```

---

## Notification Service - PriorityAlertListener

**Archivo:** `services/circleguard-notification-service/src/integrationTest/java/com/circleguard/notification/service/PriorityAlertListenerIntegrationTest.java`

**Clase:** `PriorityAlertListenerIntegrationTest`

**Servicios involucrados:**
- `notification-service` (el que corre el test)
- `auth-service` (consumido por PriorityAlertListener para obtener admins con permiso `alert:receive_priority`)

**Tests:**

**`handlePriorityAlert_consumesMessageFromKafka`**
- Metodo: `PriorityAlertListener.handlePriorityAlert(String message)` (triggered por `@KafkaListener`)
- Que se testea: Envia un mensaje JSON al topic Kafka `alert.priority` con `{"eventType":"CONFIRMED_CASE","affectedCount":3}`. Verifica que el consumer group `notification-priority-group` avanza su offset mas alla del mensaje enviado, confirmando que el listener lo consumio y proceso en 15 segundos.
- Por que es pertinente: El `PriorityAlertListener` usa `${auth.service.url}/api/v1/users/permissions/alert:receive_priority` para obtener la lista de admins. Este test valida: (1) que Kafka listener funciona, (2) que la llamada a auth-service retorna datos, (3) que el procesamiento asincrono no falla.

**Configuracion:**
- Puerto del notification-service: `8082`
- Puerto de auth-service (docker): `8180`
- Kafka topic: `alert.priority`
- Kafka group: `notification-priority-group`

**Comandos:**
```bash
# 1. Compilar el JAR de auth-service
./gradlew :services:circleguard-auth-service:bootJar

# 2. Levantar kafka
docker compose -f services/circleguard-notification-service/docker-compose.integration.yml up -d

# 3. Correr integration tests
./gradlew :services:circleguard-notification-service:integrationTest

# 4. Test especifico
./gradlew :services:circleguard-notification-service:integrationTest --tests '*PriorityAlertListenerIntegrationTest*'
```

---

## Comandos para Lanzar

```bash
# 1. Levantar infraestructura
docker compose -f services/circleguard-auth-service/docker-compose.integration.yml up -d

# 2. Correr todos los tests de integración
./gradlew :services:circleguard-auth-service:integrationTest

# 3. Correr un test específico
./gradlew :services:circleguard-auth-service:integrationTest --tests '*AuthLoginIntegrationTest*'
```

---

# Tests E2E (Newman)

Los tests E2E se ejecutan con Newman (CLI de Postman) dentro del cluster de Kubernetes, usando DNS interno para acceder a los servicios directamente sin exponerlos. Se corren en el stage `E2E Tests` de la pipeline de stage, después del deploy y antes del push a `:stage`. Si algún test falla, la pipeline se detiene y no se promueven las imágenes.

**Archivo de colección:** `tests/e2e/circleguard.postman_collection.json`  
**Archivo de entorno:** `tests/e2e/stage.postman_environment.json`

---

## Flujo 1 — Login y obtención de JWT

**Servicios involucrados:** auth-service

**Requests:**
1. `POST /api/v1/auth/login` con `{"username": "super_admin", "password": "password"}`

**Tests:**
- Status 200
- Response tiene campo `token` → se guarda como `authToken` para los flujos siguientes

**Por qué es pertinente:** Es el punto de entrada de toda sesión autenticada. Si el login falla, todos los flujos que dependen del JWT también fallarán. Valida que el sistema de autenticación JWT + base de datos local funciona end-to-end.

---

## Flujo 2 — Registro de visitante anónimo

**Servicios involucrados:** identity-service

**Requests:**
1. `POST /api/v1/identities/visitor` con nombre, email y motivo de visita

**Tests:**
- Status 200 o 201
- Response tiene `anonymousId` → se guarda para los flujos de encuesta y validación

**Por qué es pertinente:** El anonimato del visitante es el núcleo del sistema de privacidad. Valida que el identity-service asigna correctamente un UUID anónimo a una identidad real sin exponer datos personales.

---

## Flujo 3 — Cuestionario y encuesta de salud

**Servicios involucrados:** form-service

**Requests:**
1. `POST /api/v1/questionnaires` — crea cuestionario con preguntas YES_NO
2. `POST /api/v1/questionnaires/{id}/activate` — lo activa (desactiva cualquier otro)
3. `GET /api/v1/questionnaires/active` — verifica estado post-activación
4. `POST /api/v1/surveys` — envía encuesta de salud asociada al `anonymousId` del Flujo 2

**Tests:**
- Cuestionario creado con id
- Activación exitosa
- **Verificación de estado:** cuestionario activo tiene `isActive: true` y su `id` coincide exactamente con el que creamos
- **Verificación de estado:** survey retorna `id` propio y `anonymousId` correcto (el del Flujo 2) → guardado como `surveyId`

**Por qué es pertinente:** Cubre el flujo principal de uso del sistema: un usuario llena su encuesta diaria de salud. La verificación del id del cuestionario activo garantiza que la lógica de exclusividad mutua funcionó correctamente y que no quedó activo un cuestionario anterior.

---

## Flujo 4 — Dashboard analytics básico

**Servicios involucrados:** dashboard-service

**Requests:**
1. `GET /api/v1/analytics/summary`
2. `GET /api/v1/analytics/health-board`

**Tests:**
- Ambos retornan status 200 y JSON válido

**Por qué es pertinente:** Valida que el dashboard responde correctamente a las consultas más frecuentes de los administradores de salud.

---

## Flujo 5 — Generación de token QR

**Servicios involucrados:** auth-service

**Requests:**
1. `GET /api/v1/auth/qr/generate` con `Authorization: Bearer {{authToken}}`

**Tests:**
- Status 200
- Response tiene `qrToken` y `expiresIn`
- **Verificación de estado:** `qrToken` tiene formato JWT válido — empieza con `eyJ` y tiene exactamente 3 partes separadas por `.`

**Por qué es pertinente:** El token QR es el mecanismo de check-in en campus. La verificación del formato JWT garantiza que el token es consumible por el guardia — un token malformado no podría ser escaneado ni validado.

---

## Flujo 6 — Visitor Handoff

**Servicios involucrados:** identity-service, auth-service

**Requests:**
1. `POST /api/v1/identities/visitor` — registra nuevo visitante, obtiene `anonymousId`
2. `POST /api/v1/auth/visitor/handoff` con `{"anonymousId": "..."}` — genera token de handoff

**Tests:**
- Visitante registrado con anonymousId
- Handoff retorna `token` y `handoffPayload`
- **Verificación de estado:** `handoffPayload` contiene el prefijo `HANDOFF_TOKEN` y el `anonymousId` del visitante registrado
- **Verificación de estado:** `token` tiene formato JWT válido (empieza con `eyJ`)

**Por qué es pertinente:** Valida la integración entre identity-service y auth-service para el handoff de visitantes. La verificación del `anonymousId` dentro del payload garantiza que el token está vinculado al visitante correcto y no a otro usuario.

---

## Flujo 7 — Validación de certificado médico

**Servicios involucrados:** form-service

**Requests:**
1. `POST /api/v1/surveys` con `hasFever: true`, `hasCough: true`, `validationStatus: PENDING` → obtiene `certSurveyId`
2. `GET /api/v1/certificates/pending` — consulta la cola de validación
3. `POST /api/v1/certificates/{certSurveyId}/validate?status=APPROVED&adminId=...` — valida el certificado
4. `GET /api/v1/certificates/pending` — verifica estado post-validación

**Tests:**
- Survey creada con `id` propio
- Cola de pendientes retorna array
- Validación retorna status 200
- **Verificación de estado:** `certSurveyId` ya **no aparece** en la lista de pendientes tras la validación (transición PENDING → APPROVED confirmada)

**Por qué es pertinente:** Cubre el flujo de aprobación médica completo. La verificación post-validación es crítica: confirma que el cambio de estado fue persistido y que el certificado fue realmente procesado, no solo que el endpoint devolvió 200.

---

## Flujo 8 — Analytics completo

**Servicios involucrados:** dashboard-service

**Requests:**
1. `GET /api/v1/analytics/department/INGENIERIA` — stats filtradas por departamento
2. `GET /api/v1/analytics/time-series?period=hourly&limit=24` — serie temporal de 24h
3. `GET /api/v1/analytics/trends/{locationId}` — tendencias de una ubicación (UUID)

**Tests:**
- Stats por departamento retornan JSON válido
- Time-series retorna array
- Trends retornan JSON válido

**Por qué es pertinente:** Extiende el Flujo 4 para cubrir las consultas de análisis más avanzadas. Valida que el dashboard puede segmentar datos por departamento y tiempo, lo cual es esencial para que epidemiología universitaria pueda identificar focos de contagio.


# Pruebas de Estrés (Locust)

Las pruebas de estrés se ejecutan con Locust en modo headless dentro del cluster de Kubernetes, en el stage `Stress Tests` de la pipeline de stage, después de los tests E2E y antes del push a `:stage`. Si los umbrales de rendimiento no se cumplen, la pipeline falla y no se promueven las imágenes.

**Archivo:** `tests/stress/locustfile.py`

**Configuración (Jenkinsfile línea 96-102):**
```groovy
locust -f tests/stress/locustfile.py \
    --headless \
    --users 15 \
    --spawn-rate 3 \
    --run-time 60s \
    --loglevel WARNING
```

---

## Parámetros de ejecución

| Parámetro | Valor | Explicación |
|---|---|---|
| `--users` | 15 | Número de **usuarios virtuales simultáneos**. Cada usuario es un green thread independiente que ejecuta tasks secuencialmente con delays aleatorios. |
| `--spawn-rate` | 3 | Cuántos usuarios se **crean por segundo** al inicio. Con 15 usuarios y spawn-rate 3, toma ~5 segundos alcanzar los 15 usuarios (15/3 = 5s). Esto evita un spike de carga instantáneo al empezar. |
| `--run-time` | 60s | Duración total de la prueba. Los 15 usuarios operan concurrentemente durante 60 segundos. |
| `--headless` | - | Sin UI web, todo corre por CLI (adecuado para Jenkins). |

**Total de usuarios:** 15  
**Tiempo de rampa:** ~5 segundos (15 usuarios ÷ 3/s)  
**Duración efectiva:** 60 segundos de carga sostenida

---

## Tipos de usuario simulados

| Clase | Peso | Distribución | Flujo simulado |
|---|---|---|---|
| `EstudianteUser` | 5 | **50%** | login → generar QR token (caso de uso más frecuente) |
| `VisitanteUser` | 3 | **30%** | registro anónimo → consultar cuestionario activo → enviar encuesta |
| `PersonalSaludUser` | 2 | **20%** | summary → health-board → time-series → stats por departamento |

El **peso (`weight`)** determina la probabilidad relativa de elegir cada tipo de usuario. Con weights 5, 3 y 2 (total = 10), la distribución es 5/10 = 50%, 3/10 = 30%, 2/10 = 20%.

---

## Requests ejecutados por tipo de usuario

### EstudianteUser (50% del tráfico)
- `POST /api/v1/auth/login` — inicio de sesión
- `GET /api/v1/auth/qr/generate` — generar token QR (task con peso 4, más frecuente)
- `POST /api/v1/auth/login` — re-login cuando expira token (task con peso 1)

### VisitanteUser (30% del tráfico)
- `POST /api/v1/identities/visitor` — registro de visitante
- `GET /api/v1/questionnaires/active` — consultar cuestionario activo
- `POST /api/v1/surveys` — enviar encuesta de salud (task con peso 3, más frecuente)

### PersonalSaludUser (20% del tráfico)
- `GET /api/v1/analytics/summary` (peso 3)
- `GET /api/v1/analytics/health-board` (peso 2)
- `GET /api/v1/analytics/time-series?period={period}&limit={limit}` (peso 2)
- `GET /api/v1/analytics/department/{dept}` (peso 1)

---

## Tiempos de espera entre tasks

Cada usuario espera un tiempo aleatorio **entre** tasks, simulado por `wait_time = between(min, max)`:

| Usuario | Espera entre tasks |
|---|---|
| EstudianteUser | 1-3 segundos |
| VisitanteUser | 2-5 segundos |
| PersonalSaludUser | 3-8 segundos |

Esto refleja patrones realistas: estudiantes hacen check-in rápido (1-3s), mientras que personal de salud toma más tiempo entre consultas analíticas (3-8s).

---

## Métricas recolectadas

La salida se imprime en el console log de Jenkins (permanente en el historial de builds). Se generan dos bloques:

**Bloque 1 — tabla por endpoint (built-in de Locust):**  
Req count, fallos, avg/min/max, req/s y percentiles p50–p100 por endpoint individual.

**Bloque 2 — resumen custom (hook `@events.quitting`):**

```
====================================================
       METRICAS DE RENDIMIENTO - CIRCLEGUARD
====================================================
  Requests totales     : 308
  Fallos               : 0
  Tasa de error        : 0.0%
  Throughput (req/s)   : 5.18
  Latencia p50         : 11 ms
  Latencia p95         : 1100 ms
  Latencia p99         : 3500 ms
  Latencia max         : 5364 ms
====================================================
  Umbrales de aceptacion:
  - Error rate <= 5%   : OK
  - p95 <= 3000ms      : OK
====================================================
```

---

## Umbrales de aceptación

| Métrica | Umbral | Consecuencia si falla |
|---|---|---|
| Tasa de error | ≤ 5% | Pipeline falla, imágenes no promovidas |
| Latencia p95 | ≤ 3000 ms | Pipeline falla, imágenes no promovidas |

Los umbrales se evalúan sobre el **Aggregated** (todos los endpoints combinados). El hook `@events.quitting` setea `environment.process_exit_code = 1` si se supera alguno, lo que hace fallar el stage de Jenkins.

---

## Análisis de resultados (ejecución de referencia)

Con 15 usuarios concurrentes durante 60s sobre el cluster de stage:

- **308 requests totales, 0 fallos** — tasa de error 0.0%, dentro del umbral.
- **Throughput: 5.18 req/s** — distribución correcta de carga entre los 3 tipos de usuario.
- **p95 global: 1100 ms** — dentro del umbral de 3000 ms.
- **Endpoints más lentos:** `GET /analytics/health-board` y `GET /analytics/summary` con p95 ~1100–1500 ms, atribuible a consultas agregadas sobre Neo4j sin caché.
- **Endpoint más rápido:** `GET /questionnaires/active` con p95 15 ms — lectura directa de un registro único.
- **Caso atípico:** `POST /surveys` alcanza max 5364 ms, probablemente por cold start de la conexión a Kafka en el primer mensaje del pod. El p95 del endpoint individual es 3500 ms, pero al ser una fracción pequeña del tráfico total no impacta el p95 Aggregated.

El sistema maneja la carga simulada sin errores y dentro de los umbrales de latencia definidos.

---

## Pipeline Outputs (Carpeta `pipelineoutputs/`)

Esta carpeta contiene los **logs completos de ejecución de las pipelines de Jenkins** para cada ambiente/rama del proyecto.

**Archivos:**
- `master.txt` — log de la pipeline de master (producción)
- `stage.txt` — log de la pipeline de stage
- `authdev.txt`, `identitydev.txt`, `filedev.txt`, `formdev.txt`, `dashboarddev.txt`, `notificationdev.txt` — logs de las pipelines de desarrollo de cada microservicio

**Contenido:** Cada archivo contiene la salida completa del console output de Jenkins, incluyendo:
- Inicio de pipeline (usuario que triggereó, rama git)
- Creación de pods Kubernetes
- Stages ejecutados (build, test, docker build, deploy, etc.)
- Logs de contenedores
- Resultado final (SUCCESS/FAILURE)

**Para qué sirve:** Referencia para debugging de builds fallidos, análisis de timing de stages, y auditoría de despliegues históricos.

---

## Capturas (Carpeta `capturas/`)

Colección de **capturas de pantalla** con la configuración relevante del proyecto, organizadas por sección:

- **Sección 1 (1.1 - 1.7):** Configuración de Kubernetes y Jenkins — kind cluster, configuración de Jenkins, Jenkinsfile, secrets,infraestructura k8s, pipelines
- **Sección 2 (2.1 - 2.4):** Pipelines de desarrollo — dev pipelines, auth pipeline, stages
- **Sección 3 (3.1 - 3.3):** Pruebas — tests unitarios, integración, E2E y estrés
- **Sección 4 (4.1 - 4.2):** Pipeline de stage
- **Sección 5 (5.1 - 5.2):** Pipeline de master

**Para qué sirve:** Documentación visual de la configuración para referencia rápida durante setup o troubleshooting.

---

## Pipelines (Carpeta `pipelines/`)

Definiciones de **Jenkinsfile** para las pipelines de CI/CD, organizadas por ambiente:

**Estructura:**
- `pipelines/dev/` — Pipelines de desarrollo por servicio (auth, identity, form, file, dashboard, notification, gateway, promotion)
- `pipelines/stage/` — Pipeline de stage (pre-producción)
- `pipelines/master/` — Pipeline de master (producción)

**Contenido típico por pipeline:**
- Build (compile + unit tests)
- Docker build (construcción de imágenes)
- Kubernetes deploy (despliegue al cluster)
- Integration tests
- E2E tests (Newman)
- Stress tests (Locust)
- Push a registry

**Para qué sirve:** Définition as-code del flujo de entrega. Cada cambio al Jenkinsfile es versionado y auditado via git.

---

## Manifests (Carpeta `manifests/`)

Manifiestos para **configuración de infraestructura local** de desarrollo:

**Subcarpetas:**
- `manifests/kind/` — Configuración de kind cluster (`kind-config.yaml`)
- `manifests/jenkins/` — Manifiestos Helm para Jenkins (values, namespace, SA, ingress, volume)

**Contenido:**
- `kind-config.yaml` — Definición del cluster kind multi-nodo
- `jenkins-values.yaml` — Valores Helm para Jenkins
- `jenkins-namespace.yaml` — Namespace `jenkins`
- `jenkins-01-volume.yaml` — PVC para datos Jenkins
- `jenkins-02-sa.yaml` — ServiceAccount y RBAC
- `jenkins-ingress.yaml` — Ingress para Jenkins
- `script.sh` — Script de setup

**Para qué sirve:** Configuración inicial de infraestructura para levantar kind cluster y Jenkins local para desarrollo.

---

## K8s (Carpeta `k8s/`)

Configuración **Kustomize** para despliegues en Kubernetes:

**Estructura:**
- `k8s/base/` — Templates base reutilizables
  - `k8s/base/infra/` — Manifiestos de infraestructura (Kafka, Neo4j, PostgreSQL, Redis, Zookeeper, OpenLDAP)
  - `k8s/base/services/` — Manifiestos de cada microservicio (auth, identity, form, file, dashboard, notification)
- `k8s/overlays/` — Configuraciones específicas por ambiente
  - `k8s/overlays/stage/`
  - `k8s/overlays/master/`

**Contenido típico por servicio:**
- Deployment con variables de entorno
- Service (ClusterIP)
- HPA (Horizontal Pod Autoscaler)
- ConfigMap y Secrets
- probes (liveness/readiness)

**Para qué sirve:** Kubernetes-native deployment. Kustomize permite herencia y overlays para no duplicar configuración entre ambientes.

