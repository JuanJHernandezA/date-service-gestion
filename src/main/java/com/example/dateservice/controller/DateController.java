package com.example.dateservice.controller;

import com.example.dateservice.entity.Date;
import com.example.dateservice.entity.Disponibilidad;
import com.example.dateservice.service.DateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dates")
@CrossOrigin(origins = "*") 
public class DateController {

    @Autowired
    private DateService dateService;

    
    @PostMapping("/agendar")
    public ResponseEntity<String> agendarCita(@RequestBody Date nuevaCita) {
        try {
            dateService.addDate(nuevaCita);
            return ResponseEntity.ok("Cita agendada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error al agendar cita: " + e.getMessage());
        }
    }


    @GetMapping("/citas")
    public ResponseEntity<List<Date>> listarCitas(
            @RequestParam Long idPsicologo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        List<Date> citas = dateService.listarCitasPorPsicologo(idPsicologo, fecha);
        return ResponseEntity.ok(citas);
    }

    @GetMapping("/disponibilidades")
    public ResponseEntity<List<Disponibilidad>> listarDisponibilidades(
            @RequestParam Long idPsicologo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        List<Disponibilidad> disponibilidades = dateService.listarDisponibilidades(idPsicologo, fecha);
        return ResponseEntity.ok(disponibilidades);
    }

    @PostMapping("/disponibilidades")
    public ResponseEntity<?> agregarDisponibilidad(@RequestBody Disponibilidad nuevaDisponibilidad) {
        try {
            Disponibilidad disponibilidadCreada = dateService.addDisponibilidad(nuevaDisponibilidad);
            return ResponseEntity.ok(disponibilidadCreada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error al agregar disponibilidad: " + e.getMessage());
        }
    }
}
