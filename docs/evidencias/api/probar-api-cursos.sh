#!/usr/bin/env bash
# ============================================================
# Pruebas de la API REST de cursos (Guia 4 / Guia 7) - SAH
# Uso (Git Bash):  bash docs/evidencias/api/probar-api-cursos.sh 'CONTRASENA_ADMIN'
# Requiere la aplicacion corriendo en http://localhost:8080
# ============================================================

BASE="http://localhost:8080/api/cursos"
ADMIN="admin@sah.com:${1:?Indica la contrasena del admin como primer argumento}"
TMP="$(mktemp -d)"
# En Git Bash para Windows, curl nativo necesita rutas tipo C:/...
command -v cygpath >/dev/null 2>&1 && TMP="$(cygpath -m "$TMP")"

prueba() {  # prueba "<descripcion>" <codigo_esperado> <curl args...>
  local desc="$1" esperado="$2"; shift 2
  local codigo
  codigo=$(curl -s -o "$TMP/cuerpo.json" -D "$TMP/cabeceras.txt" -w "%{http_code}" "$@")
  local marca="OK  "; [ "$codigo" = "$esperado" ] || marca="FALLA"
  printf "%s | esperado %s | obtenido %s | %s\n" "$marca" "$esperado" "$codigo" "$desc"
  printf "       %s\n" "$(head -c 260 "$TMP/cuerpo.json" | tr -d '\n')"
}

JSON=(-H "Content-Type: application/json; charset=utf-8")

cat > "$TMP/nuevo.json" <<'EOF'
{"titulo":"Ingeniería social y suplantación","descripcion":"Reconocer técnicas de manipulación como vishing, pretexting y suplantación de identidad.","nivelDificultad":"intermedio","duracion":4,"numeroModulos":3,"porcentajeAprobacion":75,"id":999}
EOF
cat > "$TMP/invalido.json" <<'EOF'
{"titulo":"","descripcion":"x","nivelDificultad":"basico","duracion":0,"numeroModulos":1,"porcentajeAprobacion":150}
EOF
cat > "$TMP/enum.json" <<'EOF'
{"titulo":"Curso X","descripcion":"x","nivelDificultad":"experto","duracion":1,"numeroModulos":1,"porcentajeAprobacion":70}
EOF
cat > "$TMP/editar.json" <<'EOF'
{"titulo":"Ingeniería social y suplantación","descripcion":"Reconocer técnicas de manipulación como vishing, pretexting y suplantación de identidad. Incluye casos prácticos.","nivelDificultad":"intermedio","duracion":5,"numeroModulos":3,"porcentajeAprobacion":75}
EOF

echo "== Autenticación"
prueba "GET sin credenciales" 401 "$BASE"
prueba "GET con contraseña incorrecta" 401 -u "admin@sah.com:incorrecta" "$BASE"

echo "== Consultas"
prueba "GET todos los cursos" 200 -u "$ADMIN" "$BASE"
prueba "GET curso existente (id 1)" 200 -u "$ADMIN" "$BASE/1"
prueba "GET curso inexistente (id 9999)" 404 -u "$ADMIN" "$BASE/9999"
prueba "GET id no numérico (abc)" 400 -u "$ADMIN" "$BASE/abc"

echo "== Crear"
TITULO_PRUEBA="Ingeniería social y suplantación"
if curl -s -u "$ADMIN" "$BASE" | grep -q "\"titulo\":\"$TITULO_PRUEBA\""; then
  # Ya se creo en una ejecucion anterior: no hay DELETE, se reutiliza
  NUEVO_ID=$(curl -s -u "$ADMIN" "$BASE" | grep -o "{\"id\":[0-9]*,\"titulo\":\"$TITULO_PRUEBA\"" | grep -o '[0-9]\+' | head -1)
  echo "INFO | el curso de prueba ya existe (id $NUEVO_ID); se omite el POST 201"
else
  prueba "POST curso válido (el id enviado se ignora)" 201 -u "$ADMIN" "${JSON[@]}" --data-binary @"$TMP/nuevo.json" "$BASE"
  NUEVO_ID=$(grep -o '"id":[0-9]*' "$TMP/cuerpo.json" | head -1 | cut -d: -f2)
  echo "       Location: $(grep -i '^location:' "$TMP/cabeceras.txt" | tr -d '\r' | cut -d' ' -f2)"
fi
prueba "POST mismo título (duplicado)" 409 -u "$ADMIN" "${JSON[@]}" --data-binary @"$TMP/nuevo.json" "$BASE"
prueba "POST datos inválidos (validación por campo)" 400 -u "$ADMIN" "${JSON[@]}" --data-binary @"$TMP/invalido.json" "$BASE"
prueba "POST nivel inexistente (experto)" 400 -u "$ADMIN" "${JSON[@]}" --data-binary @"$TMP/enum.json" "$BASE"
prueba "POST JSON mal formado" 400 -u "$ADMIN" "${JSON[@]}" --data-binary '{"titulo": ' "$BASE"
prueba "POST con Content-Type text/plain" 415 -u "$ADMIN" -H "Content-Type: text/plain" --data-binary 'hola' "$BASE"

echo "== Actualizar"
prueba "PUT curso existente (id $NUEVO_ID)" 200 -u "$ADMIN" "${JSON[@]}" -X PUT --data-binary @"$TMP/editar.json" "$BASE/$NUEVO_ID"
prueba "PUT curso inexistente (id 9999)" 404 -u "$ADMIN" "${JSON[@]}" -X PUT --data-binary @"$TMP/editar.json" "$BASE/9999"
prueba "PUT datos inválidos" 400 -u "$ADMIN" "${JSON[@]}" -X PUT --data-binary @"$TMP/invalido.json" "$BASE/$NUEVO_ID"

echo "== Estado (en lugar de DELETE)"
prueba "PATCH desactivar curso" 200 -u "$ADMIN" "${JSON[@]}" -X PATCH --data-binary '{"estado":"inactivo"}' "$BASE/$NUEVO_ID/estado"
prueba "PATCH reactivar curso" 200 -u "$ADMIN" "${JSON[@]}" -X PATCH --data-binary '{"estado":"activo"}' "$BASE/$NUEVO_ID/estado"
prueba "PATCH estado inexistente (id 9999)" 404 -u "$ADMIN" "${JSON[@]}" -X PATCH --data-binary '{"estado":"activo"}' "$BASE/9999/estado"
prueba "PATCH sin estado" 400 -u "$ADMIN" "${JSON[@]}" -X PATCH --data-binary '{}' "$BASE/$NUEVO_ID/estado"
prueba "DELETE no permitido (se desactiva)" 405 -u "$ADMIN" -X DELETE "$BASE/$NUEVO_ID"

echo "== Ruta inexistente"
prueba "GET /api/no-existe" 404 -u "$ADMIN" "http://localhost:8080/api/no-existe"

rm -rf "$TMP"
