package com.example.dateservice.repository;
import com.example.dateservice.entity.Disponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Long> {

   
    Optional<Disponibilidad> findFirstByIdPsicologoAndFechaAndHoraInicioLessThanEqualAndHoraFinGreaterThanEqual(
            Long idPsicologo,
            LocalDate fecha,
            LocalTime horaInicioCita,
            LocalTime horaFinCita
    );

   
}
