function getNFromMode(modeValue) {
  const mode = Number.parseInt(String(modeValue ?? ''), 10);
  if (mode >= 1 && mode <= 4) return 1;
  if (mode === 5) return 2;
  if (mode === 6 || mode === 7) return 3;
  return null;
}

function getSFromSubbands(subbandsValue) {
  const raw = String(subbandsValue ?? '').trim();
  if (!raw) return null;
  const firstToken = raw.split(',')[0].trim();
  const sRaw = firstToken.includes(':') ? firstToken.split(':')[0].trim() : firstToken;
  const s = Number.parseFloat(sRaw);
  return Number.isFinite(s) ? s : null;
}

export function calculateBeamletsFromForum(modeValue, subbandsValue, clockValue) {
  const n = getNFromMode(modeValue);
  const s = getSFromSubbands(subbandsValue);
  const clock = Number.parseFloat(String(clockValue ?? ''));

  if (n === null || s === null || !Number.isFinite(clock)) {
    return '';
  }

  const v = (n - 1 + s / 512) * (clock / 2);
  return Number(v.toFixed(6)).toString();
}
