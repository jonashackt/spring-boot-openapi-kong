# spring-boot-openapi-kong

[![Build Status](https://github.com/jonashackt/spring-boot-openapi-kong/workflows/build/badge.svg)](https://github.com/jonashackt/spring-boot-openapi-kong/actions)
[![License](http://img.shields.io/:license-mit-blue.svg)](https://github.com/jonashackt/spring-boot-buildpack/blob/master/LICENSE)
[![renovateenabled](https://img.shields.io/badge/renovate-enabled-yellow)](https://renovatebot.com)
[![versionspringboot](https://img.shields.io/badge/dynamic/xml?color=brightgreen&url=https://raw.githubusercontent.com/jonashackt/spring-boot-openapi-kong/main/weatherbackend/pom.xml&query=%2F%2A%5Blocal-name%28%29%3D%27project%27%5D%2F%2A%5Blocal-name%28%29%3D%27parent%27%5D%2F%2A%5Blocal-name%28%29%3D%27version%27%5D&label=springboot)](https://github.com/spring-projects/spring-boot)


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

This indicates that the OpenAPI spec generation was successful. Therefore we need to have a look into the `weatherbackend/target` directory, where a file called [openapi.json](weatherbackend/target/openapi.json) should be present now (you may need to reformat the code inside you IDE to not look into a one-liner ;) ):

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


### Import OpenAPI spec into Kong

First we start the manual process in order to test drive our solution.

##### Install Insomnia Desinger with Kong Bundle plugin

On a Mac simply use brew (or have a look at https://insomnia.rest):

```
brew cask install insomnia-designer
```

Then go to https://insomnia.rest/plugins/insomnia-plugin-kong-bundle and click on `Install in Designer` & open the request in Insomnia Desinger:

![insomnia-designer-kong-bundle-plugin](screenshots/insomnia-designer-kong-bundle-plugin.png)


### Import Springdoc generated openapi.json into Insomnia Designer

Now let's try to import the generated [openapi.json](weatherbackend/target/openapi.json) into our Insomnia Designer by clicking on `Create` and then `Import from / File`:

![insomnia-designer-import-openapi-json](screenshots/insomnia-designer-import-openapi-json.png)

That was easy :) Now you can already interact with your API through Insomnia.



### Generate Kong Declarative Config from Openapi

The next step is to generate the Kong configuration from the OpenAPI specification. Therefore we need to click on the `Generate Config` button:

![insomnia-designer-kong-declarative-config](screenshots/insomnia-designer-kong-declarative-config.png)

And voilà we have our Kong declarative configuration ready:

```yaml
_format_version: "1.1"
services:
  - name: OpenAPI_definition
    url: http://localhost:8080
    plugins: []
    routes:
      - tags:
          - OAS3_import
        name: OpenAPI_definition-path-get
        methods:
          - GET
        paths:
          - /weather/general/outlook
        strip_path: false
      - tags:
          - OAS3_import
        name: OpenAPI_definition-path_1-post
        methods:
          - POST
        paths:
          - /weather/general/outlook
        strip_path: false
      - tags:
          - OAS3_import
        name: OpenAPI_definition-path_2-get
        methods:
          - GET
        paths:
          - /weather/(?<name>\S+)$
        strip_path: false
    tags:
      - OAS3_import
upstreams:
  - name: OpenAPI_definition
    targets:
      - target: localhost:8080
    tags:
      - OAS3_import

```

For now let's save this yaml inside the [kong/kong.yml](kong/kong.yml) file.




### Docker Compose with Kong DB-less deployment & declarative configuration

I have two goals here: First I want a simple deployment solution. If I could avoid it then I don't want to have multiple services only for the API gateway. I want to start small and you as a reader should be able to easily follow.

As [the official Docker Compose file](https://github.com/Kong/docker-kong/blob/master/compose/docker-compose.yml) has two (!!) database migration services, one database service and one service for Kong I was really overwhelmed at first.

What I learned in my years in the IT industry: Every component you don't have is a good component. So there must be a way to deploy Kong without that much "hassle". And I found one:

 
The documentation at https://docs.konghq.com/2.2.x/db-less-and-declarative-config/ & https://docs.konghq.com/install/docker says, that DB-less mode is possible since Kong 1.1 and has a number of benefits:

* reduced number of dependencies: no need to manage a database installation if the entire setup for your use-cases fits in memory
* it is a good fit for automation in CI/CD scenarios: configuration for entities can be kept in a single source of truth managed via a Git repository
* it enables more deployment options for Kong

But be aware that are also some drawbacks. [Not all plugins support this mode]https://docs.konghq.com/2.2.x/db-less-and-declarative-config/#plugin-compatibility) and [there is no central configuration database](https://docs.konghq.com/2.2.x/db-less-and-declarative-config/#no-central-database-coordination) if you want to run multiple Kong nodes. But for our simple setup we should be able to live with that. 


But there's another advantage: we don't need to use [decK](https://github.com/Kong/deck) here as my colleague [already outlined](https://blog.codecentric.de/en/2019/12/kong-api-gateway-declarative-configuration-using-deck-and-visualizations-with-konga/) is used with Kong for declarative config handling.

This is only needed, if you use Kong with a Database deployment! If you choose the DB-less/declarative configuration approach, your declarative file is already everything we need! :)



So let's do it. I'll try to setup the simplest possible `docker-compose.yml` here in order to spin up Kong. I'll derive it [from the official one](https://github.com/Kong/docker-kong/blob/master/compose/docker-compose.yml), the one my colleague Daniel Kocot [used in his blog posts](https://github.com/danielkocot/kong-blogposts/blob/master/docker-compose.yml) and others [like this](https://medium.com/@matias_azucas/db-less-kong-tutorial-8cbf8f70b266). 

```yaml
version: '3.7'

services:
  kong:
    image: kong:2.2.0
    environment:
      KONG_ADMIN_ACCESS_LOG: /dev/stdout
      KONG_ADMIN_ERROR_LOG: /dev/stderr
      KONG_ADMIN_LISTEN: '0.0.0.0:8001'
      KONG_DATABASE: "off"
      KONG_DECLARATIVE_CONFIG: /usr/local/kong/declarative/kong.yml
      KONG_PROXY_ACCESS_LOG: /dev/stdout
      KONG_PROXY_ERROR_LOG: /dev/stderr
    volumes:
      - ./kong/:/usr/local/kong/declarative
    networks:
      - kong-net
    ports:
      - "8000:8000/tcp"
      - "127.0.0.1:8001:8001/tcp"
      - "8443:8443/tcp"
      - "127.0.0.1:8444:8444/tcp"
    healthcheck:
      test: ["CMD", "kong", "health"]
      interval: 10s
      timeout: 10s
      retries: 10
    restart: on-failure
    deploy:
      restart_policy:
        condition: on-failure

  # no portbinding here - the actual services should be accessible through Kong
  weatherbackend:
    build: ./weatherbackend
    ports:
      - "8080"
    networks:
      - kong-net
    tty:
      true
    restart:
      unless-stopped

networks:
  kong-net:
    external: false
```

I litterally blew everything out we don't really really need in a DB-less scenario! No `kong-migrations`, `kong-migrations-up`, `kong-db` services - and no extra `Dockerfile` [as shown in this blog post](https://medium.com/@matias_azucas/db-less-kong-tutorial-8cbf8f70b266).

I only wanted to have a single `kong` service for the API gateway - and a `weatherbackend` service that is registered in Kong later.

As stated [in the docs for DB-less deployment](https://docs.konghq.com/install/docker/?_ga=2.266755086.1634614376.1604405282-930789398.1604405282) I used `KONG_DATABASE: "off"` to switch to DB-less mode and `KONG_DECLARATIVE_CONFIG: /usr/local/kong/declarative/kong.yml` to tell Kong where to get the `kong.yml` we generated with the Insomnia Designer's Kong Bungle plugin.
To have the file present at `/usr/local/kong/declarative/kong.yml`, I used a simple volume mount like this: `./kong/:/usr/local/kong/declarative`. No need to manually create the Volume as described in the docs - or to create another Dockerfile solely to load the config file into the Kong container. Simply nothing needed instead of this sweet volume!

Now this thing starts to make fun to me :)

 
Now let's fire up our Kong setup with `docker-compose up`

```
$ docker-compose up
Starting spring-boot-openapi-kong_kong_1           ... done
Starting spring-boot-openapi-kong_weatherbackend_1 ... done
Attaching to spring-boot-openapi-kong_weatherbackend_1, spring-boot-openapi-kong_kong_1
kong_1            | 2020/11/04 14:21:11 [notice] 1#0: using the "epoll" event method
kong_1            | 2020/11/04 14:21:11 [notice] 1#0: openresty/1.17.8.2
kong_1            | 2020/11/04 14:21:11 [notice] 1#0: built by gcc 9.3.0 (Alpine 9.3.0)
kong_1            | 2020/11/04 14:21:11 [notice] 1#0: OS: Linux 5.4.39-linuxkit
kong_1            | 2020/11/04 14:21:11 [notice] 1#0: getrlimit(RLIMIT_NOFILE): 1048576:1048576
kong_1            | 2020/11/04 14:21:11 [notice] 1#0: start worker processes
kong_1            | 2020/11/04 14:21:11 [notice] 1#0: start worker process 22
kong_1            | 2020/11/04 14:21:11 [notice] 1#0: start worker process 23
kong_1            | 2020/11/04 14:21:11 [notice] 1#0: start worker process 24
kong_1            | 2020/11/04 14:21:11 [notice] 1#0: start worker process 25
kong_1            | 2020/11/04 14:21:11 [notice] 23#0: *2 [lua] cache.lua:374: purge(): [DB cache] purging (local) cache, context: init_worker_by_lua*
kong_1            | 2020/11/04 14:21:11 [notice] 23#0: *2 [lua] cache.lua:374: purge(): [DB cache] purging (local) cache, context: init_worker_by_lua*
kong_1            | 2020/11/04 14:21:11 [notice] 23#0: *2 [kong] init.lua:354 declarative config loaded from /usr/local/kong/declarative/kong.yml, context: init_worker_by_lua*
weatherbackend_1  |
weatherbackend_1  |   .   ____          _            __ _ _
weatherbackend_1  |  /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
weatherbackend_1  | ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
weatherbackend_1  |  \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
weatherbackend_1  |   '  |____| .__|_| |_|_| |_\__, | / / / /
weatherbackend_1  |  =========|_|==============|___/=/_/_/_/
weatherbackend_1  |  :: Spring Boot ::        (v2.3.5.RELEASE)
weatherbackend_1  |
weatherbackend_1  | 2020-11-04 14:21:13.226  INFO 6 --- [           main] io.jonashackt.WeatherBackendApplication  : Starting WeatherBackendApplication v2.3.5.RELEASE on 209e8a7cbb36 with PID 6 (/app.jar started by root in /)
weatherbackend_1  | 2020-11-04 14:21:13.239  INFO 6 --- [           main] io.jonashackt.WeatherBackendApplication  : No active profile set, falling back to default profiles: default
weatherbackend_1  | 2020-11-04 14:21:15.920  INFO 6 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
weatherbackend_1  | 2020-11-04 14:21:15.958  INFO 6 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
weatherbackend_1  | 2020-11-04 14:21:15.960  INFO 6 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.39]
weatherbackend_1  | 2020-11-04 14:21:16.159  INFO 6 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
weatherbackend_1  | 2020-11-04 14:21:16.163  INFO 6 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 2714 ms
weatherbackend_1  | 2020-11-04 14:21:16.813  INFO 6 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
weatherbackend_1  | 2020-11-04 14:21:18.534  INFO 6 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
weatherbackend_1  | 2020-11-04 14:21:18.564  INFO 6 --- [           main] io.jonashackt.WeatherBackendApplication  : Started WeatherBackendApplication in 7.188 seconds (JVM running for 8.611)
kong_1            | 172.19.0.1 - - [04/Nov/2020:14:25:16 +0000] "GET / HTTP/1.1" 404 48 "-" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:82.0) Gecko/20100101 Firefox/82.0"
```

A crucial part here is that Kong successfully loads the declarative configuration file and logs something like `[kong] init.lua:354 declarative config loaded from /usr/local/kong/declarative/kong.yml`


If your log looks somehow like the above you can also have a look at the admin API by opening http://localhost:8001/ in your browser. [As the docs state](https://docs.konghq.com/2.2.x/db-less-and-declarative-config/#setting-up-kong-in-db-less-mode) everything should be ok, if the response says `"database": "off"` somewhere like:

![docker-compose-db-less-deploy-database-off](screenshots/docker-compose-db-less-deploy-database-off.png)  

We can also double check http://localhost:8001/status , where we have a good overview of Kong's current availability.



### Access the Spring Boot app through Kong

The next thing we need to look at is how to access our `weatherbackend` through Kong. Specifically we need to have a look into the configured Kong services, if the OpenAPI spec import worked out in the way we'd expected it in the first place.

Without using Kong's declarative configuration [we need to add services and routes manually through the Kong admin API](https://blog.codecentric.de/en/2019/09/api-management-kong-update/). But as we use declarative configuration, which we generated from the OpenAPI spec, everything is taken care for us already.


Therefore let's have a look into the list of all currently registered Kong services at http://localhost:8001/services 

![kong-admin-api-services-overview](screenshots/kong-admin-api-services-overview.png)

You can also access the Kong routes of our Spring Boot-backed service with this URL:

http://localhost:8001/services/OpenAPI_definition/routes 


Now we can use Postman, Insomnia Core or the like to access our Spring Boot app with a GET on http://localhost:8000/weather/Jonas

But right now I run into problems here:

```
kong_1            | 2020/11/04 18:56:05 [error] 24#0: *14486 connect() failed (111: Connection refused) while connecting to upstream, client: 172.19.0.1, server: kong, request: "GET /weather/Jonas HTTP/1.1", upstream: "http://127.0.0.1:8080/weather/Jonas", host: "localhost:8000"
kong_1            | 2020/11/04 18:56:05 [error] 24#0: *14486 connect() failed (111: Connection refused) while connecting to upstream, client: 172.19.0.1, server: kong, request: "GET /weather/Jonas HTTP/1.1", upstream: "http://127.0.0.1:8080/weather/Jonas", host: "localhost:8000"
kong_1            | 172.19.0.1 - - [04/Nov/2020:18:56:05 +0000] "GET /weather/Jonas HTTP/1.1" 502 75 "-" "insomnia/2020.4.2"
```





### Automating the OpenAPI-Kong import

We need to also import the OpenAPI spec everytime the code changes, since otherwise the configuration in Kong will differ with every commit!

Additionally we want to be able to run our process on our CI servers as well, since we're in 2020 and want to be sure everything runs even after code changes.

https://support.insomnia.rest/collection/105-inso-cli




## Links

https://blog.codecentric.de/en/2017/11/api-management-kong/

https://docs.konghq.com/hub/


#### Spring & OpenAPI

https://github.com/springdoc/springdoc-openapi-maven-plugin

https://stackoverflow.com/questions/59616165/what-is-the-function-of-springdoc-openapi-maven-plugin-configuration-apidocsurl

https://www.baeldung.com/spring-rest-openapi-documentation


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

