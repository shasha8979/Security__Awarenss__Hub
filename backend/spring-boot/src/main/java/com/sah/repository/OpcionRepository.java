package com.sah.repository;

import com.sah.model.Opcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpcionRepository extends JpaRepository<Opcion, Integer> {

    List<Opcion> findByPreguntaIdOrderByOrdenAscIdAsc(Integer preguntaId);
}
