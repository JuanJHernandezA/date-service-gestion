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
        System.out.println("Intentando agendar cita: " + nuevaCita);

        // Validaciones básicas
        if (nuevaCita.getIdPsicologo() == null) {
            throw new RuntimeException("El ID del psicólogo es requerido.");
        }

        if (nuevaCita.getIdCliente() == null) {
            throw new RuntimeException("El ID del cliente es requerido.");
        }

        if (nuevaCita.getFecha() == null) {
            throw new RuntimeException("La fecha es requerida.");
        }

        if (nuevaCita.getHoraInicio() == null || nuevaCita.getHoraFin() == null) {
            throw new RuntimeException("La hora de inicio y fin son requeridas.");
        }

        if (!nuevaCita.getHoraInicio().isBefore(nuevaCita.getHoraFin())) {
            throw new RuntimeException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        // Verificar si ya existe una cita en el mismo horario
        List<Date> citasExistentes = entityManager.createQuery(
                "SELECT c FROM Date c " +
                        "WHERE c.idPsicologo = :idPsicologo " +
                        "AND c.fecha = :fecha " +
                        "AND c.horaInicio < :horaFin " +
                        "AND c.horaFin > :horaInicio",
                Date.class
        )
        .setParameter("idPsicologo", nuevaCita.getIdPsicologo())
        .setParameter("fecha", nuevaCita.getFecha())
        .setParameter("horaInicio", nuevaCita.getHoraInicio())
        .setParameter("horaFin", nuevaCita.getHoraFin())
        .getResultList();

        if (!citasExistentes.isEmpty()) {
            System.out.println("Ya existe una cita en este horario.");
            throw new RuntimeException("Ya existe una cita agendada en este horario.");
        }

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
            System.out.println("No existe disponibilidad para esta cita.");
            throw new RuntimeException("No hay disponibilidad para la hora seleccionada.");
        }

        Disponibilidad disp = resultados.get(0);
        System.out.println("Disponibilidad encontrada: " + disp);

        // Eliminar la disponibilidad actual (ya no es válida tal como está)
        entityManager.remove(disp);
        System.out.println("Disponibilidad eliminada: " + disp.getId());

        // Crear nuevas disponibilidades si sobran espacios antes o después
        if (disp.getHoraInicio().isBefore(nuevaCita.getHoraInicio())) {
            Disponibilidad antes = new Disponibilidad(
                    disp.getIdPsicologo(),
                    disp.getFecha(),
                    disp.getHoraInicio(),
                    nuevaCita.getHoraInicio()
            );
            entityManager.persist(antes);
            System.out.println(" Nueva disponibilidad (antes): " + antes);
        }

        if (disp.getHoraFin().isAfter(nuevaCita.getHoraFin())) {
            Disponibilidad despues = new Disponibilidad(
                    disp.getIdPsicologo(),
                    disp.getFecha(),
                    nuevaCita.getHoraFin(),
                    disp.getHoraFin()
            );
            entityManager.persist(despues);
            System.out.println("Nueva disponibilidad (después): " + despues);
        }

        // Guardar la nueva cita
        entityManager.persist(nuevaCita);
        System.out.println("Cita registrada exitosamente: " + nuevaCita);
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

    @Transactional
    public Disponibilidad addDisponibilidad(Disponibilidad nuevaDisponibilidad) {
        System.out.println("Intentando agregar disponibilidad: " + nuevaDisponibilidad);

        // Validaciones básicas
        if (nuevaDisponibilidad.getIdPsicologo() == null) {
            throw new RuntimeException("El ID del psicólogo es requerido.");
        }

        if (nuevaDisponibilidad.getFecha() == null) {
            throw new RuntimeException("La fecha es requerida.");
        }

        if (nuevaDisponibilidad.getHoraInicio() == null || nuevaDisponibilidad.getHoraFin() == null) {
            throw new RuntimeException("La hora de inicio y fin son requeridas.");
        }

        if (!nuevaDisponibilidad.getHoraInicio().isBefore(nuevaDisponibilidad.getHoraFin())) {
            throw new RuntimeException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        // Verificar si ya existe una disponibilidad que se solape
        List<Disponibilidad> disponibilidadesExistentes = entityManager.createQuery(
                "SELECT d FROM Disponibilidad d " +
                        "WHERE d.idPsicologo = :idPsicologo " +
                        "AND d.fecha = :fecha " +
                        "AND ((d.horaInicio <= :horaInicio AND d.horaFin > :horaInicio) " +
                        "OR (d.horaInicio < :horaFin AND d.horaFin >= :horaFin) " +
                        "OR (d.horaInicio >= :horaInicio AND d.horaFin <= :horaFin))",
                Disponibilidad.class
        )
        .setParameter("idPsicologo", nuevaDisponibilidad.getIdPsicologo())
        .setParameter("fecha", nuevaDisponibilidad.getFecha())
        .setParameter("horaInicio", nuevaDisponibilidad.getHoraInicio())
        .setParameter("horaFin", nuevaDisponibilidad.getHoraFin())
        .getResultList();

        if (!disponibilidadesExistentes.isEmpty()) {
            System.out.println("Ya existe una disponibilidad que se solapa en este horario.");
            throw new RuntimeException("Ya existe una disponibilidad que se solapa en este horario.");
        }

        // Verificar si hay citas existentes en este horario que entrarían en conflicto
        List<Date> citasExistentes = entityManager.createQuery(
                "SELECT c FROM Date c " +
                        "WHERE c.idPsicologo = :idPsicologo " +
                        "AND c.fecha = :fecha " +
                        "AND c.horaInicio < :horaFin " +
                        "AND c.horaFin > :horaInicio",
                Date.class
        )
        .setParameter("idPsicologo", nuevaDisponibilidad.getIdPsicologo())
        .setParameter("fecha", nuevaDisponibilidad.getFecha())
        .setParameter("horaInicio", nuevaDisponibilidad.getHoraInicio())
        .setParameter("horaFin", nuevaDisponibilidad.getHoraFin())
        .getResultList();

        if (!citasExistentes.isEmpty()) {
            System.out.println("Ya existen citas agendadas en este horario.");
            throw new RuntimeException("No se puede agregar disponibilidad: ya existen citas agendadas en este horario.");
        }

        // Guardar la nueva disponibilidad
        entityManager.persist(nuevaDisponibilidad);
        System.out.println("Disponibilidad agregada exitosamente: " + nuevaDisponibilidad);

        return nuevaDisponibilidad;
    }
    
}


  