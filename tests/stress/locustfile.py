"""
CircleGuard - Pruebas de rendimiento y estrés con Locust
Simula tres tipos de usuarios concurrentes con flujos reales del sistema.

Umbrales de aceptación:
  - Tasa de error:  <= 5%
  - p95 latencia:   <= 3000 ms
"""

import random
from locust import HttpUser, task, between, events

AUTH_URL      = "http://auth-service.circleguard-stage.svc.cluster.local:8180"
IDENTITY_URL  = "http://identity-service.circleguard-stage.svc.cluster.local:8083"
FORM_URL      = "http://form-service.circleguard-stage.svc.cluster.local:8086"
DASHBOARD_URL = "http://dashboard-service.circleguard-stage.svc.cluster.local:8080"

ERROR_RATE_THRESHOLD = 0.05   # 5%
P95_THRESHOLD_MS     = 3000   # 3 segundos


# ---------------------------------------------------------------------------
# Tipo 1: Estudiante / usuario regular — check-in diario con QR
# Representa el 50% del tráfico (weight=5)
# ---------------------------------------------------------------------------
class EstudianteUser(HttpUser):
    """
    Flujo: login → generar QR token.
    Es el caso de uso más frecuente del sistema.
    """
    weight = 5
    wait_time = between(1, 3)
    host = AUTH_URL

    def on_start(self):
        self.token = None
        self._login()

    def _login(self):
        with self.client.post(
            "/api/v1/auth/login",
            json={"username": "super_admin", "password": "password"},
            name="POST /auth/login",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                self.token = resp.json().get("token")
                resp.success()
            else:
                resp.failure(f"Login falló: {resp.status_code}")

    @task(4)
    def generar_qr(self):
        if not self.token:
            self._login()
            return
        with self.client.get(
            "/api/v1/auth/qr/generate",
            headers={"Authorization": f"Bearer {self.token}"},
            name="GET /auth/qr/generate",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                data = resp.json()
                if "qrToken" not in data:
                    resp.failure("qrToken ausente en la respuesta")
                else:
                    resp.success()
            elif resp.status_code == 401:
                self.token = None
                resp.failure("Token expirado")
            else:
                resp.failure(f"Error inesperado: {resp.status_code}")

    @task(1)
    def re_login(self):
        """Simula renovación de sesión por expiración de token."""
        self._login()


# ---------------------------------------------------------------------------
# Tipo 2: Visitante — registro anónimo + encuesta de salud
# Representa el 30% del tráfico (weight=3)
# ---------------------------------------------------------------------------
class VisitanteUser(HttpUser):
    """
    Flujo: registrar visitante → consultar cuestionario activo → enviar encuesta.
    Valida la privacidad de identidad y el pipeline de salud.
    """
    weight = 3
    wait_time = between(2, 5)
    host = IDENTITY_URL

    def on_start(self):
        self.anonymous_id = None
        self._registrar()

    def _registrar(self):
        n = random.randint(10000, 99999)
        with self.client.post(
            "/api/v1/identities/visitor",
            json={
                "name": f"Visitante Stress {n}",
                "email": f"stress{n}@circleguard.test",
                "reason_for_visit": "Prueba de rendimiento"
            },
            name="POST /identities/visitor",
            catch_response=True
        ) as resp:
            if resp.status_code in (200, 201):
                self.anonymous_id = resp.json().get("anonymousId")
                resp.success()
            else:
                resp.failure(f"Registro visitante falló: {resp.status_code}")

    @task(1)
    def ver_cuestionario_activo(self):
        with self.client.get(
            f"{FORM_URL}/api/v1/questionnaires/active",
            name="GET /questionnaires/active",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(f"Error: {resp.status_code}")

    @task(3)
    def enviar_encuesta(self):
        if not self.anonymous_id:
            self._registrar()
            return
        with self.client.post(
            f"{FORM_URL}/api/v1/surveys",
            json={
                "anonymousId": self.anonymous_id,
                "hasFever": random.choice([True, False]),
                "hasCough": random.choice([True, False]),
                "otherSymptoms": random.choice(["", "Dolor de cabeza", "Fatiga leve"]),
                "responses": {}
            },
            name="POST /surveys",
            catch_response=True
        ) as resp:
            if resp.status_code in (200, 201):
                resp.success()
            else:
                resp.failure(f"Error al enviar encuesta: {resp.status_code}")


# ---------------------------------------------------------------------------
# Tipo 3: Personal de salud — consultas al dashboard
# Representa el 20% del tráfico (weight=2)
# ---------------------------------------------------------------------------
class PersonalSaludUser(HttpUser):
    """
    Flujo: consultar summary → health-board → time-series → stats por departamento.
    Simula el monitoreo continuo del estado de salud del campus.
    """
    weight = 2
    wait_time = between(3, 8)
    host = DASHBOARD_URL

    DEPARTAMENTOS = ["INGENIERIA", "MEDICINA", "DERECHO", "CIENCIAS", "ECONOMIA"]

    @task(3)
    def ver_summary(self):
        with self.client.get(
            "/api/v1/analytics/summary",
            name="GET /analytics/summary",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(f"Error: {resp.status_code}")

    @task(2)
    def ver_health_board(self):
        with self.client.get(
            "/api/v1/analytics/health-board",
            name="GET /analytics/health-board",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(f"Error: {resp.status_code}")

    @task(2)
    def ver_time_series(self):
        period = random.choice(["hourly", "daily"])
        limit = random.choice([12, 24, 48])
        with self.client.get(
            f"/api/v1/analytics/time-series?period={period}&limit={limit}",
            name="GET /analytics/time-series",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(f"Error: {resp.status_code}")

    @task(1)
    def ver_departamento(self):
        dept = random.choice(self.DEPARTAMENTOS)
        with self.client.get(
            f"/api/v1/analytics/department/{dept}",
            name="GET /analytics/department/{dept}",
            catch_response=True
        ) as resp:
            if resp.status_code == 200:
                resp.success()
            else:
                resp.failure(f"Error: {resp.status_code}")


# ---------------------------------------------------------------------------
# Hook de finalización: imprime métricas y evalúa umbrales
# ---------------------------------------------------------------------------
@events.quitting.add_listener
def evaluar_resultados(environment, **kwargs):
    stats = environment.runner.stats.total

    p50  = stats.get_response_time_percentile(0.50) or 0
    p95  = stats.get_response_time_percentile(0.95) or 0
    p99  = stats.get_response_time_percentile(0.99) or 0
    rmax = stats.max_response_time or 0

    # Calcular throughput real desde la duración de la prueba
    duration = (
        environment.runner.stats.last_request_timestamp
        - environment.runner.stats.start_time
    )
    throughput = stats.num_requests / duration if duration > 0 else 0

    print("\n")
    print("=" * 52)
    print("       METRICAS DE RENDIMIENTO - CIRCLEGUARD")
    print("=" * 52)
    print(f"  Requests totales     : {stats.num_requests}")
    print(f"  Fallos               : {stats.num_failures}")
    print(f"  Tasa de error        : {stats.fail_ratio * 100:.1f}%")
    print(f"  Throughput (req/s)   : {throughput:.2f}")
    print(f"  Latencia p50         : {p50:.0f} ms")
    print(f"  Latencia p95         : {p95:.0f} ms")
    print(f"  Latencia p99         : {p99:.0f} ms")
    print(f"  Latencia max         : {rmax:.0f} ms")
    print("=" * 52)
    print("  Umbrales de aceptacion:")
    print(f"  - Error rate <= 5%   : {'OK' if stats.fail_ratio <= ERROR_RATE_THRESHOLD else 'FALLO'}")
    print(f"  - p95 <= 3000ms      : {'OK' if p95 <= P95_THRESHOLD_MS else 'FALLO'}")
    print("=" * 52)
    print()

    failed = False
    if stats.fail_ratio > ERROR_RATE_THRESHOLD:
        print(f"[FALLO] Tasa de error {stats.fail_ratio*100:.1f}% supera el umbral del {ERROR_RATE_THRESHOLD*100:.0f}%")
        failed = True
    if p95 > P95_THRESHOLD_MS:
        print(f"[FALLO] p95 {p95:.0f}ms supera el umbral de {P95_THRESHOLD_MS}ms")
        failed = True

    if failed:
        environment.process_exit_code = 1
