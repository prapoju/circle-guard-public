# Patrones de diseño


## Patrones existentes en el repo

| Patrón | Categoría | Dónde se ve |
|---|---|---|
| Database per Service | Datos | Cada servicio con su DB aislada: auth-service, identity-service, dashboard-service, form-service tienen PostgreSQL propia; promotion-service combina PostgreSQL + Neo4j; notification-service y file-service no tienen DB |
| Event-Driven Architecture | Comunicación | `spring-kafka` en 4 servicios; listeners (`PriorityAlertListener`, `ExposureNotificationListener`, `CircleFencedListener`, `SurveyListener`); producers en `HealthSurveyService` y `FormService` |
| Retry | Resiliencia | `spring-retry` con `@Retryable(maxAttempts=3, backoff=@Backoff(delay=2000))` y `@Recover` en `PushServiceImpl`, `EmailServiceImpl`, `SmsServiceImpl` |
| BFF (parcial) | Arquitectura | `dashboard-service` agrega datos para el frontend Expo; `notification-service` adapta payloads multi-canal (push/email/SMS) |
| External Configuration (parcial) | Configuración | `application.yml` por servicio con variables `${VAR:default}` resueltas desde env vars en K8s |
| Health Check (básico) | Operativo | `readinessProbe` TCP sobre el puerto del servicio en los manifiestos K8s |
| Service Discovery (implícito) | Operativo | DNS nativo de Kubernetes (`http://auth-service:8180`); sin Eureka/Consul |

Los más sólidos son **Database per Service** y **Event-Driven Architecture**. Los marcados como *parcial* o *implícito* son la base sobre la que apoyamos los nuevos patrones.

## Patrones nuevos implementados

### Circuit Breaker 

Protege a un servicio cuando el downstream al que llama falla repetidamente: en vez de seguir golpeando un endpoint caído (consumiendo threads, conexiones y tiempo), el Circuit Breaker "abre el circuito" y rechaza las llamadas de inmediato durante un periodo, ejecutando un fallback. Después de ese periodo permite una llamada de prueba para ver si el downstream recuperó, y vuelve a cerrar el circuito si todo está bien.

Implementación: [Resilience4j](https://resilience4j.readme.io/) integrado vía `resilience4j-spring-boot3:2.2.0`. Tres lugares con CB:

| Cliente | Servicio | Llama a | Instance name |
|---|---|---|---|
| `IdentityClient` | auth-service | identity-service | `identityService` |
| `PromotionClient` | dashboard-service | promotion-service | `promotionService` |
| `PushServiceImpl` | notification-service | Gotify (push notif. externo) | `gotifyApi` |

#### Configuración

La configuración base es la misma en los tres `application.yml`:

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failureRateThreshold: 50          # 50% de fallos en la ventana -> OPEN
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        slowCallDurationThreshold: 2s
        slowCallRateThreshold: 50
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

Solo cambia el `instance` que cada servicio declara. La instancia hereda los valores de `default` salvo que se sobrescriban explícitamente.

#### Estados

El CB tiene tres estados:

- `CLOSED`: estado normal, las llamadas pasan al downstream. Lleva la cuenta de éxitos y fallos en una ventana deslizante.
- `OPEN`: una vez que la tasa de fallos supera `failureRateThreshold` (50% en una ventana de 10 llamadas, con mínimo 5 para empezar a evaluar), el CB se abre. Durante `waitDurationInOpenState` (30s) todas las llamadas son rechazadas sin invocar al downstream — se ejecuta el método `fallback`.
- `HALF_OPEN`: tras la espera, el CB pasa a este estado y permite hasta `permittedNumberOfCallsInHalfOpenState` (3) llamadas de prueba. Si la mayoría tiene éxito, vuelve a `CLOSED`. Si fallan, vuelve a `OPEN`.

#### Fallbacks por cliente

`IdentityClient` lanza `IdentityServiceUnavailableException` (mapeada a HTTP 503 vía `@ResponseStatus`), porque sin identidad anonimizada no se puede crear sesión y es preferible fallar rápido que devolver datos inconsistentes.

`PromotionClient` devuelve un `Map<String, Object>` con `{error: "Service unavailable"}` y un timestamp. El dashboard puede renderizar este caso degradado en vez de quedar bloqueado.

`PushServiceImpl` combina `@CircuitBreaker` con el `@Retryable` existente. Cuando el CB está abierto, el fallback se ejecuta sin tocar Gotify ni reintentar, y deja un log de auditoría con estado `CB_OPEN` distinto del `FAILED` (post-retry).

#### Prueba

`IdentityClientCircuitBreakerTest` (en auth-service) valida los cuatro escenarios principales con una instancia programática de Resilience4j:

- Estado inicial `CLOSED`.
- Transición `CLOSED -> OPEN` tras fallos repetidos.
- Recuperación `OPEN -> HALF_OPEN -> CLOSED` cuando el downstream vuelve.
- Reapertura `HALF_OPEN -> OPEN` cuando la llamada de prueba falla.

El test usa valores reducidos (sliding window de 2, wait de 500ms) para correr en milisegundos sin tocar configuración de producción.

### External Configuration (mejorado)

El patrón ya estaba implementado en su forma básica: cada `application.yml` lee variables con `${VAR:default}` y los deployments K8s las inyectaban como `env:` listadas una por una en cada manifest. Esa forma tenía dos problemas concretos:

- **Duplicación**: `IDENTITY_SERVICE_URL` aparecía en `auth-service.yml` y `notification-service.yml`; `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` se repetían en cuatro deployments distintos. Cambiar una URL implicaba editar varios archivos.
- **Secretos en texto plano**: `SPRING_DATASOURCE_PASSWORD: password` vivía en YAML versionado en Git, junto con el código. Eso es exactamente lo que el patrón debería evitar.

La mejora externaliza la configuración a dos recursos K8s — un único ConfigMap común para todos los servicios y un Secret común para credenciales — con override por ambiente vía Kustomize.

#### Estructura

```
k8s/
├── base/
│   ├── config/
│   │   ├── circleguard-config.yml    ConfigMap unico con vars compartidas
│   │   └── circleguard-secrets.yml   Secret con credenciales
│   └── services/...                  Deployments (envFrom)
└── overlays/
    ├── stage/configmap-patches.yml   Override de ENV_NAME y LOG_LEVEL
    └── master/configmap-patches.yml  Override de ENV_NAME y LOG_LEVEL
```

El `circleguard-config` contiene las vars que son comunes a varios servicios: `SPRING_DATASOURCE_USERNAME`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `SPRING_LDAP_URLS`, las URLs entre servicios (`AUTH_SERVICE_URL`, `IDENTITY_SERVICE_URL`, `PROMOTION_SERVICE_URL`) y dos vars de ambiente (`ENV_NAME`, `LOG_LEVEL`) que se sobreescriben por overlay. La password de la BD vive en el Secret.

#### Cómo lo consume cada deployment

```yaml
env:
  - name: SPRING_DATASOURCE_URL
    value: jdbc:postgresql://postgres:5432/circleguard_auth
envFrom:
  - configMapRef:
      name: circleguard-config
  - secretRef:
      name: circleguard-secrets
```

`SPRING_DATASOURCE_URL` queda como única línea hardcoded en cada deployment porque la BD es distinta por servicio (`circleguard_auth`, `circleguard_identity`, etc.) y no se puede compartir en el ConfigMap. El resto entra por `envFrom`. `notification-service` no tiene BD, así que solo usa el `envFrom`.

#### Override por ambiente

Los valores en `circleguard-config` son los de dev. Cada overlay aplica un patch que Kustomize mergea con la base:

- `overlays/stage` → `ENV_NAME: stage`, `LOG_LEVEL: DEBUG`.
- `overlays/master` → `ENV_NAME: prod`, `LOG_LEVEL: WARN`.

El servicio recibe siempre las mismas variables, pero sus valores cambian según el ambiente sin recompilar imagen ni tocar `application.yml`. Eso es el corazón del patrón: el mismo artefacto corre en múltiples ambientes leyendo configuración externa.

#### Validación

```bash
# Render del manifiesto base (dev por defecto)
kubectl kustomize k8s/base

# Render con overlay aplicado
kubectl kustomize k8s/overlays/stage
kubectl kustomize k8s/overlays/master
```

En la salida de `kubectl kustomize k8s/overlays/stage` se ve que el ConfigMap tiene `ENV_NAME: stage` y `LOG_LEVEL: DEBUG`, mientras que en `k8s/base` siguen siendo `dev`/`INFO`.

### Health Check con Actuator y HealthIndicators custom

> Pendiente
