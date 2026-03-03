package com.todaybread.server.system;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 정보를 체크하기 위한 컨트롤러입니다.
 */
@RequestMapping("/api/system")
@RestController
public class HealthController {

    @GetMapping("/health")
    public String getServerStatus(){
        return "UP";
    }
}
