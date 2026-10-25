# =====================================================================
# Demostracion de escalabilidad basica (objetivo del caso)
#   - Se levantan 3 copias de order-service
#   - order-gateway (Nginx) reparte las peticiones entre ellas
#
# Ejecutar desde la raiz del proyecto con:
#   powershell -ExecutionPolicy Bypass -File .\scripts\demo-escalabilidad.ps1
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

function Esperar-Copias($cantidad) {
    Write-Host "Esperando a que las $cantidad copias de order-service esten healthy..." -ForegroundColor Yellow
    for ($i = 1; $i -le 36; $i++) {
        $sanas = @(docker compose ps order-service --format "{{.Status}}" | Select-String "\(healthy\)").Count
        if ($sanas -ge $cantidad) {
            Write-Host "Listo: $sanas copia(s) healthy." -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 5
    }
    Write-Host "Aviso: no todas las copias llegaron a healthy. Revisa 'docker compose ps'." -ForegroundColor Red
}

# ---------------------------------------------------------------------
Titulo "1. Escalar order-service a 3 copias"
docker compose up -d --scale order-service=3
Esperar-Copias 3
Write-Host ""
docker compose ps --format "table {{.Name}}`t{{.Status}}"
Pausa

# ---------------------------------------------------------------------
Titulo "2. El balanceador reparte las peticiones entre las copias"
Write-Host "Cada peticion entra por localhost:8083 (order-gateway)." -ForegroundColor Yellow
Write-Host "La cabecera X-Served-By indica que copia la atendio:`n" -ForegroundColor Yellow
Start-Sleep -Seconds 6
for ($i = 1; $i -le 6; $i++) {
    $r = Invoke-WebRequest -Uri "http://localhost:8083/api/orders" -UseBasicParsing
    Write-Host ("  Peticion {0} -> HTTP {1} atendida por {2}" -f $i, [int]$r.StatusCode, $r.Headers["X-Served-By"])
    Start-Sleep -Milliseconds 300
}
Pausa

# ---------------------------------------------------------------------
Titulo "3. Costo de escalar: recursos de cada contenedor"
docker stats --no-stream --format "table {{.Name}}`t{{.CPUPerc}}`t{{.MemUsage}}"
Pausa

# ---------------------------------------------------------------------
Titulo "4. Volver a una sola copia"
docker compose up -d --scale order-service=1
Write-Host ""
docker compose ps order-service --format "table {{.Name}}`t{{.Status}}"
Pausa

Write-Host ""
Write-Host "Demostracion terminada." -ForegroundColor Green
