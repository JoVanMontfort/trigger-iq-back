
# ✅ Fargate Setup for TriggerIQ EKS

## ✅ 1. Create a Fargate Profile

You can define a Fargate profile using `eksctl`, Terraform, or raw AWS CLI/YAML. Here's the **YAML-based method** (works well with `eksctl` or `kubectl`).

### 🔧 Example Fargate Profile (YAML)

```yaml
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: triggeriq-eks-cluster
  region: eu-west-1

fargateProfiles:
  - name: triggeriq-fargate
    selectors:
      - namespace: jobs
      - namespace: classifier
```

> This will match all pods running in the `jobs` and `classifier` namespaces and schedule them on Fargate.

---

## ✅ 2. Apply with `eksctl`

Save the above YAML as `fargate-profile.yaml` and run:

```bash
eksctl create fargateprofile -f fargate-profile.yaml
```

---

## ✅ 3. Create Namespaces

```bash
kubectl create namespace jobs
kubectl create namespace classifier
```

**Optional**: Label them for better visibility or cost tracking:

```bash
kubectl label namespace jobs purpose=fargate
kubectl label namespace classifier purpose=fargate
```

---

## ✅ 4. Deploy a Sample Pod to Test

Here's a sample Job that runs in Fargate:

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: sample-job
  namespace: jobs
spec:
  template:
    spec:
      containers:
        - name: hello
          image: busybox
          command: ["echo", "Running on Fargate!"]
      restartPolicy: Never
```

Save as `sample-job.yaml` and apply it:

```bash
kubectl apply -f sample-job.yaml
```

Then check if it ran on Fargate:

```bash
kubectl get pods -n jobs -o wide
```

> Look for `fargate` in the `NODE` column.

---

## 🔐 IAM Role Setup (Optional but Recommended)

If your pods use AWS SDKs (e.g., Bedrock, S3), set up **IRSA (IAM Roles for Service Accounts)**:

1. Create an IAM role with the required policies.
2. Annotate the Kubernetes service account with the role ARN.
3. Deploy your pods using that service account.

> I can generate this part for you once you tell me which AWS service the pod will access (e.g., Bedrock, S3, DynamoDB).
