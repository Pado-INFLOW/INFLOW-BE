package com.pado.inflow.employee.security;

import com.pado.inflow.employee.info.command.application.service.EmployeeCommandService;
import com.pado.inflow.employee.info.command.domain.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class WebSecurity {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final EmployeeCommandService employeeService;

    private final EmployeeRepository employeeRepository;
    private final Environment env;
    private final JwtUtil jwtUtil;

    @Autowired
    public WebSecurity(BCryptPasswordEncoder bCryptPasswordEncoder,  EmployeeCommandService employeeService
            , Environment env, JwtUtil jwtUtil
    ,EmployeeRepository employeeRepository) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.employeeService = employeeService;
        this.employeeRepository=employeeRepository;
        this.env = env;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {

        // 설명.CORS 처리 및 CSRF 비활성화
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable);

        // 설명. AuthenticationManager setup
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        // 설명. loadUserByUsername 호출
        authenticationManagerBuilder.userDetailsService(employeeService)
                .passwordEncoder(bCryptPasswordEncoder);

        // 설명. 인증 매니저 생성
        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

        // 설명. 권한 설정
        http.authorizeHttpRequests(authz -> authz
                        .requestMatchers(new AntPathRequestMatcher("/actuator/health", "GET")).permitAll()
                        // 설명. 1. 로그인은 어떤 사용자도 이용 가능
                        .requestMatchers(new AntPathRequestMatcher("/api/login", "POST")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/auth/**", "POST")).permitAll()

                        // 설명. 2. employee(사원) 도메인
                        // 설명. 2.1. 사원 테이블 관련 API
                        .requestMatchers(new AntPathRequestMatcher("/api/employees/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/employees/**", "POST")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/employees/**", "DELETE")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/employees/**", "PUT")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/employees/**", "PATCH")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 설명. 2.2. 인사 발령 테이블 관련 API
                        .requestMatchers(new AntPathRequestMatcher("/api/appointments/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/appointments/**", "POST")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/appointments/**", "DELETE")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/appointments/**", "PUT")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/appointments/**", "PATCH")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 설명. 2.3. 회사 테이블 관련 API
                        .requestMatchers(new AntPathRequestMatcher("/api/companies/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 설명. 2.4. 엑셀 양식 관련 API
                        .requestMatchers(new AntPathRequestMatcher("/api/forms/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 설명. 2.5. 유효성검사 관련 API
                        .requestMatchers(new AntPathRequestMatcher("/api/validations/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")



                        // 그외.. 테이블 관련 api

                        // 설명. 3. department(부서) 도메인
                        // 사원 찾기 - 부서 폴더구조 목록 조회
                        .requestMatchers(new AntPathRequestMatcher("/api/departments/hierarchy/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        // 사원 찾기 - 검색 키워드(사원명, 사원코드, 부서명, 전화번호) 사원 목록 조회
                        .requestMatchers(new AntPathRequestMatcher("/api/departments/search/members/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        // 사원찾기 - 사원정보 상세조회
                        .requestMatchers(new AntPathRequestMatcher("/api/departments/search/members/detail/employee-code/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        // 인사관리 - 부서 상세 조회
                        .requestMatchers(new AntPathRequestMatcher("/api/departments/search/members/detail/department-code/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        // 인사관리 - 검색 키워드(부서코드, 부서명, 부서장명) 부서 목록 조회
                        .requestMatchers(new AntPathRequestMatcher("/api/departments/search/departments/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        // 인사관리 - 부서 생성을 위한 드롭다운
                        .requestMatchers(new AntPathRequestMatcher("/api/departments/dropdown/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        // 팀장권한 - 내 부서 관리
                        .requestMatchers(new AntPathRequestMatcher("/api/departments/my-department/*/members/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        // 인사관리 - 부서 삭제
                        .requestMatchers(new AntPathRequestMatcher("/api/departments/**", "DELETE")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        // 인사관리 - 부서 추가
                        .requestMatchers(new AntPathRequestMatcher("/api/departments/add-department/**", "POST")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        // 인사관리 - 부서 수정
                        .requestMatchers(new AntPathRequestMatcher("/api/departments/**", "PATCH")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")


                        // 설명. 4. attendance(근태) 도메인
                        .requestMatchers(new AntPathRequestMatcher("/api/attendance-requests/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/attendance-requests/**", "POST")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/attendance-requests/**", "DELETE")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/attendance-requests/**", "PUT")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/attendance-requests/**", "PATCH")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 설명. 5. vacation(휴가) 도메인
                        .requestMatchers(new AntPathRequestMatcher("/api/vacations/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/vacations/**", "POST")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/vacations/**", "DELETE")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/vacations/**", "PUT")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/vacations/**", "PATCH")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        /* -------------------------------------------------------------------------------------------------------------------------------------------------------------- */
                        // 설명. 6. evaluation(평가) 도메인

                        // 과제 유형 ( Task_Type )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskType/allTaskType", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskType/create", "POST")).hasAnyRole(   "EMPLOYEE", "HR", "MANAGER", "ADMIN") // 설명. 6. evaluation(평가) 도메인

                        // 과제 유형 ( Task_Type )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskType/allTaskType", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskType/create", "POST")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskType/**", "PATCH")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskType/**", "DELETE")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 과제 항목 ( Task_Item )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/departmentTasks", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/departmentTask", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/individualTasks", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/individualTask/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/AllTaskItems/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/commonTasks", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/commonTask/**", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/TaskItems/**", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 과제별 평가 ( Task_Eval )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskEval/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskEval/byEvaluationId/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskEval/**", "POST")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskEval/**", "PATCH")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 평가 등급 ( grade )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/grade/**", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 평가 피드백 ( feedback )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/feedback/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/feedback/**", "POST")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/feedback/**", "PATCH")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")


                        // 평가 정책 ( EvaluationPolicy )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluationPolicy/policySearch", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluationPolicy/**", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluationPolicy/**", "PATCH")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluationPolicy/**", "DELETE")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluationPolicy/policyCreation", "POST")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")


                        // 평가 ( Evaluation )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluation/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 평가정책별 평가 ( TaskTypeEval )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskTypeEval/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        /* -------------------------------------------------------------------------------------------------------------------------------------------------------------- */

                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskType/**", "PATCH")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskType/**", "DELETE")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")

                         // 과제 항목 ( Task_Item )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/departmentTasks", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/departmentTask", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/individualTasks", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/individualTask/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/AllTaskItems/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/commonTasks", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/commonTask/**", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskItem/TaskItems/**", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 과제별 평가 ( Task_Eval )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskEval/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskEval/byEvaluationId/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskEval/**", "POST")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskEval/**", "PATCH")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 평가 등급 ( grade )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/grade/**", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 평가 피드백 ( feedback )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/feedback/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/feedback/**", "POST")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/feedback/**", "PATCH")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")


                        // 평가 정책 ( EvaluationPolicy )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluationPolicy/policySearch", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluationPolicy/**", "GET")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluationPolicy/**", "PATCH")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluationPolicy/**", "DELETE")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluationPolicy/policyCreation", "POST")).hasAnyRole(    "EMPLOYEE", "HR", "MANAGER", "ADMIN")


                        // 평가 ( Evaluation )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/evaluation/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 평가정책별 평가 ( TaskTypeEval )
                        .requestMatchers(new AntPathRequestMatcher("/api/evaluations/taskTypeEval/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        /* -------------------------------------------------------------------------------------------------------------------------------------------------------------- */

                        // 설명. 7. payroll(급여) 도메인

                        // 사원별 연월별 급여 명세서 조회
                        .requestMatchers(new AntPathRequestMatcher("/api/payrolls/details", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 사원별 전체 급여 내역 조회
                        .requestMatchers(new AntPathRequestMatcher("/api/payrolls/all", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/payrolls/**", "POST")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/payrolls/**", "DELETE")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/payrolls/**", "PUT")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/payrolls/**", "PATCH")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 급여 기간별 조회
                        .requestMatchers(new AntPathRequestMatcher("/api/payrolls/period", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 급여 연도별 조회
                        .requestMatchers(new AntPathRequestMatcher("/api/payrolls/list")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 급여 비과세 항목 전체 조회
                        .requestMatchers(new AntPathRequestMatcher("/api/non-taxable-payrolls/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 급여 비과세 항목 등록
                        .requestMatchers(new AntPathRequestMatcher("/api/non-taxable-payrolls/**", "POST")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 급여 비과세 항목 수정
                        .requestMatchers(new AntPathRequestMatcher("/api/non-taxable-payrolls/**", "PUT")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 급여 비정기 수당 항목 전체 조회
                        .requestMatchers(new AntPathRequestMatcher("/api/irregular-allowances/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 급여 비정기 수당 항목 등록
                        .requestMatchers(new AntPathRequestMatcher("/api/irregular-allowances/**", "POST")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 급여 비정기 수당 항목 수정
                        .requestMatchers(new AntPathRequestMatcher("/api/irregular-allowances/**", "PUT")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 퇴직금 조회를 위한 api
                        .requestMatchers(new AntPathRequestMatcher("/api/severance-pay/estimate/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/severance-pay/calculate/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 설명. 8. statistics(통계) 도메인
                        .requestMatchers(new AntPathRequestMatcher("/api/statistics/**", "GET")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/statistics/**", "POST")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/statistics/**", "DELETE")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/statistics/**", "PUT")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")
                        .requestMatchers(new AntPathRequestMatcher("/api/statistics/**", "PATCH")).hasAnyRole("EMPLOYEE", "HR", "MANAGER", "ADMIN")

                        // 설명. 기타 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                // 설명. authenticationManager 등록
                .authenticationManager(authenticationManager)
                // 설명. 세션 관리 설정 (JWT 사용 시)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 설명. 인증 필터 등록
                .addFilter(getAuthenticationFilter(authenticationManager))
                // 설명. JWT 필터 추가
                .addFilterBefore(new JwtFilter(employeeService, jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 커스텀 인증 필터 설정 (로그인 URL 변경)
    private AuthenticationFilter getAuthenticationFilter(AuthenticationManager authenticationManager) {
        AuthenticationFilter authenticationFilter
                = new AuthenticationFilter(authenticationManager,employeeRepository, env, bCryptPasswordEncoder);
        authenticationFilter.setFilterProcessesUrl("/api/login"); // 로그인 처리 URL 변경
        authenticationFilter.setAuthenticationFailureHandler(authenticationFailureHandler());
        return authenticationFilter;
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new CustomAuthenticationFailureHandler();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);

        configuration.setAllowedOrigins(List.of("https://inflow.run")); // Allow frontend
//        configuration.setAllowedOrigins(List.of("http://localhost:5173/")); // 로컬
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*")); // Allow all headers

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}