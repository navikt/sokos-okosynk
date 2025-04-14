#!/bin/bash

# Ensure user is authenicated, and run login if not.
gcloud auth print-identity-token &> /dev/null
if [ $? -gt 0 ]; then
    gcloud auth login
fi
kubectl config use-context dev-fss
kubectl config set-context --current --namespace=okonomi

# Get AZURE system variables
envValue=$(kubectl exec -it $(kubectl get pods | grep sokos-okosynk | cut -f1 -d' ') -c sokos-okosynk -- env | egrep "^AZURE|^SFTP|^PDL|^OPPGAVE")

# Set AZURE as local environment variables
rm -f defaults.properties
echo "$envValue" > defaults.properties
echo "AZURE stores as defaults.properties"


PRIVATE_KEY=$(kubectl get secret okosynk-sftp-private-key -o jsonpath='{.data.SFTP_PRIVATE_KEY}' | base64 --decode)
rm -f privateKey
echo "$PRIVATE_KEY" > privateKey

sed -i '' '/^SFTP_SERVER=/ s/=.*/=10.183.32.98/' defaults.properties
sed -i '' '/^SFTP_PRIVATE_KEY=/ s/=.*/=privateKey/' defaults.properties