/**
 * design/montage.tokens.json → src/tokens.css
 *
 * 디자인 시스템(Claude Design 의 Montage)이 준 토큰 파일을 CSS 커스텀 프로퍼티와
 * 타입 스타일 클래스로 바꾼다. 색 값을 손으로 옮겨 적는 일을 없애는 것이 목적이다.
 * 디자인 시스템이 갱신되면 tokens.json 만 다시 받아서 `npm run tokens` 를 돌린다.
 */
import { readFile, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const tokens = JSON.parse(await readFile(root + 'design/montage.tokens.json', 'utf8'));

/** 색 토큰은 테마마다 값이 다르다. 문자열 하나면 모든 테마 공통. */
const valueFor = (token, themeId, firstThemeId) => {
  if (typeof token.value === 'string') return token.value;
  return token.value[themeId] ?? token.value[firstThemeId];
};

const decl = (name, value) => `  --${name}: ${value};`;

const lines = [];
lines.push('/* 이 파일은 scripts/build-tokens.mjs 가 만든다. 직접 고치지 말 것. */');
lines.push(`/* 출처: ${tokens.meta.repo} @ ${tokens.meta.ref} (${tokens.meta.synced}) */`);
lines.push('');

const themes = tokens.color.themes;
const first = themes[0].id;

// ── 1. 테마와 무관한 토큰: 간격, 모서리, 브레이크포인트, 폰트 패밀리
const flat = [];
for (const family of ['spacing', 'radius', 'breakpoint']) {
  for (const t of tokens[family]?.tokens ?? []) flat.push(decl(t.name, t.value));
}
for (const [key, stack] of Object.entries(tokens.type.families)) {
  flat.push(decl(`font-${key}`, stack));
}

// ── 2. 테마별 토큰: 색과 그림자
const themed = (themeId) => {
  const out = [];
  for (const t of tokens.color.tokens) out.push(decl(t.name, valueFor(t, themeId, first)));
  for (const t of tokens.shadow?.tokens ?? []) out.push(decl(t.name, valueFor(t, themeId, first)));
  return out;
};

lines.push(`:root {`, ...flat, '', ...themed(first), '}');
lines.push('');

for (const theme of themes.slice(1)) {
  // 명시적으로 고른 테마
  lines.push(`[data-theme="${theme.id}"] {`, ...themed(theme.id), '}');
  lines.push('');
  // 아무것도 고르지 않았으면 OS 설정을 따른다
  lines.push(`@media (prefers-color-scheme: ${theme.id}) {`);
  lines.push(`  :root:not([data-theme]) {`);
  lines.push(...themed(theme.id).map((l) => '  ' + l));
  lines.push('  }');
  lines.push('}');
  lines.push('');
}

// ── 3. 타입 스타일: 클래스 하나가 크기·행간·자간·굵기를 함께 정한다.
//     Montage 는 크기마다 자간이 다르므로 font-size 만 따로 쓰면 안 된다.
lines.push('/* 타입 스타일. font-size 를 직접 쓰지 말고 이 클래스를 쓴다. */');
for (const group of tokens.type.groups) {
  for (const s of group.styles) {
    const body = [
      `  font-family: var(--font-${s.family ?? group.family});`,
      `  font-size: ${s.fontSize};`,
      `  line-height: ${s.lineHeight};`,
      s.letterSpacing ? `  letter-spacing: ${s.letterSpacing};` : null,
      `  font-weight: ${s.fontWeight};`,
    ].filter(Boolean);
    lines.push(`.${s.name} {`, ...body, '}');
  }
}
lines.push('');

await writeFile(root + 'src/tokens.css', lines.join('\n'), 'utf8');

const count =
  tokens.color.tokens.length +
  (tokens.spacing?.tokens.length ?? 0) +
  (tokens.radius?.tokens.length ?? 0) +
  (tokens.shadow?.tokens.length ?? 0) +
  (tokens.breakpoint?.tokens.length ?? 0);
const styles = tokens.type.groups.reduce((n, g) => n + g.styles.length, 0);
console.log(`src/tokens.css — 토큰 ${count}개, 테마 ${themes.length}개, 타입 스타일 ${styles}개`);
