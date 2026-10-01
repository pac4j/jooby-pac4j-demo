<p align="center">
  <img src="https://pac4j.github.io/pac4j/img/logo-jooby.png" width="300" />
</p>

> This demo secures a Jooby application with **[jooby-pac4j](https://jooby.io/modules/pac4j)**, the Jooby implementation of **[pac4j](https://github.com/pac4j/pac4j)**, the security engine for Java.
> If it is useful to you, please ⭐ **[star pac4j on GitHub](https://github.com/pac4j/pac4j)**: it helps other developers discover it!

A minimal Jooby (Netty) demo showcasing authentication with jooby-pac4j and pac4j:
- Form login (FormClient)
- Indirect Basic Auth (IndirectBasicAuthClient)
- CAS (CasClient)

It uses Jooby v4.x (`jooby-pac4j`) and pac4j v6.x.

## Prerequisites
- JDK 21+
- Maven 3.8+
- curl (for the CAS test script)

## Build
```bash
mvn -q clean package
```

## Run
- Quick launcher (builds and starts the demo):
```bash
./run.sh
```
- Or run the fat JAR directly:
```bash
java -jar target/jooby-pac4j-demo-*.jar
```
The server starts on http://localhost:8080

## Endpoints
- / — Home page with links
- /form/index — Protected by FormClient (use username = password)
- /basicauth/index — Protected by Indirect Basic Auth (use username = password)
- /cas/index — Protected by CAS (redirects to the demo CAS server)
- /logout — Local logout via pac4j
