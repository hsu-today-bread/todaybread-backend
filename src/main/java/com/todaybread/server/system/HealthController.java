package com.todaybread.server.system;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 정보를 체크하기 위한 컨트롤러입니다.
 */
@RequestMapping("/api/system")
@RestController
public class HealthController {

    /**
     * 서버 상태를 확인합니다.
     *
     * @return 서버 상태 문자열 ("UP")
     */
    @GetMapping("/health")
    public String getServerStatus(){
        return "UP";
    }
}
