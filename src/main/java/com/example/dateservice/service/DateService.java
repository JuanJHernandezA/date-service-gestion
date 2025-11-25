package com.example.dateservice.service;

import com.example.dateservice.entity.Date;
import com.example.dateservice.entity.Disponibilidad;
import com.example.dateservice.repository.DateRepository;
import com.example.dateservice.repository.DisponibilidadRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DateService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DateRepository dateRepository;

    @Autowired
    private DisponibilidadRepository disponibilidadRepository;

    @Transactional
    public void addDate(Date nuevaCita) {
        System.out.println("Intentando agendar cita: " + nuevaCita);

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

    public void cancelarCita(Long id) {
        Date cita = dateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("La cita no existe"));

        dateRepository.delete(cita);
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

    public List<Date> listarTodasLasCitas() {
        return dateRepository.findAll();
    }

    public List<Disponibilidad> filtrarDisponibilidades(Long idPsicologo, LocalDate fecha, Integer mes, Integer anio) {
        StringBuilder jpql = new StringBuilder("SELECT d FROM Disponibilidad d WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (idPsicologo != null) {
            jpql.append(" AND d.idPsicologo = :idPsicologo");
            params.put("idPsicologo", idPsicologo);
        }

        if (fecha != null) {
            jpql.append(" AND d.fecha = :fecha");
            params.put("fecha", fecha);
        }

        if (mes != null) {
            if (mes < 1 || mes > 12) {
                throw new RuntimeException("El mes debe estar entre 1 y 12");
            }
            jpql.append(" AND MONTH(d.fecha) = :mes");
            params.put("mes", mes);
        }

        if (anio != null) {
            jpql.append(" AND YEAR(d.fecha) = :anio");
            params.put("anio", anio);
        }

        jpql.append(" ORDER BY d.fecha ASC, d.horaInicio ASC");

        TypedQuery<Disponibilidad> query = entityManager.createQuery(jpql.toString(), Disponibilidad.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    public List<Date> listarCitasPorCliente(Long idCliente) {
        return entityManager.createQuery(
                "SELECT c FROM Date c WHERE c.idCliente = :idCliente ORDER BY c.fecha DESC, c.horaInicio DESC",
                Date.class
        )
        .setParameter("idCliente", idCliente)
        .getResultList();
    }

    public List<Disponibilidad> listarTodasLasDisponibilidades() {
        return disponibilidadRepository.findAll();
    }

    @Transactional
    public Disponibilidad crearDisponibilidad(Disponibilidad disponibilidad) {
        return disponibilidadRepository.save(disponibilidad);
    }

    @Transactional
    public void crearDisponibilidadesMasivas(Long idPsicologo, LocalDate fechaInicio, LocalDate fechaFin, LocalTime horaInicio, LocalTime horaFin) {
        LocalDate fechaActual = fechaInicio;
        while (!fechaActual.isAfter(fechaFin)) {
            // Solo crear disponibilidades para días laborables (lunes a viernes)
            int diaSemana = fechaActual.getDayOfWeek().getValue();
            if (diaSemana >= 1 && diaSemana <= 5) { // 1 = Lunes, 5 = Viernes
                Disponibilidad disp = new Disponibilidad(idPsicologo, fechaActual, horaInicio, horaFin);
                disponibilidadRepository.save(disp);
            }
            fechaActual = fechaActual.plusDays(1);
        }
    }

    @Transactional
    public Disponibilidad actualizarDisponibilidad(Long id, Disponibilidad disponibilidadActualizada) {

        Disponibilidad disponibilidadExistente = disponibilidadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("La disponibilidad no existe"));

        // Validar que horaInicio sea menor que horaFin
        if (disponibilidadActualizada.getHoraInicio().isAfter(disponibilidadActualizada.getHoraFin()) ||
            disponibilidadActualizada.getHoraInicio().equals(disponibilidadActualizada.getHoraFin())) {
            throw new RuntimeException("La hora de inicio debe ser anterior a la hora de fin");
        }

        // Obtener todas las citas del psicólogo en ese día que se solapan con el nuevo rango de disponibilidad
        // Solo verificamos citas que se solapan, no todas las citas del día
        List<Date> citasSolapadas = entityManager.createQuery(
                "SELECT c FROM Date c " +
                        "WHERE c.idPsicologo = :idPsicologo " +
                        "AND c.fecha = :fecha " +
                        "AND ((c.horaInicio < :horaFin AND c.horaFin > :horaInicio))",
                Date.class
        )
        .setParameter("idPsicologo", disponibilidadActualizada.getIdPsicologo())
        .setParameter("fecha", disponibilidadActualizada.getFecha())
        .setParameter("horaInicio", disponibilidadActualizada.getHoraInicio())
        .setParameter("horaFin", disponibilidadActualizada.getHoraFin())
        .getResultList();

        // Validar que todas las citas que se solapan estén completamente dentro del nuevo rango
        // Esto permite extender la disponibilidad (aumentar horas) siempre que todas las citas solapadas estén dentro
        for (Date cita : citasSolapadas) {
            // Verificar si la cita está completamente dentro del nuevo rango
            // La cita debe empezar después o igual al inicio y terminar antes o igual al fin
            boolean citaDentroDelRango = (cita.getHoraInicio().isAfter(disponibilidadActualizada.getHoraInicio()) || 
                                          cita.getHoraInicio().equals(disponibilidadActualizada.getHoraInicio())) &&
                                         (cita.getHoraFin().isBefore(disponibilidadActualizada.getHoraFin()) || 
                                          cita.getHoraFin().equals(disponibilidadActualizada.getHoraFin()));
            
            if (!citaDentroDelRango) {
                throw new RuntimeException("No se puede modificar el horario porque existe una cita agendada que no está completamente dentro del nuevo rango. La cita está programada de " + 
                        cita.getHoraInicio() + " a " + cita.getHoraFin() + ". El nuevo rango es de " +
                        disponibilidadActualizada.getHoraInicio() + " a " + disponibilidadActualizada.getHoraFin());
            }
        }

        disponibilidadExistente.setIdPsicologo(disponibilidadActualizada.getIdPsicologo());
        disponibilidadExistente.setFecha(disponibilidadActualizada.getFecha());
        disponibilidadExistente.setHoraInicio(disponibilidadActualizada.getHoraInicio());
        disponibilidadExistente.setHoraFin(disponibilidadActualizada.getHoraFin());

        return disponibilidadRepository.save(disponibilidadExistente);
    }

    
}


  