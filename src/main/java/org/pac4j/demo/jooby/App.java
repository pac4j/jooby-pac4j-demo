package org.pac4j.demo.jooby;

import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.SessionStore;
import io.jooby.pac4j.Pac4jModule;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.core.http.url.DefaultUrlResolver;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;

public class App extends Jooby {

    {
        // pac4j stores the user profile in the web session
        setSessionStore(SessionStore.memory(Cookie.session("jooby.sid")));

        // Not protected
        get("/", ctx -> html(ctx, "<h1>Jooby pac4j Demo</h1>"
                + "<ul>"
                + "<li><a href=\"/form/index\">Protected by FormClient</a> (use login = password)</li>"
                + "<li><a href=\"/basicauth/index\">Protected by Indirect Basic Auth</a> (use login = password)</li>"
                + "<li><a href=\"/cas/index\">Protected by CAS</a> (use CAS test account)</li>"
                + "<li><a href=\"/logout\">Logout</a></li>"
                + "</ul>"
                + "<p>Profile: " + ctx.getUser() + "</p>"));

        get("/loginForm", ctx -> html(ctx, "<h2>Login Form (FormClient)</h2>"
                + "<form method=\"post\" action=\"/callback?client_name=FormClient\">"
                + "<input type=\"text\" name=\"username\" placeholder=\"username\"/>"
                + "<br/><input type=\"password\" name=\"password\" placeholder=\"password\"/>"
                + "<br/><input type=\"submit\" value=\"Login\"/>"
                + "</form>"
                + "<p><a href=\"/\">Home</a></p>"));

        // pac4j: callback (/callback) and logout (/logout) endpoints are added by the module,
        // the routes defined after it are protected according to their path
        final var authenticator = new SimpleTestUsernamePasswordAuthenticator();
        install(new Pac4jModule()
                .client("/form/*", conf -> new FormClient("/loginForm", authenticator))
                .client("/basicauth/*", conf -> new IndirectBasicAuthClient(authenticator))
                .client("/cas/*", conf -> {
                    final var casClient = new CasClient(new CasConfiguration("https://www.casserverpac4j.dev/login"));
                    // the default Jooby URL resolver would rewrite the CAS server URL with the local host
                    casClient.setUrlResolver(new DefaultUrlResolver(true));
                    return casClient;
                }));

        // Protected
        get("/form/index", ctx -> protectedPage(ctx, "Form Protected"));
        get("/basicauth/index", ctx -> protectedPage(ctx, "Indirect Basic Auth Protected"));
        get("/cas/index", ctx -> protectedPage(ctx, "CAS Protected"));
    }

    private static String protectedPage(final Context ctx, final String title) {
        final UserProfile profile = ctx.getUser();
        return html(ctx, "<h2>" + title + "</h2>"
                + "<p>Authenticated as: " + profile.getId() + "</p>"
                + "<p>Profile: " + profile + "</p>"
                + "<p><a href=\"/\">Home</a> | <a href=\"/logout\">Logout</a></p>");
    }

    private static String html(final Context ctx, final String body) {
        ctx.setResponseType(MediaType.html);
        return "<html><head><title>Jooby pac4j Demo</title></head><body>" + body + "</body></html>";
    }

    public static void main(final String[] args) {
        runApp(args, App::new);
    }
}
