eksctl create cluster --name triggeriq-fargate-cluster --region eu-west-3 --fargate --fargate-profile-name triggeriq-fargate --fargate-profile-namespaces triggeriq-fargate --node-private-networking
eksctl create cluster --name triggeriq-fargate-cluster --region eu-west-3 --version 1.30 --without-nodegroup --node-private-networking
eksctl create fargateprofile --cluster triggeriq-fargate-cluster --region eu-west-3 --name triggeriq-fargate --namespace triggeriq-fargate
aws eks --region eu-west-3 update-kubeconfig --name triggeriq-fargate-cluster