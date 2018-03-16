<p align="center">
  <img src="https://pac4j.github.io/pac4j/img/logo-jooby.png" width="300" />
</p>

**Warning**: this demo uses the deprecated `jooby-pac4j` module. See the new demo: [https://github.com/jooby-project/pac4j-starter](https://github.com/jooby-project/pac4j-starter) based on the new module: `jooby-pac4j2`.

This ```jooby-pac4j-demo``` project is a Java web application to test the [jooby-pac4j](http://jooby.org/doc/pac4j) security library with various authentication mechanisms: Facebook, Twitter, form, basic auth, CAS, SAML, OpenID Connect, JWT...

## Start & test

Build the project and launch the web app with [Jooby](http://jooby.org) on [http://localhost:8080](http://localhost:8080):

    cd jooby-pac4j-demo
    mvn jooby:run
