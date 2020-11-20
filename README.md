### Introduction

This application leverages [Spring Webflux](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/web-reactive.html#webflux) with JDBC and Flyway to deliver a reactive URL shortener microservice. 

To run the application with default settings, you must have a Postgres database named mydb on the host machine, with CREATE, UPDATE, SELECT, and INSERT permissions set for a username "user"/password "password" on the PUBLIC schema. 
__Changing these defaults, as discussed below, is highly recommended.__

The server responds to two types of requests:

- PUT requests at {root or configurable path}/
- GET requests at {root or configurable path}/{extension}

PUT requests accept input as a string. This string should represent a valid URL. If URL input is not absolute, the server will attempt to resolve it under https protocol. If the URL leads to a redirect, the server will follow all redirects and store the final URL. 

If the URL input has not been stored previously, the server will respond with a 201 Created status code. The shortened extension will be send under the Location header. If the URL already exists in the database, the server will respond with a 200 status code, and the extension will be sent in the body of the response. 


GET requests are redirected to the stored URL. If the extension is not found in the database, the server will respond with a 404 Not Found error. 

### OAuth2 Support

OAuth2 support is offered through Google and Github. For convenience, OAuth2 accounts connected to the same email address are considered the same account. 

Logged in users may visit extension /user to request a list of URLs they have submitted, formatted as:
`[URLRecord(extension=fBFgGOFq, url=http://www.google.com, hits=0)]`

##### OAuth2 Configuration 

The OAuth2 configuration file is located at shorturl/src/resources/oauthconfig.properties. To use OAuth2, simply add your client-id and client-secret from each provider in the appropriate place. 

### Configuration

All configuration options listed below are found in the application.properties file at shorturl/src/resources/application.properties.

##### Path Configuration

- webconfig.put-path - sets the path for PUT requests (default is /)
- webconfig.get-path - sets the path for GET requests (default is /)

##### Extension Configuration

Extensions are generated via JNanoid. The creators of the original Nanoid node.js module provided this [tool to calculate collision risk based on length of string and desired range of valid characters.](https://zelark.github.io/nano-id-cc/)
 See the [JNanoid README for limitations.](https://github.com/aventrix/jnanoid) 
 
- jnanoid.length - sets length of generated extensions (default is 8)
- jnanoid.chars  - sets list of accepted characters to be used when generating IDs. (default is _-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ)*

Note that adding reserved or forbidden URL characters to jnanoid.chars will cause the build to fail. 

##### PostgresQL Configuration

Default settings:

- logging.level.org.springframework.data.jdbc - sets the logging level of the database client (default is DEBUG) 
- spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
- spring.datasource.username=user
- spring.datasource.password=password
- spring.datasource.driver-class-name=org.postgresql.Driver
- spring.datasource.initialization-mode=always

##### Flyway Migration Configuration

Default settings: 
- spring.flyway.user=user
- spring.flyway.password=password
- spring.flyway.schemas=public
- spring.flyway.url=jdbc:postgresql://localhost/mydb

spring.flyway.url must match spring.datasource.url. 
