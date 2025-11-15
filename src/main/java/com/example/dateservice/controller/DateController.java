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

    @DeleteMapping("/cancelar/{id}")
    public ResponseEntity<String> cancelarCita(@PathVariable Long id) {
        try {
            dateService.cancelarCita(id);
            return ResponseEntity.ok("Cita cancelada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error al cancelar cita: " + e.getMessage());
        }
    }


    @GetMapping("/todas")
    public ResponseEntity<List<Date>> listarTodasLasCitas() {
        List<Date> citas = dateService.listarTodasLasCitas();
        return ResponseEntity.ok(citas);
    }

    @GetMapping("/cliente/{idCliente}")
    public ResponseEntity<List<Date>> listarCitasPorCliente(@PathVariable Long idCliente) {
        List<Date> citas = dateService.listarCitasPorCliente(idCliente);
        return ResponseEntity.ok(citas);
    }

    @PostMapping("/disponibilidad")
    public ResponseEntity<Disponibilidad> crearDisponibilidad(@RequestBody Disponibilidad disponibilidad) {
        try {
            Disponibilidad nueva = dateService.crearDisponibilidad(disponibilidad);
            return ResponseEntity.ok(nueva);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/disponibilidades/masivas")
    public ResponseEntity<String> crearDisponibilidadesMasivas(
            @RequestParam(required = false) Long idPsicologo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) java.time.LocalTime horaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) java.time.LocalTime horaFin,
            @RequestBody(required = false) java.util.Map<String, Object> body
    ) {
        try {
            // Si viene en el body (JSON), usar esos valores
            if (body != null && !body.isEmpty()) {
                idPsicologo = Long.valueOf(body.get("idPsicologo").toString());
                fechaInicio = LocalDate.parse(body.get("fechaInicio").toString());
                fechaFin = LocalDate.parse(body.get("fechaFin").toString());
                horaInicio = java.time.LocalTime.parse(body.get("horaInicio").toString());
                horaFin = java.time.LocalTime.parse(body.get("horaFin").toString());
            }
            
            if (idPsicologo == null || fechaInicio == null || fechaFin == null || horaInicio == null || horaFin == null) {
                return ResponseEntity.badRequest().body("Todos los parámetros son requeridos: idPsicologo, fechaInicio, fechaFin, horaInicio, horaFin");
            }
            
            dateService.crearDisponibilidadesMasivas(idPsicologo, fechaInicio, fechaFin, horaInicio, horaFin);
            return ResponseEntity.ok("Disponibilidades creadas exitosamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear disponibilidades: " + e.getMessage());
        }
    }
}
