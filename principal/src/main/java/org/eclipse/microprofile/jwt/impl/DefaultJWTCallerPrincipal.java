/*
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.eclipse.microprofile.jwt.impl;

import org.eclipse.microprofile.jwt.principal.JWTCallerPrincipal;

import javax.security.auth.Subject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A default implementation of JWTCallerPrincipal that wraps the Keycloak AccessToken.
 * @see MPAccessToken
 */
public class DefaultJWTCallerPrincipal extends JWTCallerPrincipal {
    private static Set<String> OTHER_CLAIM_NAMES;
    static {
        // Initialize the other claim names to some of the key ones in OIDC/OAuth2 but not MP JWT
        Set<String> tmp = new HashSet<>();
        OTHER_CLAIM_NAMES.add("nbf");
        OTHER_CLAIM_NAMES.add("auth_time");
        OTHER_CLAIM_NAMES.add("azp");
        OTHER_CLAIM_NAMES.add("nonce");
        OTHER_CLAIM_NAMES.add("acr");
        OTHER_CLAIM_NAMES.add("at_hash");
        OTHER_CLAIM_NAMES.add("name");
        OTHER_CLAIM_NAMES.add("given_name");
        OTHER_CLAIM_NAMES.add("family_name");
        OTHER_CLAIM_NAMES.add("email");
        OTHER_CLAIM_NAMES.add("email_verified");
        OTHER_CLAIM_NAMES.add("zoneinfo");
        OTHER_CLAIM_NAMES.add("website");
        // Until this is changed to the MP standard
        OTHER_CLAIM_NAMES.add("preferred_username");
        OTHER_CLAIM_NAMES.add("updated_at");
        OTHER_CLAIM_NAMES = Collections.unmodifiableSet(tmp);
    }
    private MPAccessToken jwt;

    public DefaultJWTCallerPrincipal(MPAccessToken jwt) {
        super(jwt.getOtherClaims().get("unique_username").toString());
        this.jwt = jwt;
    }

    @Override
    public String getRawToken() {
        return jwt.getOtherClaims().get("bearer_token").toString();
    }

    @Override
    public String getIssuer() {
        return jwt.getIssuer();
    }

    @Override
    public String[] getAudience() {
        return jwt.getAudience();
    }

    @Override
    public String getSubject() {
        return jwt.getSubject();
    }

    @Override
    public String getUniqueUsername() {
        return getName();
    }

    @Override
    public String getTokenID() {
        return jwt.getId();
    }

    @Override
    public long getExpirationTime() {
        return jwt.getExpiration();
    }

    @Override
    public long getIssuedAtTime() {
        return jwt.getIssuedAt();
    }

    @Override
    public Set<String> getGroups() {
        HashSet<String> groups = new HashSet<>();
        // First look to the global level
        List<String> globalGroups = (List<String>) jwt.getOtherClaims().get("groups");
        if(globalGroups != null) {
            groups.addAll(globalGroups);
        }
        // Next look at each service in the resource access mapping
        Map<String, MPAccessToken.Access> serviceAccess = jwt.getResourceAccess();
        for(Map.Entry<String, MPAccessToken.Access> service : serviceAccess.entrySet()) {
            List<String> serviceGroups = (List<String>) service.getValue().getOtherClaims().get("groups");
            if(serviceGroups != null) {
                String serviceName = service.getKey();
                for (String group : serviceGroups) {
                    // Add each group with the service name prepended using the SERVICE_NAME_SEPARATOR
                    String serviceGroupName = serviceName + JWTCallerPrincipal.SERVICE_NAME_SEPARATOR + group;
                    groups.add(serviceGroupName);
                }
            }
        }
        return groups;
    }

    @Override
    public Set<String> getRoles() {
        HashSet<String> roles = new HashSet<>();
        // First look to the global level
        List<String> globalRoles = (List<String>) jwt.getOtherClaims().get("roles");
        if(globalRoles != null) {
            roles.addAll(globalRoles);
        }
        // Next look at each service in the resource access mapping
        Map<String, MPAccessToken.Access> serviceAccess = jwt.getResourceAccess();
        for(Map.Entry<String, MPAccessToken.Access> service : serviceAccess.entrySet()) {
            Set<String> serviceRoles = service.getValue().getRoles();
            if(serviceRoles != null) {
                String serviceName = service.getKey();
                for (String role : serviceRoles) {
                    // Add each role with the service name prepended using the SERVICE_NAME_SEPARATOR
                    String serviceRoleName = serviceName + JWTCallerPrincipal.SERVICE_NAME_SEPARATOR + role;
                    roles.add(serviceRoleName);
                }
            }
        }
        return roles;
    }

    /**
     * Access the standard but non-MP mandated claim names this token may have. Note that the token may have even more
     * custom claims avaialable via the {@link #getOtherClaim(String)} method.
     * @return standard but non-MP mandated claim names this token may have.
     */
    @Override
    public Set<String> getOtherClaimNames() {
        return OTHER_CLAIM_NAMES;
    }

    @Override
    public Object getOtherClaim(String claimName) {
        Object claim = null;
        // Try the other claims first
        if(jwt.getOtherClaims().containsKey(claimName)) {
            claim = jwt.getOtherClaims().get(claimName);
        } else {
            // Handle the standard, but non-MP mandated claims
            switch (claimName) {
                case "nbf":
                    claim = jwt.getNotBefore();
                    break;
                case "auth_time":
                    claim = jwt.getAuthTime();
                    break;
                case "azp":
                    claim = jwt.getIssuedFor();
                    break;
                case "nonce":
                    claim = jwt.getNonce();
                    break;
                case "acr":
                    claim = jwt.getAcr();
                    break;
                case "at_hash":
                    claim = jwt.getAccessTokenHash();
                    break;
                case "name":
                    claim = jwt.getName();
                    break;
                case "given_name":
                    claim = jwt.getGivenName();
                    break;
                case "family_name":
                    claim = jwt.getFamilyName();
                    break;
                case "email":
                    claim = jwt.getEmail();
                    break;
                case "email_verified":
                    claim = jwt.getEmailVerified();
                    break;
                case "zoneinfo":
                    claim = jwt.getZoneinfo();
                    break;
                case "website":
                    claim = jwt.getWebsite();
                    break;
                case "preferred_username":
                    claim = jwt.getPreferredUsername();
                    break;
                case "updated_at":
                    claim = jwt.getUpdatedAt();
                    break;
            }
        }
        return claim;
    }

    @Override
    public boolean implies(Subject subject) {
        return false;
    }

    public String toString() {
        return toString(false);
    }
    /**
     * TODO: showAll is ignored and currently assumed true
     * @param showAll - should all claims associated with the JWT be displayed or should only those defined in the
     *                JWTPrincipal interface be displayed.
     * @return JWTCallerPrincipal string view
     */
    @Override
    public String toString(boolean showAll) {
        String toString =  "DefaultJWTCallerPrincipal{" +
                "id='" + jwt.getId() + '\'' +
                ", name='" + jwt.getName() + '\'' +
                ", expiration=" + jwt.getExpiration() +
                ", notBefore=" + jwt.getNotBefore() +
                ", issuedAt=" + jwt.getIssuedAt() +
                ", issuer='" + jwt.getIssuer() + '\'' +
                ", audience=" + Arrays.toString(jwt.getAudience()) +
                ", subject='" + jwt.getSubject() + '\'' +
                ", type='" + jwt.getType() + '\'' +
                ", issuedFor='" + jwt.issuedFor + '\'' +
                ", otherClaims=" + jwt.getOtherClaims() +
                ", authTime=" + jwt.getAuthTime() +
                ", sessionState='" + jwt.getSessionState() + '\'' +
                ", givenName='" + jwt.getGivenName() + '\'' +
                ", familyName='" + jwt.getFamilyName() + '\'' +
                ", middleName='" + jwt.getMiddleName() + '\'' +
                ", nickName='" + jwt.getNickName() + '\'' +
                ", preferredUsername='" + jwt.getPreferredUsername() + '\'' +
                ", email='" + jwt.getEmail() + '\'' +
                ", trustedCertificates=" + jwt.getTrustedCertificates() +
                ", emailVerified=" + jwt.getEmailVerified() +
                ", allowedOrigins=" + jwt.getAllowedOrigins() +
                ", updatedAt=" + jwt.getUpdatedAt() +
                ", acr='" + jwt.getAcr() + '\''
                ;
        StringBuilder tmp = new StringBuilder(toString);
        tmp.append(", realmAccess={");
        if(jwt.getRealmAccess() != null) {
            tmp.append(", roles=");
            tmp.append(jwt.getRealmAccess().getRoles());
            tmp.append(", otherClaims=");
            tmp.append(jwt.getRealmAccess().getOtherClaims());
        }
        tmp.append("}, resourceAccess={");
        for(Map.Entry<String, MPAccessToken.Access> service : jwt.getResourceAccess().entrySet()) {
            tmp.append("{");
            tmp.append(service.getKey());
            tmp.append(", roles=");
            tmp.append(service.getValue().getRoles());
            tmp.append(", otherClaims=");
            tmp.append(service.getValue().getOtherClaims());
            tmp.append(",");
        }
        tmp.setLength(tmp.length()-1);
        tmp.append("}");
        return tmp.toString();
    }

}
