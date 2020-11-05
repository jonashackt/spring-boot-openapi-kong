package io.jonashackt.weatherbackend.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                title = "weatherbackend",
                version = "v2.0"
        ),
        servers = @Server(url = "http://weatherbackend:8080")
)
public class OpenAPIConfig {
}
