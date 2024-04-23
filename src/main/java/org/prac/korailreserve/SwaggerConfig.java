package org.prac.korailreserve;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Controller
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("KTX 자동 예매 API")
                .version("1.0")
                .description("값 입력 후 헤드리스 모드로 실행이 됩니다.");
        return new OpenAPI()
                .components(new Components())
                .info(info);
    }
}
