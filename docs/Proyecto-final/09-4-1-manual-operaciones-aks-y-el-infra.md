# Manual de Operaciones — Infraestructura Azure (AKS + Platform)

> **Alcance:** Este manual cubre únicamente los módulos de infraestructura en **Azure**:
> el módulo **AKS** (`modules/aks/`) que provisiona el cluster Kubernetes, y el
> módulo **Platform** (`modules/platform/`) que provisiona la VM de Jenkins con su
> red y almacenamiento. Quedan fuera del manual: la configuración de DigitalOcean
> (DOKS), la aplicación CircleGuard, los pipelines de CI/CD, los patrones de
> diseño, y las pruebas.

---

## Índice

1. [Arquitectura general](#1-arquitectura-general)
2. [Backend de Terraform: Blob Storage](#2-backend-de-terraform-blob-storage)
3. [Colaboradores: permisos al backend](#3-colaboradores-permisos-al-backend)
4. [Credenciales de aplicación: Service Principal](#4-credenciales-de-aplicación-service-principal)
5. [Despliegue con Terraform](#5-despliegue-con-terraform)
6. [Post-despliegue: Jenkins VM y SonarQube](#6-post-despliegue-jenkins-vm-y-sonarqube)
7. [Post-despliegue: Jenkins (configuración inicial)](#7-post-despliegue-jenkins-configuración-inicial)
8. [Conectar kubectl al cluster AKS](#8-conectar-kubectl-al-cluster-aks)
9. [Configurar Kubernetes Cloud en Jenkins](#9-configurar-kubernetes-cloud-en-jenkins)
10. [Credenciales de Docker Hub en Jenkins](#10-credenciales-de-docker-hub-en-jenkins)
11. [Integrar SonarQube con Jenkins](#11-integrar-sonarqube-con-jenkins)
12. [Comandos importantes](#12-comandos-importantes)

---

## 1. Arquitectura general

El `terraform apply` desde la raíz de `terraform/` crea **17 recursos de Azure**
organizados en dos módulos:

### Módulo AKS (`modules/aks/`)

| Recurso | Nombre | Propósito |
|---|---|---|
| Resource Group | `cgp-rc` | Contenedor lógico del cluster AKS |
| AKS Cluster | `cgp-cluster` | Cluster Kubernetes con RBAC y OIDC |
| Node Pool | `default` | Pool del sistema (1× `Standard_D2_v4`) |
| Node Pool | `master` | Pool para ambiente master (1× `Standard_B2s`) |
| Node Pool | `stage` | Pool para ambiente stage (1× `Standard_B2s`) |

### Módulo Platform (`modules/platform/`)

| Recurso | Nombre | Propósito |
|---|---|---|
| Resource Group | `plat-rg` | Contenedor lógico de la plataforma CI/CD |
| Virtual Network | `jenkins` | Red aislada para la VM (rango `10.0.0.0/16`) |
| Subnet | `plat-subn` | Subred de la VM (rango `10.0.1.0/24`) |
| Public IP | `vm_jenkins_public_ip` | IP estática para acceder a Jenkins desde internet |
| NSG | `vm_jenkins_nsg` | Firewall que protege la VM |
| NSG Rule | SSH (TCP 22) | Permite conexiones SSH desde cualquier IP |
| NSG Rule | Jenkins (TCP 8080) | Permite tráfico HTTP a la interfaz web de Jenkins |
| NSG Rule | SonarQube (TCP 9000) | Permite tráfico HTTP a SonarQube |
| Network Interface | `vm_jenkins_nic` | Tarjeta de red de la VM, conectada a la subred y a la IP pública |
| Storage Account | `platsa` | Almacena logs de boot diagnostics de la VM |
| Linux VM | `jenkins` | Servidor Ubuntu 22.04 LTS con Jenkins, Docker y Docker Compose |

Además, existe un **backend de Terraform** creado manualmente (no por Terraform)
que almacena el estado remoto: un Storage Account llamado `cgpaccount` con un
container `tfstate`. Eso se explica en la siguiente sección.

---

## 2. Backend de Terraform: Blob Storage

Terraform necesita guardar su archivo de estado (`terraform.tfstate`) en un lugar
compartido para que todo el equipo pueda acceder a él. En Azure usamos **Blob
Storage** para esto.

### 2.1 Crear el Resource Group

El resource group es un contenedor lógico donde vivirán los recursos del backend.
Lo creamos con:

```bash
az group create \
  --name cgp \
  --location eastus
```

| Parámetro | Valor | Explicación |
|---|---|---|
| `--name` | `cgp` | Nombre del grupo de recursos |
| `--location` | `eastus` | Región de Azure donde se crea (East US). El backend debe estar en una región distinta a la infraestructura para sobrevivir si algo falla en la región principal |

### 2.2 Crear el Storage Account

El storage account es el servicio de Azure que ofrece almacenamiento de objetos
(blobs), archivos, colas y tablas. Necesitamos uno para guardar el estado:

```bash
az storage account create \
  --name cgpaccount \
  --resource-group cgp \
  --sku Standard_LRS
```

| Parámetro | Explicación |
|---|---|
| `--name` | Nombre único global. Debe tener entre 3 y 24 caracteres, solo minúsculas y números |
| `--resource-group` | El grupo que creamos en el paso anterior |
| `--sku Standard_LRS` | Rendimiento estándar con replicación local (dentro del mismo datacenter). Suficiente para el estado de Terraform |

### 2.3 Crear el Blob Container

Dentro del storage account, los blobs se organizan en containers. Creamos uno
llamado `tfstate`:

```bash
az storage container create \
  --name tfstate \
  --account-name cgpaccount
```

| Parámetro | Explicación |
|---|---|
| `--name` | `tfstate` — nombre del container que almacenará el archivo `.tfstate` |
| `--account-name` | El storage account del paso anterior |

### 2.4 Asignar permisos al usuario actual

Para que el usuario que ejecuta Terraform pueda leer y escribir el estado, debe
tener el rol **Storage Blob Data Contributor** sobre el storage account:

```bash
az role assignment create \
  --assignee <USER_ID> \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<SUB_ID>/resourceGroups/cgp/providers/Microsoft.Storage/storageAccounts/cgpaccount
```

| Parámetro | Explicación |
|---|---|
| `--assignee` | Object ID del usuario en Azure AD. Se obtiene con `az ad user show --id <email>` o `az ad signed-in-user show --query id -o tsv` |
| `--role` | Rol que otorga acceso de lectura/escritura a los blobs |
| `--scope` | Ruta completa del recurso. Define que el permiso aplica solo a este storage account |

---

## 3. Colaboradores: permisos al backend

Si otra persona del equipo necesita ejecutar Terraform, debe tener el mismo
permiso sobre el storage account. El comando es idéntico al anterior, cambiando
el `--assignee` por el Object ID del colaborador:

```bash
az role assignment create \
  --assignee <USER_ID> \
  --role "Storage Blob Data Contributor" \
  --scope /subscriptions/<SUB_ID>/resourceGroups/cgp/providers/Microsoft.Storage/storageAccounts/cgpaccount
```

Además, hay que compartir con el colaborador las credenciales de la aplicación
(Service Principal, sección siguiente) para que pueda autenticarse ante Azure
desde Terraform.

---

## 4. Credenciales de aplicación: Service Principal

Terraform necesita autenticarse contra Azure para crear y gestionar recursos.
Aunque uses `az login`, Terraform requiere un **Service Principal** (una
identidad de aplicación) con permisos de Contributor sobre la suscripción.

### Crear el Service Principal

```bash
az ad sp create-for-rbac \
  --role="Contributor" \
  --scopes="/subscriptions/<SUBSCRIPTION_ID>"
```

| Parámetro | Explicación |
|---|---|
| `--role="Contributor"` | Permite crear, modificar y eliminar recursos en el alcance especificado (no puede asignar permisos a otros) |
| `--scopes` | La suscripción completa. El `SUBSCRIPTION_ID` se obtiene desde el portal de Azure o con `az account show --query id -o tsv` |

### Salida del comando

El comando devuelve un JSON con cuatro valores:

```json
{
  "appId": "66a587ad-b20a-4bc9-9c67-570f4def9e53",
  "displayName": "azure-cli-...",
  "password": "XXXXXXXXXXX",
  "tenant": "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
}
```

Estos valores se usan en el archivo `terraform.tfvars`:

| Variable de tfvars | Valor del JSON |
|---|---|
| `aks_app_id` | `appId` |
| `aks_password` | `password` (sensitive) |
| `subscription_id` | `SUBSCRIPTION_ID` (el que usaste en el comando, no el del JSON) |

> **Importante:** El `password` solo se muestra una vez al crear el SP. Si se
> pierde, hay que resetearlo con `az ad sp credential reset --id <appId>`.

---

## 5. Despliegue con Terraform

### 5.1 Configurar variables

Crea tu archivo `terraform.tfvars` basándote en el ejemplo:

```bash
cp terraform.tfvars.example terraform.tfvars
```

Luego edita `terraform.tfvars` y completa los valores: subscription_id, aks_app_id,
aks_password, ubicaciones, tamaños de VM, etc.

**Generar llave SSH para Jenkins:**

La VM de Jenkins se conecta por SSH usando llave pública. Genera un par nuevo:

```bash
ssh-keygen -t ed25519
```

Esto crea dos archivos en `~/.ssh/`:
- `id_ed25519` — llave **privada**, no se comparte (opcional: `id_ed25519-cgp`)
- `id_ed25519.pub` — llave **pública**, se copia a la VM durante el `apply`

En `terraform.tfvars`, configura `jenkins_public_key_path` apuntando al archivo
`.pub`.

### 5.2 Inicializar Terraform

```bash
terraform init
```

Este comando:
1. Descarga los **providers** necesarios (AzureRM, Random, Local)
2. Configura el **backend remoto** en Azure Blob Storage (valida que la conexión funcione)
3. Descarga los **módulos** referenciados en `main.tf`

Debe ejecutarse una sola vez por configuración. Si cambias los providers o el
backend, vuelve a ejecutarlo.

### 5.3 Desplegar la infraestructura

```bash
terraform apply
```

Terraform muestra un plan con todos los recursos que va a crear (17 recursos de
Azure). Revisa que sea correcto y escribe `yes` para confirmar.

El proceso puede tardar **5-10 minutos** (lo que más demora es el cluster AKS y
la VM de Jenkins). Al finalizar, Terraform muestra los outputs:

```
Outputs:
kubernetes_cluster_master_node_name = "master"
kubernetes_cluster_name = "cgp-cluster"
kubernetes_cluster_stage_node_name = "stage"
platform_jenkins_public_ip = "20.XX.XX.XX"
platform_rg_name = "plat-rg"
resource_group_name = "cgp-rc"
```

La IP pública de Jenkins (`platform_jenkins_public_ip`) la vas a necesitar en
los pasos siguientes.

---

## 6. Post-despliegue: Jenkins VM y SonarQube

La VM de Jenkins ya está creada con Ubuntu 22.04, OpenJDK 21, Jenkins,
Docker y Docker Compose instalados (vía cloud-init). Pero **SonarQube** hay que
desplegarlo manualmente como container.

### 6.1 Validar parámetros del sistema

SonarQube requiere ciertos valores mínimos del kernel. Conéctate por SSH a la VM
y verifica:

```bash
ssh ubuntu@<PUBLIC_IP>
```

```bash
sysctl vm.max_map_count        # debe ser >= 524288
sysctl fs.file-max             # debe ser >= 131072
ulimit -n                      # debe ser >= 131072
ulimit -u                      # debe ser >= 8192
```

Si algún valor es menor, el cloud-init ya debería haberlo configurado (revisa
`/etc/sysctl.d/99-sonarqube.conf`). Si no, ajústalo manualmente.

### 6.2 Copiar la carpeta de SonarQube

Desde tu máquina local, copia la configuración de SonarQube a la VM:

```bash
scp -r ./sonarqube ubuntu@<PUBLIC_IP>:~/
```

La carpeta `terraform/sonarqube/` contiene un `docker-compose.yml` con la
imagen de SonarQube Community.

### 6.3 Agregar usuario al grupo docker

Para ejecutar `docker` sin `sudo`:

```bash
sudo usermod -aG docker $USER
```

**Cierra sesión y vuelve a conectarte** para que el cambio de grupo tenga efecto:

```bash
exit
ssh ubuntu@<PUBLIC_IP>
```

### 6.4 Iniciar SonarQube

```bash
cd sonarqube
docker compose up -d
```

SonarQube arranca en el puerto **9000**. La primera inicialización puede tardar
un par de minutos. Verifica que esté respondiendo:

```bash
curl -s http://localhost:9000 | head -5
```

Deberías ver el HTML de la página de login de SonarQube.

---

## 7. Post-despliegue: Jenkins (configuración inicial)

### 7.1 Acceder a Jenkins

Abre en el navegador: `http://<PUBLIC_IP>:8080`

La contraseña inicial se obtiene desde la VM:

```bash
ssh ubuntu@<PUBLIC_IP> "sudo cat /var/lib/jenkins/secrets/initialAdminPassword"
```

### 7.2 Instalar plugins sugeridos

En la pantalla de bienvenida, elige **Install suggested plugins**. Jenkins
instalará los plugins más comunes (Git, Pipeline, Credentials, etc.).

### 7.3 Crear el primer usuario administrador

Completa el formulario con nombre de usuario, contraseña, nombre completo y
correo electrónico. Este será el administrador de Jenkins.

### 7.4 Instalar Copy Artifacts plugin

Ve a **Manage Jenkins → Plugins → Available plugins**, busca "Copy Artifact" y
selecciona la versión sugerida. Este plugin permite copiar artefactos entre
pipeline jobs.

---

## 8. Conectar kubectl al cluster AKS

### 8.1 Obtener credenciales del cluster

Desde tu máquina local (donde tengas la sesión de Azure CLI activa):

```bash
az aks get-credentials \
  --resource-group cgp-rc \
  --name cgp-cluster
```

Esto descarga el kubeconfig del cluster AKS y lo guarda en `~/.kube/config`.

| Parámetro | Explicación |
|---|---|
| `--resource-group` | El resource group donde está el AKS (`cgp-rc`) |
| `--name` | El nombre del cluster (`cgp-cluster`) |

### 8.2 Resolver conflictos de configuración

Si ya tenías un kubeconfig previo o ves errores de conflicto, borra el archivo
y vuelve a intentar:

```bash
rm -f ~/.kube/config
az aks get-credentials --resource-group cgp-rc --name cgp-cluster
```

### 8.3 Aplicar manifiestos de Kubernetes

Los manifiestos en `terraform/k8s/` crean los namespaces, ServiceAccounts, roles
y tokens necesarios para que Jenkins se conecte al cluster:

```bash
cd terraform/k8s
./script.sh
```

Este script aplica (en orden):
1. `namespace.yml` — crea los namespaces `jenkins-agents`, `circleguard-stage`,
   `circleguard-master`
2. `jenkins-sa.yaml` — crea un ServiceAccount llamado `jenkins` en
   `jenkins-agents`, un ClusterRole con permisos para gestionar pods, servicios,
   configmaps, etc., y RoleBindings en los 4 namespaces
3. `token-secret.yaml` — crea un Secret de tipo `kubernetes.io/service-account-token`
   que Jenkins usará para autenticarse

Verifica que todo esté correcto:

```bash
kubectl get ns
kubectl get sa -n jenkins-agents
kubectl get secret -n jenkins-agents
```

---

## 9. Configurar Kubernetes Cloud en Jenkins

Jenkins necesita saber cómo llegar al cluster AKS para lanzar agents dentro de
Kubernetes.

### 9.1 Instalar el plugin Kubernetes

Ve a **Manage Jenkins → Plugins → Available plugins**, busca "Kubernetes" (versión
`4423.vb_59f230b_ce53`) e instálalo. Este plugin permite a Jenkins lanzar pods
como agents de ejecución dentro del cluster.

### 9.2 Obtener la URL del cluster

```bash
kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}'
```

Esto devuelve algo como `https://20.XX.XX.XX:443`. Cópialo.

### 9.3 Obtener el certificate authority

```bash
kubectl config view --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' | base64 -d
```

Esto devuelve el certificado CA del cluster en formato PEM. Cópialo completo.

### 9.4 Obtener el token de servicio

El token se generó en el paso 8.3 como un Secret de Kubernetes. Para obtenerlo:

```bash
kubectl get secret -n jenkins-agents jenkins-secret -o jsonpath='{.data.token}' | base64 -d
```

### 9.5 Configurar el cloud en Jenkins

1. Ve a **Manage Jenkins → Clouds → New cloud**
2. **Name:** `aks`
3. **Type:** `Kubernetes`
4. Completa los cuatro campos:

| Campo | Valor |
|---|---|
| **Kubernetes URL** | La URL del paso 9.2 |
| **Kubernetes server certificate key** | El certificado del paso 9.3 (PEM completo) |
| **Credentials** | Agrega una credencial nueva de tipo "Secret text" con el token del paso 9.4, scope **Global** |
| **Jenkins URL** | `http://<PUBLIC_IP>:8080/` (la IP pública de la VM) |

5. Marca **WebSocket** para conexiones más estables desde agents dentro del cluster

---

## 10. Credenciales de Docker Hub en Jenkins

Jenkins necesita subir imágenes a Docker Hub durante el pipeline de CI/CD.

### 10.1 Generar token en Docker Hub

1. Ve a [hub.docker.com](https://hub.docker.com) → Account Settings → **Personal Access Tokens**
2. **Generate new token**
   - **Description:** `cgp`
   - **Expiration:** `90 days`
   - **Permissions:** `Read and write access`

### 10.2 Agregar credencial en Jenkins

1. Ve a **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**
2. **Kind:** `Username with password`
3. **Username:** Tu usuario de Docker Hub
4. **Password:** El token generado (no la contraseña de tu cuenta)
5. **ID:** `dockerhub-credentials`

---

## 11. Integrar SonarQube con Jenkins

### 11.1 Instalar los plugins de SonarQube

En **Manage Jenkins → Plugins → Available plugins**, instala:

| Plugin | Versión |
|---|---|
| Sonar Quality Gates | `364.v67a_f255f340f` |
| SonarQube Scanner | `2.18.2` |

### 11.2 Deshabilitar validación de webhooks locales

Por defecto, Jenkins bloquea los webhooks que apuntan a localhost (como el que
SonarQube necesita enviar a la VM). Para deshabilitarlo:

1. Ve a **Manage Jenkins → Security**
2. Busca **Forcing local webhooks validation**
3. **Desmarca** la opción (esto permite que SonarQube en la misma VM envíe
   webhooks a Jenkins sin ser bloqueado)

> **Advertencia:** Solo deshabilita esto en entornos controlados. En producción,
> considera usar HTTPS y un dominio real.

### 11.3 Crear el proyecto en SonarQube

1. Abre `http://<PUBLIC_IP>:9000` e inicia sesión (admin/admin por defecto)
2. Crea un nuevo proyecto llamado `cgp`

### 11.4 Crear un webhook a Jenkins

En el proyecto `cgp` de SonarQube:

1. Ve a **Project Settings → Webhooks**
2. **Create**
   - **Name:** `jenkins-webhook`
   - **URL:** `http://<PUBLIC_IP>:8080/sonarqube-webhook/`

### 11.5 Generar token de SonarQube

1. Ve a **Account → Security**
2. **Generate Token**
   - **Name:** `cgp`
   - **Expiration:** `90 days`
3. Copia el token generado

### 11.6 Configurar SonarQube Server en Jenkins

1. Ve a **Manage Jenkins → System**
2. Busca **SonarQube servers**
3. **Add SonarQube**
   - **Name:** `sonarqube`
   - **Server URL:** `http://<PUBLIC_IP>:9000`
   - **Server authentication token:** Agrega una credencial de tipo "Secret text"
     con el token del paso 11.5

---

## 12. Comandos importantes

### 12.1 Detener el cluster AKS

Detener el cluster libera los costos de cómputo (los nodos se apagan), pero
mantiene la configuración:

```bash
az aks stop \
  -n cgp-cluster \
  -g cgp-rc
```

### 12.2 Iniciar el cluster AKS

Reanuda un cluster detenido:

```bash
az aks start \
  -n cgp-cluster \
  -g cgp-rc
```

### 12.3 Destruir toda la infraestructura

Elimina **todos** los recursos creados por Terraform (AKS, VM, redes, etc.):

```bash
terraform destroy
```

Terraform mostrará un plan de destrucción. Revisa y confirma con `yes`.

### 12.4 Reconstruir solo el cluster AKS

A veces quieres reiniciar el cluster sin tocar la VM de Jenkins ni la red. La
secuencia completa es:

```bash
# 1. Destruir solo el módulo AKS
terraform destroy -target="module.aks-cluster"

# 2. Planear y aplicar solo el módulo AKS
terraform plan -target=module.aks-cluster -out aks
terraform apply "aks"

# 3. Limpiar kubeconfig local (la IP del cluster pudo cambiar)
rm -f ~/.kube/config

# 4. Obtener nuevo kubeconfig
az aks get-credentials \
  --resource-group cgp-rc \
  --name cgp-cluster

# 5. Re-aplicar manifiestos K8s
cd k8s
./script.sh

# 6. Actualizar los datos de Kubernetes Cloud en Jenkins
#    - URL del cluster (kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}')
#    - Certificate key (kubectl config view --raw -o jsonpath='{.clusters[0].cluster.certificate-authority-data}' | base64 -d)
#    - Token (kubectl get secret -n jenkins-agents jenkins-secret -o jsonpath='{.data.token}' | base64 -d)
```
