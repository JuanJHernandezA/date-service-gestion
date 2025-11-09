package com.example.dateservice.service;

import com.example.dateservice.entity.Date;
import com.example.dateservice.repository.DateRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DateService {

    private final DateRepository DateRepository;

    public DateService(DateRepository DateRepository) {
        this.DateRepository = DateRepository;
    }

    public Date addDate(Date Date) {
        return DateRepository.save(Date);
    }

    public List<Date> getAllDates() {
        return DateRepository.findAll();
    }
}
