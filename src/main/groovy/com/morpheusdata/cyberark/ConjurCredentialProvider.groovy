package com.morpheusdata.cyberark

import com.morpheusdata.core.CredentialProvider
import com.morpheusdata.core.MorpheusContext
import com.morpheusdata.core.Plugin
import com.morpheusdata.core.util.HttpApiClient
import com.morpheusdata.model.AccountCredential
import com.morpheusdata.model.AccountIntegration
import com.morpheusdata.model.Icon
import com.morpheusdata.model.OptionType
import com.morpheusdata.response.ServiceResponse
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
class ConjurCredentialProvider implements CredentialProvider {
    MorpheusContext morpheusContext
    Plugin plugin

    ConjurCredentialProvider(Plugin plugin, MorpheusContext morpheusContext) {
        this.morpheusContext = morpheusContext
        this.plugin = plugin
    }

    /**
     * Periodically called to test the status of the credential provider.
     * @param integration the referenced integration object to be loaded
     */
    @Override
    void refresh(AccountIntegration integration) {
        //NOTHING TODO FOR NOW
    }

    /**
     * Used to load credential information on the fly from the datastore. The data map should be the credential data to be loaded on the fly
     * @param integration the referenced integration object to be loaded
     * @param credential the credential reference to be loaded.
     * @param opts any custom options such as proxySettings if necessary (future use)
     * @return
     */
    @Override
    ServiceResponse<Map> loadCredentialData(AccountIntegration integration, AccountCredential credential, Map opts) {
        String secretPathSuffix = integration.getConfigProperty("secretPath") ?: ''
        String organization = integration.getConfigProperty("organization")
        HttpApiClient apiClient = new HttpApiClient()
        try {
            def authResults = authToken(apiClient,integration)
            if(authResults.success) {

                //we gotta fetch from Conjur
                if(secretPathSuffix &&  !secretPathSuffix.endsWith('/')) {
                    secretPathSuffix = secretPathSuffix + '/'
                }
                String conjurPath="/secrets/${organization}/variable/" + secretPathSuffix + formatApiName(credential.name)
                def apiResults = apiClient.callApi(integration.serviceUrl,conjurPath.toString(),null,null,new HttpApiClient.RequestOptions(headers: ['Authorization': authResults.data]),'GET')
                if(apiResults.success) {
                    Map data = new JsonSlurper().parseText(apiResults.content) as Map
                    ServiceResponse<Map> response = new ServiceResponse<>(true,null,null,data)
                    return response
                } else {
                    return ServiceResponse.error(apiResults.error)
                }
            } else {
                return ServiceResponse.error(authResults.error)
            }
            
        } finally {
            apiClient.shutdownClient()
        }
    }

    protected ServiceResponse<String> authToken(HttpApiClient client, AccountIntegration integration) {
        String username = integration.serviceUsername
        String apiKey = integration.servicePassword
        String organization = integration.getConfigProperty("organization")

        def authResults = client.callApi(integration.serviceUrl,"authn/${organization}/${username}/authenticate".toString(),null,null,new HttpApiClient.RequestOptions(headers: ['Accept-Encoding': "base64"], body: apiKey),'POST')
        if(authResults.success) {
            return ServiceResponse.success("Token token=\"${authResults.content}\"".toString())
        } else {
            return ServiceResponse.error("Authentication Failed with CyberArk Conjur")
        }
    }

    /**
     * Deletes the credential on the remote integration.
     * @param integration the referenced integration object containing information necessary to connect to the endpoint
     * @param credential the credential to be deleted
     * @param opts any custom options such as proxySettings if necessary (future use)
     * @return
     */
    @Override
    ServiceResponse<AccountCredential> deleteCredential(AccountIntegration integration, AccountCredential credential, Map opts) {
        String secretPathSuffix = integration.getConfigProperty("secretPath") ?: ''
        String organization = integration.getConfigProperty("organization")
        HttpApiClient apiClient = new HttpApiClient()
        try {
            def authResults = authToken(apiClient,integration)
            if(authResults.success) {
                 //we gotta fetch from Conjur
                if(secretPathSuffix && !secretPathSuffix.endsWith('/')) {
                    secretPathSuffix = secretPathSuffix + '/'
                }
                String conjurPath="/secrets/${organization}/variable/" + secretPathSuffix + formatApiName(credential.name)
                def apiResults = apiClient.callApi(integration.serviceUrl,conjurPath.toString(),null,null,new HttpApiClient.RequestOptions(headers: ['Authorization': authResults.data], body:''),'POST')
                if(apiResults.success) {
                    ServiceResponse<AccountCredential> response = new ServiceResponse<>(true,null,null,credential)
                    return response
                } else {
                    return ServiceResponse.error(apiResults.error,null,credential)
                }
            } else {
                return ServiceResponse.error(authResults.error)
            }
           
        } finally {
            apiClient.shutdownClient()
        }
    }

    /**
     * Creates the credential on the remote integration.
     * @param integration the referenced integration object containing information necessary to connect to the endpoint
     * @param credential the credential to be created
     * @param opts any custom options such as proxySettings if necessary (future use)
     * @return
     */
    @Override
    ServiceResponse<AccountCredential> createCredential(AccountIntegration integration, AccountCredential credential, Map opts) {
        String secretPathSuffix = integration.getConfigProperty("secretPath") ?: ''
        String organization = integration.getConfigProperty("organization")
        HttpApiClient apiClient = new HttpApiClient()
        try {

            def authResults = authToken(apiClient,integration)
            if(authResults.success) {
                //we gotta fetch from Conjur
                if(secretPathSuffix && !secretPathSuffix.endsWith('/')) {
                    secretPathSuffix = secretPathSuffix + '/'
                }
                String conjurPath="/secrets/${organization}/variable/" + secretPathSuffix + formatApiName(credential.name)
                def apiResults = apiClient.callApi(integration.serviceUrl,conjurPath.toString(),null,null,new HttpApiClient.RequestOptions(headers: ['Authorization': authResults.data], body: JsonOutput.toJson(credential.data)),'POST')
                if(apiResults.success) {
                    ServiceResponse<AccountCredential> response = new ServiceResponse<>(true,null,null,credential)
                    return response
                } else {
                    return ServiceResponse.error(apiResults.error,null,credential)
                }
            } else {
                return ServiceResponse.error(authResults.error)
            }
        } finally {
            apiClient.shutdownClient()
        }
    }

    /**
     * Updates the credential on the remote integration.
     * @param integration the referenced integration object containing information necessary to connect to the endpoint
     * @param credential the credential to be updated
     * @param opts any custom options such as proxySettings if necessary (future use)
     * @return
     */
    @Override
    ServiceResponse<AccountCredential> updateCredential(AccountIntegration integration, AccountCredential credential, Map opts) {
        String secretPathSuffix = integration.getConfigProperty("secretPath") ?: ''
        String organization = integration.getConfigProperty("organization")

        HttpApiClient apiClient = new HttpApiClient()
        try {
            def authResults = authToken(apiClient,integration)
            if(authResults.success) {
                //we gotta fetch from Conjur
                if (secretPathSuffix && !secretPathSuffix.endsWith('/')) {
                    secretPathSuffix = secretPathSuffix + '/'
                }
                String conjurPath = "/secrets/${organization}/variable/" + secretPathSuffix + formatApiName(credential.name)
                def apiResults = apiClient.callApi(integration.serviceUrl, conjurPath.toString(),null,null, new HttpApiClient.RequestOptions(headers: ['Authorization': authResults.data], body: JsonOutput.toJson(credential.data)), 'POST')
                if (apiResults.success) {
                    ServiceResponse<AccountCredential> response = new ServiceResponse<>(true, null, null, credential)
                    return response
                } else {
                    return ServiceResponse.error(apiResults.error, null, credential)
                }
            }else {
                return ServiceResponse.error(authResults.error)
            }
        } finally {
            apiClient.shutdownClient()
        }
    }

    /**
     * Validation Method used to validate all inputs applied to the integration of an Credential Provider upon save.
     * If an input fails validation or authentication information cannot be verified, Error messages should be returned
     * via a {@link ServiceResponse} object where the key on the error is the field name and the value is the error message.
     * If the error is a generic authentication error or unknown error, a standard message can also be sent back in the response.
     *
     * @param integration The Integration Object contains all the saved information regarding configuration of the Credential Provider.
     * @param opts any custom payload submission options may exist here
     * @return A response is returned depending on if the inputs are valid or not.
     */
    @Override
    ServiceResponse<Map> verify(AccountIntegration integration, Map opts) {
        HttpApiClient apiClient = new HttpApiClient()
        try {
            def authResults = authToken(apiClient,integration)
            if(authResults.success) {

                def apiResults = apiClient.callApi(integration.serviceUrl,'/whoami',null,null,new HttpApiClient.RequestOptions(headers: ['Authorization': authResults.data]),'GET')
                if(apiResults.success) {
                    ServiceResponse<Map> response = new ServiceResponse<>(true,null,null,[:])
                    return response
                } else {
                    return ServiceResponse.error(apiResults.error)
                }
            } else {
                return ServiceResponse.error(authResults.error)
            }

        } finally {
            apiClient.shutdownClient()
        }
    }

    /**
     * Provide custom configuration options when creating a new {@link AccountIntegration}
     * @return a List of OptionType
     */
    @Override
    List<OptionType> getIntegrationOptionTypes() {
        return [
                new OptionType(code: 'conjur.serviceUrl', name: 'Service URL', inputType: OptionType.InputType.TEXT, fieldName: 'serviceUrl', fieldLabel: 'API Url', fieldContext: 'domain', displayOrder: 0),
                new OptionType(code: 'conjur.serviceUsername', name: 'Service Username', inputType: OptionType.InputType.TEXT, fieldName: 'serviceUsername', fieldLabel: 'Username', fieldContext: 'domain', displayOrder: 1),
                new OptionType(code: 'conjur.serviceApiKey', name: 'Service ApiKey', inputType: OptionType.InputType.PASSWORD, fieldName: 'servicePassword', fieldLabel: 'API Key', fieldContext: 'domain', displayOrder: 2),
                new OptionType(code: 'conjur.organization', name: 'Organization', inputType: OptionType.InputType.TEXT, fieldName: 'organization', fieldLabel: 'Organization', fieldContext: 'config', displayOrder: 3),
                new OptionType(code: 'conjur.secretPath', name: 'Secret Path', inputType: OptionType.InputType.TEXT,placeHolderText: 'morpheus-credentials/', fieldName: 'secretPath', fieldLabel: 'Secret Path', fieldContext: 'config', displayOrder: 4)
        ]
    }

    /**
     * Returns the Credential Integration logo for display when a user needs to view or add this integration
     * @since 0.12.3
     * @return Icon representation of assets stored in the src/assets of the project.
     */
    @Override
    Icon getIcon() {
        return new Icon(path:"cyberark.svg", darkPath: "cyberark-white.svg")
    }

    /**
     * Returns the Morpheus Context for interacting with data stored in the Main Morpheus Application
     *
     * @return an implementation of the MorpheusContext for running Future based rxJava queries
     */
    @Override
    MorpheusContext getMorpheus() {
        return morpheusContext
    }

    /**
     * Returns the instance of the Plugin class that this provider is loaded from
     * @return Plugin class contains references to other providers
     */
    @Override
    Plugin getPlugin() {
        return plugin
    }

    /**
     * A unique shortcode used for referencing the provided provider. Make sure this is going to be unique as any data
     * that is seeded or generated related to this provider will reference it by this code.
     * @return short code string that should be unique across all other plugin implementations.
     */
    @Override
    String getCode() {
        return "conjur"
    }

    /**
     * Provides the provider name for reference when adding to the Morpheus Orchestrator
     * NOTE: This may be useful to set as an i18n key for UI reference and localization support.
     *
     * @return either an English name of a Provider or an i18n based key that can be scanned for in a properties file.
     */
    @Override
    String getName() {
        return "Conjur"
    }

    static protected formatApiName(String name) {
        String rtn = name
        if(rtn) {
            rtn = rtn.replace(' - ', '-')
            rtn = rtn.replace(' ', '-')
            rtn = rtn.replace('/', '-')
            rtn = rtn.toLowerCase()
        }
        return URLEncoder.encode(rtn)
    }
}