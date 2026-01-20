#!/bin/bash

# Ensure user is authenicated, and run login if not.
gcloud auth print-identity-token &> /dev/null
if [ $? -gt 0 ]; then
    gcloud auth login
fi

# Suppress kubectl config output
kubectl config use-context dev-fss
kubectl config set-context --current --namespace=okonomi

# Get pod name
POD_NAME=$(kubectl get pods --no-headers | grep sokos-okosynk | head -n1 | awk '{print $1}')

if [ -z "$POD_NAME" ]; then
    echo "Error: No sokos-okosynk pod found" >&2
    exit 1
fi

echo "Fetching environment variables from pod: $POD_NAME"

# Get system variables
envValue=$(kubectl exec "$POD_NAME" -c sokos-okosynk -- env | egrep "^AZURE|^SFTP|^PDL|^OPPGAVE" | sort)

# Set local environment variables
rm -f defaults.properties
echo "$envValue" > defaults.properties
echo "Environment variables saved to defaults.properties"

sed -i '' '/^SFTP_SERVER=/ s/=.*/=10.183.32.98/' defaults.properties

PRIVATE_KEY=$(kubectl get secret okosynk-sftp-private-key -o jsonpath='{.data.SFTP_PRIVATE_KEY}' | base64 --decode)
rm -f privateKey
echo "$PRIVATE_KEY" > privateKey
grep -q '^SFTP_PRIVATE_KEY=' defaults.properties && \
sed -i '' '/^SFTP_PRIVATE_KEY=/ s/=.*/=privateKey/' defaults.properties || \
echo 'SFTP_PRIVATE_KEY=privateKey' >> defaults.properties