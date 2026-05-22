# NapTx — Sistema de Pagos Semi-Offline con Blockchain

NapTx es una aplicación Android que permite realizar pagos con tokens NPTX sobre la red de blockchain de Ethereum (red de pruebas Sepolia) **sin que el emisor necesite conexión a internet durante el proceso de pago**. El intercambio de información entre emisor y receptor se realiza mediante **códigos QR**, y todas las operaciones críticas quedan registradas en el smart contract desplegado en Sepolia.

---

## Índice

- [Descripción general](#descripción-general)
- [Arquitectura](#arquitectura)
- [Flujo de pago](#flujo-de-pago)
- [Seguridad](#seguridad)
- [Smart Contract](#smart-contract)
- [Requisitos](#requisitos)
- [Instalación y ejecución](#instalación-y-ejecución)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Configuración de red](#configuración-de-red)

---

## Descripción general

**NapTx** hereda del token ERC-20 propio de la aplicación: **NPTX**. Los usuarios pueden:

| Acción | Descripción |
|---|---|
| Crear wallet | Genera un par de claves Ethereum en el dispositivo, cifrado con biometría |
| Comprar NPTX | Envía SepoliaETH al contrato y recibe tokens NPTX a cambio |
| Vender NPTX | Devuelve tokens NPTX al contrato y recupera SepoliaETH |
| Actuar como **Emisor** (pagador) | Registrarse, gestionar whitelist, generar pagos QR offline |
| Actuar como **Receptor** | Escanear QR del emisor, preparar y confirmar pago en blockchain |
| Ver estado | Consultar balances NPTX y ETH, hash actual y enlace a Etherscan |

---

## Arquitectura

El proyecto sigue una arquitectura en capas:

```
app/src/main/java/com/example/offlinepaymentsystem/
├── data/
│   ├── AppDatabase.java          # Base de datos Room (singleton)
│   ├── blockchain/
│   │   └── Web3Manager.java      # Toda la lógica de interacción con Ethereum
│   ├── local/
│   │   ├── WalletManager.java    # Gestión de clave privada + biometría
│   │   ├── DeviceIdManager.java  # Identificador de dispositivo (Keccak256 del Android ID)
│   │   ├── PagoPendienteDao.java # DAO Room para pagos
│   │   └── WhitelistDao.java     # DAO Room para whitelist
│   └── repository/
│       ├── PagoRepository.java
│       └── WhitelistRepository.java
├── model/
│   ├── PagoPendiente.java        # Entidad Room — estados: PREPARADO, CONFIRMADO, REVERTIDO, FALLIDO
│   ├── WhitelistItem.java        # Entidad Room — receptor autorizado + límite de gasto
│   └── Emisor.java               # Estado del emisor en blockchain (no persistido en Room) --> en Shared Preferences
├── ui/
│   ├── main/MainActivity.java
│   ├── wallet/CrearWalletActivity.java
│   ├── tokens/{Comprar,Vender}TokensActivity.java
│   ├── emisor/{Emisor,RegistrarEmisor,GestionarWhitelist,GenerarPago,ConfirmarPago,ActualizarHash}Activity.java
│   └── receptor/{Receptor,EscanearPago,CompletarPago}Activity.java
└── utils/
    ├── Constants.java            # URLs, address del contrato, claves de SharedPreferences
    ├── CryptoConstants.java      # Chain ID, gas limits, firmas de eventos
    ├── CryptoUtils.java          # Conversiones wei↔ETH, bytes helpers
    └── QRCodeHelper.java         # Generación de QR con ZXing
```

**Persistencia local:** Room Database con dos tablas (`pagos_pendientes` y `whitelist`).  
**Persistencia de wallet:** Fichero cifrado (`wallet_encrypted.dat`) + vector de inicialización (`wallet_iv.dat`) en el directorio privado de la app.

---

## Flujo de pago

El sistema implementa un protocolo de **dos fases y cuatro QRs** que permite al emisor autorizar un pago completamente offline mientras el receptor interactúa con la blockchain.

```
EMISOR (sin conexión)                     RECEPTOR (con conexión)
        │                                          │
        │  1. Firma el pago con biometría          │
        │     (hashUsado, importe, receptor,       │
        │      timestamp, nonce, deviceId)         │
        │                                          │
        │────────── QR1: pago firmado ───────────► │
        │                                          │
        │                         2. Verifica que  │
        │                            el QR está    │
        │                            dirigido a él │
        │                                          │
        │                         3. Llama a       │
        │                            prepararPago()│
        │                            en blockchain │
        │                                          │
        │◄──── QR2: {pagoId, hashPreparado} ─────  │
        │                                          │
        │  4. Verifica offline que el              │
        │     hashPreparado coincide con           │
        │     los datos que él firmó               │
        │                                          │
        │  5. Firma la confirmación con biometría  │
        │     (pagoId, hashPreparado)              │
        │                                          │
        │────────── QR3: confirmación ───────────► │
        │                                          │
        │                         6. Llama a       │
        │                            confirmarPago()
        │                            en blockchain │
        │                            (transfiere   │
        │                            tokens NPTX)  │
        │                                          │
        │◄──── QR4: {hashFinal} ─────────────────  │
        │                                          │
        │  7. Guarda el hashFinal como nuevo       │
        │     hashActual para el siguiente pago    │
        │                                          │
```

> El emisor no necesita conexión a internet en ningún momento del proceso de pago. Solo necesita conexión para el registro inicial, la configuración de la whitelist y la compra/venta de tokens.

### Estados del pago (`PagoPendiente.Estado`)

| Estado | Descripción |
|---|---|
| `PREPARADO` | QR1 procesado en blockchain, esperando confirmación del emisor |
| `CONFIRMADO` | QR3 procesado, transferencia de tokens ejecutada |
| `REVERTIDO` | Timeout expirado sin confirmación, pago cancelado |
| `FALLIDO` | Error durante el procesamiento |

---

## Seguridad

### Protección de la clave privada

La clave privada Ethereum **nunca se almacena en claro**:

1. Se genera un par de claves EC con `Keys.createEcKeyPair()` (Web3j / BouncyCastle).
2. Se crea una clave **AES-256-GCM** en el **Android Keystore** con alias `naptx_wallet_cipher_key`, marcada como `setUserAuthenticationRequired(true)`.
3. La clave privada se cifra con AES-GCM. El resultado cifrado y el IV se guardan en ficheros internos.
4. Cada acceso a la clave (firma de transacciones, firma de pagos) requiere **autenticación biométrica** que desbloquea el `Cipher` asociado al Keystore.
5. Tras el uso, los bytes descifrados se borran de memoria con `Arrays.fill(decryptedKey, (byte) 0)`.

### Vinculación al dispositivo

El `deviceId` se calcula como `Keccak256(Android ID)` y se registra en el smart contract al darse de alta como emisor. Cualquier operación debe incluir este identificador, vinculando el emisor a su dispositivo físico e impidiendo el uso de la clave privada desde otro dispositivo.

### Protección anti-replay

Cada mensaje firmado incluye un **nonce aleatorio** (`SecureRandom`) y un **timestamp Unix**. El smart contract rechaza mensajes ya procesados o con timestamps fuera de la ventana de tolerancia configurada.

### Encadenamiento de hashes (prevención de doble gasto)

El contrato mantiene un `hashActual` por emisor que evoluciona con cada pago confirmado. La app lo persiste en `SharedPreferences` (`HASH_ACTUAL`) y lo incluye como `hashUsado` en el siguiente pago, creando una cadena criptográfica que garantiza que el emisor solo puede tener un pago válido pendiente en cada momento.

---

## Smart Contract

- **Red:** Ethereum Sepolia Testnet
- **Dirección:** `0x0AC55694CD16e75eCcbf585F0b813f3625E64905` (puedes cambiarla desplegando otro contrato e incluyendo su dirección en `Constants.java`
- **ABI:** `app/src/main/assets/OfflinePaymentSystem.json`
- **Proveedor RPC:** Infura Sepolia

### Funciones principales

| Función | Llamada por | Descripción |
|---|---|---|
| `registrar(deviceId, timestamp, nonce, firma)` | Emisor (online) | Da de alta al emisor con su dispositivo |
| `configurarWhitelist(receptores[], limites[], timestamp, nonce, firma)` | Emisor (online) | Establece receptores autorizados con límite por receptor |
| `prepararPago(hashUsado, amount, receptor, timestamp, nonce, deviceId, firma)` | Receptor | Fase 1: verifica la firma del emisor y reserva la transferencia |
| `confirmarPago(pagoId, hashPreparado, firmaConfirmacion)` | Receptor | Fase 2: verifica la confirmación del emisor y ejecuta la transferencia |
| `revertirPago(pagoId)` | Cualquiera | Cancela un pago que ha superado el timeout sin confirmarse |
| `comprarTokens()` payable | Cualquiera | Intercambia SepoliaETH por NPTX |
| `venderTokens(cantidad)` | Cualquiera | Intercambia NPTX por SepoliaETH |
| `balanceOf(address)` | Consulta | Balance NPTX (ERC-20) |
| `obtenerEstadoEmisor(address)` | Consulta | Devuelve `hashActual`, `registrado`, `timestampUltimoPago`, `deviceId` |

### Eventos

| Evento | Descripción |
|---|---|
| `EmisorRegistrado(address, bytes32, bytes32, uint256)` | Emisor registrado; contiene `hashInicial` |
| `PagoPreparado(bytes32, address, address, uint256, bytes32, uint256)` | Fase 1 completada; contiene `pagoId` y `hashPreparado` |
| `PagoConfirmado(bytes32, address, address, uint256, bytes32, uint256)` | Fase 2 completada; contiene `hashFinal` |
| `PagoRevertido(bytes32, address, address, uint256)` | Pago cancelado por timeout |
| `WhitelistConfigurada(address, uint256)` | Whitelist actualizada |
| `TokensComprados(address, uint256, uint256)` | Compra de tokens ejecutada |
| `TokensVendidos(address, uint256, uint256)` | Venta de tokens ejecutada |

---

### Primeros pasos en la app

1. **Crear wallet** — genera y cifra tu clave privada Ethereum con tu huella dactilar.
2. **Obtener SepoliaETH** — desde un faucet de Sepolia (tu dirección se muestra al consultar el estado).
3. **Comprar NPTX** — intercambia SepoliaETH por tokens NPTX.
4. **Registrar emisor** — vincula tu wallet y dispositivo al smart contract (requiere conexión).
5. **Gestionar whitelist** — añade las direcciones de los receptores autorizados y sus límites (requiere conexión).
6. **Generar pago** — selecciona receptor, introduce importe y firma con huella dactilar (no requiere conexión).

---

## Configuración de red

Los parámetros de conexión a la red se encuentran en `utils/Constants.java` y `utils/CryptoConstants.java`:
