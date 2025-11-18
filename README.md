# Spring Boot Message Feed with Zero Trust Architecture

A Spring Boot application implementing a secure message feed following Zero Trust security principles, using Auth0 for authentication and JWT-based authorization.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Zero Trust Principles](#zero-trust-principles)
4. [Setup Instructions](#setup-instructions)
5. [Project Structure](#project-structure)
6. [API Endpoints](#api-endpoints)
7. [Security Implementation](#security-implementation)
8. [Running the Application](#running-the-application)

---

## Overview

This application demonstrates a minimal Spring Boot message feed system that implements Zero Trust architecture principles. The system includes:

- **Web Client**: HTML + JavaScript + CSS for user interface
- **Backend API**: Spring Boot REST API for message management
- **Authentication**: Auth0 OAuth2/OIDC integration
- **Authorization**: JWT-based token validation on every request
- **Data Storage**: In-memory HashMap for message persistence

### Features

- User authentication via Auth0
- Post messages through a web form
- View the last 10 messages in a feed
- Automatic message refresh every 5 seconds
- Client IP tracking and timestamp recording
- Secure API endpoints with JWT validation

---

## Architecture

### System Components

```
┌─────────────────┐
│   Web Browser   │
│  (HTML/JS/CSS)  │
└────────┬────────┘
         │
         │ HTTP Requests
         │ (with JWT tokens)
         │
┌────────▼─────────────────────────────────┐
│      Spring Boot Application             │
│  ┌─────────────────────────────────────┐ │
│  │  Security Layer (Zero Trust)         │ │
│  │  - JWT Validation                    │ │
│  │  - Request Authentication            │ │
│  └─────────────────────────────────────┘ │
│  ┌─────────────────────────────────────┐ │
│  │  Controllers (MVC Pattern)          │ │
│  │  - HomeController (View)            │ │
│  │  - MessageController (REST API)     │ │
│  └─────────────────────────────────────┘ │
│  ┌─────────────────────────────────────┐ │
│  │  Service Layer                      │ │
│  │  - MessageService                   │ │
│  └─────────────────────────────────────┘ │
│  ┌─────────────────────────────────────┐ │
│  │  Data Layer                         │ │
│  │  - In-memory HashMap<Message>       │ │
│  └─────────────────────────────────────┘ │
└───────────────────────────────────────────┘
         │
         │ OAuth2/OIDC
         │
┌────────▼────────┐
│     Auth0       │
│  (Identity      │
│   Provider)     │
└─────────────────┘
```

### Data Flow

#### 1. Authentication Flow

```
User → Login Button → Auth0 → OAuth2 Authorization Code →
Spring Boot → Exchange Code for JWT → Store Session →
Redirect to Home Page
```

#### 2. Post Message Flow

```
User Input → JavaScript Fetch API → POST /api/messages →
Security Filter (JWT Validation) → MessageController →
MessageService → Store in HashMap → Return Response →
Update UI
```

#### 3. Get Messages Flow

```
Page Load → JavaScript Fetch API → GET /api/messages →
Security Filter (JWT Validation) → MessageController →
MessageService → Retrieve Last 10 Messages →
Return JSON → Display in Feed
```

### Endpoints

| Endpoint                     | Method | Description                 | Authentication Required         |
| ---------------------------- | ------ | --------------------------- | ------------------------------- |
| `/`                          | GET    | Home page with message feed | No (but API calls require auth) |
| `/api/messages`              | POST   | Create a new message        | Yes (JWT)                       |
| `/api/messages`              | GET    | Get last 10 messages        | Yes (JWT)                       |
| `/oauth2/authorization/okta` | GET    | Initiate Auth0 login        | No                              |
| `/logout`                    | POST   | Logout and clear session    | No                              |

### Data Model

**Message Object:**

```json
{
  "message": "User's message text",
  "clientIp": "192.168.1.1",
  "timestamp": "2025-11-18T01:00:00"
}
```

---

## Zero Trust Principles

This application implements Zero Trust architecture through the following principles:

### 1. Never Trust, Always Verify

**Implementation:**

- Every API request (`/api/**`) requires authentication
- JWT tokens are validated on every request, not just once
- No implicit trust based on network location or previous authentication
- Server-side validation of all tokens using Spring Security OAuth2 Resource Server

**Code Example:**

```java
// SecurityConfig.java
.requestMatchers("/api/**").authenticated()
.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
```

### 2. Authentication and Authorization Per Request

**Implementation:**

- Each API call includes JWT token validation
- No session-based trust - tokens are verified independently
- Authorization checks happen at the controller level
- `@AuthenticationPrincipal` ensures user identity is verified for each request

**Code Example:**

```java
// MessageController.java
@PostMapping
public ResponseEntity<Message> postMessage(
    @RequestBody Map<String, String> request,
    @AuthenticationPrincipal OidcUser principal) {
    // principal is validated on every request
}
```

### 3. Minimal Surface Exposure

**Implementation:**

- Only necessary endpoints are exposed
- Public endpoints (`/`, `/oauth2/**`) are separated from protected API endpoints
- API endpoints (`/api/**`) are isolated and require authentication
- No unnecessary information leakage in error messages

**Security Configuration:**

```java
.requestMatchers("/", "/images/**", "/login/**", "/oauth2/**").permitAll()
.requestMatchers("/api/**").authenticated()
```

### 4. Separation of Public Client and Protected API

**Implementation:**

- **Public Client**: HTML/JS/CSS served from `/` (accessible without auth)
- **Protected API**: REST endpoints under `/api/**` (require JWT)
- Clear separation between presentation layer and business logic
- Client-side JavaScript makes authenticated requests to protected endpoints

**Architecture Pattern:**

```
Public Web Page (/)
    ↓
JavaScript Fetch API
    ↓
Protected REST API (/api/messages)
    ↓
JWT Validation
    ↓
Business Logic
```

### 5. Strict Token Validation

**Implementation:**

- JWT tokens are validated against Auth0 issuer
- Token signature verification
- Token expiration checking
- Issuer URI validation in `application.yml`

**Configuration:**

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://dev-dy8cqv5kgmrx6jhn.us.auth0.com/
```

---

## Security Implementation

### Authentication Flow

1. **User Login**: Redirects to Auth0 OAuth2 login page
2. **Authorization Code**: Auth0 returns authorization code
3. **Token Exchange**: Spring Boot exchanges code for ID token and access token
4. **Session Creation**: Tokens stored in server-side session
5. **JWT Validation**: Subsequent API requests validate JWT tokens

### Authorization Flow

1. **Client Request**: JavaScript sends request with session cookie
2. **Security Filter**: Spring Security intercepts request
3. **Token Extraction**: JWT extracted from session
4. **Token Validation**: Validated against Auth0 issuer
5. **Request Processing**: If valid, request proceeds to controller
6. **Response**: JSON response returned to client

### Security Layers

```
Request → SecurityFilterChain → JWT Validation →
Controller → Service → Response
```

---

## Setup Instructions

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Auth0 account

### Step 1: Configure Auth0

1. Create an Auth0 account at [auth0.com](https://auth0.com)
2. Create a new Application (Regular Web Application)
3. Configure URLs:
   - **Allowed Callback URLs**: `http://localhost:3000/login/oauth2/code/okta`
   - **Allowed Logout URLs**: `http://localhost:3000`
   - **Allowed Web Origins**: `http://localhost:3000`

### Step 2: Update Configuration

Edit `src/main/resources/application.yml`:

```yaml
okta:
  oauth2:
    issuer: https://YOUR-DOMAIN.auth0.com/
    client-id: YOUR-CLIENT-ID
    client-secret: YOUR-CLIENT-SECRET

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://YOUR-DOMAIN.auth0.com/

server:
  port: 3000
```

### Step 3: Build and Run

```bash
mvn clean package
mvn spring-boot:run
```

### Step 4: Access Application

Open browser to: `http://localhost:3000`

---

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── co/edu/escuelaing/securetutorialauth0/
│   │       ├── Securetutorialauth0.java      # Main application
│   │       ├── SecurityConfig.java           # Security configuration
│   │       ├── HomeController.java           # View controller
│   │       ├── MessageController.java        # REST API controller
│   │       ├── MessageService.java           # Business logic
│   │       └── Message.java                  # Data model
│   └── resources/
│       ├── application.yml                   # Configuration
│       └── templates/
│           └── index.html                    # Web interface
└── pom.xml                                   # Maven dependencies
```

### MVC Pattern Implementation

- **Model**: `Message.java` - Data structure
- **View**: `index.html` - HTML/CSS/JavaScript interface
- **Controller**:
  - `HomeController.java` - Serves the view
  - `MessageController.java` - Handles REST API requests

---

## API Endpoints

### POST /api/messages

Create a new message.

**Request:**

```json
{
  "message": "Hello, World!"
}
```

**Response:**

```json
{
  "message": "Hello, World!",
  "clientIp": "127.0.0.1",
  "timestamp": "2025-11-18T01:00:00"
}
```

**Authentication:** Required (JWT token)

### GET /api/messages

Get the last 10 messages.

**Response:**

```json
[
  {
    "message": "Hello, World!",
    "clientIp": "127.0.0.1",
    "timestamp": "2025-11-18T01:00:00"
  },
  ...
]
```

**Authentication:** Required (JWT token)

---

## Reflection on Software and Security Principles

### Alignment with Class Principles

1. **Defense in Depth**: Multiple security layers (OAuth2, JWT validation, endpoint protection)
2. **Least Privilege**: Only authenticated users can post/view messages
3. **Fail Secure**: Unauthenticated requests are rejected, not allowed
4. **Separation of Concerns**: Clear separation between client, API, and authentication
5. **Single Responsibility**: Each component has a specific role (Controller, Service, Model)

### Zero Trust Benefits

- **Reduced Attack Surface**: Only authenticated requests reach business logic
- **No Implicit Trust**: Every request is verified independently
- **Audit Trail**: Client IP and timestamp tracking for all messages
- **Scalability**: Stateless JWT validation allows horizontal scaling
- **Compliance**: Meets modern security standards for API protection

---

## Troubleshooting

### Port Already in Use

Change port in `application.yml`:

```yaml
server:
  port: 8080
```

### Invalid Redirect URI

Ensure Auth0 callback URL matches exactly:

```
http://localhost:3000/login/oauth2/code/okta
```

### 401 Unauthorized

- Clear browser cookies
- Re-login through Auth0
- Verify JWT token is being sent with requests

---

## References

- [Auth0 Spring Boot Authorization Tutorial](https://auth0.com/blog/spring-boot-authorization-tutorial-secure-an-api-java/)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [Zero Trust Architecture Principles](https://www.nist.gov/publications/zero-trust-architecture)
