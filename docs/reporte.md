# Pruebas

## Tests Unitarios para Auth Service (QrTokenService)

### 1. testQrToken_ContainsCorrectSubject

**Archivo:** `services/circleguard-auth-service/src/test/java/com/circleguard/auth/service/QrTokenServiceTest.java` (NUEVO)

**Metodo:** `QrTokenService.generateQrToken(UUID anonymousId)`

**Recibe:** `UUID anonymousId` - el identificador anónimo del usuario

**Que se testea:** Se genera un token JWT y se verifica que el "sub" (subject) del payload contiene exactamente el UUID que se pasó. Se decodifica el token y se extrae el claim "sub".

**Por que es pertinente:** El token QR es usado por la app móvil para registrar presencia. Si el subject no contiene el ID correcto, el sistema no podrá vincular el check-in al usuario anónimo correcto. Esto rompería el rastreo de contactos y la generación de estadísticas de proximidad. Este test garantiza que el token transporta la identidad correcta.

---

### 2. testQrToken_ExpiresCorrectly

**Archivo:** `QrTokenServiceTest.java` (mismo archivo)

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

**Archivo:** `IdentityVaultServiceTest.java` (mismo archivo)

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