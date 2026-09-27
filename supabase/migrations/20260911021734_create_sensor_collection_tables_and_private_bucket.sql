
create table public.experiments (
 id uuid primary key default gen_random_uuid(),
 experiment_code text not null unique,
 operator_name text not null,
 device_id text not null,
 sensor_type text not null check (sensor_type in ('imu','temperature','gps','combined')),
 activity text not null,
 body_location text,
 started_at timestamptz not null,
 ended_at timestamptz,
 sampling_rate_hz numeric check (sampling_rate_hz > 0),
 raw_file_path text,
 notes text,
 created_at timestamptz not null default now(),
 check (ended_at is null or ended_at >= started_at)
);
comment on table public.experiments is '센서 수집 실험 목록. 시간은 시간대를 포함해 입력. raw_file_path는 sensor-raw 버킷 내 경로.';
create table public.temperature_readings (
 id bigint generated always as identity primary key,
 experiment_id uuid not null references public.experiments(id) on delete restrict,
 sample_no bigint not null check(sample_no >= 0),
 measured_at timestamptz not null,
 adc_value integer check(adc_value >= 0),
 temperature_c double precision,
 created_at timestamptz not null default now(),
 unique(experiment_id,sample_no),
 check (adc_value is not null or temperature_c is not null)
);
comment on table public.temperature_readings is 'NTC 10K 측정값. temperature_c 단위 섭씨. ADC 범위는 보드 해상도에 따름.';
create index temperature_experiment_time_idx on public.temperature_readings(experiment_id,measured_at);
create table public.gps_readings (
 id bigint generated always as identity primary key,
 experiment_id uuid not null references public.experiments(id) on delete restrict,
 sample_no bigint not null check(sample_no >= 0),
 measured_at timestamptz not null,
 latitude double precision check(latitude between -90 and 90),
 longitude double precision check(longitude between -180 and 180),
 speed_mps double precision check(speed_mps >= 0),
 satellites integer check(satellites >= 0),
 has_fix boolean not null,
 hdop double precision check(hdop >= 0),
 created_at timestamptz not null default now(),
 unique(experiment_id,sample_no),
 check(not has_fix or (latitude is not null and longitude is not null))
);
comment on table public.gps_readings is 'GPS 위경도는 decimal degrees, 속도는 m/s. Fix 실패 시 좌표는 NULL로 기록.';
create index gps_experiment_time_idx on public.gps_readings(experiment_id,measured_at);
create table public.imu_summaries (
 id bigint generated always as identity primary key,
 experiment_id uuid not null references public.experiments(id) on delete restrict,
 window_start timestamptz not null,
 window_end timestamptz not null,
 sample_count integer not null check(sample_count > 0),
 accel_magnitude_mean_mps2 double precision check(accel_magnitude_mean_mps2 >= 0),
 accel_magnitude_max_mps2 double precision check(accel_magnitude_max_mps2 >= 0),
 gyro_magnitude_mean_dps double precision check(gyro_magnitude_mean_dps >= 0),
 gyro_magnitude_max_dps double precision check(gyro_magnitude_max_dps >= 0),
 processing_version text not null,
 created_at timestamptz not null default now(),
 unique(experiment_id,window_start,window_end,processing_version),
 check(window_end > window_start)
);
comment on table public.imu_summaries is 'IMU 구간 요약. 가속도 벡터 크기는 m/s², 각속도 벡터 크기는 degree/s. 원본 6축 값은 CSV 보관. 처리 방식은 processing_version으로 구분.';
alter table public.experiments enable row level security;
alter table public.temperature_readings enable row level security;
alter table public.gps_readings enable row level security;
alter table public.imu_summaries enable row level security;
revoke all on public.experiments,public.temperature_readings,public.gps_readings,public.imu_summaries from anon,authenticated;
grant all on public.experiments,public.temperature_readings,public.gps_readings,public.imu_summaries to service_role;
insert into storage.buckets(id,name,public) values('sensor-raw','sensor-raw',false);
