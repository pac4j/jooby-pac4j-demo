package org.pac4j.demo.jooby;

import java.io.File;
import java.util.Optional;

import org.jooby.*;
import org.jooby.hbs.Hbs;
import org.jooby.pac4j.Auth;
import org.jooby.pac4j.AuthStore;
import org.pac4j.cas.client.CasClient;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.direct.ParameterClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.http.profile.HttpProfile;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.jwt.profile.JwtGenerator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.StravaClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.client.SAML2ClientConfiguration;

public class App extends Jooby {

  {

    /** Template engine (just to do a better UI). */
    use(new Hbs());

    get("*", (req, rsp) -> {
      boolean loggedIn = req.session().get(Auth.ID).toOptional().isPresent();
      req.set("loggedIn", loggedIn);
    });

    /** Home page. */
    get("/", req -> {
      return Results.html("index");
    });

    // generate token
    get("/generate-token", req -> {
      UserProfile profile = getUserProfile(req);
      // TODO: how can I access the config here?
      JwtGenerator jwtGenerator = new JwtGenerator("12345678901234567890123456789012");
      String token = jwtGenerator.generate(profile);
      return Results.html("index").put("token", token);
    });

    /**
     * Configure all the pac4j clients
     */
    use(new Auth()
        /** OpenID Connect . */
        .client("/oidc/**", conf -> {
          OidcClient oidcClient = new OidcClient();
          oidcClient.setClientID(conf.getString("oidc.clientID"));
          oidcClient.setSecret(conf.getString("oidc.secret"));
          oidcClient.setDiscoveryURI(conf.getString("oidc.discoveryURI"));
          oidcClient.addCustomParam("prompt", "consent");
          return oidcClient;
        })
        /** Saml. */
        .client("/saml2/**", conf -> {
          final SAML2ClientConfiguration cfg = new SAML2ClientConfiguration(
              conf.getString("saml.keystore"),
              conf.getString("saml.keystorePass"),
              conf.getString("saml.privateKeyPass"),
              conf.getString("saml.identityProviderMetadataPath"));
          cfg.setMaximumAuthenticationLifetime(3600);
          cfg.setServiceProviderEntityId(conf.getString("saml.serviceProviderEntityID"));
          cfg.setServiceProviderMetadataPath(
              new File("target", "sp-metadata.xml").getAbsolutePath());
          return new SAML2Client(cfg);
        })
        /** Facebook. */
        .client("/facebook/**", conf -> {
          return new FacebookClient(conf.getString("fb.key"), conf.getString("fb.secret"));
        })
        /** Twitter. */
        .client("/twitter/**", conf -> {
          return new TwitterClient(conf.getString("twitter.key"), conf.getString("twitter.secret"));
        })
        /** Form. */
        .form("/form/**")
        /** Basic. */
        .basic("/basic/**")
        /** CAS. */
        .client("/cas/**", conf -> {
          final CasClient client = new CasClient();
          client.setCasLoginUrl(conf.getString("cas.loginURL"));
          return client;
        })
        /** Strave. */
        .client("/strava/**", conf -> {
          final StravaClient client = new StravaClient();
          client.setApprovalPrompt(conf.getString("strava.approvalPrompt"));
          client.setKey(conf.getString("strava.key"));
          client.setSecret(conf.getString("strava.secret"));
          client.setScope(conf.getString("strava.scope"));
          return client;
        })
        /** REST authent with JWT for a token passed in the url as the token parameter. */
        .client("/rest-jwt/**", conf -> {
          ParameterClient client = new ParameterClient("token",
              new JwtAuthenticator(conf.getString("jwt.salt")));
          client.setSupportGetRequest(true);
          client.setSupportPostRequest(false);
          return client;
        })
        .client("/direct/**",
            new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator()))
        .authorizer("admin", "/form/admin/**", (ctx, profile) -> {
          if (!(profile instanceof HttpProfile)) {
            return false;
          }
          final HttpProfile httpProfile = (HttpProfile) profile;
          final String username = httpProfile.getUsername();
          return username.startsWith("jle");
        }));

    /** One handler for logged user. */
    @SuppressWarnings("unchecked")
    Route.OneArgHandler handler = req -> {
      UserProfile profile = getUserProfile(req);

      return Results.html("profile")
          .put("client", profile.getClass().getSimpleName().replace("Profile", ""))
          .put("profile", profile);
    };

    get("/profile", handler);

    get("/oidc", handler);

    get("/saml2", handler);

    get("/facebook", handler);

    get("/twitter", handler);

    get("/form", handler);

    get("/form/admin", handler);

    get("/basic", handler);

    get("/cas", handler);

    get("/strava", handler);

    get("/rest-jwt", handler);

    get("/direct", handler);

    get("/generate-token", handler);
  }

  private UserProfile getUserProfile(final Request req) throws Exception {
    // show profile or 401
    Optional<String> profileId = req.session().get(Auth.ID).toOptional();
    if (!profileId.isPresent()) {
      throw new Err(Status.UNAUTHORIZED);
    }
    AuthStore<UserProfile> store = req.require(AuthStore.class);
    return store.get(profileId.get()).get();
  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }
}
