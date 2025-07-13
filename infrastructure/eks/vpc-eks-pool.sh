#!/bin/bash
for cluster in $(aws eks list-clusters --query "clusters[]" --output text); do
  vpc_id=$(aws eks describe-cluster --name "$cluster" \
            --query "cluster.resourcesVpcConfig.vpcId" --output text)
  echo "Cluster: $cluster is in VPC: $vpc_id"
done
