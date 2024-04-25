# Deploy AI powered PetClinic with telemetry on Azure Container Apps

This is a quickstart that shows you deploy PetClinic App powered by OpenAI on Azure Container Apps, with OpenTelemetry enabled to collect and read logs and traces in Azure Monitor Application Insights.

## Prerequisites

This quickstart requires the following to be installed on your machine.

* Unix-like operating system installed. For example, Ubuntu, Azure Linux, macOS, WSL2.
* [Git](https://git-scm.com/downloads)
* [Azure CLI 2.59.0 or higher version](https://learn.microsoft.com/cli/azure/install-azure-cli?view=azure-cli-latest)
* [JDK 17](https://docs.microsoft.com/java/openjdk/download?WT.mc_id=asa-java-judubois#openjdk-17)
* [Maven](https://maven.apache.org/download.cgi)

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

You define the following variables used in the quickstart. Please replace placeholders with valid values.

```bash
UNIQUE_VALUE=<unique-identifier>
RESOURCE_GROUP_NAME=${UNIQUE_VALUE}rg
LOCATION=eastus
ACA_ENV=${UNIQUE_VALUE}env
APP_INSIGHTS=${UNIQUE_VALUE}appinsights
ACA_AI_NAME=${UNIQUE_VALUE}ai
AZURE_OPENAI_ENDPOINT="<azure-openai-endpoint>"
AZURE_OPENAI_KEY="<azure-openai-key>"
AZURE_SEARCH_ENDPOINT="<azure-search-endpoint>"
AZURE_SEARCH_KEY="<azure-search-key>"
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

## Collect and read OpenTelemetry data in Azure Container Apps 

OpenTelemetry agents live within your container app environment. You configure agent settings through the Azure CLI.

The managed OpenTelemetry agent accepts the following destinations:

* Azure Monitor Application Insights
* Datadog
* Any OTLP endpoint (For example: New Relic or Honeycomb)

In this tutorial, you use Azure Monitor Application Insights as the destination.

First, create an Azure Application Insights resource to receive OpenTelemetry data.

```bash
logAnalyticsWorkspace=$(az monitor log-analytics workspace list \
    -g $RESOURCE_GROUP_NAME \
    --query "[0].name" -o tsv)

az monitor app-insights component create \
    --app $APP_INSIGHTS \
    -g $RESOURCE_GROUP_NAME \
    -l $LOCATION \
    --workspace $logAnalyticsWorkspace
```

Next, enable OpenTelemetry for the Azure Container Apps environment and configure it to send data to the Azure Application Insights resource.

```bash
appInsightsConn=$(az monitor app-insights component show \
    --app $APP_INSIGHTS \
    -g $RESOURCE_GROUP_NAME \
    --query 'connectionString' -o tsv)

az containerapp env telemetry app-insights set \
  --name $ACA_ENV \
  --resource-group $RESOURCE_GROUP_NAME \
  --connection-string $appInsightsConn \
  --enable-open-telemetry-logs true \
  --enable-open-telemetry-traces true
```

## Deploy PetClinic AI 

The PetClinic AI integrates with OpenAI service using LangChain for Java. Follow instructions below to deploy it on ACA.

First, prepare the source code for PetClinic AI by cloning the repository and target branch.

```bash
cd $WORKING_DIR
git clone --single-branch --branch ai https://github.com/seanli1988/petclinic.git petclinic-ai
cd petclinic-ai
```

Next, build and deploy the PetClinic AI service to Azure Container Apps.

```bash
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
	AZURE_OPENAI_ENDPOINT=$AZURE_OPENAI_ENDPOINT \
	AZURE_OPENAI_KEY=$AZURE_OPENAI_KEY \
	AZURE_SEARCH_ENDPOINT=$AZURE_SEARCH_ENDPOINT \
	AZURE_SEARCH_KEY=$AZURE_SEARCH_KEY \
    --min-replicas 1
```

## Deploy PetClinic app

The PetClinic app talks to PetClinic AI service. Follow instructions below to deploy it on ACA.

First, prepare the source code for PetClinic app by cloning the repository.

```bash
cd $WORKING_DIR
git clone https://github.com/seanli1988/petclinic.git
cd petclinic
```

Next, build and deploy the PetClinic app to Azure Container Apps.

```bash
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
	SPRING_DATASOURCE_URL=jdbc:otel:h2:mem:db \
	SPRING_DATASOURCE_DRIVER_CLASS_NAME=io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver \
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

## References

Refer to the following links for more information.

1. [LangChain for Java: Supercharge your Java application with the power of LLMs](https://github.com/langchain4j/langchain4j)
1. [Collect and read OpenTelemetry data in Azure Container Apps](https://learn.microsoft.com/azure/container-apps/opentelemetry-agents?tabs=azure-cli)
1. [OpenTelemetry Collector](https://opentelemetry.io/docs/collector/deployment/agent/)
1. [How to instrument Spring Boot with OpenTelemetry](https://opentelemetry.io/docs/languages/java/automatic/spring-boot/)
1. [Create an Azure Monitor Application Insights using Azure CLI](https://learn.microsoft.com/azure/azure-monitor/app/create-workspace-resource?tabs=bicep#create-a-resource-automatically)
1. [Connect applications in Azure Container Apps](https://learn.microsoft.com/azure/container-apps/connect-apps?tabs=bash)
