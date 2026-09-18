# AirPowerServer Project Documentation

This document provides a comprehensive overview of the AirPowerServer project, including its architecture, dependencies, and key components.

## 1. Project Overview

AirPowerServer is a Spring Boot application written in Kotlin that serves as a backend for the AirPower ecosystem. It acts as an intermediary between a client application and a ThingsBoard server, providing an authentication layer and additional business logic.

## 2. Architecture

The project follows a standard layered architecture:

- **`config`**: Contains configuration classes for security, database connections, and other application settings.
- **`controller`**: Exposes RESTful API endpoints for interacting with the application.
- **`dto`**: Defines Data Transfer Objects used for communication between the client and the server.
- **`entity`**: Defines the data models (JPA entities) for the application.
- **`repository`**: Contains Spring Data JPA repositories for accessing the database.
- **`service`**: Implements the business logic of the application.

## 3. Dependencies

The project uses the following key dependencies:

- **Spring Boot**: For building the application.
- **Spring Data JPA**: For database access.
- **Spring Security**: For authentication and authorization.
- **Kotlin**: The primary programming language.
- **PostgreSQL**: The database used by the application.
- **JWT (Java JWT)**: For creating and verifying JSON Web Tokens.
- **Ktor**: For making HTTP requests to the ThingsBoard server.
- **Jackson**: For JSON serialization and deserialization.
- **Lombok**: To reduce boilerplate code.
- **Coroutines**: For asynchronous programming.

## 4. Configuration

The application's configuration is defined in `src/main/resources/application.properties`. Key configuration properties include:

- **HTTPS Configuration**: The server is configured to use HTTPS on port 8443.
- **JWT Properties**: The secret and expiration time for JWTs are defined here.
- **Database Configuration**: The application connects to two PostgreSQL databases: `airpower` and `thingsboard`.
- **ThingsBoard API URL**: The URL of the ThingsBoard server.

## 5. Key Components

### 5.1. Authentication

Authentication is handled by the `AuthController`. The login process involves the following steps:

1. The client sends a login request with a username and password to the `/api/v1/auth/login` endpoint.
2. The `AuthController` authenticates the user with the ThingsBoard server.
3. If the authentication is successful, the server creates or updates a corresponding user in the `airpower` database.
4. The server generates a JWT and a refresh token and returns them to the client.

### 5.2. Entities

The project defines the following JPA entities:

- **`AirPowerUser`**: Represents a user of the application.
- **`Role`**: Represents a user role.
- **`PersistToken`**: Stores JWTs and refresh tokens in the database.

### 5.3. Repositories

The project uses Spring Data JPA repositories to interact with the database:

- **`AirPowerUserRepository`**: For accessing `AirPowerUser` entities.
- **`RoleRepository`**: For accessing `Role` entities.
- **`TokenRepository`**: For accessing `PersistToken` entities.

### 5.4. Services

The project's business logic is implemented in the following services:

- **`AirPowerUserDetailServiceImpl`**: Implements the `UserDetailsService` interface for Spring Security.
- **`AirPowerTokenService`**: For generating and validating JWTs.
- **`ThingsBoardAuthService`**: For authenticating with the ThingsBoard server.
- **`UserDeviceService`**: For retrieving device information for a user.
- **`ThingsBoardAlarmService`**: For retrieving alarm information from the ThingsBoard server.

### 5.5. Controllers

The project exposes the following RESTful API endpoints:

- **`AuthController`**: Handles authentication and token management.
- **`DevicesController`**: Provides endpoints for retrieving device information.
- **`AlarmsController`**: Provides endpoints for retrieving alarm information.

## 6. Ecosystem Integration

The AirPowerServer is part of a larger ecosystem that includes:

- **A client application**: This application interacts with the AirPowerServer to access data and functionality.
- **A ThingsBoard server**: This server is used for device management, data collection, and visualization.

The AirPowerServer acts as a bridge between the client application and the ThingsBoard server, providing a unified API and an additional layer of security.
