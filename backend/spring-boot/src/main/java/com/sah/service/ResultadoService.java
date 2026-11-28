package com.sah.service;

import com.sah.model.Resultado;
import com.sah.repository.ResultadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta de resultados de evaluaciones.
 *
 * Regla acordada: un usuario puede presentar varias veces la misma
 * evaluación y para estadísticas/reportes cuenta su MEJOR intento
 * (mayor porcentaje; si hay empate, el intento más antiguo).
 */
@Service
public class ResultadoService {

    private final ResultadoRepository resultadoRepository;

    public ResultadoService(ResultadoRepository resultadoRepository) {
        this.resultadoRepository = resultadoRepository;
    }

    @Transactional(readOnly = true)
    public List<Resultado> listar(Integer evaluacionId) {
        return evaluacionId == null
                ? resultadoRepository.findAllByOrderByFechaPresentacionDescIdDesc()
                : resultadoRepository.findByEvaluacionIdOrderByFechaPresentacionDescIdDesc(evaluacionId);
    }

    @Transactional(readOnly = true)
    public Resultado buscarConRespuestas(Integer id) {
        return resultadoRepository.findConRespuestasById(id)
                .orElseThrow(() -> new ValidacionException("El resultado no existe."));
    }

    /**
     * Deja solo el mejor intento de cada par (usuario, evaluación),
     * conservando el orden de la lista recibida.
     */
    public static List<Resultado> mejoresIntentos(List<Resultado> resultados) {

        Comparator<Resultado> mejor = Comparator
                .comparing(Resultado::getPorcentaje)
                .thenComparing(Resultado::getIntento, Comparator.reverseOrder());

        Map<String, Resultado> mejores = new LinkedHashMap<>();

        for (Resultado r : resultados) {
            mejores.merge(r.getUsuarioId() + "-" + r.getEvaluacionId(), r,
                    (actual, nuevo) -> mejor.compare(nuevo, actual) > 0 ? nuevo : actual);
        }

        return resultados.stream()
                .filter(r -> mejores.get(r.getUsuarioId() + "-" + r.getEvaluacionId()) == r)
                .toList();
    }
}
