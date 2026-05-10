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
1. `POST /api/v1/questionnaires` — crea un cuestionario con preguntas YES_NO
2. `POST /api/v1/questionnaires/{id}/activate` — lo activa (desactiva cualquier otro)
3. `GET /api/v1/questionnaires/active` — verifica que hay un cuestionario activo
4. `POST /api/v1/surveys` — envía encuesta de salud asociada al `anonymousId` del Flujo 2

**Tests:**
- Cuestionario creado con id
- Activación exitosa
- Cuestionario activo tiene `isActive: true`
- Encuesta enviada con status 200 o 201

**Por qué es pertinente:** Cubre el flujo principal de uso del sistema: un usuario llena su encuesta diaria de salud. Valida la lógica de exclusividad mutua (solo un cuestionario activo) y el envío de surveys.

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
- `qrToken` se guarda en entorno para usos futuros

**Por qué es pertinente:** El token QR es el mecanismo de check-in en campus. Valida que un usuario autenticado puede generar el código que luego escanea el guardia. Si este endpoint falla, nadie puede registrar su entrada.

---

## Flujo 6 — Check-in con token QR

**Servicios involucrados:** auth-service, notification-service

**Requests:**
1. `POST /api/v1/auth/qr/validate` con el `qrToken` generado en el Flujo 5

**Tests:**
- Status 200
- Response confirma el check-in con timestamp

**Por qué es pertinente:** Este flujo completa el ciclo del token QR. El guardia escanea el código generado por el usuario y lo valida en el sistema. Es el paso final para registrar la presencia del visitante en el campus y es necesario para el rastreo de contactos.

---

## Flujo 7 — Visitor Handoff

**Servicios involucrados:** identity-service, auth-service

**Requests:**
1. `POST /api/v1/identities/visitor` — registra nuevo visitante, obtiene `anonymousId`
2. `POST /api/v1/auth/visitor/handoff` con `{"anonymousId": "..."}` — genera token de handoff

**Tests:**
- Visitante registrado con anonymousId
- Handoff retorna `token` y `handoffPayload`
- `handoffPayload` contiene el prefijo `HANDOFF_TOKEN`

**Por qué es pertinente:** Cubre el flujo de transferencia de sesión para visitantes que necesitan continuar usando el sistema desde otro dispositivo o contexto. Valida la integración entre identity-service y auth-service para un caso de uso menos común pero crítico.

---

## Flujo 8 — Validación de certificado médico

**Servicios involucrados:** form-service

**Requests:**
1. `POST /api/v1/surveys` con `hasFever: true`, `hasCough: true` y `validationStatus: PENDING` → obtiene `surveyId`
2. `GET /api/v1/certificates/pending` — verifica que el survey aparece en la cola de validación
3. `POST /api/v1/certificates/{surveyId}/validate?status=APPROVED&adminId=...` — valida el certificado

**Tests:**
- Survey creada y guardada con id
- Lista de pendientes no está vacía
- Validación retorna status 200

**Por qué es pertinente:** Cubre el flujo de aprobación médica, que es el más sensible del sistema. Un estudiante con síntomas envía su encuesta, queda pendiente de revisión, y el personal de salud la aprueba o rechaza. Valida que la transición de estados (PENDING → APPROVED) funciona correctamente y que el endpoint de validación persiste el cambio.

---

## Flujo 9 — Analytics completo

**Servicios involucrados:** dashboard-service

**Requests:**
1. `GET /api/v1/analytics/department/INGENIERIA` — stats filtradas por departamento
2. `GET /api/v1/analytics/time-series?period=hourly&limit=24` — serie temporal de 24h
3. `GET /api/v1/analytics/trends/1` — tendencias de una ubicación específica

**Tests:**
- Stats por departamento retornan JSON válido
- Time-series retorna array
- Trends retornan JSON válido

**Por qué es pertinente:** Extiende el Flujo 4 para cubrir las consultas de análisis más avanzadas. Valida que el dashboard puede segmentar datos por departamento y tiempo, lo cual es esencial para que epidemiología universitaria pueda identificar focos de contagio.

