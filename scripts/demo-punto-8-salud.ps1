# =====================================================================
# Demostracion del punto 8: comprobaciones de salud (HEALTHCHECK)
#   - Condicion de servicio saludable: GET /actuator/health responde UP (HTTP 200)
#   - Se simula un fallo deteniendo customer-db
#
# Ejecutar desde la raiz del proyecto con:
#   powershell -ExecutionPolicy Bypass -File .\scripts\demo-punto-8-salud.ps1
# =====================================================================

Set-Location (Split-Path -Parent $PSScriptRoot)

function Titulo($texto) {
    Write-Host ""
    Write-Host "=====================================================================" -ForegroundColor Cyan
    Write-Host " $texto" -ForegroundColor Cyan
    Write-Host "=====================================================================" -ForegroundColor Cyan
}

function Pausa {
    Write-Host ""
    Read-Host "  >> Toma la captura de esta seccion y pulsa Enter para continuar"
}

function Estado-Salud($contenedor) {
    return (docker inspect --format "{{.State.Health.Status}}" $contenedor).Trim()
}

function Esperar-Estado($contenedor, $esperado, $maxSegundos) {
    Write-Host "Esperando a que $contenedor este '$esperado' (maximo $maxSegundos s)..." -ForegroundColor Yellow
    for ($s = 0; $s -le $maxSegundos; $s += 5) {
        $actual = Estado-Salud $contenedor
        Write-Host "  [$s s] $contenedor -> $actual"
        if ($actual -eq $esperado) { return }
        Start-Sleep -Seconds 5
    }
}

function Mostrar-Health($url) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing
        Write-Host "HTTP $([int]$r.StatusCode)" -ForegroundColor Green
        Write-Host ([System.Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray()))
    } catch {
        $code = 0
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        Write-Host "HTTP $code" -ForegroundColor Red
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message }
    }
}

# ---------------------------------------------------------------------
Titulo "1. Estado inicial: todos los contenedores healthy"
docker compose ps --format "table {{.Name}}`t{{.Status}}"
Pausa

# ---------------------------------------------------------------------
Titulo "2. Condicion de salud: /actuator/health de customer-service"
Write-Host "Un servicio es saludable cuando este endpoint responde UP (HTTP 200)." -ForegroundColor Yellow
Write-Host "Incluye el estado de su base de datos (componente 'db')." -ForegroundColor Yellow
Mostrar-Health "http://localhost:8081/actuator/health"
Pausa

# ---------------------------------------------------------------------
Titulo "3. SIMULAR FALLO: detener customer-db"
docker compose stop customer-db
Esperar-Estado "customer-service" "unhealthy" 120
Write-Host ""
docker compose ps --format "table {{.Name}}`t{{.Status}}"
Pausa

# ---------------------------------------------------------------------
Titulo "4. customer-service sigue encendido, pero responde DOWN (HTTP 503)"
Mostrar-Health "http://localhost:8081/actuator/health"
Write-Host "`nEl resto de la plataforma sigue funcionando. Ejemplo: product-service" -ForegroundColor Yellow
Mostrar-Health "http://localhost:8082/actuator/health"
Pausa

# ---------------------------------------------------------------------
Titulo "5. RECUPERACION: volver a encender customer-db"
docker compose start customer-db
Esperar-Estado "customer-service" "healthy" 120
Write-Host ""
docker compose ps --format "table {{.Name}}`t{{.Status}}"
Write-Host "`ncustomer-service se recupero solo, sin reiniciarlo:" -ForegroundColor Yellow
Mostrar-Health "http://localhost:8081/actuator/health"
Pausa

Write-Host ""
Write-Host "Demostracion terminada." -ForegroundColor Green
