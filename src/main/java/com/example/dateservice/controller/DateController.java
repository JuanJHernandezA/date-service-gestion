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
            @RequestParam(required = false) Long idPsicologo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        // Si no se proporcionan filtros, retornar todas las disponibilidades
        if (idPsicologo == null && fecha == null) {
            List<Disponibilidad> todasLasDisponibilidades = dateService.listarTodasLasDisponibilidades();
            return ResponseEntity.ok(todasLasDisponibilidades);
        }
        // Si se proporcionan ambos filtros, usar el método existente
        if (idPsicologo != null && fecha != null) {
            List<Disponibilidad> disponibilidades = dateService.listarDisponibilidades(idPsicologo, fecha);
            return ResponseEntity.ok(disponibilidades);
        }
        // Si solo se proporciona un filtro, retornar todas y filtrar manualmente (o crear métodos específicos)
        // Por ahora, retornamos todas si falta algún parámetro
        List<Disponibilidad> todasLasDisponibilidades = dateService.listarTodasLasDisponibilidades();
        return ResponseEntity.ok(todasLasDisponibilidades);
    }

    @GetMapping("/disponibilidades/filtrar")
    public ResponseEntity<List<Disponibilidad>> filtrarDisponibilidades(
            @RequestParam(required = false) Long idPsicologo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer anio
    ) {
        List<Disponibilidad> disponibilidades = dateService.filtrarDisponibilidades(idPsicologo, fecha, mes, anio);
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

    @PutMapping("/modificar/{id}")
    public ResponseEntity<?> modificarCita(
            @PathVariable Long id,
            @RequestBody Date citaModificada
    ) {
        try {
            Date citaActualizada = dateService.modificarCita(id, citaModificada);
            return ResponseEntity.ok(citaActualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error al modificar cita: " + e.getMessage());
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

    @PutMapping("/disponibilidades/{id}")
    public ResponseEntity<?> actualizarDisponibilidad(
            @PathVariable Long id,
            @RequestBody Disponibilidad disponibilidadActualizada
    ) {
        try {
            Disponibilidad disponibilidadActualizadaResult = dateService.actualizarDisponibilidad(id, disponibilidadActualizada);
            return ResponseEntity.ok(disponibilidadActualizadaResult);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error al actualizar disponibilidad: " + e.getMessage());
        }
    }

    @GetMapping("/disponibilidades/todas")
    public ResponseEntity<List<Disponibilidad>> listarTodasLasDisponibilidades() {
        List<Disponibilidad> disponibilidades = dateService.listarTodasLasDisponibilidades();
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
