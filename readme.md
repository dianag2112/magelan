# 🍽️ Magelan — Restaurant Management Platform

Magelan is a full-featured restaurant web application built with **Spring Boot 3**, following a clean **three-tier architecture**.  
It manages **menu items, orders, table bookings, user accounts, and payments**, integrating with an external microservice and a 3rd-party joke API.

---

## 🚀 Features

### 🧑‍🍳 Customer Features
- View restaurant menu
- Add products to an order (cart)
- Start and complete payment
- Book a table
- View order history
- Download **PDF receipt** for completed orders
- View a random joke using a 3rd-party REST API

### 🛠️ Admin Features
- Manage menu (products & categories)
- Manage all orders (submit → confirm → complete)
- Manage all table bookings
- Access Admin Panel dashboard

### 🔐 Authentication & Authorization
- Spring Security login system
- Users have roles: `ROLE_USER` or `ROLE_ADMIN`
- Admins cannot access user-only endpoints (e.g., `/book-table`)
- Global exception handling with AOP logging

---

## 🧩 Architecture (Three-Tier)

Magelan follows a **classical layered architecture**:

### **1️⃣ Presentation Layer**
- Thymeleaf HTML templates
- Controllers (`/web/...`)
- Authentication views
- User/admin pages

### **2️⃣ Service Layer**
Business logic lives here:
- OrderService
- BookingService
- UserService
- ReceiptService (PDF generation)
- JokeService (external API integration)

Also includes:
- **Spring Events** (e.g., order submitted)
- **AOP cross-cutting concerns** (performance logging)

### **3️⃣ Data Access Layer**
- JPA entities
- Repositories for each aggregate
- MySQL

---

## ☁️ Integrations

### 🔌 **Payment Microservice** (`payment-svc`)
Communication via **Feign Client**:

- Create payment
- Process payment
- Fetch payment details (used for PDF receipts)

### 🤣 **3rd Party REST API Integration**
Random jokes using  
`https://official-joke-api.appspot.com/random_joke`

### 🧾 **PDF Receipt Generator**
Order receipts generated with **OpenPDF**.

---

## 🛠️ Technologies Used

- **Java 17**
- **Spring Boot 3**
- **Spring MVC**
- **Spring Security**
- **Spring Data JPA**
- **Spring Events**
- **AOP (Aspect-Oriented Programming)**
- **OpenFeign**
- **OpenPDF (PDF export)**
- **MySQL**
- **Thymeleaf**
- **Lombok**
- **JUnit 5 + Mockito**

---

## 🧪 Testing Strategy

- **Unit tests**
    - Services (OrderService, BookingService, etc.)
- **Integration tests**
    - Repository tests using H2 (e.g., BookingRepositoryTest)
- **API tests**
    - Controllers tested via MockMvc

---

