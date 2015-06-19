package org.leleuj;

import org.jooby.Jooby;
import org.jooby.pac4j.Auth;
import org.pac4j.http.profile.HttpProfile;

public class App extends Jooby {

  {

    use(new Auth().form());

    get("/form", req -> "Welcome " + req.require(HttpProfile.class).getId());

  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }
}
