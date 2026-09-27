create table public.sensor_team_members (
 user_id uuid primary key references auth.users(id) on delete cascade,
 display_name text not null,
 active boolean not null default true,
 created_at timestamptz not null default now()
);
comment on table public.sensor_team_members is '수집 프로그램 접근 허용 명단. 관리자만 대시보드에서 등록/비활성화. Organization 팀원과 별개.';
alter table public.sensor_team_members enable row level security;
revoke all on public.sensor_team_members from anon,authenticated;
grant select on public.sensor_team_members to authenticated;
grant all on public.sensor_team_members to service_role;
create policy sensor_member_read_self on public.sensor_team_members for select to authenticated using (user_id=(select auth.uid()));
grant select,insert on public.experiments to authenticated;
create policy sensor_team_select on public.experiments for select to authenticated using (exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active));
create policy sensor_team_insert on public.experiments for insert to authenticated with check (exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active));
grant select,insert on public.temperature_readings to authenticated;
create policy sensor_team_select on public.temperature_readings for select to authenticated using (exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active));
create policy sensor_team_insert on public.temperature_readings for insert to authenticated with check (exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active));
grant select,insert on public.gps_readings to authenticated;
create policy sensor_team_select on public.gps_readings for select to authenticated using (exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active));
create policy sensor_team_insert on public.gps_readings for insert to authenticated with check (exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active));
grant select,insert on public.imu_summaries to authenticated;
create policy sensor_team_select on public.imu_summaries for select to authenticated using (exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active));
create policy sensor_team_insert on public.imu_summaries for insert to authenticated with check (exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active));
grant update on public.experiments to authenticated;
create policy sensor_team_update on public.experiments for update to authenticated using (exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active)) with check (exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active));
grant usage on sequence public.temperature_readings_id_seq,public.gps_readings_id_seq,public.imu_summaries_id_seq to authenticated;
create policy sensor_raw_team_read on storage.objects for select to authenticated using (bucket_id='sensor-raw' and exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active));
create policy sensor_raw_team_upload on storage.objects for insert to authenticated with check (
 bucket_id='sensor-raw' and (storage.foldername(name))[1] in ('imu','temperature','gps') and exists (select 1 from public.sensor_team_members m where m.user_id = (select auth.uid()) and m.active)
);
