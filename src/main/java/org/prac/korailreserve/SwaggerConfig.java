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
                .description("**사용자ID** = 코레일 회원번호<br>"
                        + "**출발,도착역** = '역' 제외 (서울, 부산, 울산(통도사), 경주 ...)<br>"
                        + "**월, 일, 시간, 분** = 두자릿 수 ( 1일 : 01)");

        return new OpenAPI()
                .components(new Components())
                .info(info);
    }
}
