package com.example.be.domain;

import com.example.be.domain.envsensor.entity.EnvSensor;
import com.example.be.domain.envsensor.entity.SensorType;
import com.example.be.domain.envsensor.repository.EnvSensorRepository;
import com.example.be.domain.manager.entity.Manager;
import com.example.be.domain.manager.entity.ManagerRole;
import com.example.be.domain.manager.repository.ManagerRepository;
import com.example.be.domain.process.entity.Process;
import com.example.be.domain.process.repository.ProcessRepository;
import com.example.be.domain.wearabledevice.entity.DeviceType;
import com.example.be.domain.wearabledevice.entity.WearableDevice;
import com.example.be.domain.wearabledevice.repository.WearableDeviceRepository;
import com.example.be.domain.worker.entity.Worker;
import com.example.be.domain.worker.repository.WorkerRepository;
import com.example.be.domain.zone.entity.Zone;
import com.example.be.domain.zone.repository.ZoneRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 연관 엔티티를 응답에 담는 목록 API 가 <b>트랜잭션 밖에서도</b> 동작하는지 확인한다.
 *
 * <p>다른 API 테스트는 클래스에 {@code @Transactional} 이 붙어 있어 MockMvc 요청 중에도
 * 영속성 컨텍스트가 열려 있다. 그래서 지연 로딩이 조용히 성공하고, 운영(OSIV off)에서만
 * {@code LazyInitializationException} 으로 500 이 나는 문제를 잡지 못한다.
 * 이 클래스는 일부러 {@code @Transactional} 을 붙이지 않고, 데이터는 커밋한 뒤 직접 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("목록 API - 트랜잭션 밖 지연 로딩")
class ListApiOutsideTransactionTest {

    private static final String LOGIN_ID = "lazy-admin01";
    private static final String PASSWORD = "password123!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ManagerRepository managerRepository;
    @Autowired
    private ProcessRepository processRepository;
    @Autowired
    private ZoneRepository zoneRepository;
    @Autowired
    private WorkerRepository workerRepository;
    @Autowired
    private WearableDeviceRepository deviceRepository;
    @Autowired
    private EnvSensorRepository sensorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private Zone zone;

    @BeforeEach
    void setUp() throws Exception {
        managerRepository.save(Manager.builder()
                .loginId(LOGIN_ID).password(passwordEncoder.encode(PASSWORD))
                .name(LOGIN_ID).role(ManagerRole.ADMIN).build());
        token = login();

        Process process = processRepository.save(Process.builder().name("지연로딩공정").build());
        zone = zoneRepository.save(Zone.builder().process(process).zoneCode("LZ-1").name("지연로딩구역").build());
    }

    @AfterEach
    void tearDown() {
        // 커밋된 데이터라 롤백되지 않는다. FK 순서대로 지운다.
        deviceRepository.deleteAllInBatch();
        sensorRepository.deleteAllInBatch();
        workerRepository.deleteAllInBatch();
        zoneRepository.deleteAllInBatch();
        processRepository.deleteAllInBatch();
        managerRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("구역이 배정된 작업자가 있어도 작업자 목록을 조회할 수 있다")
    void workerList_withZone() throws Exception {
        workerRepository.save(Worker.builder().zone(zone).employeeNo("LZ-0001").name("김철수").build());

        mockMvc.perform(get("/api/workers")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].zoneCode").value("LZ-1"))
                .andExpect(jsonPath("$.data.content[0].processName").value("지연로딩공정"));
    }

    @Test
    @DisplayName("구역 필터로 작업자 목록을 조회할 수 있다")
    void workerList_filterByZone() throws Exception {
        workerRepository.save(Worker.builder().zone(zone).employeeNo("LZ-0001").name("김철수").build());

        mockMvc.perform(get("/api/workers")
                        .param("zoneId", String.valueOf(zone.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].zoneName").value("지연로딩구역"));
    }

    @Test
    @DisplayName("작업자가 배정된 디바이스가 있어도 디바이스 목록을 조회할 수 있다")
    void deviceList_withWorker() throws Exception {
        Worker worker = workerRepository.save(
                Worker.builder().zone(zone).employeeNo("LZ-0001").name("김철수").build());
        deviceRepository.save(WearableDevice.builder()
                .worker(worker).serialNo("LZ-BAND-0001").deviceType(DeviceType.BAND).build());

        mockMvc.perform(get("/api/wearable-devices")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].workerName").value("김철수"))
                .andExpect(jsonPath("$.data.content[0].employeeNo").value("LZ-0001"));
    }

    @Test
    @DisplayName("환경 센서 목록을 트랜잭션 밖에서 조회해도 구역 정보가 나온다")
    void sensorList_withZone() throws Exception {
        sensorRepository.save(EnvSensor.builder().zone(zone).sensorType(SensorType.NOISE).build());

        mockMvc.perform(get("/api/env-sensors")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].zoneCode").value("LZ-1"))
                .andExpect(jsonPath("$.data.content[0].zoneName").value("지연로딩구역"));
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":"%s"}""".formatted(LOGIN_ID, PASSWORD)))
                .andExpect(status().isOk()).andReturn();

        String body = result.getResponse().getContentAsString();
        String marker = "\"accessToken\":\"";
        int start = body.indexOf(marker) + marker.length();
        return body.substring(start, body.indexOf('"', start));
    }
}
