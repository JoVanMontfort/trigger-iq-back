# 🏗️ High-Level Stack Overview — TriggerIQ Backend

## 🚀 Overview
TriggerIQ's backend is built for scalability, modularity, and serverless compatibility, leveraging AWS managed services where possible. The system is container-first and event-driven, with a focus on microservice isolation, CI/CD, and observability.

---

## 📦 Core Components

| Layer              | Technology Stack                                                                 |
|--------------------|----------------------------------------------------------------------------------|
| **Container Orchestration** | Amazon EKS (Elastic Kubernetes Service)                                     |
| **Serverless Compute** | AWS Lambda (event-driven tasks, low-frequency compute)                      |
| **Networking**      | Amazon VPC, AWS PrivateLink, Security Groups                                    |
| **Container Runtime** | AWS Fargate (for running EKS pods without managing EC2 nodes)                  |
| **Service Mesh (Optional)** | AWS App Mesh or Istio (for traffic control, retries, observability)         |
| **API Gateway**     | AWS API Gateway (public APIs), Internal ALB Ingress Controller (for services)  |
| **Authentication**  | AWS Cognito / IAM + OIDC with Spring Boot Security                             |

---

## 🧱 Application Architecture

### 🔄 Core Microservices
- Built using **Spring Boot 3.4.x**
- RESTful APIs and background workers
- **Stateless** services deployed as containers via Fargate on EKS
- Event consumption handled via **AWS Lambda** when latency tolerance permits

### 🧩 Integration Points

| Use Case                     | Service/Tool                      |
|-----------------------------|-----------------------------------|
| Scheduled Background Jobs    | AWS EventBridge + AWS Lambda      |
| Data Persistence             | Amazon RDS (PostgreSQL)           |
| Configuration Management     | AWS Parameter Store / Secrets Manager |
| Messaging & Event Bus        | Amazon SNS/SQS                    |
| Object Storage               | Amazon S3                         |
| ML/AI Integration            | Amazon Bedrock (via REST calls)   |

---

## ⚙️ DevOps & Deployment

| Tool                     | Purpose                             |
|--------------------------|-------------------------------------|
| GitHub Actions           | CI/CD pipeline for Docker & Lambda  |
| Amazon ECR               | Container image registry            |
| Helm                     | Kubernetes manifest templating      |
| Terraform                | IaC for AWS infrastructure          |
| Spotless, Jib, MapStruct | Code quality and container builds   |

---

## 🔐 Security

- IAM Roles for Service Accounts (IRSA)
- Private subnets for all sensitive workloads
- KMS-encrypted secrets and SSM parameters
- VPC endpoint gateways for secure AWS service access

---

## 📊 Observability

| Tool                  | Role                            |
|-----------------------|----------------------------------|
| Amazon CloudWatch     | Metrics, Logs, Alarms            |
| Prometheus + Grafana  | Pod-level metrics (via EKS)      |
| AWS X-Ray             | Distributed tracing              |

---

## 📎 Diagram (optional for Notion/Figma)
- EKS (Fargate profiles) → Spring Boot pods
- Lambda (triggered by API Gateway, EventBridge)
- PostgreSQL on RDS
- S3 for storage
- All within a secure VPC
