package com.sah.exception;

import com.sah.dto.ErrorRespuestaDTO;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo centralizado de errores de la API REST.
 *
 * Todas las respuestas de error tienen el mismo formato JSON
 * (ErrorRespuestaDTO) y nunca exponen trazas ni detalles internos.
 */
@RestControllerAdvice
public class ManejadorGlobalErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

    /** 400: fallan las anotaciones @Valid del DTO. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuestaDTO> validacion(
            MethodArgumentNotValidException ex, HttpServletRequest peticion) {

        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        return responder(HttpStatus.BAD_REQUEST,
                "Los datos enviados no son válidos.", peticion, campos);
    }

    /** 400: JSON mal formado o valor de enumeracion inexistente. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuestaDTO> jsonInvalido(
            HttpMessageNotReadableException ex, HttpServletRequest peticion) {

        return responder(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es un JSON válido o contiene "
                        + "valores no permitidos.", peticion, Map.of());
    }

    /** 400: por ejemplo /api/cursos/abc cuando se espera un numero. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuestaDTO> tipoInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest peticion) {

        return responder(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor no válido.",
                peticion, Map.of());
    }

    /** 404: el recurso no existe. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuestaDTO> noEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest peticion) {

        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), peticion, Map.of());
    }

    /** 404: la ruta no existe. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuestaDTO> rutaInexistente(
            NoResourceFoundException ex, HttpServletRequest peticion) {

        return responder(HttpStatus.NOT_FOUND,
                "La ruta solicitada no existe.", peticion, Map.of());
    }

    /** 403: regla de permisos del servicio. */
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorRespuestaDTO> accesoDenegado(
            AccesoDenegadoException ex, HttpServletRequest peticion) {

        return responder(HttpStatus.FORBIDDEN, ex.getMessage(), peticion, Map.of());
    }

    /** 409: dato unico repetido (regla del servicio). */
    @ExceptionHandler(DatoDuplicadoException.class)
    public ResponseEntity<ErrorRespuestaDTO> duplicado(
            DatoDuplicadoException ex, HttpServletRequest peticion) {

        return responder(HttpStatus.CONFLICT, ex.getMessage(), peticion, Map.of());
    }

    /** 409: la base de datos rechazo la operacion (clave unica o foranea). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuestaDTO> integridad(
            DataIntegrityViolationException ex, HttpServletRequest peticion) {

        log.warn("Violación de integridad en {}: {}",
                peticion.getRequestURI(), ex.getMostSpecificCause().getMessage());

        return responder(HttpStatus.CONFLICT,
                "La operación entra en conflicto con datos existentes.",
                peticion, Map.of());
    }

    /** 400: otras reglas de negocio del servicio. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorRespuestaDTO> reglaNegocio(
            IllegalArgumentException ex, HttpServletRequest peticion) {

        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), peticion, Map.of());
    }

    /** 405: metodo HTTP no permitido en la ruta. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuestaDTO> metodoNoPermitido(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest peticion) {

        return responder(HttpStatus.METHOD_NOT_ALLOWED,
                "El método " + ex.getMethod() + " no está permitido en esta ruta.",
                peticion, Map.of());
    }

    /** 415: Content-Type distinto del que acepta la ruta. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorRespuestaDTO> formatoNoSoportado(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest peticion) {

        String esperado = ex.getSupportedMediaTypes().isEmpty()
                ? "application/json"
                : ex.getSupportedMediaTypes().get(0).toString();

        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "El contenido debe enviarse como " + esperado + ".",
                peticion, Map.of());
    }

    /** 400: falta el archivo o un parametro obligatorio (carga CSV). */
    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorRespuestaDTO> parteFaltante(
            Exception ex, HttpServletRequest peticion) {

        return responder(HttpStatus.BAD_REQUEST,
                "Debes adjuntar el archivo en el campo 'archivo'.",
                peticion, Map.of());
    }

    /** 413: el archivo supera el tamaño máximo (spring.servlet.multipart). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorRespuestaDTO> archivoMuyGrande(
            MaxUploadSizeExceededException ex, HttpServletRequest peticion) {

        return responder(HttpStatus.CONTENT_TOO_LARGE,
                "El archivo supera el tamaño máximo permitido (2 MB).",
                peticion, Map.of());
    }

    /** 400: la petición no es multipart o está mal formada. */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorRespuestaDTO> multipartInvalido(
            MultipartException ex, HttpServletRequest peticion) {

        return responder(HttpStatus.BAD_REQUEST,
                "La petición debe enviarse como multipart/form-data con el campo 'archivo'.",
                peticion, Map.of());
    }

    /** 500: error no previsto. Se registra en el log, pero no se expone. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuestaDTO> errorInterno(
            Exception ex, HttpServletRequest peticion) {

        log.error("Error no controlado en {}", peticion.getRequestURI(), ex);

        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intenta de nuevo más tarde.",
                peticion, Map.of());
    }

    private ResponseEntity<ErrorRespuestaDTO> responder(
            HttpStatus estado,
            String mensaje,
            HttpServletRequest peticion,
            Map<String, String> campos) {

        ErrorRespuestaDTO cuerpo = new ErrorRespuestaDTO(
                LocalDateTime.now(),
                estado.value(),
                estado.getReasonPhrase(),
                mensaje,
                peticion.getRequestURI(),
                campos
        );

        return ResponseEntity.status(estado).body(cuerpo);
    }
}
