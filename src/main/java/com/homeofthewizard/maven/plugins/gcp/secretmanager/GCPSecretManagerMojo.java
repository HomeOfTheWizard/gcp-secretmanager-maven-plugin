package com.homeofthewizard.maven.plugins.gcp.secretmanager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.homeofthewizard.maven.plugins.gcp.secretmanager.config.ComplexMapping;
import com.homeofthewizard.maven.plugins.gcp.secretmanager.config.Mapping;
import com.homeofthewizard.maven.plugins.gcp.secretmanager.config.OutputMethod;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.apache.commons.compress.java.util.jar.Pack200.Packer.LATEST;

@Mojo(name = "pull", defaultPhase = LifecyclePhase.COMPILE)
public class GCPSecretManagerMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "projectId", required = true)
    protected String projectId;

    @Parameter(property = "mappings", required = true)
    protected List<Mapping> mappings;

    @Parameter(property = "complexMappings")
    protected List<ComplexMapping> complexMappings;

    @Parameter(defaultValue = "MavenProperties", property = "vault.outputMethod")
    protected OutputMethod outputMethod;

    @Parameter(property = "vault.existingStorePath")
    protected String existingStorePath;

    @Parameter(property = "vault.storePassword")
    protected String storePassword;

    @Parameter(property = "vault.storeType")
    protected String storeType;

    @Parameter(defaultValue = "false", property = "gcp.secretmanager.skipExecution")
    protected boolean skipExecution;

    public static final ObjectMapper mapper = new ObjectMapper();


    @Override
    public void execute() {
        getLog().info(String.format("Fetching %d secrets from SecretManager for Project [%s]", mappings.size(), projectId));
        Map<String, String> retrievedSecrets = loadSecrets(mappings.stream().map(Mapping::getKey).toList());
        for (Mapping mapping : mappings) {
            if (!retrievedSecrets.containsKey(mapping.getKey())) {
                String message = String.format("No value found in project %s for key %s", projectId, mapping.getKey());
                throw new NoSuchElementException(message);
            }
            getLog().info(String.format("Flushing secrets to [%s]", outputMethod));
            outputMethod.flush(this.project.getProperties(), retrievedSecrets, mapping, storePassword, existingStorePath, storeType);
        }

        Map<String, String> retrievedComplexSecrets = loadSecrets(complexMappings.stream().map(ComplexMapping::getKey).toList());
        for (ComplexMapping mapping : complexMappings) {
            if (!retrievedComplexSecrets.containsKey(mapping.getKey())) {
                String message = String.format("No value found in project %s for complex mapping key %s", projectId, mapping.getKey());
                throw new NoSuchElementException(message);
            }
            getLog().info(String.format("Flushing complex secrets to [%s]", outputMethod));
            var secretContent = retrievedComplexSecrets.get(mapping.getKey());

            try {
                var subSecretKeys = mapper.readValue(secretContent, new TypeReference<HashMap<String,String>>() {});
                for(Mapping m : mapping.getMappings()){
                    if (!subSecretKeys.containsKey(m.getKey())) {
                        String message = String.format("No value found in complex secret key %s, for subkey %s", mapping.getKey(), m.getKey());
                        throw new NoSuchElementException(message);
                    }
                    outputMethod.flush(this.project.getProperties(), subSecretKeys, m, storePassword, existingStorePath, storeType);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
    * Helper method to load the actual secrets
    *
    * @param keys of secrets to load
    * @return Map of key value pairs for secrets
    */
    private Map<String, String> loadSecrets(List<String> keys) {
        getLog().info("Connecting to GCP");
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            return keys.stream().collect(Collectors.toMap(k -> k, k -> client.accessSecretVersion(SecretVersionName.of(projectId, k, LATEST)).getPayload().getData().toStringUtf8()));
        } catch (IOException e) {
            getLog().warn(String.format("Error retrieving value from secret manager: %s", e.getMessage()));
            return Maps.newHashMap();
        }
    }
}
