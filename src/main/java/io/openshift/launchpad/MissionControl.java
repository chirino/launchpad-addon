/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package io.openshift.launchpad;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Facade for the Mission Control component
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@Singleton
public class MissionControl
{
   private static final String LAUNCHPAD_MISSIONCONTROL_SERVICE_HOST = "LAUNCHPAD_MISSIONCONTROL_SERVICE_HOST";
   private static final String LAUNCHPAD_MISSIONCONTROL_SERVICE_PORT = "LAUNCHPAD_MISSIONCONTROL_SERVICE_PORT";

   public static final String VALIDATION_MESSAGE_OK = "OK";

   private final URI missionControlValidationURI;
   private final URI missionControlOpenShiftURI;

   @Inject
   public MissionControl()
   {
      this(getEnvVarOrSysProp(LAUNCHPAD_MISSIONCONTROL_SERVICE_HOST, "launchpad-missioncontrol"),
               Integer.parseInt(getEnvVarOrSysProp(LAUNCHPAD_MISSIONCONTROL_SERVICE_PORT, "8080")));
   }

   public MissionControl(String host, int port)
   {
      missionControlValidationURI = UriBuilder.fromPath("/api/validate").host(host).scheme("http")
               .port(port).build();
      missionControlOpenShiftURI = UriBuilder.fromPath("/api/openshift").host(host).scheme("http")
               .port(port).build();
   }

   /**
    * Validates if the OpenShift project exists
    * 
    * @param authHeader
    * @param project
    * @return a validation message, returns {@link #VALIDATION_MESSAGE_OK} if the project does not exist
    */
   public String validateOpenShiftProjectExists(String authHeader, String project, String cluster)
   {
      String validationMessage;
      try
      {
         UriBuilder builder = UriBuilder.fromUri(missionControlValidationURI).path("/project/" + project);
         if (cluster != null)
         {
            builder.queryParam("cluster", cluster);
         }
         URI targetURI = builder.build();
         if (head(targetURI, authHeader) == Response.Status.OK.getStatusCode())
         {
            validationMessage = "OpenShift Project '" + project + "' already exists";
         }
         else
         {
            validationMessage = VALIDATION_MESSAGE_OK;
         }
      }
      catch (Exception e)
      {
         String message = e.getMessage();
         Throwable root = rootException(e);
         if (root instanceof UnknownHostException || root instanceof ConnectException)
         {
            validationMessage = "Mission Control is offline and cannot validate the OpenShift Project Name";
         }
         else
         {
            if (root.getMessage() != null)
            {
               message = root.getMessage();
            }
            validationMessage = "Error while validating OpenShift Project Name: " + message;
         }
      }
      return validationMessage;
   }

   public String validateGitHubRepositoryExists(String authHeader, String repository)
   {
      String validationMessage;
      try
      {
         URI targetURI = UriBuilder.fromUri(missionControlValidationURI).path("/repository/" + repository).build();
         if (head(targetURI, authHeader) == Response.Status.OK.getStatusCode())
         {
            validationMessage = "GitHub Repository '" + repository + "' already exists";
         }
         else
         {
            validationMessage = VALIDATION_MESSAGE_OK;
         }
      }
      catch (Exception e)
      {
         String message = e.getMessage();
         Throwable root = rootException(e);
         if (root instanceof UnknownHostException || root instanceof ConnectException)
         {
            validationMessage = "Mission Control is offline and cannot validate the GitHub Repository Name";
         }
         else
         {
            if (root.getMessage() != null)
            {
               message = root.getMessage();
            }
            validationMessage = "Error while validating GitHub Repository Name: " + message;
         }
      }
      return validationMessage;
   }

   public String validateOpenShiftTokenExists(String authHeader, String cluster)
   {
      String validationMessage;
      try
      {
         UriBuilder builder = UriBuilder.fromUri(missionControlValidationURI).path("/token/openshift");
         if (cluster != null)
         {
            builder.queryParam("cluster", cluster);
         }
         URI targetURI = builder.build();
         if (head(targetURI, authHeader) == Response.Status.OK.getStatusCode())
         {
            validationMessage = VALIDATION_MESSAGE_OK;
         }
         else
         {
            validationMessage = "OpenShift Token does not exist";
         }
      }
      catch (Exception e)
      {
         String message = e.getMessage();
         Throwable root = rootException(e);
         if (root instanceof UnknownHostException || root instanceof ConnectException)
         {
            validationMessage = "Mission Control is offline and cannot validate if the OpenShift token exists";
         }
         else
         {
            if (root.getMessage() != null)
            {
               message = root.getMessage();
            }
            validationMessage = "Error while validating if the OpenShift Token exists: " + message;
         }
      }
      return validationMessage;
   }

   public String validateGitHubTokenExists(String authHeader)
   {
      String validationMessage;
      try
      {
         URI targetURI = UriBuilder.fromUri(missionControlValidationURI).path("/token/github").build();
         if (head(targetURI, authHeader) == Response.Status.OK.getStatusCode())
         {
            validationMessage = VALIDATION_MESSAGE_OK;
         }
         else
         {
            validationMessage = "GitHub Token does not exist";
         }
      }
      catch (Exception e)
      {
         String message = e.getMessage();
         Throwable root = rootException(e);
         if (root instanceof UnknownHostException || root instanceof ConnectException)
         {
            validationMessage = "Mission Control is offline and cannot validate if the GitHub token exists";
         }
         else
         {
            if (root.getMessage() != null)
            {
               message = root.getMessage();
            }
            validationMessage = "Error while validating if the GitHub Token exists: " + message;
         }
      }
      return validationMessage;
   }

   public List<String> getOpenShiftClusters(String authHeader)
   {
      URI targetURI = UriBuilder.fromUri(missionControlOpenShiftURI).path("/clusters").build();
      try {
         return perform(client -> client
                 .target(targetURI)
                 .request(MediaType.APPLICATION_JSON_TYPE)
                 .header(HttpHeaders.AUTHORIZATION, authHeader)
                 .get().readEntity(new GenericType<List<String>>() {
                 }));
      } catch (Exception e) {
         return Collections.emptyList();
      }
   }

   public List<String> getProjects(String authHeader, String cluster)
   {
      UriBuilder builder = UriBuilder.fromUri(missionControlOpenShiftURI).path("/projects");
      if (cluster != null)
      {
         builder.queryParam("cluster", cluster);
      }
      URI targetURI = builder.build();
      return perform(client -> client
               .target(targetURI)
               .request(MediaType.APPLICATION_JSON_TYPE)
               .header(HttpHeaders.AUTHORIZATION, authHeader)
               .get().readEntity(new GenericType<List<String>>()
               {
               }));
   }

   private Throwable rootException(Exception e)
   {
      Throwable root = e;
      while (root.getCause() != null)
      {
         root = root.getCause();
      }
      return root;
   }

   private int head(URI targetURI, String authHeader) throws ProcessingException
   {
      return perform(client -> client.target(targetURI).request()
               .header(HttpHeaders.AUTHORIZATION, authHeader)
               .head().getStatus());
   }

   private static String getEnvVarOrSysProp(String name, String defaultValue)
   {
      return System.getProperty(name, System.getenv().getOrDefault(name, defaultValue));
   }

   private <T> T perform(Function<Client, T> request)
   {
      Client client = null;
      try
      {
         client = ClientBuilder.newClient();
         return request.apply(client);
      }
      finally
      {
         if (client != null)
         {
            client.close();
         }
      }
   }

}