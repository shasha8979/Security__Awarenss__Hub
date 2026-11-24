
package com.sah.repository;

import com.sah.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {

    boolean existsByNit(String nit);

    boolean existsByNitAndIdNot(String nit, Integer id);
}
