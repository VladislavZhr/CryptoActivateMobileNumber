# 💸 CryptoPayment & Virtual Number Management Service

## Overview

This project is a **commercial-grade, secure, and user-friendly service** designed to handle cryptocurrency payments and manage virtual number activations. It provides a complete operational cycle for users and administrators, supporting seamless financial transactions and real-time monitoring.

> 🚀 Built for reliability, automation, and simplicity in handling **crypto-payments** and **virtual number services**.

---

## 🔑 Key Features

- **Multi-Gateway Crypto Payments**  
  Integration with popular crypto gateways (e.g., CryptoCloud) to support BTC, ETH, USDT, and more.

- **Webhook Handling**  
  Automatic processing of `POST`-requests from payment providers with real-time status updates.

- **Secure Authentication**  
  JWT-based multi-level authentication and user role management.

- **Transaction Dashboard**  
  Real-time display of transactions with filters, sorting, and search capabilities.

- **Payment Notifications**  
  Email or in-app notifications about payment status.

- **Robust Security**  
  - Signature verification on incoming requests  
  - Strict JWT validation  
  - Input validation to prevent forgery and injection attacks  
  - Secure handling of secret keys and sensitive configuration

---

## ⚙️ Technology Stack

| Layer       | Technology                               |
|-------------|------------------------------------------|
| Backend     | Java 17, Spring Boot, Hibernate          |
| Database    | PostgreSQL                               |
| Frontend    | React                                    |
| Integrations| CryptoCloud (or similar crypto gateway)  |
| Deployment  | Heroku with HTTPS support                |

---

## 🧩 Architecture

The system follows a modular architecture with clear separation of responsibilities:

- **Controller Layer:** Handles HTTP requests, authorization, validation.
- **Service Layer:** Business logic for payments, transactions, user flows.
- **Persistence Layer:** Data access through Hibernate and PostgreSQL.
- **Integration Layer:** Communication with crypto-gateways and webhook handling.
- **Security Module:** JWT filters, signature verification, input sanitation.

---

## 🛠️ Deployment

### Requirements

- Java 17+
- PostgreSQL database
- Heroku account with SSL/HTTPS enabled

