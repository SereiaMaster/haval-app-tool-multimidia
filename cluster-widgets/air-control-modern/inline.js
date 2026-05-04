var fs = require('fs');
var path = require('path');

/** Output filename (single inlined HTML for theme / cluster). */
var OUTPUT_HTML = 'app_modern.html';
/** Android raw resource name (must be lowercase [a-z0-9_].). */
var ANDROID_RAW_NAME = 'app_modern.html';

// Função para processar um HTML e inlinear CSS/JS
function processHtml(htmlPath, outputPath) {
  console.log(`🔄 Processando: ${htmlPath}`);

  if (!fs.existsSync(htmlPath)) {
    console.log(`❌ Arquivo não encontrado: ${htmlPath}`);
    return;
  }

  var htmlContent = fs.readFileSync(htmlPath, 'utf8');

  // Inline CSS
  var cssRegex = /<link[^>]*href=([^>\s]+\.css)[^>]*>/g;
  var cssMatch;
  while ((cssMatch = cssRegex.exec(htmlContent)) !== null) {
    var cssPath = cssMatch[1].replace(/['"]/g, '');

    var cleanCssPath = cssPath.startsWith('/') ? cssPath.substring(1) : cssPath;
    var fullCssPath;

    if (cleanCssPath.startsWith('src/')) {
      fullCssPath = path.join(__dirname, cleanCssPath);
    } else {
      fullCssPath = path.join(__dirname, 'dist', cleanCssPath);
    }

    if (fs.existsSync(fullCssPath)) {
      var cssContent = fs.readFileSync(fullCssPath, 'utf8');
      htmlContent = htmlContent.split(cssMatch[0]).join('<style>' + cssContent + '</style>');
      console.log('✅ CSS inlined:', cssPath);
    }
  }

  // Inline JavaScript
  var jsRegex = /<script\s+([^>]*?)src=["']?([^"'\s>]+\.js)["']?([^>]*?)><\/script>/gi;
  var jsMatch;
  console.log('🔍 Buscando scripts para inlinear...');
  while ((jsMatch = jsRegex.exec(htmlContent)) !== null) {
    console.log(`✨ Tag de script encontrada: ${jsMatch[0]}`);
    var beforeSrc = jsMatch[1];
    var jsPath = jsMatch[2];
    var afterSrc = jsMatch[3];

    var cleanJsPath = jsPath.startsWith('/') ? jsPath.substring(1) : jsPath;
    var fullJsPath = path.join(__dirname, 'dist', cleanJsPath);
    console.log(`🔍 Tentando inlinear JS: ${jsPath} -> ${fullJsPath}`);

    if (fs.existsSync(fullJsPath)) {
      console.log(`✅ JS encontrado: ${fullJsPath}`);
      var jsContent = fs.readFileSync(fullJsPath, 'utf8');

      jsContent = jsContent.replace(/\/\/# sourceMappingURL=.*/g, '');

      var attributes = (beforeSrc + ' ' + afterSrc).trim();
      var isModule = attributes.includes('type="module"') || attributes.includes('type=module');

      var scriptTag = isModule ? '<script type="module">' : '<script>';
      var replacement = scriptTag + jsContent + '</script>';

      htmlContent = htmlContent.split(jsMatch[0]).join(replacement);

      if (fs.existsSync(fullJsPath)) {
        fs.unlinkSync(fullJsPath);
      }
      console.log('✅ JS inlined:', jsPath + (isModule ? ' (as module)' : ''));
    }
  }

  fs.writeFileSync(outputPath, htmlContent, 'utf8');
  console.log(`✅ HTML gerado: ${outputPath}`);
}

function inlineDynamicAssets(htmlPath) {
  console.log(`🔍 Buscando assets dinâmicos em: ${htmlPath}`);
  var htmlContent = fs.readFileSync(htmlPath, 'utf8');
  var distDir = path.join(__dirname, 'dist');
  if (!fs.existsSync(distDir)) return;
  var files = fs.readdirSync(distDir);

  var changed = false;
  files.forEach(function (file) {
    if (file.endsWith('.css') && !file.includes('.map')) {
      if (htmlContent.includes(file)) {
        var filePath = path.join(distDir, file);
        var content = fs.readFileSync(filePath, 'utf8');
        var base64 = Buffer.from(content).toString('base64');
        var dataUri = 'data:text/css;base64,' + base64;

        const escapedFile = file.replace(/\./g, '\\.');
        const resolveRegex = new RegExp(
          'module\\.bundle\\.resolve\\((["\'])' + escapedFile + '(["\'])\\)([^,;\\n\\r)]*)',
          'g'
        );

        if (resolveRegex.test(htmlContent)) {
          console.log(`📦 Inlining dynamic asset (wrapped-robust): ${file}`);
          htmlContent = htmlContent.replace(resolveRegex, '"' + dataUri + '"');
          changed = true;
        }

        const importMapRegex = new RegExp('(["\']):\\s*(["\'])/' + escapedFile + '(["\'])', 'g');
        if (importMapRegex.test(htmlContent)) {
          console.log(`📦 Inlining dynamic asset (regex-importmap): ${file}`);
          htmlContent = htmlContent.replace(importMapRegex, '$1:$2' + dataUri + '$3');
          changed = true;
        }

        const plainRegex = new RegExp('(["\'])([\\./]*)' + escapedFile + '(["\'])', 'g');
        if (plainRegex.test(htmlContent)) {
          console.log(`📦 Inlining dynamic asset (regex-plain): ${file}`);
          htmlContent = htmlContent.replace(plainRegex, '$1' + dataUri + '$3');
          changed = true;
        }

        fs.unlinkSync(filePath);
      }
    }
  });

  if (changed) {
    fs.writeFileSync(htmlPath, htmlContent, 'utf8');
    console.log(`✅ Assets dinâmicos inlined em: ${htmlPath}`);
  }
}

console.log('🚀 Build air-control-modern → ' + OUTPUT_HTML + ' ...');

var indexHtmlPath = path.join(__dirname, 'dist', 'index.html');
var appOutputPath = path.join(__dirname, 'dist', OUTPUT_HTML);
processHtml(indexHtmlPath, appOutputPath);

inlineDynamicAssets(appOutputPath);

var androidRawPath = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'res', 'raw', ANDROID_RAW_NAME);
try {
  fs.copyFileSync(appOutputPath, androidRawPath);
  console.log(`✅ Copiado para Android: ${androidRawPath}`);
  console.log('   (R.raw.app_modern no Kotlin — ainda precisas apontar o WebView para este raw se quiseres substituir o cockpit padrão.)');
} catch (err) {
  console.error(`❌ Erro ao copiar para Android: ${err.message}`);
}

var assetsDir = path.join(__dirname, 'dist', 'assets');
if (fs.existsSync(assetsDir)) {
  var assetFiles = fs.readdirSync(assetsDir);
  if (assetFiles.length === 0) {
    fs.rmdirSync(assetsDir);
    console.log('✅ Pasta assets removida');
  }
}

var cssFiles = ['night.style.css', 'light.style.css', 'style.css'];
cssFiles.forEach(function (cssFile) {
  var cssPath = path.join(__dirname, 'dist', cssFile);
  if (fs.existsSync(cssPath)) {
    fs.unlinkSync(cssPath);
    console.log(`✅ CSS removido: ${cssFile}`);
  }
});

console.log('🎉 Build completo!');
console.log('  📄 dist/' + OUTPUT_HTML);
