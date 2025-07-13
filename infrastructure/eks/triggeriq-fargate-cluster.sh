eksctl create cluster --name triggeriq-fargate-cluster --region eu-west-3 --fargate --fargate-profile-name triggeriq-fargate --fargate-profile-namespaces triggeriq-fargate --node-private-networking
eksctl create cluster --name triggeriq-fargate-cluster --region eu-west-3 --version 1.30 --without-nodegroup --node-private-networking
eksctl create fargateprofile --cluster triggeriq-fargate-cluster --region eu-west-3 --name triggeriq-fargate --namespace triggeriq-fargate
aws eks --region eu-west-3 update-kubeconfig --name triggeriq-fargate-cluster

eksctl utils update-cluster-logging --enable-types all --region eu-west-3 --cluster triggeriq-fargate-cluster

aws eks describe-cluster --name triggeriq-fargate-cluster --region eu-west-3 --query "cluster.logging.clusterLogging[*].{Type:type,Enabled:enabled}" --output table

eksctl enable fargate-logging --cluster triggeriq-fargate-cluster --region eu-west-3

aws iam attach-role-policy --role-name eksctl-triggeriq-fargate-cl-FargatePodExecutionRole-p02qdskRV3ir --policy-arn arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy

kubectl run hello-fargate-logging-test --image=busybox --restart=Never --namespace=jobs --labels=app=hello-logging --command -- sh -c "echo Hello from Fargate with CloudWatch logging! && sleep 60"

eksctl create addon --name coredns --cluster triggeriq-fargate-cluster --region eu-west-3 --service-account-role-arn arn:aws:iam::<your-account-id>:role/AmazonEKSFargatePodExecutionRole --force

eksctl create fargateprofile --cluster triggeriq-fargate-cluster --region eu-west-3 --name coredns --namespace kube-system --selectors "k8s-app=coredns"

kubectl -n kube-system patch deployment coredns --type='json' -p='[{
    "op": "add",
    "path": "/spec/template/spec/tolerations/-",
    "value": {
      "key": "eks.amazonaws.com/compute-type",
      "operator": "Equal",
      "value": "fargate",
      "effect": "NoSchedule"
    }
  }]'

eksctl create nodegroup --cluster triggeriq-fargate-cluster --region eu-west-3 --name coredns-ec2-nodegroup --node-type t3.small --nodes 1 ---nodes-min 1 --nodes-max 1 --managed --node-role arn:aws:iam::811904917041:role/eksctl-triggeriq-fargate-cl-NodeInstanceRole-ABCDE12345

eksctl create nodegroup --cluster triggeriq-fargate-cluster --region eu-west-3 --name coredns-ec2-nodegroup --node-type t3.small --nodes 1 --nodes-min 1 --nodes-max 1 --managed

aws ec2 import-key-pair --key-name my-custom-key --public-key-material fileb://

aws ec2 describe-instances --instance-id i-xxxxxxxxxxxxxxxxx --query 'Reservations[*].Instances[*].{Subnet:SubnetId,PublicIP:PublicIpAddress,SG:SecurityGroups[*].GroupId}' --output table

helm upgrade -i aws-node-termination-handler eks/aws-node-termination-handler --namespace kube-system --set nodeSelector.lifecycle=EC2 --set tolerations[0].key=aws-node-termination-handler --set tolerations[0].operator=Exists --create-namespace

aws ec2 describe-instances --filters "Name=tag:kubernetes.io/cluster/<cluster-name>,Values=owned" --query 'Reservations[*].Instances[*].{ID:InstanceId,Type:InstanceType,State:State.Name}'

aws iam create-policy --policy-name CloudWatchLogsPolicy --policy-document file://cloudwatch-policy.json

aws iam create-role --role-name EC2CloudWatchLogsRole --assume-role-policy-document file://trust-policy.json

aws iam attach-role-policy --role-name EC2CloudWatchLogsRole --policy-arn arn:aws:iam::123456789012:policy/CloudWatchLogsPolicy

aws iam create-instance-profile --instance-profile-name EC2CloudWatchLogsInstanceProfile

aws iam add-role-to-instance-profile --instance-profile-name EC2CloudWatchLogsInstanceProfile --role-name EC2CloudWatchLogsRole

aws ec2 associate-iam-instance-profile --instance-id i-xxxxxxxxxxxxxxxxx --iam-instance-profile Name=EC2CloudWatchLogsInstanceProfile

sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -c file:/opt/aws/amazon-cloudwatch-agent/bin/config.json -s
