package com.example.leavemanagement.integration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
@SpringBootTest
class LeaveRequestOverlapRepositoryTest extends PostgresIntegrationTest {
 @Autowired JdbcTemplate jdbc;
 @Test void activeAmOrFullDayCannotOverlapButPmCan() {var user=UUID.randomUUID();var employee=UUID.randomUUID();var type=UUID.randomUUID();var policy=UUID.randomUUID();jdbc.update("insert into user_account values (?,?,?,?,?,?,?,?,?)",user,"u","u","hash",true,Instant.now(),Instant.now(),Instant.now(),0);jdbc.update("insert into employee_profile(id,employee_number,user_account_id,display_name,email,active,created_at,updated_at,version) values (?,?,?,?,?,?,?,?,0)",employee,"E1",user,"Employee","e@example.com",true,Instant.now(),Instant.now());jdbc.update("insert into leave_type values (?,?,?,?,?,0)",type,"ANNUAL","Annual",null,true);jdbc.update("insert into leave_policy_version values (?,?,?,?,?,?,?,?,?,?,?,?)",policy,type,1,LocalDate.of(2026,1,1),null,true,true,"EXCLUDE","EXCLUDE",false,1,Instant.now());var first=insertRequest(employee,type,policy,"first");var second=insertRequest(employee,type,policy,"second");jdbc.update("insert into leave_request_slot values (?,?,?,?,?,true)",UUID.randomUUID(),first,employee,LocalDate.of(2026,9,1),"AM");jdbc.update("insert into leave_request_slot values (?,?,?,?,?,true)",UUID.randomUUID(),second,employee,LocalDate.of(2026,9,1),"PM");assertThatThrownBy(()->jdbc.update("insert into leave_request_slot values (?,?,?,?,?,true)",UUID.randomUUID(),second,employee,LocalDate.of(2026,9,1),"AM")).isInstanceOf(DuplicateKeyException.class);}
 private UUID insertRequest(UUID employee,UUID type,UUID policy,String key){var id=UUID.randomUUID();jdbc.update("insert into leave_request(id,employee_id,leave_type_id,submitted_policy_version_id,start_date,end_date,duration_mode,chargeable_units,reason,status,submitted_at,policy_snapshot,idempotency_key,version) values (?,?,?,?,?,?,?,?,?,?,?,?,?,0)",id,employee,type,policy,LocalDate.of(2026,9,1),LocalDate.of(2026,9,1),"FULL_DAY",2,"Rest","PENDING",Instant.now(),"{}",key);return id;}
}
