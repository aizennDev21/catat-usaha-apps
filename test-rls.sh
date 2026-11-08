#!/usr/bin/env bash
cd "$(dirname "$0")"
export $(grep -E '^SUPABASE_' .env | xargs)
PASS="Test12345!"

signup() {
  curl -s "$SUPABASE_URL/auth/v1/signup" \
    -H "apikey: $SUPABASE_ANON_KEY" -H "Content-Type: application/json" \
    -d "{\"email\":\"$1\",\"password\":\"$PASS\"}"
}
login() {
  curl -s "$SUPABASE_URL/auth/v1/token?grant_type=password" \
    -H "apikey: $SUPABASE_ANON_KEY" -H "Content-Type: application/json" \
    -d "{\"email\":\"$1\",\"password\":\"$PASS\"}"
}
get_session() {
  local r
  r=$(signup "$1")
  if [ -z "$(echo "$r" | jq -r '.access_token // empty')" ]; then r=$(login "$1"); fi
  echo "$r"
}
api() {  # $1 path | $2 token (boleh kosong) | $3 body JSON (boleh kosong)
  local args=(-s -w "\n[HTTP %{http_code}]" "$SUPABASE_URL/rest/v1/$1"
    -H "apikey: $SUPABASE_ANON_KEY" -H "Content-Type: application/json"
    -H "Prefer: return=representation")
  [ -n "$2" ] && args+=(-H "Authorization: Bearer $2")
  [ -n "$3" ] && args+=(-d "$3")
  curl "${args[@]}"; echo
}

: "${EMAIL_A:?isi EMAIL_A}"; : "${EMAIL_B:?isi EMAIL_B}"
SA=$(get_session "$EMAIL_A"); SB=$(get_session "$EMAIL_B")
TOKEN_A=$(echo "$SA" | jq -r '.access_token // empty'); ID_A=$(echo "$SA" | jq -r '.user.id // empty')
TOKEN_B=$(echo "$SB" | jq -r '.access_token // empty')
if [ -z "$TOKEN_A" ] || [ -z "$TOKEN_B" ]; then
  echo "GAGAL login/signup. Respons A:"; echo "$SA"; echo "Respons B:"; echo "$SB"; exit 1
fi

echo; echo "1) A membuat profil sendiri (harapan: HTTP 201)"
api user_profiles "$TOKEN_A" "{\"user_id\":\"$ID_A\",\"full_name\":\"A\",\"business_name\":\"Toko A\"}"
echo "2) B membaca semua profil (harapan: [] dan HTTP 200)"
api user_profiles "$TOKEN_B" ""
echo "3) B menulis atas nama A (harapan: DITOLAK, HTTP 403/42501)"
api user_profiles "$TOKEN_B" "{\"user_id\":\"$ID_A\",\"full_name\":\"Palsu\",\"business_name\":\"X\"}"
echo "4) Tanpa login, hanya key (harapan: DITOLAK atau kosong)"
api user_profiles "" ""
