#!/usr/bin/env bash
# ============================================================
# Pruebas de la carga masiva de usuarios por CSV - SAH (Sesion 8)
#
# Uso (Git Bash):  bash docs/evidencias/api/probar-api-csv.sh 'CONTRASENA_ADMIN'
# Requiere la aplicacion corriendo en http://localhost:8080 y la empresa
# de pruebas "SecureCorp Pruebas API" (NIT 900123456-7), que crea
# probar-api-sah.sh.
#
# Archivo usado: docs/evidencias/api/usuarios-ejemplo.csv (8 filas):
#   3 validas (laura/carlos/marta.csv@sah-pruebas.com) y 5 con error.
# La primera ejecucion registra 3; las siguientes registran 0, porque
# los correos ya existen (la API no borra usuarios).
# Con MAIL_ENABLED=true cada usuario recibe su contraseña temporal.
# ============================================================

API="http://localhost:8080/api"
ADMIN="admin@sah.com:${1:?Indica la contrasena del admin como primer argumento}"
DIR="$(cd "$(dirname "$0")" && pwd)"
command -v cygpath >/dev/null 2>&1 && DIR="$(cygpath -m "$DIR")"
CSV="$DIR/usuarios-ejemplo.csv"
RUTA="$API/usuarios/carga-csv"

TMP="$(mktemp -d)"
command -v cygpath >/dev/null 2>&1 && TMP="$(cygpath -m "$TMP")"
TOTAL=0; FALLAS=0

prueba() {  # prueba "<descripcion>" <codigo_esperado> <curl args...>
  local desc="$1" esperado="$2"; shift 2
  local codigo
  codigo=$(curl -s -o "$TMP/cuerpo.json" -w "%{http_code}" "$@")
  TOTAL=$((TOTAL+1))
  local marca="OK  "; if [ "$codigo" != "$esperado" ]; then marca="FALLA"; FALLAS=$((FALLAS+1)); fi
  printf "%s | esperado %s | obtenido %s | %s\n" "$marca" "$esperado" "$codigo" "$desc"
  printf "       %s\n" "$(head -c 400 "$TMP/cuerpo.json" | tr -d '\n')"
}

# Verificacion previa: si la clave del admin no sirve, todas las pruebas
# con admin darian 401 y el resultado no diria nada util
VERIFICACION=$(curl -s -o /dev/null -w "%{http_code}" -u "$ADMIN" "$API/cursos")
if [ "$VERIFICACION" != "200" ]; then
  echo "ERROR: la API respondio $VERIFICACION al autenticar a admin@sah.com."
  echo "  - Revisa la contrasena (entre comillas simples: 'mi clave')."
  echo "  - Si tiene tildes o ñ, Git Bash puede enviarla mal: usa una clave sin ellas"
  echo "    (cambiala con: C:\\xampp\\php\\php.exe database\\restablecer_clave.php admin@sah.com)."
  echo "  - Comprueba que la aplicacion este corriendo en http://localhost:8080."
  rm -rf "$TMP"
  exit 1
fi

printf 'texto' > "$TMP/usuarios.txt"
printf '\n' > "$TMP/vacio.csv"
printf 'nombres;apellidos\nAna;Gomez\n' > "$TMP/sin-columnas.csv"
printf 'nombres;apellidos;correo;nit_empresa\nA;B;c@x.com;1\n' > "$TMP/estudiante.csv"

echo "=================== CARGA MASIVA CSV ==================="
prueba "Carga CSV de ejemplo (resumen con errores por linea)" 200 \
  -u "$ADMIN" -F "archivo=@$CSV;type=text/csv" "$RUTA"
prueba "Sin credenciales" 401 -F "archivo=@$CSV;type=text/csv" "$RUTA"
prueba "Credenciales incorrectas" 401 -u "admin@sah.com:clave-incorrecta" \
  -F "archivo=@$CSV;type=text/csv" "$RUTA"
prueba "Archivo que no es .csv" 400 -u "$ADMIN" -F "archivo=@$TMP/usuarios.txt" "$RUTA"
prueba "Archivo vacio" 400 -u "$ADMIN" -F "archivo=@$TMP/vacio.csv" "$RUTA"
prueba "Cabecera sin columnas obligatorias" 400 -u "$ADMIN" \
  -F "archivo=@$TMP/sin-columnas.csv" "$RUTA"
prueba "Sin campo 'archivo'" 400 -u "$ADMIN" -F "otro=@$CSV" "$RUTA"
prueba "Cuerpo JSON en lugar de multipart" 415 -u "$ADMIN" \
  -H "Content-Type: application/json" -d '{}' "$RUTA"

echo
echo "Resultado: $((TOTAL-FALLAS))/$TOTAL pruebas correctas"
rm -rf "$TMP"
[ "$FALLAS" -eq 0 ]
