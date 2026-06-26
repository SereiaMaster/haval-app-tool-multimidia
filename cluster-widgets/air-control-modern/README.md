# air-control-modern

Cópia de trabalho baseada em **`air-control`**, para desenvolveres **o teu tema / cockpit** sem alterar o pacote principal até estares pronto.

## O que isto muda em relação ao `air-control`

| | `air-control` | `air-control-modern` |
|--|----------------|----------------------|
| Nome npm | `cluster-air-ui` | `cluster-air-modern` |
| Build inlined | `dist/app.html` → copia `res/raw/app.html` | `dist/app_modern.html` → copia **`res/raw/app_modern.html`** |
| Efeito | Substitui o cluster **padrão** do APK | **Não** toca no `app.html` original |

## Desenvolvimento no browser

Requer **Node 18+**.

```bash
cd cluster-widgets/air-control-modern
npm install
npm run dev
```

Abre o URL que o Parcel mostrar (ex.: `http://localhost:1234`). Em dev, carrega-se o `testing-utils.js` como no projeto original.

## Build para o carro / Android

```bash
npm run build
```

Gera `dist/app_modern.html` e copia para `app/src/main/res/raw/app_modern.html`.

### Como usar no app

1. **Tema instalável (ThemeManager):** cria `cluster-widgets/Themes/<Nome>/theme.xml` com `<mainFile>app_modern.html</mainFile>`, coloca o HTML gerado nessa pasta e instala/copia para `filesDir/themes/` no dispositivo, depois escolhe o tema em **Telas**.
2. **Substituir o cockpit por defeito:** ou copias o conteúdo de `app_modern.html` para o fluxo que lê `R.raw.app`, ou alteras `InstrumentProjector2` / `readAppContent` para ler `R.raw.app_modern` quando quiseres testar (mudança Kotlin explícita).

## Nota

O código JS (`src/core/...`) é o **mesmo** que no `air-control` na cópia inicial. Podes ir **editando estilos e componentes** aqui; merges futuros do `air-control` upstream terão de ser feitos **à mão** ou com git se quiseres manter os dois alinhados.
