# 🏗️ High-Level Stack Overview — TriggerIQ Backend

## 🎯 Use Case

TriggerIQ needs:

- **APIs and services that can scale** → 🐳 Amazon EKS (Kubernetes)
- **Event-driven short tasks or classifiers** → ⚡ AWS Lambda
- **Background containers (e.g. batch jobs) without managing infra** → 🚀 AWS Fargate

---

## ✅ Step-by-Step Plan

### 🧱 1. EKS Cluster Setup (with Fargate)

**Purpose:** Run long-lived services: APIs, dashboard backend, task queue processors.

#### 🛠️ Tools

- Terraform or `eksctl` (your choice)
- Amazon VPC with public/private subnets
- EKS cluster with Fargate profiles
- IAM roles for service accounts (IRSA)

#### EKS Workloads

- `triggeriq-api`: main REST/gRPC interface
- `classifier-service`: background job runner (can use Bedrock)
- `dashboard`: admin UI backend

---

### ⚙️ 2. Fargate Task Definitions

**Purpose:** Run isolated containerized jobs without managing servers.

#### 🧪 Example Workloads

- `feedback-daily-trainer`: runs nightly to fine-tune or reclassify feedback
- `batch-analytics-runner`: processes feedback in batches

Define a task definition, upload your container to ECR, and trigger it with CloudWatch or EventBridge.

---

### ⚡ 3. AWS Lambda Functions

**Purpose:** Handle event-based tasks (fast, stateless)

#### 🧩 Functions

- `feedbackSentimentClassifier`: triggered by new feedback in S3/DynamoDB
- `webhookRouter`: triggered by HTTP event
- `autoResolveTrigger`: evaluates whether an alert can be resolved

> **Runtime:** Java (or Python/Node for faster cold starts — optional)

---

### ☁️ 4. Other AWS Services

- **API Gateway** – for triggering Lambda externally
- **S3** – for feedback uploads, logs
- **EventBridge / SQS** – for decoupling workloads
- **CloudWatch** – for logs and alarms
- **DynamoDB / Aurora** – to persist events/metadata

---

## 📦 Folder / Repo Layout (Mono-Repo Style)

```plaintext
triggeriq/
├── infrastructure/
│   ├── eks/           # Terraform or eksctl setup
│   └── lambda/        # CDK or SAM deployments
├── services/
│   ├── api-service/   # EKS deployed app
│   ├── classifier/    # Fargate + EKS hybrid
│   └── lambdas/
│       ├── feedbackClassifier/
│       └── webhookRouter/
└── charts/            # Helm charts for EKS services
```

---

## 🧪 Next Steps

Would you like to start with:

- ✅ Terraform-based EKS + Fargate setup
- ✅ SAM or CDK template to deploy Java Lambdas
- ✅ Spring Boot service Dockerized and ready for EKS
- ✅ CI/CD setup (e.g., GitHub Actions) for all of the above
