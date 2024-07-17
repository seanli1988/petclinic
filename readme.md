# Spring PetClinic Sample Application [![Build Status](https://github.com/spring-projects/spring-petclinic/actions/workflows/maven-build.yml/badge.svg)](https://github.com/spring-projects/spring-petclinic/actions/workflows/maven-build.yml)

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/spring-projects/spring-petclinic) [![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://github.com/codespaces/new?hide_repo_select=true&ref=main&repo=7517918)

## Understanding the Spring Petclinic application with a few diagrams

[See the presentation here](https://speakerdeck.com/michaelisvy/spring-petclinic-sample-application)

## Run Petclinic locally

This is a modified version of Petclinic and includes two opentelemetry dependencies: opentelemetry-spring-boot-starter and opentelemetry-jdbc. To run this sample locally you must either: A) remove these dependencies B) include -Dotel.sdk.disabled=true as a JVM parameter or C) start up an opentelmetry collector locally. You may follow instructions from the dockerized-otel-agent branch for option C. 

```bash
git clone https://github.com/spring-projects/spring-petclinic.git
cd spring-petclinic
./mvnw package
java -jar target/*.jar
```

You can then access the Petclinic at <http://localhost:8080/>.

<img width="1042" alt="petclinic-screenshot" src="https://cloud.githubusercontent.com/assets/838318/19727082/2aee6d6c-9b8e-11e6-81fe-e889a5ddfded.png">


## Deploy to Azure Container Apps

## Azure CLI setup

Check your Azure CLI version and upgrade to the required version if needed.

```bash
az --version
az upgrade
```

Install or update the Azure Application Insights and Azure Container Apps extensions.

```bash
az extension add -n application-insights --upgrade --allow-preview true
az extension add --name containerapp --upgrade --allow-preview true
```

Sign in to Azure CLI if you haven't already done so.

```bash
az login
```

Set the default subscription to use.

```bash
az account set --subscription "<subscription-id>"
```

Register the `Microsoft.App` and `Microsoft.OperationalInsights` namespaces if they're not already registered in your Azure subscription.

```bash
az provider register --namespace Microsoft.App
az provider register --namespace Microsoft.OperationalInsights
```

## Define variables

Please replace placeholder values in <> with your own.

```bash


UNIQUE_VALUE=pc-$(date +%s)
LOCATION=eastus

RESOURCE_GROUP_NAME=${UNIQUE_VALUE}rg
ACA_ENV=${UNIQUE_VALUE}env
APP_INSIGHTS=${UNIQUE_VALUE}appinsights
CONFIG_SERVER=springconfigserver
CONFIG_SERVER_GIT_URI="https://github.com/seanli1988/petclinic.git"
ACA_AI_NAME=${UNIQUE_VALUE}ai
ACA_PETCLINIC_NAME=${UNIQUE_VALUE}petclinic
WORKING_DIR=$(pwd)
```

## Create an Azure Container Apps Environment

Create a resource group and an Azure Container Apps environment.

```bash
az group create \
    -n $RESOURCE_GROUP_NAME \
    -l ${LOCATION}

az containerapp env create \
    --resource-group $RESOURCE_GROUP_NAME \
    --location $LOCATION \
    --name $ACA_ENV
```

## Set up a managed Config Server for Spring microservices

Azure Container Apps provides a managed Config Server for Spring microservices. The Config Server is a centralized configuration service that serves configuration data to client applications.

Create a Config Server and configure it to use a Git repository as the backend.

```bash
az containerapp env java-component config-server-for-spring create \
  --environment $ACA_ENV \
  --resource-group $RESOURCE_GROUP_NAME \
  --name $CONFIG_SERVER \
  --configuration spring.cloud.config.server.git.uri=$CONFIG_SERVER_GIT_URI
```

## Deploy PetClinic app

The PetClinic app talks to PetClinic AI service. Follow instructions below to deploy it on ACA.

First, prepare the source code for PetClinic app by cloning the repository.

```bash
cd $WORKING_DIR
git clone git@github.com:seanli1988/petclinic.git
cd petclinic
```

Next, build and deploy the PetClinic app to Azure Container Apps.

```bash
# Build the artifact
mvn clean package -DskipTests=true

# Deploy the artifact to ACA and bind to config server
az containerapp create \
    --resource-group $RESOURCE_GROUP_NAME \
    --name $ACA_PETCLINIC_NAME \
    --environment $ACA_ENV \
    --artifact ./target/spring-petclinic-3.2.0-SNAPSHOT.jar \
    --target-port 8080 \
    --ingress 'external' \
    --env-vars \
        PETCLINIC_AI_HOST=http://${ACA_AI_NAME} \
        SPRING_DATASOURCE_URL=jdbc:otel:h2:mem:db \
        SPRING_DATASOURCE_DRIVER_CLASS_NAME=io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver \
    --min-replicas 1 \
    --bind $CONFIG_SERVER
```

# Spring Boot Admin
```bash
SBA_SERVER=sbaserver
ROLE_NAME=${UNIQUE_VALUE}role
```

## Create Spring Boot Admin server
```bash
az containerapp env java-component admin-for-spring create \
    --environment $ACA_ENV \
    --resource-group $RESOURCE_GROUP_NAME \
    --name $SBA_SERVER
```

## Bind the app to Spring Boot Admin
```bash
az containerapp update \
  --name $ACA_PETCLINIC_NAME \
  --resource-group $RESOURCE_GROUP_NAME \
  --bind $SBA_SERVER
```

## View the dashboard
```bash
az role definition create --role-definition '{
    "Name": $ROLE_NAME,
    "IsCustom": true,
    "Description": "Can access managed Java Component dashboards in managed environments",
    "Actions": ["Microsoft.App/managedEnvironments/write"],
    "AssignableScopes": ["/subscriptions/<SUBSCRIPTION_ID>"]
}'

export ENVIRONMENT_ID=$(az containerapp env show --name $ACA_ENV --resource-group $RESOURCE_GROUP_NAME --query id -o tsv)
az role assignment create --assignee <USER_OR_SERVICE_PRINCIPAL_ID> --role $ROLE_NAME --scope $ENVIRONMENT_ID
az containerapp env java-component admin-for-spring show --environment $ACA_ENV --resource-group $RESOURCE_GROUP_NAME --name $SBA_SERVER --query properties.ingress.fqdn -o tsv
```

## License

The Spring PetClinic sample application is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).
