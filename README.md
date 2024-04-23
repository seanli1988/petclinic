# Deploy AI powered PetClinic on Azure Container Apps

This is a quickstart that shows you deploy PetClinic App powered by OpenAI on Azure Container Apps.

## Prerequisites

This quickstart requires the following to be installed on your machine.

* Unix-like operating system installed. For example, Ubuntu, Azure Linux, macOS, WSL2.
* [Git](https://git-scm.com/downloads)
* [Azure CLI](https://learn.microsoft.com/cli/azure/install-azure-cli?view=azure-cli-latest)
* [JDK 17](https://docs.microsoft.com/java/openjdk/download?WT.mc_id=asa-java-judubois#openjdk-17)
* [Maven](https://maven.apache.org/download.cgi)

## Define variables

You define the following variables used in the quickstart. Please replace placeholder(s) with valid value(s).

```bash
UNIQUE_VALUE=<unique-identifier>
RESOURCE_GROUP_NAME=${UNIQUE_VALUE}rg
LOCATION=eastus
ACA_ENV=${UNIQUE_VALUE}env
ACA_AI_NAME=${UNIQUE_VALUE}ai
ACA_PETCLINIC_NAME=${UNIQUE_VALUE}petclinic
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

## Prepare the source code

Prepare the source code for PetClinic and its AI service by cloning the repository:

```bash
git clone https://github.com/seanli1988/petclinic.git
cd petclinic
```

## Deploy PetClinic AI 

The PetClinic AI integrates with OpenAI service using LangChain for Java. Run the following commands to deploy it on ACA. Please replace placeholder(s) with valid value(s).

```bash
# Checkout the branch for PetClinic AI
git checkout ai

# Build the artifact
mvn clean package -DskipTests=true

# Deploy the artifact to ACA
az containerapp create \
    --resource-group $RESOURCE_GROUP_NAME \
    --name $ACA_AI_NAME \
    --environment $ACA_ENV \
    --artifact ./target/demo-0.0.1-SNAPSHOT.jar \
    --target-port 8080 \
    --ingress 'internal' \
    --env-vars \
	AZURE_OPENAI_ENDPOINT="<azure-openai-endpoint>" \
	AZURE_OPENAI_KEY="<azure-openai-key>" \
	AZURE_SEARCH_ENDPOINT="<azure-search-endpoint>" \
	AZURE_SEARCH_KEY="<azure-search-key>" \
    --min-replicas 1
```

## Deploy PetClinic app

The PetClinic app talks to PetClinic AI service. Run the following commands to deploy it on ACA.

```bash
# Checkout the branch for PetClinic app
git checkout main

# Build the artifact
mvn clean package -DskipTests=true

# Deploy the artifact to ACA
az containerapp create \
    --resource-group $RESOURCE_GROUP_NAME \
    --name $ACA_PETCLINIC_NAME \
    --environment $ACA_ENV \
    --artifact ./target/spring-petclinic-3.2.0-SNAPSHOT.jar \
    --target-port 8080 \
    --ingress 'external' \
    --env-vars \
	PETCLINIC_AI_HOST=http://${ACA_AI_NAME} \
    --min-replicas 1
```

Run the following command to ouput the URL of the PetClinic, and open the URL in your web browser to talk with OpenAI!

```bash
PETCLINIC_APP_URL=https://$(az containerapp show \
    --resource-group $RESOURCE_GROUP_NAME \
    --name $ACA_PETCLINIC_NAME \
    --query properties.configuration.ingress.fqdn \
    --output tsv | tr -d '\r')
echo "PetClinic app URL: $PETCLINIC_APP_URL"
```

## Clean up

Congratulations! You have successfully deployed the AI powered PetClinic to Azure Container Apps. 

Now you can clean up the resources to avoid incurring charges if they are not needed. Run the following command to delete the resource group and all resources created in this tutorial.

```bash
az group delete \
    --name $RESOURCE_GROUP_NAME \
    --yes --no-wait
```
