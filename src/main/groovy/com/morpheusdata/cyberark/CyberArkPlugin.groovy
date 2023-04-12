/*
* Copyright 2022 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.morpheusdata.cyberark

import com.morpheusdata.core.Plugin
import com.morpheusdata.model.OptionType
import groovy.util.logging.Slf4j
import groovy.json.*
import com.morpheusdata.core.MorpheusContext

/**
 * The entrypoint of the Vault Plugin. This is where multiple providers can be registered (if necessary).
 * In the case of Vault, a simple CredentialProvider is registered that enables functionality for those areas of automation.
 * 
 * @author David Estes, Chris Taylor
 */
@Slf4j
class CyberArkPlugin extends Plugin {

	@Override
	String getCode() {
		return 'morpheus-conjur-plugin'
	}

	@Override
	void initialize() {
		ConjurCredentialProvider conjurCredentialProvider = new ConjurCredentialProvider(this, morpheus)
		this.pluginProviders.put("conjur", conjurCredentialProvider)
		this.pluginProviders.put("conjur-cypher", new ConjurCypherProvider(this,morpheus))
		this.setName("CyberArk Conjur")
		this.settings << new OptionType (
		name: 'Conjur API Url',
		code: 'conjur-cypher-plugin-url',
		fieldName: 'conjurPluginServiceUrl',
		displayOrder: 0,
		fieldLabel: 'Conjur API Url',
		helpText: 'The full URL of the Conjur Server. For example: https://example.conjur.server:8443',
		required: true,
		inputType: OptionType.InputType.TEXT
	)
	
	this.settings << new OptionType (
		name: 'Conjur Username',
		code: 'conjur-cypher-plugin-username',
		fieldName: 'conjurPluginUsername',
		displayOrder: 1,
		fieldLabel: 'Conjur Username',
		required: true,
		inputType: OptionType.InputType.TEXT
	)
	
	this.settings << new OptionType (
		name: 'Conjur API Key',
		code: 'conjur-cypher-plugin-api-key',
		fieldName: 'conjurPluginApiKey',
		displayOrder: 2,
		fieldLabel: 'Conjur Username API Key',
		required: true,
		inputType: OptionType.InputType.PASSWORD
	)
	
	this.settings << new OptionType (
		name: 'Conjur Organization',
		code: 'conjur-cypher-plugin-organization',
		fieldName: 'conjurPluginOrganization',
		displayOrder: 3,
		fieldLabel: 'Conjur Organization',
		required: true,
		inputType: OptionType.InputType.TEXT
	)
	
	this.settings << new OptionType (
		name: 'Conjur clearSecretOnDeletion',
		code: 'conjur-cypher-plugin-clearSecretOnDeletion',
		fieldName: 'conjurPluginClearSecretOnDeletion',
		displayOrder: 4,
		fieldLabel: 'Clear Secret On Deletion',
		inputType: OptionType.InputType.CHECKBOX
	)
	
	}

	/**
	 * Called when a plugin is being removed from the plugin manager (aka Uninstalled)
	 */
	@Override
	void onDestroy() {
		//nothing to do for now
	}
	
	public  String getUrl() {
		def rtn
		def settings = getSettings(this.morpheus, this)
		if (settings.conjurPluginServiceUrl) {
			rtn = settings.conjurPluginServiceUrl
		}
		return rtn
	}

	public String getUsername() {
		def rtn
		def settings = getSettings(this.morpheus, this)
		if (settings.conjurPluginUsername) {
			rtn = settings.conjurPluginUsername
		}
		return rtn
	}

	public String getApiKey() {
		def rtn
		def settings = getSettings(this.morpheus, this)
		if (settings.conjurPluginApiKey) {
			rtn = settings.conjurPluginApiKey
		}
		return rtn
	}

	public String getOrganization() {
		def rtn
		def settings = getSettings(this.morpheus, this)
		if (settings.conjurPluginOrganization) {
			rtn = settings.conjurPluginOrganization
		}
		return rtn
	}

	public String getClearSecretOnDeletion() {
		def rtn
		def settings = getSettings(this.morpheus, this)
		if (settings.conjurPluginClearSecretOnDeletion) {
			rtn = settings.conjurPluginClearSecretOnDeletion
		}
		return rtn
	}

	private getSettings(MorpheusContext morpheusContext, Plugin plugin) {
		def settingsOutput = null
		try {
			def settings = morpheusContext.getSettings(plugin)
			settings.subscribe(
				{ outData -> 
					settingsOutput = outData
				},
				{ error ->
				  log.error("Error subscribing to settings")
				}
			)
		} catch(Exception e) {
			log.error("Error obtaining Conjur plugin settings")
		}
		if (settingsOutput) {
			JsonSlurper slurper = new JsonSlurper()
			return slurper.parseText(settingsOutput)
		} else {
			return [:]
		}
	}
}
