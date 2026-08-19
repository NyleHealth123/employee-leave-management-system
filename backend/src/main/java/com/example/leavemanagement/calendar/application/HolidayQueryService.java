package com.example.leavemanagement.calendar.application;
import com.example.leavemanagement.policy.persistence.CompanyHolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
@Service @Transactional(readOnly=true)
public class HolidayQueryService {
 private final CompanyHolidayRepository holidays;public HolidayQueryService(CompanyHolidayRepository holidays){this.holidays=holidays;}
 public Set<LocalDate> activeDates(LocalDate from,LocalDate to){return holidays.findAllByActiveTrueAndDateBetween(from,to).stream().map(h->h.getDate()).collect(java.util.stream.Collectors.toUnmodifiableSet());}
 public List<HolidayView> list(LocalDate from,LocalDate to){return holidays.findAllByActiveTrueAndDateBetweenOrderByDate(from,to).stream().map(h->new HolidayView(h.getId(),h.getDate(),h.getName(),h.isActive(),h.getVersion())).toList();}
 public record HolidayView(UUID id,LocalDate date,String name,boolean active,long version){}
}

