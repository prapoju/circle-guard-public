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

---

# Tests de Integración

## Auth Service - Login Flow

### 1. testLogin_ReturnsJwtWithAnonymousId

**Archivo:** `services/circleguard-auth-service/src/integrationTest/java/com/circleguard/auth/AuthLoginIntegrationTest.java`

**Metodo:** `LoginController.login()` (HTTP POST `/api/v1/auth/login`)

**Que se testea:** El flujo completo de login: usuario envía credenciales LDAP → auth valida contra LDAP → auth obtiene anonymousId de identity-service → auth retorna JWT con anonymousId.

**Por que es pertinente:** Este es el punto de entrada principal de la app. Si el login falla, ningún usuario puede autenticarse. El test valida la comunicación entre auth-service e identity-service.

## Comandos para Lanzar

```bash
# 1. Levantar infraestructura
docker compose -f services/circleguard-auth-service/docker-compose.integration.yml up -d

# 2. Correr todos los tests de integración
./gradlew :services:circleguard-auth-service:integrationTest

# 3. Correr un test específico
./gradlew :services:circleguard-auth-service:integrationTest --tests '*AuthLoginIntegrationTest*'
```

## Correr Integration Tests

```bash
./gradlew :services:circleguard-auth-service:integrationTest
```

## Correr un Test Específico

```bash
./gradlew :services/circleguard-auth-service:integrationTest --tests '*AuthLoginIntegrationTest*'
```
