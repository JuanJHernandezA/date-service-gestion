package com.example.dateservice.controller;

import com.example.dateservice.entity.Date;
import com.example.dateservice.service.DateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dates")
@CrossOrigin("*")
public class DateController {

    private final DateService DateService;

    public DateController(DateService DateService) {
        this.DateService = DateService;
    }

    @PostMapping("/addDate")
    public ResponseEntity<Date> addDate(@RequestBody Date Date) {
        return ResponseEntity.ok(DateService.addDate(Date));
    }

    @GetMapping("/getDates")
    public ResponseEntity<List<Date>> getAllDates() {
        return ResponseEntity.ok(DateService.getAllDates());
    }
}
