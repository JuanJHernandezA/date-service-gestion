package com.example.dateservice.service;

import com.example.dateservice.entity.Date;
import com.example.dateservice.entity.Disponibilidad;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(DateService.class) 
@Transactional
class DateServiceTest {

    @Autowired
    private DateService dateService;

    @PersistenceContext
    private EntityManager entityManager;

    private final Long idPsicologo = 1L;

    @BeforeEach
    void setup() {
       
        Disponibilidad disponibilidad = new Disponibilidad(
                idPsicologo,
                LocalDate.of(2025, 11, 10),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        );
        entityManager.persist(disponibilidad);
    }

    @Test
    void testAgendarCitaDivideDisponibilidad() {
       
        Date cita = new Date(
                idPsicologo,
                100L,
                LocalDate.of(2025, 11, 10),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        dateService.addDate(cita);

        
        List<Date> citas = entityManager
                .createQuery("SELECT c FROM Date c", Date.class)
                .getResultList();

        assertEquals(1, citas.size(), "Debe haber una cita agendada");

        
        List<Disponibilidad> disponibilidades = entityManager
                .createQuery("SELECT d FROM Disponibilidad d ORDER BY d.horaInicio", Disponibilidad.class)
                .getResultList();

        assertEquals(2, disponibilidades.size(), "Debe haber dos disponibilidades después de agendar");

        assertEquals(LocalTime.of(9, 0), disponibilidades.get(0).getHoraInicio());
        assertEquals(LocalTime.of(10, 0), disponibilidades.get(0).getHoraFin());

        assertEquals(LocalTime.of(11, 0), disponibilidades.get(1).getHoraInicio());
        assertEquals(LocalTime.of(13, 0), disponibilidades.get(1).getHoraFin());
    }

    @Test
    void testAgendarCitaSinDisponibilidadLanzaExcepcion() {
      
        Date cita = new Date(
                idPsicologo,
                200L,
                LocalDate.of(2025, 11, 10),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0)
        );

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dateService.addDate(cita));
        assertTrue(ex.getMessage().contains("No hay disponibilidad"));
    }

    @Test
    void testAgendarCitaEliminaDisponibilidadExacta() {
      
        Date cita = new Date(
                idPsicologo,
                300L,
                LocalDate.of(2025, 11, 10),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        );

        dateService.addDate(cita);

       
        List<Date> citas = entityManager
                .createQuery("SELECT c FROM Date c", Date.class)
                .getResultList();
        assertEquals(1, citas.size());

        
        List<Disponibilidad> disponibilidades = entityManager
                .createQuery("SELECT d FROM Disponibilidad d", Disponibilidad.class)
                .getResultList();
        assertTrue(disponibilidades.isEmpty(), "No debe quedar disponibilidad");
    }  

    @Test
    void testActualizarDisponibilidadExito() {
    Disponibilidad existente = entityManager.createQuery(
            "SELECT d FROM Disponibilidad d WHERE d.idPsicologo = :idPsicologo AND d.fecha = :fecha",
            Disponibilidad.class)
        .setParameter("idPsicologo", idPsicologo)
        .setParameter("fecha", LocalDate.of(2025, 11, 10))
        .getSingleResult();

    Long idDisp = existente.getId();

    Disponibilidad actualizada = new Disponibilidad(
            idPsicologo,
            existente.getFecha(),
            LocalTime.of(8, 0),
            LocalTime.of(12, 0)
    );

    Disponibilidad resultado = dateService.actualizarDisponibilidad(idDisp, actualizada);

    assertNotNull(resultado, "Debe retornar la disponibilidad actualizada");
    assertEquals(LocalTime.of(8, 0), resultado.getHoraInicio());
    assertEquals(LocalTime.of(12, 0), resultado.getHoraFin());

    Disponibilidad desdeDb = entityManager.find(Disponibilidad.class, idDisp);
    assertNotNull(desdeDb);
    assertEquals(LocalTime.of(8, 0), desdeDb.getHoraInicio());
    assertEquals(LocalTime.of(12, 0), desdeDb.getHoraFin());
}

@Test
void testActualizarDisponibilidadConCitasSolapadasLanzaExcepcion() {
    Date cita = new Date(
            idPsicologo,
            400L,
            LocalDate.of(2025, 11, 10),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0)
    );
    entityManager.persist(cita);
    entityManager.flush();

    Disponibilidad existente = entityManager.createQuery(
            "SELECT d FROM Disponibilidad d WHERE d.idPsicologo = :idPsicologo AND d.fecha = :fecha",
            Disponibilidad.class)
        .setParameter("idPsicologo", idPsicologo)
        .setParameter("fecha", LocalDate.of(2025, 11, 10))
        .getSingleResult();

    Long idDisp = existente.getId();

    // Intentar reducir la disponibilidad de 9:00-13:00 a 9:00-10:30
    // La cita está de 10:00-11:00, por lo que quedaría fuera del nuevo rango (termina a las 10:30)
    Disponibilidad propuesta = new Disponibilidad(
            idPsicologo,
            existente.getFecha(),
            LocalTime.of(9, 0),
            LocalTime.of(10, 30)
    );

    RuntimeException ex = assertThrows(RuntimeException.class,
            () -> dateService.actualizarDisponibilidad(idDisp, propuesta));
    assertTrue(ex.getMessage().contains("No se puede modificar el horario"),
            "Debe lanzar excepción indicando que existen citas agendadas en ese rango");
}

@Test
void testActualizarDisponibilidadHorasInvalidasLanzaExcepcion() {
    // Obtener la disponibilidad existente
    Disponibilidad existente = entityManager.createQuery(
            "SELECT d FROM Disponibilidad d WHERE d.idPsicologo = :idPsicologo AND d.fecha = :fecha",
            Disponibilidad.class)
        .setParameter("idPsicologo", idPsicologo)
        .setParameter("fecha", LocalDate.of(2025, 11, 10))
        .getSingleResult();

    Long idDisp = existente.getId();

    // Caso 1: horaInicio igual a horaFin
    Disponibilidad igualHoras = new Disponibilidad(
            idPsicologo,
            existente.getFecha(),
            LocalTime.of(10, 0),
            LocalTime.of(10, 0)
    );

    RuntimeException ex1 = assertThrows(RuntimeException.class,
            () -> dateService.actualizarDisponibilidad(idDisp, igualHoras));
    assertTrue(ex1.getMessage().contains("La hora de inicio debe ser anterior"),
            "Debe lanzar excepción cuando horaInicio == horaFin");

    // Caso 2: horaInicio después de horaFin
    Disponibilidad horasInvertidas = new Disponibilidad(
            idPsicologo,
            existente.getFecha(),
            LocalTime.of(14, 0),
            LocalTime.of(13, 0)
    );

    RuntimeException ex2 = assertThrows(RuntimeException.class,
            () -> dateService.actualizarDisponibilidad(idDisp, horasInvertidas));
    assertTrue(ex2.getMessage().contains("La hora de inicio debe ser anterior"),
            "Debe lanzar excepción cuando horaInicio > horaFin");
}

    // Tests para crearDisponibilidad
    @Test
    void testCrearDisponibilidadExito() {
        Disponibilidad nueva = new Disponibilidad(
                2L,
                LocalDate.of(2025, 12, 15),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );

        Disponibilidad resultado = dateService.crearDisponibilidad(nueva);

        assertNotNull(resultado, "Debe retornar la disponibilidad creada");
        assertNotNull(resultado.getId(), "Debe tener un ID asignado");
        assertEquals(2L, resultado.getIdPsicologo());
        assertEquals(LocalDate.of(2025, 12, 15), resultado.getFecha());
        assertEquals(LocalTime.of(10, 0), resultado.getHoraInicio());
        assertEquals(LocalTime.of(12, 0), resultado.getHoraFin());

        Disponibilidad desdeDb = entityManager.find(Disponibilidad.class, resultado.getId());
        assertNotNull(desdeDb, "Debe estar persistida en la base de datos");
    }

    @Test
    void testCrearDisponibilidadConHoraInicioNulaLanzaExcepcion() {
        Disponibilidad nueva = new Disponibilidad(
                2L,
                LocalDate.of(2025, 12, 15),
                null,
                LocalTime.of(12, 0)
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dateService.crearDisponibilidad(nueva));
        assertTrue(ex.getMessage().contains("La hora de inicio y fin son requeridas"),
                "Debe lanzar excepción cuando horaInicio es null");
    }

    @Test
    void testCrearDisponibilidadConHoraFinNulaLanzaExcepcion() {
        Disponibilidad nueva = new Disponibilidad(
                2L,
                LocalDate.of(2025, 12, 15),
                LocalTime.of(10, 0),
                null
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dateService.crearDisponibilidad(nueva));
        assertTrue(ex.getMessage().contains("La hora de inicio y fin son requeridas"),
                "Debe lanzar excepción cuando horaFin es null");
    }

    @Test
    void testCrearDisponibilidadConHoraInicioMayorQueHoraFinLanzaExcepcion() {
        Disponibilidad nueva = new Disponibilidad(
                2L,
                LocalDate.of(2025, 12, 15),
                LocalTime.of(14, 0),
                LocalTime.of(12, 0)
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dateService.crearDisponibilidad(nueva));
        assertTrue(ex.getMessage().contains("La hora de inicio debe ser anterior"),
                "Debe lanzar excepción cuando horaInicio > horaFin");
    }

    @Test
    void testCrearDisponibilidadConHoraInicioIgualAHoraFinLanzaExcepcion() {
        Disponibilidad nueva = new Disponibilidad(
                2L,
                LocalDate.of(2025, 12, 15),
                LocalTime.of(12, 0),
                LocalTime.of(12, 0)
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dateService.crearDisponibilidad(nueva));
        assertTrue(ex.getMessage().contains("La hora de inicio debe ser anterior"),
                "Debe lanzar excepción cuando horaInicio == horaFin");
    }

    // Tests para crearDisponibilidadesMasivas
    @Test
    void testCrearDisponibilidadesMasivasExito() {
        LocalDate fechaInicio = LocalDate.of(2025, 12, 1);
        LocalDate fechaFin = LocalDate.of(2025, 12, 5);
        LocalTime horaInicio = LocalTime.of(9, 0);
        LocalTime horaFin = LocalTime.of(13, 0);

        dateService.crearDisponibilidadesMasivas(3L, fechaInicio, fechaFin, horaInicio, horaFin);

        List<Disponibilidad> disponibilidades = entityManager
                .createQuery("SELECT d FROM Disponibilidad d WHERE d.idPsicologo = :idPsicologo ORDER BY d.fecha",
                        Disponibilidad.class)
                .setParameter("idPsicologo", 3L)
                .getResultList();

        assertEquals(5, disponibilidades.size(), "Debe crear 5 disponibilidades (lunes a viernes)");
        
        for (Disponibilidad disp : disponibilidades) {
            assertEquals(3L, disp.getIdPsicologo());
            assertEquals(horaInicio, disp.getHoraInicio());
            assertEquals(horaFin, disp.getHoraFin());
            assertTrue(disp.getFecha().getDayOfWeek().getValue() >= 1 && 
                      disp.getFecha().getDayOfWeek().getValue() <= 5,
                      "Solo debe crear disponibilidades en días laborables");
        }
    }

    @Test
    void testCrearDisponibilidadesMasivasSoloDiasLaborables() {
        LocalDate fechaInicio = LocalDate.of(2025, 12, 6); // Sábado
        LocalDate fechaFin = LocalDate.of(2025, 12, 8); // Lunes
        LocalTime horaInicio = LocalTime.of(9, 0);
        LocalTime horaFin = LocalTime.of(13, 0);

        dateService.crearDisponibilidadesMasivas(4L, fechaInicio, fechaFin, horaInicio, horaFin);

        List<Disponibilidad> disponibilidades = entityManager
                .createQuery("SELECT d FROM Disponibilidad d WHERE d.idPsicologo = :idPsicologo ORDER BY d.fecha",
                        Disponibilidad.class)
                .setParameter("idPsicologo", 4L)
                .getResultList();

        assertEquals(1, disponibilidades.size(), "Debe crear solo 1 disponibilidad (lunes, salta sábado y domingo)");
        assertEquals(LocalDate.of(2025, 12, 8), disponibilidades.get(0).getFecha(), "Debe ser el lunes");
    }

    @Test
    void testCrearDisponibilidadesMasivasConHoraInicioNulaLanzaExcepcion() {
        LocalDate fechaInicio = LocalDate.of(2025, 12, 1);
        LocalDate fechaFin = LocalDate.of(2025, 12, 5);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dateService.crearDisponibilidadesMasivas(5L, fechaInicio, fechaFin, null, LocalTime.of(13, 0)));
        assertTrue(ex.getMessage().contains("La hora de inicio y fin son requeridas"),
                "Debe lanzar excepción cuando horaInicio es null");
    }

    @Test
    void testCrearDisponibilidadesMasivasConHoraFinNulaLanzaExcepcion() {
        LocalDate fechaInicio = LocalDate.of(2025, 12, 1);
        LocalDate fechaFin = LocalDate.of(2025, 12, 5);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dateService.crearDisponibilidadesMasivas(5L, fechaInicio, fechaFin, LocalTime.of(9, 0), null));
        assertTrue(ex.getMessage().contains("La hora de inicio y fin son requeridas"),
                "Debe lanzar excepción cuando horaFin es null");
    }

    @Test
    void testCrearDisponibilidadesMasivasConHorasInvalidasLanzaExcepcion() {
        LocalDate fechaInicio = LocalDate.of(2025, 12, 1);
        LocalDate fechaFin = LocalDate.of(2025, 12, 5);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dateService.crearDisponibilidadesMasivas(5L, fechaInicio, fechaFin, 
                        LocalTime.of(14, 0), LocalTime.of(12, 0)));
        assertTrue(ex.getMessage().contains("La hora de inicio debe ser anterior"),
                "Debe lanzar excepción cuando horaInicio > horaFin");
    }

    @Test
    void testCrearDisponibilidadesMasivasConHoraInicioIgualAHoraFinLanzaExcepcion() {
        LocalDate fechaInicio = LocalDate.of(2025, 12, 1);
        LocalDate fechaFin = LocalDate.of(2025, 12, 5);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> dateService.crearDisponibilidadesMasivas(5L, fechaInicio, fechaFin, 
                        LocalTime.of(12, 0), LocalTime.of(12, 0)));
        assertTrue(ex.getMessage().contains("La hora de inicio debe ser anterior"),
                "Debe lanzar excepción cuando horaInicio == horaFin");
    }
}
