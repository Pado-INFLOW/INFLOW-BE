package com.pado.inflow.employee.info.command.application.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.pado.inflow.common.exception.CommonException;
import com.pado.inflow.common.exception.ErrorCode;
import com.pado.inflow.config.S3Config;
import com.pado.inflow.employee.info.command.domain.aggregate.dto.request.RequestEmployeeDTO;
import com.pado.inflow.employee.info.command.domain.aggregate.dto.request.RequestUpdateEmployeeDTO;
import com.pado.inflow.employee.info.command.domain.aggregate.dto.response.ResponseContractDTO;
import com.pado.inflow.employee.info.command.domain.aggregate.dto.response.ResponseEmployeeDTO;
import com.pado.inflow.employee.info.command.domain.aggregate.entity.Contract;
import com.pado.inflow.employee.info.command.domain.aggregate.entity.Employee;
import com.pado.inflow.employee.info.command.domain.repository.ContractRepository;
import com.pado.inflow.employee.info.command.domain.repository.EmployeeRepository;
import com.pado.inflow.employee.info.enums.EmployeeRole;
import com.pado.inflow.employee.info.enums.ResignationStatus;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service("employeeCommandService")
public class EmployeeCommandService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    private final SmsService smsService; // 문자 전송 서비스 추가

    //설명. AWS 설정
    private final AmazonS3Client s3Client;
    private final ContractRepository contractRepository;
    private final S3Config s3Config;

    @Autowired
    public EmployeeCommandService(EmployeeRepository employeeRepository
            , ModelMapper modelMapper
            , BCryptPasswordEncoder bCryptPasswordEncoder
            , SmsService smsService
            , AmazonS3Client s3Client
            , ContractRepository contractRepository
            , S3Config s3Config
    ) {
        this.employeeRepository = employeeRepository;
        this.modelMapper = modelMapper;
        this.bCryptPasswordEncoder =bCryptPasswordEncoder;
        this.smsService=smsService;
        this.s3Client = s3Client;
        this.contractRepository = contractRepository;
        this.s3Config = s3Config;
    }

    //설명.1.1 사원 등록 ( 환영 메시지를 전송, 초기 비밀번호: "사번!성명@생년월일")
    @Transactional
    public List<ResponseEmployeeDTO> registerEmployees(List<RequestEmployeeDTO> employeeDTOs) {
        List<Employee> employees = employeeDTOs.stream()
                .map(dto -> {
                    //설명.1.1.1 요청 바디의 값 매핑
                    Employee employee = modelMapper.map(dto, Employee.class);

                    //설명.1.1.2 초기 비밀번호 생성 및 암호화
                    String initialPassword = generateInitialPassword(dto);
                    employee.setPassword(encodePassword(initialPassword));

                    //설명.1.1.3 employee_role 설정
                    if ("DP002".equals(dto.getDepartmentCode())) {
                        employee.setEmployeeRole(EmployeeRole.HR);
                    } else if ("P005".equals(dto.getPositionCode())) {
                        employee.setEmployeeRole(EmployeeRole.MANAGER);
                    } else {
                        employee.setEmployeeRole(EmployeeRole.EMPLOYEE);
                    }

                    // 설명.1.1.4. 기본값 설정
                    employee.setAttendanceStatusTypeCode("AS001");
                    employee.setProfileImgUrl("https://inflow-emp-profile.s3.ap-northeast-2.amazonaws.com/emp_basic_profile.png");
                    employee.setResignationStatus(ResignationStatus.N);
                    employee.setJoinDate(LocalDate.now());

                    // 설명.1.1.5 연봉 계산 (월급 * 12)
                    if (dto.getMonthlySalary() != null) {
                        employee.setSalary(dto.getMonthlySalary() * 12);
                    } else {
                        throw new CommonException(ErrorCode.INVALID_INPUT_VALUE);
                    }

                    // 설명.1.1.6. 사원 정보 저장
                    Employee savedEmployee = employeeRepository.save(employee);

                    // 설명.1.1.7. 초기 계약서 생성 및 저장
                    List<Contract> contracts = createInitialContracts(savedEmployee);
                    contractRepository.saveAll(contracts);
                    
                    return savedEmployee;
                })
                .collect(Collectors.toList());

         //문자 전송 및 응답 생성 (문자 전송은 주석 처리된 상태)
        employees.forEach(employee -> {
             String welcomeMessage = generateWelcomeMessage(employee);
             smsService.sendSms(employee.getPhoneNumber(), welcomeMessage); // 문자 전송
         });

        return employees.stream()
                .map(employee -> modelMapper.map(employee, ResponseEmployeeDTO.class))
                .collect(Collectors.toList());
    }


    //설명.1.2 초기 비밀번호 생성
    private String generateInitialPassword(RequestEmployeeDTO dto) {
        return String.format("%s!%s@%s",
                dto.getEmployeeNumber(),
                dto.getName(),
                dto.getBirthDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        );
    }

     //설명.1.3. 환영 메시지 생성
     private String generateWelcomeMessage(Employee employee) {
         return String.format(
                 "\n[InFlow 환영 메시지]\n\n"
                         + "안녕하세요, %s님, InFlow 입사를 진심으로 축하드립니다!\n"
                         + "귀하의 사번은 \"%s\"입니다.\n\n"
                         + "처음 로그인을 위해 아래 단계를 따라주세요:\n"
                         + "1. InFlow 웹사이트에 접속합니다.\n"
                         + "2. 로그인 화면에서 사번을 입력해주세요.\n초기 비밀번호는 \"사번!성명@생년월일\" 입니다.\n"
                         + "(초기 비밀번호 예시: 202400000!인플로@19990308)\n"
                         + "3. 로그인 후, 보안을 위해 비밀번호를 반드시 변경해주세요.\n\n"
                         + "문제가 발생하거나 도움이 필요하시면 관리자에게 문의 바랍니다.\n\n",
                 employee.getName(),
                 employee.getEmployeeNumber()
         );
     }


    //설명.1.4. 비밀번호 암호화
    private String encodePassword(String password) {
        try {
            return bCryptPasswordEncoder.encode(password);
        } catch (Exception e) {
            throw new CommonException(ErrorCode.PASSWORD_ENCODING_FAILED);
        }
    }

    // 설명.1.1.9 회원가입 시 초기 계약서 생성 메서드
    private List<Contract> createInitialContracts(Employee employee) {
        List<Contract> contracts = new ArrayList<>();

        // 근로계약서
        Contract employmentContract = new Contract();
        employmentContract.setContractType("EMPLOYMENT");
        employmentContract.setEmployeeId(employee.getEmployeeId());
        employmentContract.setFileUrl(null); // 초기 파일 URL은 NULL
        employmentContract.setContractStatus("SIGNING"); // 초기 상태는 SIGNING
        employmentContract.setCreatedAt(null); // 등록 시각은 NULL
        employmentContract.setConsentStatus("N"); // 초기 동의 여부는 N
        contracts.add(employmentContract);

        // 비밀유지서약서
        Contract securityContract = new Contract();
        securityContract.setContractType("SECURITY");
        securityContract.setEmployeeId(employee.getEmployeeId());
        securityContract.setFileUrl(null); // 초기 파일 URL은 NULL
        securityContract.setContractStatus("SIGNING"); // 초기 상태는 SIGNING
        securityContract.setCreatedAt(null); // 등록 시각은 NULL
        securityContract.setConsentStatus("N"); // 초기 동의 여부는 N
        contracts.add(securityContract);

        return contracts;
    }
    /**
     * 설명. 2.1 사원 정보 수정 (ID 기준)
     */
    @Transactional
    public ResponseEmployeeDTO updateEmployeeById(Long employeeId, String email, String phoneNumber,
                                                  String streetAddress, String detailedAddress, MultipartFile profileImg) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_EMPLOYEE));

        // 필드 업데이트
        if (email != null) {
            employee.setEmail(email);
        }
        if (phoneNumber != null) {
            employee.setPhoneNumber(phoneNumber);
        }
        if (streetAddress != null) {
            employee.setStreetAddress(streetAddress);
        }
        if (detailedAddress != null) {
            employee.setDetailedAddress(detailedAddress);
        }

        // 프로필 이미지 업데이트 처리
        if (profileImg != null && !profileImg.isEmpty()) {
            String defaultProfileImgUrl = "https://inflow-emp-profile.s3.ap-northeast-2.amazonaws.com/emp_basic_profile.png";

            // 기존 프로필 이미지가 기본 이미지가 아닌 경우 삭제
            if (!defaultProfileImgUrl.equals(employee.getProfileImgUrl())) {
                deleteExistingProfileImage(employee.getProfileImgUrl());
            }

            // 새 프로필 이미지 업로드
            String newProfileImgUrl = uploadProfileImageToS3(profileImg, employeeId);
            employee.setProfileImgUrl(newProfileImgUrl);
        }

        // JPA를 통한 수정
        Employee updatedEmployee = employeeRepository.save(employee);
        return modelMapper.map(updatedEmployee, ResponseEmployeeDTO.class);
    }

    /**
     * 설명. 2.1.1 기존 프로필 이미지를 삭제하는 메서드
     */
    private void deleteExistingProfileImage(String imageUrl) {
        try {
            String bucketName = s3Config.getInflowEmpProfileBucket();
            String key = extractKeyFromUrl(imageUrl);

            s3Client.deleteObject(bucketName, key);
            System.out.println("Deleted existing image from S3: " + key);
        } catch (Exception e) {
            System.err.println("Failed to delete existing image: " + e.getMessage());
        }
    }

    /**
     * 설명. 2.1.2. S3 URL에서 파일 키 추출
     */
    private String extractKeyFromUrl(String url) {
        String bucketName = s3Config.getInflowEmpProfileBucket();
        return url.substring(url.indexOf(bucketName) + bucketName.length() + 1);
    }

    /**
     * 설명. 2.1.3. 새 프로필 이미지를 S3에 업로드하는 메서드
     */
    private String uploadProfileImageToS3(MultipartFile profileImg, Long employeeId) {
        try {
            String bucketName = s3Config.getInflowEmpProfileBucket();
            String fileName = employeeId + "_profile"; // 사원 ID_ 프로필

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(profileImg.getContentType());
            metadata.setContentLength(profileImg.getSize());

            s3Client.putObject(bucketName, fileName, profileImg.getInputStream(), metadata);

            return s3Client.getUrl(bucketName, fileName).toString();
        } catch (IOException e) {
            throw new CommonException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    //설명. 3. 비밀번호 재설정
    public void resetPassword(Long employeeId, String newPassword) {
        // 사번으로 사원 조회
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_USER_ID));

        // 새 비밀번호 암호화 및 저장
        String encryptedPassword = bCryptPasswordEncoder.encode(newPassword);
        employee.setPassword(encryptedPassword);

        employeeRepository.save(employee);
    }

    //설명. 4. 메서드
    //설명. 4.1. 사원 정보 업데이트시 공통 필드 업데이트 로직
    private void updateEmployeeFields(Employee employee, RequestUpdateEmployeeDTO updateEmployeeDTO) {
        // 설명. 요청 DTO 필드에 값이 있는 경우에만
        if (updateEmployeeDTO.getEmail() != null) {
            employee.setEmail(updateEmployeeDTO.getEmail());
        }
        if (updateEmployeeDTO.getPhoneNumber() != null) {
            employee.setPhoneNumber(updateEmployeeDTO.getPhoneNumber());
        }
        if (updateEmployeeDTO.getProfileImgUrl() != null) {
            employee.setProfileImgUrl(updateEmployeeDTO.getProfileImgUrl());
        }
        if (updateEmployeeDTO.getStreetAddress() != null) {
            employee.setStreetAddress(updateEmployeeDTO.getStreetAddress());
        }
        if (updateEmployeeDTO.getDetailedAddress() != null) {
            employee.setDetailedAddress(updateEmployeeDTO.getDetailedAddress());
        }
    }

    // 설명.5. 시큐리티를 위한 설정 메서드
    //  로그인 시 security가 자동으로 호출하는 메소드 */
    @Override
    public UserDetails loadUserByUsername(String employeeNumber) throws UsernameNotFoundException {
        // 1. employeeNumber를 기준으로 사용자 조회
        Employee loginEmployee = employeeRepository.findByEmployeeNumber(employeeNumber)
                .orElseThrow(() -> new CommonException(ErrorCode.UNAUTHORIZED_ACCESS));

        // 2. 비밀번호 처리 (소셜 로그인 시 비밀번호가 없을 경우 기본값 설정)
        String encryptedPwd = loginEmployee.getPassword();
        if (encryptedPwd == null) {
            encryptedPwd = "{noop}";  // 비밀번호가 없을 경우 기본값 설정
        }

        // 3. 권한 정보를 userRole 필드에서 가져와서 변환
        List<GrantedAuthority> grantedAuthorities = Collections.singletonList(
                // "ROLE_EMPLOYEE" 또는 "ROLE_HR" 또는 "ROLE_MANAGER " 또는 "ROLE_ADMIN"
                new SimpleGrantedAuthority("ROLE_" + loginEmployee.getEmployeeRole().name())
        );

        // 4. UserDetails 객체 반환
        return new User(loginEmployee.getEmployeeNumber(), encryptedPwd,
                true, true, true, true,
                grantedAuthorities);
    }

    // 설명. 6. 서명된 계약서 등록 (수정)
    @Transactional
    public ResponseContractDTO updateContract(
            Long contractId,
            MultipartFile file
    ) throws IOException {
        // 1. 기존 계약서 조회
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_CONTRACT));

        // 2. 기존 계약서에 파일 URL이 존재하면 수정 불가 예외 발생
        if (contract.getFileUrl() != null && !contract.getFileUrl().isEmpty()) {
            throw new CommonException(ErrorCode.DUPLICATE_CONTRACT);
        }

        // 3. 파일 이름 생성 (계약서 종류 + 사원ID + 타임스탬프)
        String fileName = contract.getContractType() + "_" + contract.getEmployeeId();

        // 4. S3에 파일 업로드
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        String bucketName = s3Config.getInflowContractBucket();
        s3Client.putObject(bucketName, fileName, file.getInputStream(), metadata);

        // 5. S3 URL 생성
        String fileUrl = s3Client.getUrl(bucketName, fileName).toString();

        // 6. 계약서 정보 업데이트
        contract.setFileUrl(fileUrl);                    // 파일 URL 갱신
        contract.setFileName(fileName);                  // 파일 이름 갱신
        contract.setContractStatus("REGISTERED");        // 계약 상태 변경
        contract.setConsentStatus("Y");                  // 동의 상태 변경
        contract.setCreatedAt(LocalDateTime.now().withNano(0));      // 수정된 시간 설정

        contract = contractRepository.save(contract);    // 업데이트된 계약서 저장

        // 7. 응답 DTO 생성 및 반환
        return new ResponseContractDTO(
                contract.getContractId(),
                contract.getContractType(),
                contract.getFileName(),
                contract.getFileUrl(),
                contract.getContractStatus(),
                contract.getConsentStatus(),
                contract.getCreatedAt().toString(),
                contract.getEmployeeId()
        );
    }
}
