#!/usr/bin/env bash
# Verifica un respaldo: integridad, restauración real en una base temporal y
# que cada foto registrada exista en el archivo de fotos.
# Uso: scripts/verificar-respaldo.sh [carpeta-del-respaldo]   (por defecto, el último)
set -euo pipefail
umask 077
export LC_ALL=C

PROYECTO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$PROYECTO/.env" ]]; then
  set -a; source "$PROYECTO/.env"; set +a
fi

CONTENEDOR="${DB_CONTAINER:-inventario-db}"
USUARIO="${DB_USER:-inventario}"
RAIZ="${BACKUP_DIR:-$HOME/inventario-backups}"

fallar() { echo "ERROR: $*" >&2; exit 1; }

if [[ $# -ge 1 ]]; then
  RESPALDO="$1"
else
  RESPALDO="$(find "$RAIZ" -mindepth 1 -maxdepth 1 -type d -name '????-??-??_??????' | sort | tail -n 1)"
fi
[[ -n "${RESPALDO:-}" && -d "$RESPALDO" ]] || fallar "no hay respaldos que verificar"
[[ "$(docker inspect -f '{{.State.Running}}' "$CONTENEDOR" 2>/dev/null || true)" == "true" ]] \
  || fallar "el contenedor '$CONTENEDOR' no está corriendo"

echo "Verificando: $RESPALDO"

echo "→ Sumas de verificación..."
(cd "$RESPALDO" && sha256sum --check --quiet SHA256SUMS) || fallar "el respaldo está dañado o fue alterado"

TEMPORAL="verificacion_$$"
limpiar() { docker exec "$CONTENEDOR" dropdb -U "$USUARIO" --if-exists "$TEMPORAL" >/dev/null 2>&1 || true; }
trap limpiar EXIT

echo "→ Restaurando en una base temporal..."
docker exec "$CONTENEDOR" createdb -U "$USUARIO" "$TEMPORAL"
docker exec -i "$CONTENEDOR" pg_restore -U "$USUARIO" -d "$TEMPORAL" --no-owner < "$RESPALDO/db.dump" \
  || fallar "no se pudo restaurar el volcado"

echo "→ Contenido restaurado:"
for tabla in planeador producto evento evento_item foto; do
  n="$(docker exec "$CONTENEDOR" psql -U "$USUARIO" -d "$TEMPORAL" -tAc "select count(*) from $tabla")"
  printf '   %-12s %s\n' "$tabla" "$n"
done

echo "→ Comprobando que cada foto registrada exista en el respaldo de fotos..."
REGISTRADAS="$(docker exec "$CONTENEDOR" psql -U "$USUARIO" -d "$TEMPORAL" -tAc \
  "select ruta from foto union select foto_referencia from producto where foto_referencia is not null" \
  | grep -v '^$' | sort || true)"
EXISTENTES="$(tar -tf "$RESPALDO/uploads.tar" | sed 's#^\./##' | grep -v '/$' | grep -v '^$' | sort || true)"

FALTANTES="$(comm -23 <(printf '%s\n' "$REGISTRADAS") <(printf '%s\n' "$EXISTENTES") | grep -v '^$' || true)"
HUERFANOS="$(comm -13 <(printf '%s\n' "$REGISTRADAS") <(printf '%s\n' "$EXISTENTES") | grep -vc '^$' || true)"

if [[ -n "$FALTANTES" ]]; then
  echo "✗ Fotos registradas en la base pero AUSENTES en el respaldo:" >&2
  printf '%s\n' "$FALTANTES" | sed 's/^/     /' >&2
  fallar "el respaldo está incompleto: hay evidencia sin su archivo"
fi

TOTAL="$(printf '%s\n' "$REGISTRADAS" | grep -vc '^$' || true)"
echo "✓ Respaldo verificado: $TOTAL fotos registradas, todas presentes."
[[ "$HUERFANOS" -gt 0 ]] && echo "  (aviso: $HUERFANOS archivos sin registro en la base; es inofensivo)"
exit 0