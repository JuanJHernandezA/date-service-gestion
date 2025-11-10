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
@Import(DateService.class) // Importa el servicio real con EntityManager
@Transactional
class DateServiceTest {

    @Autowired
    private DateService dateService;

    @PersistenceContext
    private EntityManager entityManager;

    private final Long idPsicologo = 1L;

    @BeforeEach
    void setup() {
        // Crear disponibilidad inicial (9:00 - 13:00)
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
        // Cita de 10:00 a 11:00
        Date cita = new Date(
                idPsicologo,
                100L,
                LocalDate.of(2025, 11, 10),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)
        );

        dateService.addDate(cita);

        // 🔹 Verificar que se guardó la cita
        List<Date> citas = entityManager
                .createQuery("SELECT c FROM Date c", Date.class)
                .getResultList();

        assertEquals(1, citas.size(), "Debe haber una cita agendada");

        // 🔹 Verificar que se crearon dos nuevas disponibilidades
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
        // Cita fuera del rango disponible
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
        // Cita que ocupa todo el rango 9:00–13:00
        Date cita = new Date(
                idPsicologo,
                300L,
                LocalDate.of(2025, 11, 10),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0)
        );

        dateService.addDate(cita);

        // 🔹 Verificar que se guardó la cita
        List<Date> citas = entityManager
                .createQuery("SELECT c FROM Date c", Date.class)
                .getResultList();
        assertEquals(1, citas.size());

        // 🔹 Verificar que no quedan disponibilidades
        List<Disponibilidad> disponibilidades = entityManager
                .createQuery("SELECT d FROM Disponibilidad d", Disponibilidad.class)
                .getResultList();
        assertTrue(disponibilidades.isEmpty(), "No debe quedar disponibilidad");
    }
}
