# =====================================================================
# Punto 9: pruebas funcionales de la plataforma completa
# Recorre el flujo de negocio de principio a fin y comprueba cada resultado.
#
# Requisito: la plataforma levantada (docker compose up -d) y todo healthy.
# Ejecutar desde la raiz del proyecto con:
#   powershell -ExecutionPolicy Bypass -File .\scripts\pruebas-funcionales.ps1
# =====================================================================

Set-Location (Split-Path -Parent $PSScriptRoot)

$CUSTOMERS     = "http://localhost:8081"
$PRODUCTS      = "http://localhost:8082"
$ORDERS        = "http://localhost:8083"
$DELIVERIES    = "http://localhost:8084"
$NOTIFICATIONS = "http://localhost:8085"

$resultados = New-Object System.Collections.ArrayList

function Titulo($texto) {
    Write-Host ""
    Write-Host "--- $texto ---" -ForegroundColor Cyan
}

function Llamar($metodo, $url, $cuerpo) {
    $params = @{ Method = $metodo; Uri = $url; UseBasicParsing = $true;
                 ContentType = "application/json; charset=utf-8" }
    if ($null -ne $cuerpo) {
        $params.Body = [System.Text.Encoding]::UTF8.GetBytes(($cuerpo | ConvertTo-Json -Depth 6))
    }
    try {
        $r = Invoke-WebRequest @params
        $texto = [System.Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())
        $json = $null
        if ($texto) { $json = $texto | ConvertFrom-Json }
        return [pscustomobject]@{ Status = [int]$r.StatusCode; Body = $json }
    } catch {
        $code = 0
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        return [pscustomobject]@{ Status = $code; Body = $null }
    }
}

function Verificar($prueba, $esperado, $obtenido) {
    $ok = ("$esperado" -eq "$obtenido")
    [void]$resultados.Add([pscustomobject]@{
        Prueba = $prueba; Esperado = "$esperado"; Obtenido = "$obtenido";
        Resultado = $(if ($ok) { "OK" } else { "FALLO" }) })
    $color = $(if ($ok) { "Green" } else { "Red" })
    Write-Host ("  [{0}] {1} -> esperado: {2} | obtenido: {3}" -f $(if ($ok) { "OK" } else { "FALLO" }), $prueba, $esperado, $obtenido) -ForegroundColor $color
}

$ts = Get-Date -Format "yyyyMMddHHmmss"

# ---------------------------------------------------------------------
Titulo "1. Salud de los cinco microservicios"
foreach ($s in @(@("customer-service", $CUSTOMERS), @("product-service", $PRODUCTS),
                 @("order-service (via gateway)", $ORDERS), @("delivery-service", $DELIVERIES),
                 @("notification-service", $NOTIFICATIONS))) {
    $r = Llamar "GET" "$($s[1])/actuator/health" $null
    Verificar "Health de $($s[0])" "UP" $r.Body.status
}

# ---------------------------------------------------------------------
Titulo "2. Registrar cliente (customer-service)"
$r = Llamar "POST" "$CUSTOMERS/api/customers" @{
    name = "Cliente Prueba $ts"; email = "prueba$ts@mail.com";
    phone = "0990000000"; address = "Av. de las Pruebas 123, Quito" }
Verificar "Crear cliente" 201 $r.Status
$clienteId = $r.Body.id
Write-Host "  Cliente creado con id $clienteId"

# ---------------------------------------------------------------------
Titulo "3. Registrar productos (product-service)"
$r = Llamar "POST" "$PRODUCTS/api/products" @{
    name = "Leche 1 L ($ts)"; description = "Prueba funcional"; price = 1.10; stock = 20 }
Verificar "Crear producto A" 201 $r.Status
$prodA = $r.Body.id
$r = Llamar "POST" "$PRODUCTS/api/products" @{
    name = "Pan integral ($ts)"; description = "Prueba funcional"; price = 2.35; stock = 10 }
Verificar "Crear producto B" 201 $r.Status
$prodB = $r.Body.id
Write-Host "  Productos creados: A=$prodA (stock 20), B=$prodB (stock 10)"

# ---------------------------------------------------------------------
Titulo "4. Crear pedido con varios productos (order-service)"
$r = Llamar "POST" "$ORDERS/api/orders" @{
    customerId = $clienteId;
    items = @( @{ productId = $prodA; quantity = 3 }, @{ productId = $prodB; quantity = 2 } ) }
Verificar "Crear pedido" 201 $r.Status
$pedidoId = $r.Body.id
Verificar "Total calculado (3 x 1.10 + 2 x 2.35)" "8.00" $(([decimal]$r.Body.total).ToString("0.00", [System.Globalization.CultureInfo]::InvariantCulture))
Verificar "Direccion copiada desde customer-service" "Av. de las Pruebas 123, Quito" $r.Body.shippingAddress
Verificar "Estado inicial del pedido" "CREATED" $r.Body.status
Verificar "Aviso enviado a notification-service" "SENT" $r.Body.notificationStatus

# ---------------------------------------------------------------------
Titulo "5. El stock se desconto en product-service"
Verificar "Stock de A (20 - 3)" 17 $((Llamar "GET" "$PRODUCTS/api/products/$prodA" $null).Body.stock)
Verificar "Stock de B (10 - 2)" 8  $((Llamar "GET" "$PRODUCTS/api/products/$prodB" $null).Body.stock)

# ---------------------------------------------------------------------
Titulo "6. Casos de error en la creacion de pedidos"
$r = Llamar "POST" "$ORDERS/api/orders" @{ customerId = 999999; items = @( @{ productId = $prodA; quantity = 1 } ) }
Verificar "Cliente inexistente" 404 $r.Status
$r = Llamar "POST" "$ORDERS/api/orders" @{ customerId = $clienteId; items = @( @{ productId = $prodA; quantity = 1 }, @{ productId = $prodB; quantity = 1000 } ) }
Verificar "Stock insuficiente" 409 $r.Status
Verificar "Todo o nada: el stock de A no cambio" 17 $((Llamar "GET" "$PRODUCTS/api/products/$prodA" $null).Body.stock)
$r = Llamar "POST" "$ORDERS/api/orders" @{ customerId = $clienteId; items = @() }
Verificar "Pedido sin productos" 400 $r.Status

# ---------------------------------------------------------------------
Titulo "7. Registrar la entrega (delivery-service)"
$r = Llamar "POST" "$DELIVERIES/api/deliveries" @{ orderId = $pedidoId; courier = "Repartidor $ts" }
Verificar "Registrar entrega" 201 $r.Status
$entregaId = $r.Body.id
Verificar "Estado de la entrega" "IN_TRANSIT" $r.Body.status
Verificar "Pedido pasa a IN_DELIVERY" "IN_DELIVERY" $((Llamar "GET" "$ORDERS/api/orders/$pedidoId" $null).Body.status)
$r = Llamar "POST" "$DELIVERIES/api/deliveries" @{ orderId = $pedidoId; courier = "Otro" }
Verificar "Segunda entrega del mismo pedido" 409 $r.Status

# ---------------------------------------------------------------------
Titulo "8. Completar la entrega"
$r = Llamar "PATCH" "$DELIVERIES/api/deliveries/$entregaId/status" @{ status = "DELIVERED" }
Verificar "Marcar entrega como DELIVERED" 200 $r.Status
Verificar "Pedido pasa a DELIVERED" "DELIVERED" $((Llamar "GET" "$ORDERS/api/orders/$pedidoId" $null).Body.status)
$r = Llamar "PATCH" "$DELIVERIES/api/deliveries/$entregaId/status" @{ status = "DELIVERED" }
Verificar "Volver a entregarla" 409 $r.Status

# ---------------------------------------------------------------------
Titulo "9. Notificaciones recibidas por el cliente (notification-service)"
$avisos = (Llamar "GET" "$NOTIFICATIONS/api/notifications" $null).Body | Where-Object { $_.customerId -eq $clienteId }
$registrado = @($avisos | Where-Object { $_.message -like "*#$pedidoId fue registrado*" }).Count
$entregado  = @($avisos | Where-Object { $_.message -like "*#$pedidoId fue entregado*" }).Count
Verificar "Aviso de pedido registrado" 1 $registrado
Verificar "Aviso de pedido entregado" 1 $entregado

# ---------------------------------------------------------------------
Write-Host ""
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host " RESUMEN DE PRUEBAS FUNCIONALES" -ForegroundColor Cyan
Write-Host "=====================================================================" -ForegroundColor Cyan
$resultados | Format-Table -AutoSize
$fallos = @($resultados | Where-Object { $_.Resultado -eq "FALLO" }).Count
$total = $resultados.Count
if ($fallos -eq 0) {
    Write-Host "Todas las pruebas pasaron: $total de $total." -ForegroundColor Green
} else {
    Write-Host "$fallos de $total pruebas fallaron." -ForegroundColor Red
}
