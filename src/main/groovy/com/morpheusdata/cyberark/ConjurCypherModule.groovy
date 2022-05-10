package com.morpheusdata.cyberark

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.morpheusdata.cypher.Cypher;
import com.morpheusdata.cypher.CypherMeta;
import com.morpheusdata.cypher.CypherModule;
import com.morpheusdata.cypher.CypherObject
import com.morpheusdata.cypher.util.RestApiUtil
import com.morpheusdata.cypher.util.ServiceResponse
import groovy.util.logging.Slf4j;

@Slf4j
class ConjurCypherModule implements CypherModule {

    Cypher cypher;
    @Override
    public void setCypher(Cypher cypher) {
        this.cypher = cypher;
    }

    @Override
    public CypherObject write(String relativeKey, String path, String value, Long leaseTimeout, String leaseObjectRef, String createdBy) {
        if(value != null && value.length() > 0) {
            String key = relativeKey;
            if(path != null) {
                key = path + "/" + key;
            }
            if(relativeKey.startsWith("config/")) {
                System.out.println("Writing to : " + key);
                return new CypherObject(key,value,0l, leaseObjectRef, createdBy);
            } else {
                String conjurUrl = cypher.read("conjur/config/url").value;
                String conjurUsername = cypher.read("conjur/config/username").value;
                String conjurApiKey = cypher.read("conjur/config/apiKey").value;
                String conjurOrg = cypher.read("conjur/config/org").value;
                String conjurToken = getAuthToken(conjurUrl,conjurOrg,conjurUsername,conjurApiKey)
                //we gotta fetch from conjur
                String conjurPath="/secrets/${conjurOrg}/variable/" + relativeKey

                RestApiUtil.RestOptions restOptions = new RestApiUtil.RestOptions();
                restOptions.headers = new LinkedHashMap<>();
                restOptions.headers.put("Authorization",conjurToken);

                restOptions.body = value
//                restOptions.body =
                try {
                    ServiceResponse resp = RestApiUtil.callApi(conjurUrl,conjurPath,null,null,restOptions,"POST");
                    if(resp.getSuccess()) {
                        return new CypherObject(key,value,leaseTimeout, leaseObjectRef, createdBy);
                    } else {
                        return null;
                    }
                } catch(Exception ex) {
                    return null;
                }
            }

        } else {
            return null; //we dont write no value to a key
        }
    }

    @Override
    public CypherObject read(String relativeKey, String path, Long leaseTimeout, String leaseObjectRef, String createdBy) {
        String key = relativeKey;
        if(path != null) {
            key = path + "/" + key;
        }
        if(relativeKey.startsWith("config/")) {
            return null;
        } else {
            String conjurUrl = cypher.read("conjur/config/url").value;
            String conjurUsername = cypher.read("conjur/config/username").value;
            String conjurApiKey = cypher.read("conjur/config/apiKey").value;
            String conjurOrg = cypher.read("conjur/config/org").value;
            String conjurToken = getAuthToken(conjurUrl,conjurOrg,conjurUsername,conjurApiKey)
            //we gotta fetch from conjur
            String conjurPath="/secrets/${conjurOrg}/variable/" + relativeKey
            RestApiUtil.RestOptions restOptions = new RestApiUtil.RestOptions();
            restOptions.headers = new LinkedHashMap<>();
            restOptions.headers.put("Authorization",conjurToken);
            try {
                ServiceResponse resp = RestApiUtil.callApi(conjurUrl,conjurPath,null,null,restOptions,"GET");
                if(resp.getSuccess()) {
                    ObjectMapper mapper = new ObjectMapper();

                    CypherObject conjurResult = new CypherObject(key,resp.getContent(),leaseTimeout,leaseObjectRef, createdBy);
                    conjurResult.shouldPersist = false;
                    return conjurResult;
                } else {
                    log.error("Error Fetching cypher key: ${resp}")
                    return null;//throw exception?
                }
            } catch(Exception ex) {
                ex.printStackTrace();
                return null;
            }

        }
    }

    @Override
    public boolean delete(String relativeKey, String path, CypherObject object) {
        if(relativeKey.startsWith("config/")) {
            return true;
        } else {
            String conjurUrl = cypher.read("conjur/config/url").value;
            String conjurUsername = cypher.read("conjur/config/username").value;
            String conjurApiKey = cypher.read("conjur/config/apiKey").value;
            String conjurOrg = cypher.read("conjur/config/org").value;
            String conjurToken = getAuthToken(conjurUrl,conjurOrg,conjurUsername,conjurApiKey)
            //we gotta fetch from conjur
            String conjurPath="/secrets/${conjurOrg}/variable/" + relativeKey
            //TODO: HTTP Client time
            RestApiUtil.RestOptions restOptions = new RestApiUtil.RestOptions();
            restOptions.headers = new LinkedHashMap<>();
            restOptions.headers.put("Authorization",conjurToken);
            restOptions.body = ''
            try {
                RestApiUtil.callApi(conjurUrl,conjurPath,null,null,restOptions,"POST");
            } catch(Exception ex) {

            }
            return true;
        }
    }

    protected getAuthToken(String conjurUrl, String conjurOrg, String conjurUsername, String conjurApiKey) {
        RestApiUtil.RestOptions restOptions = new RestApiUtil.RestOptions();
        restOptions.headers = new LinkedHashMap<>();
        restOptions.headers.put("Accept-Encoding",'base64');
        restOptions.body = conjurApiKey
        ServiceResponse resp = RestApiUtil.callApi(conjurUrl,"authn/${conjurOrg}/${conjurUsername}/authenticate",null,null,restOptions,"POST");
        if(resp.getSuccess()) {
            return "Token token=\"${resp.getContent()}\""
        } else {
            return null;//throw exception?
        }

    }


    @Override
    public String getUsage() {
        StringBuilder usage = new StringBuilder();

        usage.append("This allows secret data to be fetched from a CyberArk Conjur integration. This can be configured in the conjur/config key setup");

        return usage.toString();
    }

    @Override
    public String getHTMLUsage() {
        return null;
    }
}
