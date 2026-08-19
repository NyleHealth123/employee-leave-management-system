package com.example.leavemanagement.calendar.api;
import com.example.leavemanagement.calendar.application.HolidayQueryService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/holidays")
public class HolidayController {
 private final HolidayQueryService holidays;public HolidayController(HolidayQueryService holidays){this.holidays=holidays;}
 @GetMapping public List<HolidayQueryService.HolidayView> list(@RequestParam LocalDate from,@RequestParam LocalDate to){return holidays.list(from,to);}
}

