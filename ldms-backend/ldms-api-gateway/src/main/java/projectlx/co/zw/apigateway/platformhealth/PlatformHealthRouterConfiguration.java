package projectlx.co.zw.apigateway.platformhealth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import projectlx.co.zw.apigateway.platformhealth.dto.PlatformHealthResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Configuration
public class PlatformHealthRouterConfiguration {

    @Bean
    public RouterFunction<ServerResponse> platformHealthRoutes(PlatformHealthAggregator platformHealthAggregator) {
        return RouterFunctions.route(
                GET("/ldms-api-gateway/v1/backoffice/platform-health/snapshot")
                        .and(accept(MediaType.APPLICATION_JSON, MediaType.ALL)),
                request -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                Mono.fromCallable(platformHealthAggregator::snapshot)
                                        .subscribeOn(Schedulers.boundedElastic()),
                                PlatformHealthResponse.class));
    }
}
