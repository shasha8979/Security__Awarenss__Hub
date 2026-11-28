
package com.sah.repository;

import com.sah.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {

    boolean existsByNit(String nit);

    // Carga masiva: el CSV identifica la empresa por su NIT
    Optional<Empresa> findByNit(String nit);

    boolean existsByNitAndIdNot(String nit, Integer id);
}
