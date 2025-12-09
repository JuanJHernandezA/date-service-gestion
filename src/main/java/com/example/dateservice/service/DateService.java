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

    public void cancelarCita(Long id) {
        Date cita = dateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("La cita no existe"));

        dateRepository.delete(cita);
    }

    @Transactional
    public Date modificarCita(Long id, Date citaModificada) {
        System.out.println("Intentando modificar cita con ID: " + id);

        Date citaExistente = dateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("La cita no existe"));

        if (citaModificada.getIdPsicologo() == null) {
            throw new RuntimeException("El ID del psicólogo es requerido.");
        }

        if (citaModificada.getIdCliente() == null) {
            throw new RuntimeException("El ID del cliente es requerido.");
        }

        if (citaModificada.getFecha() == null) {
            throw new RuntimeException("La fecha es requerida.");
        }

        if (citaModificada.getHoraInicio() == null || citaModificada.getHoraFin() == null) {
            throw new RuntimeException("La hora de inicio y fin son requeridas.");
        }

        if (!citaModificada.getHoraInicio().isBefore(citaModificada.getHoraFin())) {
            throw new RuntimeException("La hora de inicio debe ser anterior a la hora de fin.");
        }

        boolean horarioCambio = !citaExistente.getFecha().equals(citaModificada.getFecha()) ||
                !citaExistente.getHoraInicio().equals(citaModificada.getHoraInicio()) ||
                !citaExistente.getHoraFin().equals(citaModificada.getHoraFin());

        if (horarioCambio) {

            List<Disponibilidad> disponibilidadesAdyacentes = entityManager.createQuery(
                            "SELECT d FROM Disponibilidad d " +
                                    "WHERE d.idPsicologo = :idPsicologo " +
                                    "AND d.fecha = :fecha " +
                                    "AND ((d.horaFin = :horaInicio) OR (d.horaInicio = :horaFin))",
                            Disponibilidad.class
                    )
                    .setParameter("idPsicologo", citaExistente.getIdPsicologo())
                    .setParameter("fecha", citaExistente.getFecha())
                    .setParameter("horaInicio", citaExistente.getHoraInicio())
                    .setParameter("horaFin", citaExistente.getHoraFin())
                    .getResultList();

            Disponibilidad disponibilidadAntes = null;
            Disponibilidad disponibilidadDespues = null;

            for (Disponibilidad disp : disponibilidadesAdyacentes) {
                if (disp.getHoraFin().equals(citaExistente.getHoraInicio())) {
                    disponibilidadAntes = disp;
                }
                if (disp.getHoraInicio().equals(citaExistente.getHoraFin())) {
                    disponibilidadDespues = disp;
                }
            }

            if (disponibilidadAntes != null && disponibilidadDespues != null) {

                disponibilidadAntes.setHoraFin(disponibilidadDespues.getHoraFin());
                entityManager.merge(disponibilidadAntes);
                entityManager.remove(disponibilidadDespues);
            } else if (disponibilidadAntes != null) {

                disponibilidadAntes.setHoraFin(citaExistente.getHoraFin());
                entityManager.merge(disponibilidadAntes);
            } else if (disponibilidadDespues != null) {

                disponibilidadDespues.setHoraInicio(citaExistente.getHoraInicio());
                entityManager.merge(disponibilidadDespues);
            } else {

                Disponibilidad nuevaDisponibilidad = new Disponibilidad(
                        citaExistente.getIdPsicologo(),
                        citaExistente.getFecha(),
                        citaExistente.getHoraInicio(),
                        citaExistente.getHoraFin()
                );
                entityManager.persist(nuevaDisponibilidad);
            }

            List<Date> citasExistentes = entityManager.createQuery(
                            "SELECT c FROM Date c " +
                                    "WHERE c.idPsicologo = :idPsicologo " +
                                    "AND c.fecha = :fecha " +
                                    "AND c.horaInicio < :horaFin " +
                                    "AND c.horaFin > :horaInicio " +
                                    "AND c.id != :idCitaActual",
                            Date.class
                    )
                    .setParameter("idPsicologo", citaModificada.getIdPsicologo())
                    .setParameter("fecha", citaModificada.getFecha())
                    .setParameter("horaInicio", citaModificada.getHoraInicio())
                    .setParameter("horaFin", citaModificada.getHoraFin())
                    .setParameter("idCitaActual", id)
                    .getResultList();

            if (!citasExistentes.isEmpty()) {
                System.out.println("Ya existe una cita en el nuevo horario.");
                throw new RuntimeException("Ya existe una cita agendada en el nuevo horario.");
            }

            List<Disponibilidad> resultados = entityManager.createQuery(
                            "SELECT d FROM Disponibilidad d " +
                                    "WHERE d.idPsicologo = :idPsicologo " +
                                    "AND d.fecha = :fecha " +
                                    "AND d.horaInicio <= :horaInicio " +
                                    "AND d.horaFin >= :horaFin",
                            Disponibilidad.class
                    )
                    .setParameter("idPsicologo", citaModificada.getIdPsicologo())
                    .setParameter("fecha", citaModificada.getFecha())
                    .setParameter("horaInicio", citaModificada.getHoraInicio())
                    .setParameter("horaFin", citaModificada.getHoraFin())
                    .setMaxResults(1)
                    .getResultList();

            if (resultados.isEmpty()) {
                System.out.println("No existe disponibilidad para el nuevo horario.");
                throw new RuntimeException("No hay disponibilidad para el nuevo horario seleccionado.");
            }

            Disponibilidad disp = resultados.get(0);
            System.out.println("Disponibilidad encontrada para nuevo horario: " + disp);

            entityManager.remove(disp);
            System.out.println("Disponibilidad eliminada: " + disp.getId());

            if (disp.getHoraInicio().isBefore(citaModificada.getHoraInicio())) {
                Disponibilidad antes = new Disponibilidad(
                        disp.getIdPsicologo(),
                        disp.getFecha(),
                        disp.getHoraInicio(),
                        citaModificada.getHoraInicio()
                );
                entityManager.persist(antes);
                System.out.println("Nueva disponibilidad (antes): " + antes);
            }

            if (disp.getHoraFin().isAfter(citaModificada.getHoraFin())) {
                Disponibilidad despues = new Disponibilidad(
                        disp.getIdPsicologo(),
                        disp.getFecha(),
                        citaModificada.getHoraFin(),
                        disp.getHoraFin()
                );
                entityManager.persist(despues);
                System.out.println("Nueva disponibilidad (después): " + despues);
            }
        }

        citaExistente.setIdPsicologo(citaModificada.getIdPsicologo());
        citaExistente.setIdCliente(citaModificada.getIdCliente());
        citaExistente.setFecha(citaModificada.getFecha());
        citaExistente.setHoraInicio(citaModificada.getHoraInicio());
        citaExistente.setHoraFin(citaModificada.getHoraFin());


        Date citaActualizada = dateRepository.save(citaExistente);
        System.out.println("Cita modificada exitosamente: " + citaActualizada);

        return citaActualizada;
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
        // Validar que horaInicio sea menor que horaFin
        if (disponibilidad.getHoraInicio() == null || disponibilidad.getHoraFin() == null) {
            throw new RuntimeException("La hora de inicio y fin son requeridas.");
        }
        
        if (!disponibilidad.getHoraInicio().isBefore(disponibilidad.getHoraFin())) {
            throw new RuntimeException("La hora de inicio debe ser anterior a la hora de fin.");
        }
        
        return disponibilidadRepository.save(disponibilidad);
    }

    @Transactional
    public void crearDisponibilidadesMasivas(Long idPsicologo, LocalDate fechaInicio, LocalDate fechaFin, LocalTime horaInicio, LocalTime horaFin) {
        // Validar que horaInicio sea menor que horaFin
        if (horaInicio == null || horaFin == null) {
            throw new RuntimeException("La hora de inicio y fin son requeridas.");
        }
        
        if (!horaInicio.isBefore(horaFin)) {
            throw new RuntimeException("La hora de inicio debe ser anterior a la hora de fin.");
        }
        
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


  