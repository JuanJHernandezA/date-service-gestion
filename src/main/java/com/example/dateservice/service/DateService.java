package com.example.dateservice.service;

import com.example.dateservice.entity.Date;
import com.example.dateservice.entity.Disponibilidad;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DateService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void addDate(Date nuevaCita) {
        System.out.println("➡️ Intentando agendar cita: " + nuevaCita);

        // Buscar disponibilidad que cubra completamente la franja de la cita
        List<Disponibilidad> resultados = entityManager.createQuery(
                "SELECT d FROM Disponibilidad d " +
                        "WHERE d.idPsicologo = :idPsicologo " +
                        "AND d.fecha = :fecha " +
                        "AND d.horaInicio <= :horaInicio " +
                        "AND d.horaFin >= :horaFin",
                Disponibilidad.class
        )
        .setParameter("idPsicologo", nuevaCita.getIdPsicologo())
        .setParameter("fecha", nuevaCita.getFecha())
        .setParameter("horaInicio", nuevaCita.getHoraInicio())
        .setParameter("horaFin", nuevaCita.getHoraFin())
        .setMaxResults(1)
        .getResultList();

        if (resultados.isEmpty()) {
            System.out.println("❌ No existe disponibilidad para esta cita.");
            throw new RuntimeException("No hay disponibilidad para la hora seleccionada.");
        }

        Disponibilidad disp = resultados.get(0);
        System.out.println("✅ Disponibilidad encontrada: " + disp);

        // Eliminar la disponibilidad actual (ya no es válida tal como está)
        entityManager.remove(disp);
        System.out.println("🗑️ Disponibilidad eliminada: " + disp.getId());

        // Crear nuevas disponibilidades si sobran espacios antes o después
        if (disp.getHoraInicio().isBefore(nuevaCita.getHoraInicio())) {
            Disponibilidad antes = new Disponibilidad(
                    disp.getIdPsicologo(),
                    disp.getFecha(),
                    disp.getHoraInicio(),
                    nuevaCita.getHoraInicio()
            );
            entityManager.persist(antes);
            System.out.println("🟩 Nueva disponibilidad (antes): " + antes);
        }

        if (disp.getHoraFin().isAfter(nuevaCita.getHoraFin())) {
            Disponibilidad despues = new Disponibilidad(
                    disp.getIdPsicologo(),
                    disp.getFecha(),
                    nuevaCita.getHoraFin(),
                    disp.getHoraFin()
            );
            entityManager.persist(despues);
            System.out.println("🟩 Nueva disponibilidad (después): " + despues);
        }

        // Guardar la nueva cita
        entityManager.persist(nuevaCita);
        System.out.println("📅 Cita registrada exitosamente: " + nuevaCita);
    }

    public List<Date> listarCitasPorPsicologo(Long idPsicologo, LocalDate fecha) {
        return entityManager.createQuery(
                "SELECT c FROM Date c WHERE c.idPsicologo = :idPsicologo AND c.fecha = :fecha",
                Date.class
        )
        .setParameter("idPsicologo", idPsicologo)
        .setParameter("fecha", fecha)
        .getResultList();
    }

    public List<Disponibilidad> listarDisponibilidades(Long idPsicologo, LocalDate fecha) {
        return entityManager.createQuery(
                "SELECT d FROM Disponibilidad d WHERE d.idPsicologo = :idPsicologo AND d.fecha = :fecha",
                Disponibilidad.class
        )
        .setParameter("idPsicologo", idPsicologo)
        .setParameter("fecha", fecha)
        .getResultList();
    }

    
}


  