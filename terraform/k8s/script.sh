kubectl apply -f namespace.yml
kubectl apply -f jenkins-sa.yaml
kubectl apply -f token-secret.yaml
kubectl describe secrets/jenkins-secret -n jenkins-agents
