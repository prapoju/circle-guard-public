# Pruebas

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