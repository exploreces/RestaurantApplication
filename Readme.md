# 🍽️ Restaurant Management Application

A full-stack backend application built using **Spring Boot**, **DynamoDB**, **Docker**, and deployed on **Kubernetes**.

This platform enables users to:

- View available dishes 🍲
- Book tables 🪑
- Place orders 🧾
- Submit feedback 📝
- Waiters can also place orders for users for convenience.

> 🧑‍🤝‍🧑 This is a **group project** developed collaboratively to streamline restaurant operations.

---

## 🛠️ Tech Stack

- **Java 17** - Programming Language
- **Spring Boot** - Backend framework
- **DynamoDB** - NoSQL AWS-hosted database
- **Spring Security** - Authentication and Authorization
- **Swagger** - API documentation
- **Docker** - Containerization
- **Kubernetes** - Deployment orchestration

---

## ✅ Features

- Secure user and waiter login with Spring Security
- Table booking system
- Dish browsing
- Order placement and tracking
- Feedback submission
- Waiter-assisted order creation
- Swagger UI for interactive API documentation
- AWS DynamoDB for scalable and flexible data storage

---

## 📄 Environment Setup

Create a `.env` file in the root of your project and add the following AWS credentials required for DynamoDB access:

```env
# AWS Credentials (Required)
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
AWS_SESSION_TOKEN=your_aws_session_token
```

---

## 📦 Prerequisites

Before running the project, make sure the following tools are installed:

- Java 17+
- Maven
- Docker
- Kubernetes (Minikube, Docker Desktop, or Cloud Provider)
- AWS account with DynamoDB
- IntelliJ IDEA / Eclipse / VSCode

---

## ▶️ How to Run

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/restaurant-app.git
cd restaurant-app

```

### 2. Build the Project

```bash
mvn clean install
./mvnw spring-boot:run

```

---

## 📜 Accessing Swagger UI

Once the application is running (locally):

👉 Open: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

Use Swagger to explore and test all available APIs.

---

## 🙏 Acknowledgment

Thank you to my amazing team for their contributions and collaboration on this project. 🙌  
