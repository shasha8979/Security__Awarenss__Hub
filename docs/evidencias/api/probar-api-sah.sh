#!/usr/bin/env bash
# ============================================================
# Pruebas de la API REST de empresas, usuarios y evaluaciones - SAH
# (Guia 4: CRUD y codigos HTTP / Guia 7: validacion, errores, seguridad)
#
# Uso (Git Bash):  bash docs/evidencias/api/probar-api-sah.sh 'CONTRASENA_ADMIN'
# Requiere la aplicacion corriendo en http://localhost:8080
#
# Datos de prueba que crea (y reutiliza en ejecuciones siguientes,
# porque la API no borra registros):
#   - Empresa  "SecureCorp Pruebas API" (NIT 900123456-7)
#   - Usuario  estudiante.api@sah-pruebas.com (rol empleado). Se deja
#              INACTIVO al terminar: no puede iniciar sesion.
#   - Evaluacion "Evaluación de fundamentos" del curso 1
# ============================================================

API="http://localhost:8080/api"
ADMIN="admin@sah.com:${1:?Indica la contrasena del admin como primer argumento}"
EST_CORREO="estudiante.api@sah-pruebas.com"
EST_CLAVE="Prueba-Api-2026"
ESTUDIANTE="$EST_CORREO:$EST_CLAVE"

TMP="$(mktemp -d)"
command -v cygpath >/dev/null 2>&1 && TMP="$(cygpath -m "$TMP")"
TOTAL=0; FALLAS=0

prueba() {  # prueba "<descripcion>" <codigo_esperado> <curl args...>
  local desc="$1" esperado="$2"; shift 2
  local codigo
  codigo=$(curl -s -o "$TMP/cuerpo.json" -D "$TMP/cabeceras.txt" -w "%{http_code}" "$@")
  TOTAL=$((TOTAL+1))
  local marca="OK  "; if [ "$codigo" != "$esperado" ]; then marca="FALLA"; FALLAS=$((FALLAS+1)); fi
  printf "%s | esperado %s | obtenido %s | %s\n" "$marca" "$esperado" "$codigo" "$desc"
  printf "       %s\n" "$(head -c 230 "$TMP/cuerpo.json" | tr -d '\n')"
}

# id del objeto de una lista JSON que contiene "campo":"valor"
buscar_id() {  # buscar_id <url> <campo> <valor>
  curl -s -u "$ADMIN" "$1" | tr '}' '\n' | grep -F "\"$2\":\"$3\"" \
    | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2
}

id_creado() { grep -o '"id":[0-9]*' "$TMP/cuerpo.json" | head -1 | cut -d: -f2; }

J=(-H "Content-Type: application/json; charset=utf-8")
cuerpo() { printf '%s' "$1" > "$TMP/$2.json"; }

# ------------------------------------------------------------
echo "=================== EMPRESAS ==================="
cuerpo '{"nombre":"SecureCorp Pruebas API","nit":"900123456-7","correo":"contacto@securecorp.test","telefono":"+57 601 555 0100","direccion":"Calle 100 # 10-20, Bogotá","tipo":"privada"}' emp
cuerpo '{"nombre":"","nit":"ABC","correo":"no-es-correo","telefono":"tel: 123","tipo":null}' emp_inv
cuerpo '{"nombre":"Empresa X","nit":"123456","correo":"x@x.com","tipo":"ong"}' emp_enum
cuerpo '{"nombre":"SecureCorp Pruebas API","nit":"900123456-7","correo":"seguridad@securecorp.test","telefono":"+57 601 555 0199","direccion":"Calle 100 # 10-20, Bogotá","tipo":"mixta"}' emp_edit

prueba "GET todas las empresas" 200 -u "$ADMIN" "$API/empresas"
prueba "GET empresa existente (id 1)" 200 -u "$ADMIN" "$API/empresas/1"
prueba "GET empresa inexistente (id 9999)" 404 -u "$ADMIN" "$API/empresas/9999"

EMP_ID=$(buscar_id "$API/empresas" nombre "SecureCorp Pruebas API")
if [ -n "$EMP_ID" ]; then
  echo "INFO | la empresa de prueba ya existe (id $EMP_ID); se omite el POST 201"
else
  prueba "POST empresa válida" 201 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/emp.json" "$API/empresas"
  EMP_ID=$(id_creado)
fi
prueba "POST NIT repetido" 409 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/emp.json" "$API/empresas"
prueba "POST datos inválidos (errores por campo)" 400 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/emp_inv.json" "$API/empresas"
prueba "POST tipo inexistente (ong)" 400 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/emp_enum.json" "$API/empresas"
prueba "PUT empresa existente (id $EMP_ID)" 200 -u "$ADMIN" "${J[@]}" -X PUT --data-binary @"$TMP/emp_edit.json" "$API/empresas/$EMP_ID"
prueba "PUT empresa inexistente (id 9999)" 404 -u "$ADMIN" "${J[@]}" -X PUT --data-binary @"$TMP/emp_edit.json" "$API/empresas/9999"
prueba "PATCH estado no válido (borrado)" 400 -u "$ADMIN" "${J[@]}" -X PATCH --data-binary '{"estado":"borrado"}' "$API/empresas/$EMP_ID/estado"
prueba "PATCH desactivar empresa" 200 -u "$ADMIN" "${J[@]}" -X PATCH --data-binary '{"estado":"inactivo"}' "$API/empresas/$EMP_ID/estado"
prueba "PATCH reactivar empresa" 200 -u "$ADMIN" "${J[@]}" -X PATCH --data-binary '{"estado":"activo"}' "$API/empresas/$EMP_ID/estado"
prueba "DELETE no permitido" 405 -u "$ADMIN" -X DELETE "$API/empresas/$EMP_ID"

# ------------------------------------------------------------
echo "=================== USUARIOS ==================="
cuerpo "{\"nombres\":\"Estudiante\",\"apellidos\":\"Pruebas API\",\"correo\":\"$EST_CORREO\",\"password\":\"$EST_CLAVE\",\"rol\":\"empleado\",\"empresaId\":$EMP_ID}" usu
cuerpo '{"nombres":"","apellidos":"X","correo":"correo-malo","password":"123","rol":"superadmin"}' usu_inv
cuerpo '{"nombres":"Sin","apellidos":"Empresa","correo":"sin.empresa@sah-pruebas.com","password":"ClaveSegura1","rol":"empleado"}' usu_sin_emp
cuerpo '{"nombres":"Sin","apellidos":"Clave","correo":"sin.clave@sah-pruebas.com","rol":"admin"}' usu_sin_clave
cuerpo "{\"nombres\":\"Estudiante\",\"apellidos\":\"Pruebas API Editado\",\"correo\":\"$EST_CORREO\",\"rol\":\"empleado\",\"empresaId\":$EMP_ID}" usu_edit

prueba "GET todos los usuarios" 200 -u "$ADMIN" "$API/usuarios"
if grep -qi '"password"' "$TMP/cuerpo.json"; then
  echo "FALLA | la respuesta expone el campo password"; FALLAS=$((FALLAS+1))
else
  echo "OK   | la respuesta NO incluye contraseñas ni hashes"
fi
TOTAL=$((TOTAL+1))
prueba "GET usuarios filtrados por rol=empleado" 200 -u "$ADMIN" "$API/usuarios?rol=empleado"
prueba "GET usuario existente (id 1)" 200 -u "$ADMIN" "$API/usuarios/1"
prueba "GET usuario inexistente (id 9999)" 404 -u "$ADMIN" "$API/usuarios/9999"

EST_ID=$(buscar_id "$API/usuarios" correo "$EST_CORREO")
if [ -n "$EST_ID" ]; then
  echo "INFO | el estudiante de prueba ya existe (id $EST_ID); se reactiva para las pruebas"
  curl -s -o /dev/null -u "$ADMIN" "${J[@]}" -X PATCH --data-binary '{"estado":"activo"}' "$API/usuarios/$EST_ID/estado"
else
  prueba "POST estudiante válido (contraseña cifrada con BCrypt)" 201 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/usu.json" "$API/usuarios"
  EST_ID=$(id_creado)
fi
prueba "POST correo repetido" 409 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/usu.json" "$API/usuarios"
prueba "POST datos inválidos (correo, clave corta, rol)" 400 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/usu_inv.json" "$API/usuarios"
prueba "POST estudiante sin empresa" 400 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/usu_sin_emp.json" "$API/usuarios"
prueba "POST usuario sin contraseña" 400 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/usu_sin_clave.json" "$API/usuarios"
prueba "PUT usuario existente sin cambiar contraseña" 200 -u "$ADMIN" "${J[@]}" -X PUT --data-binary @"$TMP/usu_edit.json" "$API/usuarios/$EST_ID"
prueba "PUT usuario inexistente (id 9999)" 404 -u "$ADMIN" "${J[@]}" -X PUT --data-binary @"$TMP/usu_edit.json" "$API/usuarios/9999"
prueba "PATCH el admin no puede desactivarse a sí mismo" 400 -u "$ADMIN" "${J[@]}" -X PATCH --data-binary '{"estado":"inactivo"}' "$API/usuarios/1/estado"
prueba "DELETE no permitido" 405 -u "$ADMIN" -X DELETE "$API/usuarios/$EST_ID"

echo "--- Autorización por rol (estudiante de prueba)"
prueba "Estudiante: GET cursos (lectura permitida)" 200 -u "$ESTUDIANTE" "$API/cursos"
prueba "Estudiante: GET usuarios (prohibido)" 403 -u "$ESTUDIANTE" "$API/usuarios"
prueba "Estudiante: GET empresas (prohibido)" 403 -u "$ESTUDIANTE" "$API/empresas"
prueba "Estudiante: POST curso (prohibido)" 403 -u "$ESTUDIANTE" "${J[@]}" --data-binary '{"titulo":"x"}' "$API/cursos"
prueba "Desactivar estudiante de prueba" 200 -u "$ADMIN" "${J[@]}" -X PATCH --data-binary '{"estado":"inactivo"}' "$API/usuarios/$EST_ID/estado"
prueba "Estudiante inactivo no puede autenticarse" 401 -u "$ESTUDIANTE" "$API/cursos"

# ------------------------------------------------------------
echo "================= EVALUACIONES ================="
cuerpo '{"cursoId":1,"titulo":"Evaluación de fundamentos","descripcion":"Preguntas sobre confidencialidad, integridad y disponibilidad de la información.","porcentajeAprobacion":70}' eva
cuerpo '{"cursoId":9999,"titulo":"Sin curso","descripcion":"x","porcentajeAprobacion":70}' eva_sin_curso
cuerpo '{"cursoId":null,"titulo":"","descripcion":"","porcentajeAprobacion":0}' eva_inv
cuerpo '{"cursoId":1,"titulo":"Evaluación de fundamentos","descripcion":"Preguntas sobre confidencialidad, integridad y disponibilidad. Incluye casos.","porcentajeAprobacion":80}' eva_edit

prueba "GET todas las evaluaciones" 200 -u "$ADMIN" "$API/evaluaciones"
prueba "GET evaluaciones del curso 1" 200 -u "$ADMIN" "$API/evaluaciones?cursoId=1"
prueba "GET evaluación inexistente (id 9999)" 404 -u "$ADMIN" "$API/evaluaciones/9999"

EVA_ID=$(buscar_id "$API/evaluaciones" titulo "Evaluación de fundamentos")
if [ -n "$EVA_ID" ]; then
  echo "INFO | la evaluación de prueba ya existe (id $EVA_ID); se omite el POST 201"
else
  prueba "POST evaluación válida" 201 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/eva.json" "$API/evaluaciones"
  EVA_ID=$(id_creado)
fi
prueba "GET evaluación existente (id $EVA_ID)" 200 -u "$ADMIN" "$API/evaluaciones/$EVA_ID"
prueba "POST mismo título en el mismo curso" 409 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/eva.json" "$API/evaluaciones"
prueba "POST con curso inexistente" 400 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/eva_sin_curso.json" "$API/evaluaciones"
prueba "POST datos inválidos (errores por campo)" 400 -u "$ADMIN" "${J[@]}" --data-binary @"$TMP/eva_inv.json" "$API/evaluaciones"
prueba "PUT evaluación existente" 200 -u "$ADMIN" "${J[@]}" -X PUT --data-binary @"$TMP/eva_edit.json" "$API/evaluaciones/$EVA_ID"
prueba "PUT evaluación inexistente (id 9999)" 404 -u "$ADMIN" "${J[@]}" -X PUT --data-binary @"$TMP/eva_edit.json" "$API/evaluaciones/9999"
prueba "PATCH desactivar evaluación" 200 -u "$ADMIN" "${J[@]}" -X PATCH --data-binary '{"estado":"inactivo"}' "$API/evaluaciones/$EVA_ID/estado"
prueba "PATCH reactivar evaluación" 200 -u "$ADMIN" "${J[@]}" -X PATCH --data-binary '{"estado":"activo"}' "$API/evaluaciones/$EVA_ID/estado"
prueba "DELETE no permitido" 405 -u "$ADMIN" -X DELETE "$API/evaluaciones/$EVA_ID"

echo "================================================"
echo "Resultado: $((TOTAL-FALLAS)) de $TOTAL pruebas correctas"
rm -rf "$TMP"
