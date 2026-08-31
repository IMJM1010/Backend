package com.example.be.domain.wearabledevice;

import com.example.be.domain.manager.entity.Manager;
import com.example.be.domain.manager.entity.ManagerRole;
import com.example.be.domain.manager.repository.ManagerRepository;
import com.example.be.domain.vitalrecord.entity.VitalRecord;
import com.example.be.domain.vitalrecord.repository.VitalRecordRepository;
import com.example.be.domain.wearabledevice.entity.DeviceType;
import com.example.be.domain.wearabledevice.entity.WearableDevice;
import com.example.be.domain.wearabledevice.repository.WearableDeviceRepository;
import com.example.be.domain.worker.entity.Worker;
import com.example.be.domain.worker.repository.WorkerRepository;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("웨어러블 디바이스 · 생체 기록 API")
class DeviceVitalApiTest {

    private static final String PASSWORD = "password123!";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ManagerRepository managerRepository;
    @Autowired
    private WorkerRepository workerRepository;
    @Autowired
    private WearableDeviceRepository deviceRepository;
    @Autowired
    private VitalRecordRepository vitalRecordRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String memberToken;
    private Worker worker;

    @BeforeEach
    void setUp() throws Exception {
        saveManager("admin01", ManagerRole.ADMIN);
        saveManager("member01", ManagerRole.MANAGER);
        adminToken = login("admin01");
        memberToken = login("member01");

        worker = workerRepository.save(Worker.builder()
                .employeeNo("2026-0001").name("김철수").build());
    }

    /* ---------- 디바이스 ---------- */

    @Test
    @DisplayName("디바이스를 등록하면 연결 상태는 DISCONNECTED 로 시작한다")
    void createDevice() throws Exception {
        mockMvc.perform(post("/api/wearable-devices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workerId":%d,"serialNo":"BAND-0001","deviceType":"BAND"}"""
                                .formatted(worker.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.serialNo").value("BAND-0001"))
                .andExpect(jsonPath("$.data.connectionStatus").value("DISCONNECTED"))
                .andExpect(jsonPath("$.data.workerName").value("김철수"));
    }

    @Test
    @DisplayName("시리얼 번호가 중복되면 409 를 반환한다")
    void createDevice_duplicateSerial() throws Exception {
        saveDevice(worker, "BAND-0001", DeviceType.BAND);

        mockMvc.perform(post("/api/wearable-devices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serialNo":"BAND-0001","deviceType":"TAG"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SERIAL_NO"));
    }

    @Test
    @DisplayName("한 작업자에게 같은 종류의 디바이스를 두 개 배정할 수 없다")
    void createDevice_duplicateTypeForWorker() throws Exception {
        saveDevice(worker, "BAND-0001", DeviceType.BAND);

        mockMvc.perform(post("/api/wearable-devices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workerId":%d,"serialNo":"BAND-0002","deviceType":"BAND"}"""
                                .formatted(worker.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_DEVICE_TYPE_FOR_WORKER"));
    }

    @Test
    @DisplayName("다른 종류라면 같은 작업자에게 함께 배정할 수 있다")
    void createDevice_differentTypeForSameWorker() throws Exception {
        saveDevice(worker, "BAND-0001", DeviceType.BAND);

        mockMvc.perform(post("/api/wearable-devices")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workerId":%d,"serialNo":"HELMET-0001","deviceType":"HELMET"}"""
                                .formatted(worker.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("MANAGER 도 디바이스 상태는 갱신할 수 있다")
    void updateStatus_byManager() throws Exception {
        WearableDevice device = saveDevice(worker, "BAND-0001", DeviceType.BAND);

        mockMvc.perform(patch("/api/wearable-devices/" + device.getId() + "/status")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batteryLevel":78,"connectionStatus":"CONNECTED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batteryLevel").value(78))
                .andExpect(jsonPath("$.data.connectionStatus").value("CONNECTED"))
                .andExpect(jsonPath("$.data.lastSyncedAt").isNotEmpty());
    }

    @Test
    @DisplayName("배터리 잔량이 100 을 넘으면 400 을 반환한다")
    void updateStatus_batteryOutOfRange() throws Exception {
        WearableDevice device = saveDevice(worker, "BAND-0001", DeviceType.BAND);

        mockMvc.perform(patch("/api/wearable-devices/" + device.getId() + "/status")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batteryLevel":150}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("batteryLevel"));
    }

    @Test
    @DisplayName("상태 갱신으로는 RETIRED 가 되지 않는다")
    void updateStatus_cannotRetire() throws Exception {
        WearableDevice device = saveDevice(worker, "BAND-0001", DeviceType.BAND);

        mockMvc.perform(patch("/api/wearable-devices/" + device.getId() + "/status")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"connectionStatus":"RETIRED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.connectionStatus").value("DISCONNECTED"));
    }

    @Test
    @DisplayName("MANAGER 는 디바이스를 등록할 수 없다")
    void createDevice_byManager_forbidden() throws Exception {
        mockMvc.perform(post("/api/wearable-devices")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"serialNo":"BAND-0009","deviceType":"BAND"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("폐기 처리하면 RETIRED 가 되고 작업자 배정이 해제된다")
    void retireDevice() throws Exception {
        WearableDevice device = saveDevice(worker, "BAND-0001", DeviceType.BAND);

        mockMvc.perform(delete("/api/wearable-devices/" + device.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/wearable-devices/" + device.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.connectionStatus").value("RETIRED"))
                .andExpect(jsonPath("$.data.workerId").doesNotExist());
    }

    @Test
    @DisplayName("작업자가 착용 중인 디바이스 목록을 조회할 수 있다")
    void getDevicesByWorker() throws Exception {
        saveDevice(worker, "BAND-0001", DeviceType.BAND);
        saveDevice(worker, "HELMET-0001", DeviceType.HELMET);

        mockMvc.perform(get("/api/workers/" + worker.getId() + "/wearable-devices")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    /* ---------- 생체 기록 ---------- */

    @Test
    @DisplayName("생체 기록을 수집할 수 있다")
    void createVitalRecord() throws Exception {
        WearableDevice device = saveDevice(worker, "BAND-0001", DeviceType.BAND);

        mockMvc.perform(post("/api/vital-records")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":%d,"heartRate":78,"bodyTemp":36.5,"stepCount":4210,"measuredAt":"%s"}"""
                                .formatted(device.getId(), LocalDateTime.now().minusMinutes(1).format(ISO))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.heartRate").value(78))
                .andExpect(jsonPath("$.data.workerName").value("김철수"));
    }

    @Test
    @DisplayName("측정 불가능한 심박수는 400 으로 거절한다")
    void createVitalRecord_outOfRange() throws Exception {
        WearableDevice device = saveDevice(worker, "BAND-0001", DeviceType.BAND);

        mockMvc.perform(post("/api/vital-records")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":%d,"heartRate":500,"measuredAt":"%s"}"""
                                .formatted(device.getId(), LocalDateTime.now().format(ISO))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[0].field").value("heartRate"));
    }

    @Test
    @DisplayName("미래 시각으로 들어온 측정값은 거절한다")
    void createVitalRecord_futureMeasuredAt() throws Exception {
        WearableDevice device = saveDevice(worker, "BAND-0001", DeviceType.BAND);

        mockMvc.perform(post("/api/vital-records")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":%d,"heartRate":78,"measuredAt":"%s"}"""
                                .formatted(device.getId(), LocalDateTime.now().plusHours(2).format(ISO))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FUTURE_MEASURED_AT"));
    }

    @Test
    @DisplayName("폐기된 디바이스로는 기록을 수집할 수 없다")
    void createVitalRecord_retiredDevice() throws Exception {
        WearableDevice device = saveDevice(worker, "BAND-0001", DeviceType.BAND);
        mockMvc.perform(delete("/api/wearable-devices/" + device.getId())
                .header("Authorization", "Bearer " + adminToken));

        mockMvc.perform(post("/api/vital-records")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deviceId":%d,"heartRate":78,"measuredAt":"%s"}"""
                                .formatted(device.getId(), LocalDateTime.now().format(ISO))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEVICE_RETIRED"));
    }

    @Test
    @DisplayName("기간 없이 목록을 조회하면 400 을 반환한다")
    void listVitalRecords_withoutPeriod() throws Exception {
        mockMvc.perform(get("/api/vital-records")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }

    @Test
    @DisplayName("조회 기간이 7일을 넘으면 400 을 반환한다")
    void listVitalRecords_periodTooLong() throws Exception {
        mockMvc.perform(get("/api/vital-records")
                        .param("from", LocalDateTime.now().minusDays(30).format(ISO))
                        .param("to", LocalDateTime.now().format(ISO))
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PERIOD_TOO_LONG"));
    }

    @Test
    @DisplayName("시작 일시가 종료 일시보다 늦으면 400 을 반환한다")
    void listVitalRecords_invalidPeriod() throws Exception {
        mockMvc.perform(get("/api/vital-records")
                        .param("from", LocalDateTime.now().format(ISO))
                        .param("to", LocalDateTime.now().minusDays(1).format(ISO))
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD"));
    }

    @Test
    @DisplayName("기간 안의 기록만 조회된다")
    void listVitalRecords_withinPeriod() throws Exception {
        WearableDevice device = saveDevice(worker, "BAND-0001", DeviceType.BAND);
        saveVitalRecord(device, 70, LocalDateTime.now().minusHours(1));
        saveVitalRecord(device, 90, LocalDateTime.now().minusDays(10));

        mockMvc.perform(get("/api/vital-records")
                        .param("deviceId", String.valueOf(device.getId()))
                        .param("from", LocalDateTime.now().minusDays(1).format(ISO))
                        .param("to", LocalDateTime.now().format(ISO))
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].heartRate").value(70));
    }

    @Test
    @DisplayName("실시간 조회는 가장 최근 측정값 1건을 반환한다")
    void realtime() throws Exception {
        WearableDevice device = saveDevice(worker, "BAND-0001", DeviceType.BAND);
        saveVitalRecord(device, 70, LocalDateTime.now().minusHours(3));
        saveVitalRecord(device, 95, LocalDateTime.now().minusMinutes(1));
        saveVitalRecord(device, 80, LocalDateTime.now().minusHours(1));

        mockMvc.perform(get("/api/vital-records/realtime")
                        .param("workerId", String.valueOf(worker.getId()))
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.heartRate").value(95));
    }

    @Test
    @DisplayName("기록이 없는 작업자의 실시간 조회는 404 를 반환한다")
    void realtime_noRecord() throws Exception {
        mockMvc.perform(get("/api/vital-records/realtime")
                        .param("workerId", String.valueOf(worker.getId()))
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_VITAL_RECORD"));
    }

    @Test
    @DisplayName("MANAGER 는 생체 기록을 삭제할 수 없다")
    void deleteVitalRecord_byManager_forbidden() throws Exception {
        WearableDevice device = saveDevice(worker, "BAND-0001", DeviceType.BAND);
        VitalRecord record = saveVitalRecord(device, 70, LocalDateTime.now());

        mockMvc.perform(delete("/api/vital-records/" + record.getId())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    /* ---------- 헬퍼 ---------- */

    private void saveManager(String loginId, ManagerRole role) {
        managerRepository.save(Manager.builder()
                .loginId(loginId).password(passwordEncoder.encode(PASSWORD))
                .name(loginId).role(role).build());
    }

    private WearableDevice saveDevice(Worker worker, String serialNo, DeviceType type) {
        return deviceRepository.save(WearableDevice.builder()
                .worker(worker).serialNo(serialNo).deviceType(type).build());
    }

    private VitalRecord saveVitalRecord(WearableDevice device, int heartRate, LocalDateTime measuredAt) {
        return vitalRecordRepository.save(VitalRecord.builder()
                .device(device).heartRate(heartRate)
                .bodyTemp(new BigDecimal("36.5")).stepCount(1000)
                .measuredAt(measuredAt).build());
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
