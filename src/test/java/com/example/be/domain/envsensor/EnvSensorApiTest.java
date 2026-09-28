package com.example.be.domain.envsensor;

import com.example.be.domain.envsensor.entity.EnvSensor;
import com.example.be.domain.envsensor.entity.SensorType;
import com.example.be.domain.envsensor.repository.EnvSensorRepository;
import com.example.be.domain.manager.entity.Manager;
import com.example.be.domain.manager.entity.ManagerRole;
import com.example.be.domain.manager.repository.ManagerRepository;
import com.example.be.domain.process.entity.Process;
import com.example.be.domain.process.repository.ProcessRepository;
import com.example.be.domain.zone.entity.Zone;
import com.example.be.domain.zone.repository.ZoneRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("환경 센서 API")
class EnvSensorApiTest {

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
    private EnvSensorRepository sensorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String memberToken;
    private Zone zoneA;
    private Zone zoneB;

    @BeforeEach
    void setUp() throws Exception {
        saveManager("admin01", ManagerRole.ADMIN);
        saveManager("member01", ManagerRole.MANAGER);
        adminToken = login("admin01");
        memberToken = login("member01");

        Process process = processRepository.save(Process.builder().name("허브공정").build());
        zoneA = zoneRepository.save(Zone.builder().process(process).zoneCode("A-1").name("적치장").build());
        zoneB = zoneRepository.save(Zone.builder().process(process).zoneCode("B-1").name("작업대").build());
    }

    /* ---------- 등록 ---------- */

    @Test
    @DisplayName("ADMIN 은 센서를 등록할 수 있고, 단위를 생략하면 종류의 기본 단위가 들어간다")
    void create_defaultUnit() throws Exception {
        mockMvc.perform(post("/api/env-sensors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneId":%d,"sensorType":"NOISE","thresholdMax":85}""".formatted(zoneA.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sensorType").value("NOISE"))
                .andExpect(jsonPath("$.data.unit").value("dB"))
                .andExpect(jsonPath("$.data.zoneCode").value("A-1"))
                .andExpect(jsonPath("$.data.thresholdMin").doesNotExist())
                .andExpect(jsonPath("$.data.thresholdMax").value(85));
    }

    @Test
    @DisplayName("MANAGER 는 센서를 등록할 수 없다")
    void create_byManager_forbidden() throws Exception {
        mockMvc.perform(post("/api/env-sensors")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneId":%d,"sensorType":"NOISE"}""".formatted(zoneA.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("임계값 하한이 상한보다 크면 400 을 반환한다")
    void create_invalidThreshold() throws Exception {
        mockMvc.perform(post("/api/env-sensors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneId":%d,"sensorType":"TEMPERATURE","thresholdMin":40,"thresholdMax":10}"""
                                .formatted(zoneA.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_THRESHOLD_RANGE"));
    }

    @Test
    @DisplayName("없는 구역에 등록하면 404 를 반환한다")
    void create_unknownZone() throws Exception {
        mockMvc.perform(post("/api/env-sensors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneId":99999,"sensorType":"TEMPERATURE"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ZONE_NOT_FOUND"));
    }

    @Test
    @DisplayName("구역과 종류가 없으면 검증 실패 400 을 반환한다")
    void create_validationFailure() throws Exception {
        mockMvc.perform(post("/api/env-sensors")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    /* ---------- 조회 ---------- */

    @Test
    @DisplayName("zoneId, sensorType 으로 센서를 필터링할 수 있다")
    void list_filter() throws Exception {
        saveSensor(zoneA, SensorType.TEMPERATURE);
        saveSensor(zoneA, SensorType.NOISE);
        saveSensor(zoneB, SensorType.TEMPERATURE);

        mockMvc.perform(get("/api/env-sensors")
                        .param("zoneId", String.valueOf(zoneA.getId()))
                        .param("sensorType", "TEMPERATURE")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].zoneCode").value("A-1"));
    }

    @Test
    @DisplayName("구역 내 환경 센서 목록을 조회할 수 있다")
    void listByZone() throws Exception {
        saveSensor(zoneA, SensorType.TEMPERATURE);
        saveSensor(zoneB, SensorType.OXYGEN);

        mockMvc.perform(get("/api/zones/" + zoneB.getId() + "/env-sensors")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].sensorType").value("OXYGEN"));
    }

    @Test
    @DisplayName("없는 구역의 센서 목록을 조회하면 404 를 반환한다")
    void listByZone_unknownZone() throws Exception {
        mockMvc.perform(get("/api/zones/99999/env-sensors").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ZONE_NOT_FOUND"));
    }

    @Test
    @DisplayName("없는 센서를 조회하면 404 를 반환한다")
    void get_notFound() throws Exception {
        mockMvc.perform(get("/api/env-sensors/99999").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SENSOR_NOT_FOUND"));
    }

    /* ---------- 수정 ---------- */

    @Test
    @DisplayName("임계값 한쪽만 보내도 기존 값과 합쳐서 범위를 검사한다")
    void update_thresholdMergedWithExisting() throws Exception {
        EnvSensor sensor = sensorRepository.save(EnvSensor.builder().zone(zoneA)
                .sensorType(SensorType.TEMPERATURE)
                .thresholdMin(new BigDecimal("5.00")).thresholdMax(new BigDecimal("35.00")).build());

        mockMvc.perform(patch("/api/env-sensors/" + sensor.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"thresholdMin":40}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_THRESHOLD_RANGE"));

        mockMvc.perform(patch("/api/env-sensors/" + sensor.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"thresholdMax":38}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.thresholdMin").value(5.0))
                .andExpect(jsonPath("$.data.thresholdMax").value(38));
    }

    @Test
    @DisplayName("다른 구역으로 옮기고, 종류만 바꾸면 단위도 새 종류의 기본값으로 바뀐다")
    void update_moveZoneAndChangeType() throws Exception {
        EnvSensor sensor = saveSensor(zoneA, SensorType.TEMPERATURE);

        mockMvc.perform(patch("/api/env-sensors/" + sensor.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneId":%d,"sensorType":"FINE_DUST"}""".formatted(zoneB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zoneCode").value("B-1"))
                .andExpect(jsonPath("$.data.sensorType").value("FINE_DUST"))
                .andExpect(jsonPath("$.data.unit").value("㎍/㎥"));
    }

    @Test
    @DisplayName("MANAGER 는 센서를 수정할 수 없다")
    void update_byManager_forbidden() throws Exception {
        EnvSensor sensor = saveSensor(zoneA, SensorType.TEMPERATURE);

        mockMvc.perform(patch("/api/env-sensors/" + sensor.getId())
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"thresholdMax":30}"""))
                .andExpect(status().isForbidden());
    }

    /* ---------- 삭제 ---------- */

    @Test
    @DisplayName("ADMIN 은 센서를 삭제할 수 있다")
    void delete_byAdmin() throws Exception {
        EnvSensor sensor = saveSensor(zoneA, SensorType.TEMPERATURE);

        mockMvc.perform(delete("/api/env-sensors/" + sensor.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/env-sensors/" + sensor.getId()).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("센서가 설치된 구역은 삭제할 수 없다")
    void deleteZone_withSensors_rejected() throws Exception {
        saveSensor(zoneA, SensorType.TEMPERATURE);

        mockMvc.perform(delete("/api/zones/" + zoneA.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ZONE_HAS_SENSORS"));
    }

    /* ---------- 헬퍼 ---------- */

    private EnvSensor saveSensor(Zone zone, SensorType type) {
        return sensorRepository.save(EnvSensor.builder().zone(zone).sensorType(type).build());
    }

    private void saveManager(String loginId, ManagerRole role) {
        managerRepository.save(Manager.builder()
                .loginId(loginId).password(passwordEncoder.encode(PASSWORD))
                .name(loginId).role(role).build());
    }

    private String login(String loginId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":"%s"}""".formatted(loginId, PASSWORD)))
                .andExpect(status().isOk()).andReturn();

        String body = result.getResponse().getContentAsString();
        String marker = "\"accessToken\":\"";
        int start = body.indexOf(marker) + marker.length();
        return body.substring(start, body.indexOf('"', start));
    }
}
