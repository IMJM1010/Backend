package com.example.be.domain.worker;

import com.example.be.domain.manager.entity.Manager;
import com.example.be.domain.manager.entity.ManagerRole;
import com.example.be.domain.manager.repository.ManagerRepository;
import com.example.be.domain.process.entity.Process;
import com.example.be.domain.process.repository.ProcessRepository;
import com.example.be.domain.worker.entity.SafetyStatus;
import com.example.be.domain.worker.entity.Worker;
import com.example.be.domain.worker.repository.WorkerRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("작업자 API")
class WorkerApiTest {

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
    @DisplayName("작업자를 등록하면 구역과 공정 정보가 함께 내려온다")
    void create_withZone() throws Exception {
        mockMvc.perform(post("/api/workers")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneId":%d,"employeeNo":"2026-0001","name":"김철수"}""".formatted(zoneA.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.employeeNo").value("2026-0001"))
                .andExpect(jsonPath("$.data.zoneCode").value("A-1"))
                .andExpect(jsonPath("$.data.processName").value("허브공정"))
                .andExpect(jsonPath("$.data.safetyStatus").value("NORMAL"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("구역 없이 등록하면 구역 관련 필드가 모두 null 이다")
    void create_withoutZone() throws Exception {
        mockMvc.perform(post("/api/workers")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNo":"2026-0002","name":"이영희"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.zoneId").doesNotExist())
                .andExpect(jsonPath("$.data.processName").doesNotExist());
    }

    @Test
    @DisplayName("사번이 중복되면 409 를 반환한다")
    void create_duplicateEmployeeNo() throws Exception {
        saveWorker(zoneA, "2026-0001", "김철수");

        mockMvc.perform(post("/api/workers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeNo":"2026-0001","name":"동명이인"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMPLOYEE_NO"));
    }

    @Test
    @DisplayName("없는 구역에 배정하면 404 를 반환한다")
    void create_unknownZone() throws Exception {
        mockMvc.perform(post("/api/workers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneId":99999,"employeeNo":"2026-0003","name":"박민수"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ZONE_NOT_FOUND"));
    }

    /* ---------- 조회 ---------- */

    @Test
    @DisplayName("zoneId 로 작업자를 필터링할 수 있다")
    void list_filterByZone() throws Exception {
        saveWorker(zoneA, "2026-0001", "김철수");
        saveWorker(zoneB, "2026-0002", "이영희");

        mockMvc.perform(get("/api/workers")
                        .param("zoneId", String.valueOf(zoneA.getId()))
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("김철수"));
    }

    @Test
    @DisplayName("safetyStatus 로 위험 작업자만 추릴 수 있다")
    void list_filterBySafetyStatus() throws Exception {
        saveWorker(zoneA, "2026-0001", "김철수");
        Worker danger = saveWorker(zoneA, "2026-0002", "이영희");
        danger.changeSafetyStatus(SafetyStatus.DANGER);
        workerRepository.save(danger);

        mockMvc.perform(get("/api/workers")
                        .param("safetyStatus", "DANGER")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("이영희"));
    }

    @Test
    @DisplayName("구역 내 작업자 목록을 조회할 수 있다")
    void listByZone() throws Exception {
        saveWorker(zoneA, "2026-0001", "김철수");
        saveWorker(zoneB, "2026-0002", "이영희");

        mockMvc.perform(get("/api/zones/" + zoneA.getId() + "/workers")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    /* ---------- 상태 변경 ---------- */

    @Test
    @DisplayName("안전상태를 변경할 수 있다")
    void changeSafetyStatus() throws Exception {
        Worker worker = saveWorker(zoneA, "2026-0001", "김철수");

        mockMvc.perform(patch("/api/workers/" + worker.getId() + "/safety-status")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"safetyStatus":"EMERGENCY"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.safetyStatus").value("EMERGENCY"));
    }

    @Test
    @DisplayName("안전상태가 비어 있으면 400 을 반환한다")
    void changeSafetyStatus_validationFailure() throws Exception {
        Worker worker = saveWorker(zoneA, "2026-0001", "김철수");

        mockMvc.perform(patch("/api/workers/" + worker.getId() + "/safety-status")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("safetyStatus"));
    }

    @Test
    @DisplayName("작업자를 다른 구역으로 옮길 수 있다")
    void changeZone() throws Exception {
        Worker worker = saveWorker(zoneA, "2026-0001", "김철수");

        mockMvc.perform(patch("/api/workers/" + worker.getId() + "/zone")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneId":%d}""".formatted(zoneB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zoneCode").value("B-1"));
    }

    @Test
    @DisplayName("zoneId 에 null 을 보내면 구역 미배정 상태가 된다")
    void changeZone_unassign() throws Exception {
        Worker worker = saveWorker(zoneA, "2026-0001", "김철수");

        mockMvc.perform(patch("/api/workers/" + worker.getId() + "/zone")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"zoneId":null}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.zoneId").doesNotExist());
    }

    /* ---------- 퇴사 ---------- */

    @Test
    @DisplayName("퇴사 처리하면 재직 여부가 false 가 되고 구역에서도 빠진다")
    void resign() throws Exception {
        Worker worker = saveWorker(zoneA, "2026-0001", "김철수");

        mockMvc.perform(delete("/api/workers/" + worker.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/workers/" + worker.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false))
                .andExpect(jsonPath("$.data.zoneId").doesNotExist());
    }

    @Test
    @DisplayName("MANAGER 는 퇴사 처리를 할 수 없다")
    void resign_byManager_forbidden() throws Exception {
        Worker worker = saveWorker(zoneA, "2026-0001", "김철수");

        mockMvc.perform(delete("/api/workers/" + worker.getId())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("퇴사자의 안전상태는 변경할 수 없다")
    void changeSafetyStatus_resignedWorker() throws Exception {
        Worker worker = saveWorker(zoneA, "2026-0001", "김철수");
        mockMvc.perform(delete("/api/workers/" + worker.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/workers/" + worker.getId() + "/safety-status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"safetyStatus":"DANGER"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORKER_NOT_ACTIVE"));
    }

    /* ---------- 구역 삭제 가드 ---------- */

    @Test
    @DisplayName("재직 작업자가 남아 있으면 구역을 삭제할 수 없다")
    void deleteZone_withActiveWorkers_rejected() throws Exception {
        saveWorker(zoneA, "2026-0001", "김철수");

        mockMvc.perform(delete("/api/zones/" + zoneA.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ZONE_HAS_WORKERS"));
    }

    @Test
    @DisplayName("작업자를 퇴사 처리하면 구역을 삭제할 수 있다")
    void deleteZone_afterResign() throws Exception {
        Worker worker = saveWorker(zoneA, "2026-0001", "김철수");

        mockMvc.perform(delete("/api/workers/" + worker.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/zones/" + zoneA.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    /* ---------- 헬퍼 ---------- */

    private void saveManager(String loginId, ManagerRole role) {
        managerRepository.save(Manager.builder()
                .loginId(loginId).password(passwordEncoder.encode(PASSWORD))
                .name(loginId).role(role).build());
    }

    private Worker saveWorker(Zone zone, String employeeNo, String name) {
        return workerRepository.save(Worker.builder()
                .zone(zone).employeeNo(employeeNo).name(name).build());
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
