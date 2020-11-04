# spring-boot-openapi-kong

[![Build Status](https://github.com/jonashackt/spring-boot-openapi-kong/workflows/build/badge.svg)](https://github.com/jonashackt/spring-boot-openapi-kong/actions)
[![License](http://img.shields.io/:license-mit-blue.svg)](https://github.com/jonashackt/spring-boot-buildpack/blob/master/LICENSE)


Example project showing how to integrate Spring Boot microservices with Kong API Gateway


Bringing together Kong & Spring Boot. But wait, what is https://github.com/Kong/kong ?

> Kong is a cloud-native, fast, scalable, and distributed Microservice Abstraction Layer (also known as an API Gateway or API Middleware). 


### Step by step...

Some microservices to access with Kong... I once worked heavily with the Spring Cloud Netflix tooling.

Here's the example project: https://github.com/jonashackt/cxf-spring-cloud-netflix-docker and the blog post I wrote back then https://blog.codecentric.de/en/2017/05/ansible-docker-windows-containers-scaling-spring-cloud-netflix-docker-compose

The goal is to rebuild the project using Kong https://github.com/Kong/kong


### Idea & Setup

Konga GUI

docker-compose

Integration: OpenAPI - Spring - Kong - Insomnia Designer



agnostical! more pattern like

Setup idea: Spring Boot REST / WebFlux --> [generate OpenAPI spec yamls via springdoc-openapi-maven-plugin](https://www.baeldung.com/spring-rest-openapi-documentation) --> Insomnia config file with [Kong Bundle plugin](https://insomnia.rest/plugins/insomnia-plugin-kong-bundle/) --> import into Kong and run via decK (normal Kong gateway without EE)

Nothing really there right now:  https://www.google.com/search?q=openapi+spring+boot+kong

* No change in Spring Boot dev workflow required, no custom annotations
* elegant integration of Kong and Spring Boot services

PLUS: CI process to regularly generate OpenAPI specs from Spring code -> and automatically import into Kong, which is an enterprise feature - or it is possible via:

Insomnia Inso CLI (https://support.insomnia.rest/collection/105-inso-cli)

See "From OpenAPI spec to configuration as code" in https://blog.codecentric.de/en/2020/09/offloading-and-more-from-reedelk-data-integration-services-through-kong-enterprise/


Next:
(backwards: OpenAPI --> Kong --> code)





### Create a Spring Boot App with REST endpoints

This is the easy part. We all know where to start: Go to start.spring.io and create a Spring REST app skeleton.

As I wanted to rebuild my good old Spring Cloud Netflix / Eureka based apps, I simply too the `weatherbackend` app from https://github.com/jonashackt/cxf-spring-cloud-netflix-docker/tree/master/weatherbackend

But why didn't I go with a reactive WebFlux based app? 


##### The current problem with springdoc-openapi and WebFlux based Spring Boot apps

WebFlux based Spring Boot Apps need some `springdoc-openapi` specific classes right now in order to fully generate the OpenAPI live documentation in the end. See the demos at

https://github.com/springdoc/springdoc-openapi-demos

And especially the webflux functional demo at: https://github.com/springdoc/springdoc-openapi-demos/blob/master/springdoc-openapi-spring-boot-2-webflux-functional where these imports are used:

```java
import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;
```

I can fully discourage to go with this approach, but for this project I wanted a 100% "springdoc-free" standard Spring Boot app, where the springdoc feature are __ONLY__ used to generate OpenAPI specs - and not rely onto some dependencies from springdoc. Since that would imply that every Spring Boot project that wanted to adopt the solution outlined here would need to integrate springdoc classes in their projects.



### Generate an OpenAPI spec with the springdoc-openapi-maven-plugin

See the docs at https://github.com/springdoc/springdoc-openapi-maven-plugin on how to use the springdoc-openapi-maven-plugin.

> The aim of springdoc-openapi-maven-plugin is to generate json and yaml OpenAPI description during build time. The plugin works during integration-tests phase, and generates the OpenAPI description. The plugin works in conjunction with spring-boot-maven plugin.

But in order to successfully run the springdoc-openapi-maven-plugin, we need to add the [springdoc-openapi-ui](https://github.com/springdoc/springdoc-openapi) plugin (for Tomcat / Spring MVC based apps) or the [springdoc-openapi-webflux-ui](https://github.com/springdoc/springdoc-openapi#spring-webflux-support-with-annotated-controllers) plugin (for Reactive WebFlux / Netty based apps) to our [hellobackend/pom.xml](hellobackend/pom.xml):

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-webflux-ui</artifactId>
    <version>1.4.8</version>
</dependency>
```

Otherwise the `springdoc-openapi-maven-plugin` will run into errors like this (as described [in this so answer](https://stackoverflow.com/a/64677754/4964553)):

```
[INFO] --- springdoc-openapi-maven-plugin:1.1:generate (default) @ hellobackend ---
[ERROR] An error has occured: Response code 404
[INFO]
[INFO] --- spring-boot-maven-plugin:2.3.5.RELEASE:stop (post-integration-test) @ hellobackend ---
[INFO] Stopping application...
2020-11-04 10:18:36.851  INFO 42036 --- [on(4)-127.0.0.1] inMXBeanRegistrar$SpringApplicationAdmin : Application shutdown requested.
```

As a sidenote: if you fire up your Spring Boot app from here with `mvn spring-boot:run`, you can access the live API documentation already at http://localhost:8080/swagger-ui.html

![openapi-swagger-ui](screenshots/openapi-swagger-ui.png)

Now we can add the `springdoc-openapi-maven-plugin` to our [hellobackend/pom.xml](hellobackend/pom.xml):

```xml
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<executions>
					<execution>
						<id>pre-integration-test</id>
						<goals>
							<goal>start</goal>
						</goals>
					</execution>
					<execution>
						<id>post-integration-test</id>
						<goals>
							<goal>stop</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
			<plugin>
				<groupId>org.springdoc</groupId>
				<artifactId>springdoc-openapi-maven-plugin</artifactId>
				<version>1.1</version>
				<executions>
					<execution>
						<phase>integration-test</phase>
						<goals>
							<goal>generate</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
``` 

As you see we also need to tell the `spring-boot-maven-plugin` to start and stop the integration test phases.

In order to generate the Open API spec we need to execute Maven with:

```
mvn verify
```

The output should contain something like that:

```
...
[INFO] --- springdoc-openapi-maven-plugin:1.1:generate (default) @ hellobackend ---
2020-11-04 10:26:09.579  INFO 42143 --- [ctor-http-nio-2] o.springdoc.api.AbstractOpenApiResource  : Init duration for springdoc-openapi is: 29 ms
...
```

This indicates that the OpenAPI spec generation was successful. Therefore we need to have a look into the `hellobackend/target` directory, where a file called [openapi.json](hellobackend/target/openapi.json) should be present now (you may need to reformat the code inside you IDE to not look into a one-liner ;) ):

```json
{
  "openapi": "3.0.1",
  "info": {
    "title": "OpenAPI definition",
    "version": "v0"
  },
  "servers": [
    {
      "url": "http://localhost:8080",
      "description": "Generated server url"
    }
  ],
  "paths": {
    "/weather/general/outlook": {
      "get": {
        "tags": [
          "weather-backend-controller"
        ],
        "operationId": "infoAboutGeneralOutlook",
        "responses": {
          "200": {
            "description": "OK",
            "content": {
              "application/json": {
                "schema": {
                  "type": "string"
                }
              }
            }
          }
        }
      }
}}}
```



##### Install Insomnia Desinger with Kong Bundle plugin

On a Mac simply (or have a look at https://insomnia.rest):

```
brew cask install insomnia-designer
```

Then go to https://insomnia.rest/plugins/insomnia-plugin-kong-bundle and click on `Install in Designer` & open the request in Insomnia Desinger:

![insomnia-designer-kong-bundle-plugin](screenshots/insomnia-designer-kong-bundle-plugin.png)





## Links

https://blog.codecentric.de/en/2017/11/api-management-kong/

https://docs.konghq.com/hub/


#### Spring & OpenAPI

https://github.com/springdoc/springdoc-openapi-maven-plugin

https://stackoverflow.com/questions/59616165/what-is-the-function-of-springdoc-openapi-maven-plugin-configuration-apidocsurl


#### Insomnia (Core) & Insomia Designer

> Insomnia (Core) is a graphical REST client - just like postman

https://blog.codecentric.de/2020/02/testen-und-debuggen-mit-insomnia/

Since 2019 Insomnia (Core) is part of Kong - and is the basis for Kong Studio (Enterprise)

https://github.com/Kong/insomnia


> Insomnia Designer is a OpenAPI / Swagger Desktop App

https://blog.codecentric.de/en/2020/06/introduction-to-insomnia-designer/

> you can preview your specification using Swagger UI

https://medium.com/@rmharrison/an-honest-review-of-insomnia-designer-and-insomnia-core-62e24a447ce


With https://insomnia.rest/plugins/insomnia-plugin-kong-bundle/ you can deploy API definitions into Kong API gateway

To see the integration in action, have a look on https://blog.codecentric.de/en/2020/09/offloading-and-more-from-reedelk-data-integration-services-through-kong-enterprise/ 

https://github.com/codecentric/reedelk-bookingintegrationservice




#### decK

> declarative configuration and drift detection for Kong

https://blog.codecentric.de/en/2019/12/kong-api-gateway-declarative-configuration-using-deck-and-visualizations-with-konga/

https://github.com/Kong/deck



#### Deployment incl. Konga

Official Kong docker-compose template: https://github.com/Kong/docker-kong/blob/master/compose/docker-compose.yml

https://blog.codecentric.de/en/2019/09/api-management-kong-update/

https://github.com/danielkocot/kong-blogposts/blob/master/docker-compose.yml


First question: What is this `kong-migration` Docker container about? [Daniel answers it](https://blog.codecentric.de/en/2019/09/api-management-kong-update/):

> the kong-migration service is used for the initial generation of the objects in the kong-database. Unfortunately, the configuration of the database is not managed by the kong service.



Idea: Database-less deployment possible for showcases?



> Konga: graphical dashboard

https://github.com/pantsel/konga#running-konga

https://blog.codecentric.de/en/2019/12/kong-api-gateway-declarative-configuration-using-deck-and-visualizations-with-konga/

Konga with it's own DB: https://github.com/asyrjasalo/kongpose/blob/master/docker-compose.yml

Konga on the same DB as Kong: https://github.com/abrahamjoc/docker-compose-kong-konga/blob/master/docker-compose.yml

