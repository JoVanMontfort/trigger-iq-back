# 🧠 TriggerIQ x AWS Generative AI Stack Overview

**Last updated:** 2025-07-13 10:27:06

---

## 🚀 Project: TriggerIQ

TriggerIQ is a multilingual, AI-enhanced platform for customer feedback analysis, automation, and intelligent resolution using GenAI technologies.

---

## 🏗️ High-Level Architecture

### ⚙️ Stack Components

| Layer              | Technology                                 |
|-------------------|--------------------------------------------|
| Frontend          | Angular, Tailwind CSS                      |
| Backend           | Spring Boot (Java), JHipster               |
| AI Integration    | Amazon Bedrock (Claude, Titan), OpenAI API|
| Kubernetes        | Amazon EKS + Fargate                       |
| Serverless        | AWS Lambda (for lightweight triggers)      |
| Data Storage      | Amazon S3, PostgreSQL                      |
| Deployment        | Terraform, GitHub Actions (CI/CD)          |

---

## 🧠 GenAI Usage

| Feature                     | Tech                           |
|----------------------------|--------------------------------|
| Autoresolve Suggestions    | Bedrock + Java SDK             |
| Sentiment Classification   | Bedrock Titan / Claude         |
| Language Support           | Prompt Engineering via Bedrock |
| Future: Custom RAG models  | Fine-tuning on Bedrock / Sagemaker|

---

## ☁️ AWS Services Involved

- **Amazon EKS**: Main Kubernetes control plane
- **AWS Fargate**: Serverless pod infrastructure for selected workloads
- **Amazon Bedrock**: Foundation model access (Claude, Titan, etc.)
- **AWS Lambda**: Lightweight automation and triggers
- **Amazon S3**: Document ingestion, feedback logs
- **Amazon RDS / PostgreSQL**: Metadata and user data
- **Amazon Route 53**: DNS for `triggeriq.eu`
- **IAM**: Least-privilege identity setup

---

## 🛠️ DevOps / IaC

- `eksctl` for EKS + Fargate profiles
- `kubectl` for deployment management
- `Terraform` for infrastructure provisioning
- GitHub Actions for CI/CD automation

---

## 🔄 Fargate Setup

```bash
eksctl create cluster \
  --name triggeriq-fargate-cluster \
  --region eu-west-1 \
  --version 1.30 \
  --without-nodegroup \
  --node-private-networking

eksctl create fargateprofile \
  --cluster triggeriq-fargate-cluster \
  --region eu-west-1 \
  --name triggeriq-fargate \
  --namespace triggeriq-fargate
```

---

## 📌 Next Steps

- Integrate Java client with Bedrock
- Add custom namespace selectors for Fargate
- Benchmark GenAI workloads (latency/cost)
- Optional: Setup RAG pipeline or fine-tune Claude

