package com.example.dateservice.repository;
import com.example.dateservice.entity.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {

    // Busca una disponibilidad que cubra desde horaInicio <= horaInicioCita AND horaFin >= horaFinCita
    Optional<Disponibilidad> findFirstByIdPsicologoAndFechaAndHoraInicioLessThanEqualAndHoraFinGreaterThanEqual(
            Long idPsicologo,
            LocalDate fecha,
            LocalTime horaInicioCita,
            LocalTime horaFinCita
    );

    // (opcional) otros métodos útiles, por ejemplo para listar disponibilidades por psicólogo y fecha:
    // List<Disponibilidad> findByIdPsicologoAndFecha(Long idPsicologo, LocalDate fecha);
}
