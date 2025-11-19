# Spring Boot Message Feed con Arquitectura Zero Trust

Una aplicación Spring Boot que implementa un feed de mensajes seguro siguiendo los principios de seguridad Zero Trust, usando Auth0 para autenticación y autorización basada en JWT.

## Resumen

Esta aplicación demuestra un sistema de feed de mensajes mínimo en Spring Boot que implementa los principios de arquitectura Zero Trust. El sistema incluye:

- **Cliente Web**: HTML + JavaScript + CSS para la interfaz de usuario
- **API Backend**: API REST de Spring Boot para gestión de mensajes
- **Autenticación**: Integración con Auth0 OAuth2/OIDC
- **Autorización**: Validación de tokens JWT en cada solicitud
- **Almacenamiento de Datos**: HashMap en memoria para persistencia de mensajes

### Demo

https://github.com/user-attachments/assets/a12b7079-364b-402e-af83-b612ff94c4e8

### Características

- Autenticación de usuarios mediante Auth0
- Publicar mensajes a través de un formulario web
- Ver los últimos 10 mensajes en un feed
- Actualización automática de mensajes cada 5 segundos
- Seguimiento de IP del cliente y registro de timestamps
- Endpoints de API seguros con validación JWT

---

### Flujo de Datos

#### 1. Flujo de Autenticación

```
Usuario → Botón Login → Auth0 → Código de Autorización OAuth2 →
Spring Boot → Intercambiar Código por JWT → Almacenar Sesión →
Redirigir a Página Principal
```

#### 2. Flujo de Publicar Mensaje

```
Entrada del Usuario → JavaScript Fetch API → POST /api/messages →
Filtro de Seguridad (Validación JWT) → MessageController →
MessageService → Almacenar en HashMap → Retornar Respuesta →
Actualizar UI
```

#### 3. Flujo de Obtener Mensajes

```
Carga de Página → JavaScript Fetch API → GET /api/messages →
Filtro de Seguridad (Validación JWT) → MessageController →
MessageService → Recuperar Últimos 10 Mensajes →
Retornar JSON → Mostrar en Feed
```

### Endpoints

| Endpoint                     | Método | Descripción                    | Autenticación Requerida               |
| ---------------------------- | ------ | ------------------------------ | ------------------------------------- |
| `/`                          | GET    | Página principal con feed      | No (pero llamadas API requieren auth) |
| `/api/messages`              | POST   | Crear un nuevo mensaje         | Sí (JWT)                              |
| `/api/messages`              | GET    | Obtener últimos 10 mensajes    | Sí (JWT)                              |
| `/oauth2/authorization/okta` | GET    | Iniciar login con Auth0        | No                                    |
| `/logout`                    | POST   | Cerrar sesión y limpiar sesión | No                                    |

### Modelo de Datos

**Objeto Message:**

```json
{
  "message": "Texto del mensaje del usuario",
  "clientIp": "192.168.1.1",
  "timestamp": "2025-11-18T01:00:00"
}
```

---

## Principios Zero Trust

Esta aplicación implementa la arquitectura Zero Trust a través de los siguientes principios:

### 1. Nunca Confiar, Siempre Verificar

**Implementación:**

- Cada solicitud a la API (`/api/**`) requiere autenticación
- Los tokens JWT se validan en cada solicitud, no solo una vez
- No hay confianza implícita basada en ubicación de red o autenticación previa
- Validación del lado del servidor de todos los tokens usando Spring Security OAuth2

**Ejemplo de Código:**

```java
// SecurityConfig.java
.requestMatchers("/api/**").authenticated()
.oauth2Login(withDefaults())
```

### 2. Autenticación y Autorización por Solicitud

**Implementación:**

- Cada llamada a la API incluye validación de token JWT
- No hay confianza basada en sesión - los tokens se verifican independientemente
- Las verificaciones de autorización ocurren a nivel del controlador
- `@AuthenticationPrincipal` asegura que la identidad del usuario se verifica en cada solicitud

**Ejemplo de Código:**

```java
// MessageController.java
@PostMapping
public ResponseEntity<Message> postMessage(
    @RequestBody Map<String, String> request,
    @AuthenticationPrincipal OidcUser principal) {
    // principal se valida en cada solicitud
}
```

### 3. Exposición Mínima de Superficie

**Implementación:**

- Solo se exponen los endpoints necesarios
- Los endpoints públicos (`/`, `/oauth2/**`) están separados de los endpoints protegidos de la API
- Los endpoints de la API (`/api/**`) están aislados y requieren autenticación
- No hay fuga de información innecesaria en mensajes de error

**Configuración de Seguridad:**

```java
.requestMatchers("/", "/images/**", "/login/**", "/oauth2/**").permitAll()
.requestMatchers("/api/**").authenticated()
```

### 4. Separación de Cliente Público y API Protegida

**Implementación:**

- **Cliente Público**: HTML/JS/CSS servidos desde `/` (accesible sin autenticación)
- **API Protegida**: Endpoints REST bajo `/api/**` (requieren JWT)
- Separación clara entre capa de presentación y lógica de negocio
- JavaScript del lado del cliente hace solicitudes autenticadas a endpoints protegidos

**Patrón de Arquitectura:**

```
Página Web Pública (/)
    ↓
JavaScript Fetch API
    ↓
API REST Protegida (/api/messages)
    ↓
Validación JWT
    ↓
Lógica de Negocio
```

### 5. Validación Estricta de Tokens

**Implementación:**

- Los tokens JWT se validan contra el emisor de Auth0
- Verificación de firma del token
- Verificación de expiración del token
- Validación de URI del emisor en `application.yml`

**Configuración:**

```yaml
okta:
  oauth2:
    issuer: https://dev-dy8cqv5kgmrx6jhn.us.auth0.com/
```

---

## Implementación de Seguridad

### Flujo de Autenticación

1. **Login del Usuario**: Redirige a la página de login OAuth2 de Auth0
2. **Código de Autorización**: Auth0 retorna código de autorización
3. **Intercambio de Token**: Spring Boot intercambia código por token ID y token de acceso
4. **Creación de Sesión**: Tokens almacenados en sesión del lado del servidor
5. **Validación JWT**: Las solicitudes subsecuentes a la API validan tokens JWT

### Flujo de Autorización

1. **Solicitud del Cliente**: JavaScript envía solicitud con cookie de sesión
2. **Filtro de Seguridad**: Spring Security intercepta la solicitud
3. **Extracción de Token**: JWT extraído de la sesión
4. **Validación de Token**: Validado contra el emisor de Auth0
5. **Procesamiento de Solicitud**: Si es válido, la solicitud procede al controlador
6. **Respuesta**: Respuesta JSON retornada al cliente

### Capas de Seguridad

```
Solicitud → SecurityFilterChain → Validación JWT →
Controlador → Servicio → Respuesta
```

---

## Instrucciones de Configuración

### Prerrequisitos

- Java 17 o superior
- Maven 3.6+
- Cuenta de Auth0

### Paso 1: Configurar Auth0

1. Crea una cuenta en Auth0 en [auth0.com](https://auth0.com)
2. Crea una nueva Aplicación (Regular Web Application)
3. Configura las URLs:
   - **Allowed Callback URLs**: `http://localhost:3000/login/oauth2/code/okta`
   - **Allowed Logout URLs**: `http://localhost:3000`
   - **Allowed Web Origins**: `http://localhost:3000`

### Paso 2: Actualizar Configuración

Edita `src/main/resources/application.yml`:

```yaml
okta:
  oauth2:
    issuer: https://TU-DOMINIO.auth0.com/
    client-id: TU-CLIENT-ID
    client-secret: TU-CLIENT-SECRET

server:
  port: 3000
```

### Paso 3: Compilar y Ejecutar

```bash
mvn clean package
mvn spring-boot:run
```

### Paso 4: Acceder a la Aplicación

Abre el navegador en: `http://localhost:3000`

---

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/
│   │   └── co/edu/escuelaing/securetutorialauth0/
│   │       ├── Securetutorialauth0.java      # Aplicación principal
│   │       ├── SecurityConfig.java           # Configuración de seguridad
│   │       ├── HomeController.java           # Controlador de vista
│   │       ├── MessageController.java        # Controlador de API REST
│   │       ├── MessageService.java           # Lógica de negocio
│   │       └── Message.java                  # Modelo de datos
│   └── resources/
│       ├── application.yml                   # Configuración
│       └── templates/
│           └── index.html                    # Interfaz web
└── pom.xml                                   # Dependencias de Maven
```

### Implementación del Patrón MVC

- **Modelo**: `Message.java` - Estructura de datos
- **Vista**: `index.html` - Interfaz HTML/CSS/JavaScript
- **Controlador**:
  - `HomeController.java` - Sirve la vista
  - `MessageController.java` - Maneja solicitudes de API REST

---

## Endpoints de la API

### POST /api/messages

Crear un nuevo mensaje.

**Solicitud:**

```json
{
  "message": "¡Hola, Mundo!"
}
```

**Respuesta:**

```json
{
  "message": "¡Hola, Mundo!",
  "clientIp": "127.0.0.1",
  "timestamp": "2025-11-18T01:00:00"
}
```

**Autenticación:** Requerida (token JWT)

### GET /api/messages

Obtener los últimos 10 mensajes.

**Respuesta:**

```json
[
  {
    "message": "¡Hola, Mundo!",
    "clientIp": "127.0.0.1",
    "timestamp": "2025-11-18T01:00:00"
  },
  ...
]
```

**Autenticación:** Requerida (token JWT)

---

## Reflexión sobre Principios de Software y Seguridad

### Alineación con Principios de Clase

1. **Defensa en Profundidad**: Múltiples capas de seguridad (OAuth2, validación JWT, protección de endpoints)
2. **Privilegio Mínimo**: Solo usuarios autenticados pueden publicar/ver mensajes
3. **Fallar de Forma Segura**: Las solicitudes no autenticadas se rechazan, no se permiten
4. **Separación de Responsabilidades**: Separación clara entre cliente, API y autenticación
5. **Responsabilidad Única**: Cada componente tiene un rol específico (Controlador, Servicio, Modelo)

### Beneficios de Zero Trust

- **Superficie de Ataque Reducida**: Solo las solicitudes autenticadas llegan a la lógica de negocio
- **Sin Confianza Implícita**: Cada solicitud se verifica independientemente
- **Rastro de Auditoría**: Seguimiento de IP del cliente y timestamp para todos los mensajes
- **Escalabilidad**: La validación JWT sin estado permite escalado horizontal
- **Cumplimiento**: Cumple con estándares modernos de seguridad para protección de API

---








