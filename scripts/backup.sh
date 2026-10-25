#!/usr/bin/env bash
# Respaldo conjunto de la base de datos y las fotos de evidencia.
# Uso: scripts/backup.sh
set -euo pipefail
umask 077   # solo tu usuario puede leer los respaldos

PROYECTO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$PROYECTO/.env" ]]; then
  set -a; source "$PROYECTO/.env"; set +a
fi

CONTENEDOR="${DB_CONTAINER:-inventario-db}"
BD="${DB_NAME:-inventario}"
USUARIO="${DB_USER:-inventario}"
FOTOS="${STORAGE_ROOT:-$HOME/inventario-uploads}"
RAIZ="${BACKUP_DIR:-$HOME/inventario-backups}"
CONSERVAR="${BACKUP_KEEP:-14}"

fallar() { echo "ERROR: $*" >&2; exit 1; }

command -v docker >/dev/null || fallar "docker no está instalado"
[[ "$(docker inspect -f '{{.State.Running}}' "$CONTENEDOR" 2>/dev/null || true)" == "true" ]] \
  || fallar "el contenedor '$CONTENEDOR' no está corriendo (docker compose up -d)"
[[ -d "$FOTOS" ]] \
  || fallar "no existe la carpeta de fotos '$FOTOS' (revisa STORAGE_ROOT). No se respalda sin evidencia."

SELLO="$(date +%Y-%m-%d_%H%M%S)"
DESTINO="$RAIZ/$SELLO"
PARCIAL="$RAIZ/.$SELLO.parcial"
mkdir -p "$PARCIAL"
trap 'rm -rf "$PARCIAL"' EXIT

echo "→ Base de datos..."
docker exec "$CONTENEDOR" pg_dump -U "$USUARIO" -d "$BD" -Fc > "$PARCIAL/db.dump"
docker exec -i "$CONTENEDOR" pg_restore --list < "$PARCIAL/db.dump" > /dev/null \
  || fallar "el volcado de la base de datos no es legible"
FILAS_FOTO="$(docker exec "$CONTENEDOR" psql -U "$USUARIO" -d "$BD" -tAc 'select count(*) from foto')"

echo "→ Fotos..."
tar -cf "$PARCIAL/uploads.tar" -C "$FOTOS" .
ARCHIVOS="$(tar -tf "$PARCIAL/uploads.tar" | grep -vc '/$' || true)"

cat > "$PARCIAL/manifest.txt" <<EOF
fecha=$SELLO
base_de_datos=$BD
fotos_registradas_en_base=$FILAS_FOTO
archivos_en_la_carpeta=$ARCHIVOS
EOF
(cd "$PARCIAL" && sha256sum db.dump uploads.tar manifest.txt > SHA256SUMS)

mv "$PARCIAL" "$DESTINO"
echo "✓ Respaldo creado: $DESTINO ($(du -sh "$DESTINO" | cut -f1))"

# Conservar solo los últimos $CONSERVAR respaldos
mapfile -t VIEJOS < <(find "$RAIZ" -mindepth 1 -maxdepth 1 -type d -name '????-??-??_??????' \
  | sort | head -n "-$CONSERVAR")
for viejo in "${VIEJOS[@]}"; do
  rm -rf -- "$viejo"
  echo "  eliminado respaldo antiguo: $(basename "$viejo")"
done