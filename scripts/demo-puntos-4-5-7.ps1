# =====================================================================
# Demostracion de los puntos 4, 5 y 7 del caso practico
#   Punto 4: solo se publican los puertos necesarios
#   Punto 7: cambiar la configuracion SIN reconstruir la imagen
#   Punto 5: los datos sobreviven aunque se eliminen los contenedores
#
# Ejecutar desde la raiz del proyecto con:
#   powershell -ExecutionPolicy Bypass -File .\scripts\demo-puntos-4-5-7.ps1
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

function Esperar-Saludable {
    Write-Host "Esperando a que los contenedores esten healthy (puede tardar 1-2 minutos)..." -ForegroundColor Yellow
    for ($i = 1; $i -le 48; $i++) {
        $estados = docker compose ps --format "{{.Status}}"
        $sanos = @($estados | Select-String "\(healthy\)").Count
        $pendientes = @($estados | Select-String "starting|unhealthy").Count
        if ($sanos -ge 9 -and $pendientes -eq 0) {
            Write-Host "Listo: $sanos contenedores healthy." -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 5
    }
    Write-Host "Aviso: algunos contenedores no llegaron a healthy. Revisa 'docker compose ps'." -ForegroundColor Red
}

function Mostrar-Datos {
    Write-Host "`n--- Clientes (customer-db) ---" -ForegroundColor Green
    docker compose exec -T customer-db psql -U customer_user -d customer_db -c "SELECT id, name, email FROM customers ORDER BY id;"
    Write-Host "--- Productos (product-db) ---" -ForegroundColor Green
    docker compose exec -T product-db psql -U product_user -d product_db -c "SELECT id, name, price, stock FROM products ORDER BY id;"
    Write-Host "--- Pedidos (order-db) ---" -ForegroundColor Green
    docker compose exec -T order-db psql -U order_user -d order_db -c "SELECT id, customer_id, total, status, notification_status FROM orders ORDER BY id;"
    Write-Host "--- Entregas (delivery-db) ---" -ForegroundColor Green
    docker compose exec -T delivery-db psql -U delivery_user -d delivery_db -c "SELECT id, order_id, courier, status FROM deliveries ORDER BY id;"
}

# ---------------------------------------------------------------------
Titulo "1. Aplicar la nueva configuracion SIN --build"
Write-Host "Fijate: Docker NO construye ninguna imagen; solo recrea los contenedores cuya configuracion cambio." -ForegroundColor Yellow
docker compose up -d
Esperar-Saludable
Pausa

# ---------------------------------------------------------------------
Titulo "2. PUNTO 7: la configuracion cambio, la imagen NO"
Write-Host "Valor de NOTIFICATION_RETRY_MS dentro del contenedor (viene del archivo .env):" -ForegroundColor Yellow
docker compose exec -T order-service printenv NOTIFICATION_RETRY_MS
Write-Host "`nImagen que usa order-service (misma imagen y misma fecha de creacion de antes):" -ForegroundColor Yellow
docker compose images order-service
Pausa

# ---------------------------------------------------------------------
Titulo "3. PUNTO 4: puertos publicados hacia el exterior"
Write-Host "Las bases de datos y order-service ya NO publican puertos; solo los servicios y el gateway:" -ForegroundColor Yellow
docker compose ps --format "table {{.Name}}`t{{.Ports}}"
Pausa

# ---------------------------------------------------------------------
Titulo "4. PUNTO 5 (antes): datos guardados en las bases"
Mostrar-Datos
Pausa

# ---------------------------------------------------------------------
Titulo "5. PUNTO 5: ELIMINAR todos los contenedores (docker compose down)"
docker compose down
Write-Host "`nContenedores que quedan (la lista debe estar vacia):" -ForegroundColor Yellow
docker compose ps -a
Write-Host "`nVolumenes (siguen existiendo; 'down' sin -v no los borra):" -ForegroundColor Yellow
docker volume ls --filter "name=gestion-pedidos-uce"
Pausa

# ---------------------------------------------------------------------
Titulo "6. PUNTO 5: RECREAR todos los contenedores (docker compose up -d)"
docker compose up -d
Esperar-Saludable
docker compose ps --format "table {{.Name}}`t{{.Status}}"
Pausa

# ---------------------------------------------------------------------
Titulo "7. PUNTO 5 (despues): los datos siguen ahi"
Write-Host "Son contenedores NUEVOS, pero los datos son los mismos porque viven en los volumenes." -ForegroundColor Yellow
Mostrar-Datos
Write-Host "`nY a traves de la API de customer-service:" -ForegroundColor Yellow
Invoke-RestMethod http://localhost:8081/api/customers | Format-Table id, name, email
Pausa

Write-Host ""
Write-Host "Demostracion terminada." -ForegroundColor Green
