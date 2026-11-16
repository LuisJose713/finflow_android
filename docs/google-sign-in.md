# Acceso y registro con Google en Android

El proyecto incluye el identificador **público** del cliente OAuth Web que utiliza
el servidor de FinFlow. Un checkout limpio de `main` muestra Google en Inicio de
sesión, Registro y Seguridad sin depender de una variable configurada en el PC.
No se incluye ningún secreto de cliente ni credencial de firma.

`FINFLOW_GOOGLE_WEB_CLIENT_ID` puede reemplazar ese valor mediante `-P` de Gradle,
una propiedad de usuario o una variable de entorno. Un valor vacío o inválido
detiene la compilación para impedir publicar otro APK sin acceso con Google.

## Comprobación antes de publicar

1. Actualizar `main` y compilar/firmar en Android Studio como de costumbre.
2. En el mismo proyecto de Google Cloud del cliente Web, comprobar el cliente
   OAuth **Android**: paquete `com.softdevcode.finflow_android` y huella SHA-1
   del certificado que firma el APK distribuido. Una clave de firma nueva
   necesita su propia huella registrada. No compartir el almacén ni su contraseña.
   El APK publicado como `v1.2.9` usa la huella SHA-1
   `7D:52:B9:85:73:FE:83:63:EF:E6:E3:D1:8E:0D:20:4C:D3:68:A7:3E`.
   Mantener la misma firma permite actualizar esa instalación.
3. Instalar el APK firmado en un dispositivo con Google Play Services. Comprobar
   `Continuar con Google` en Inicio de sesión y `Crear cuenta con Google` en
   Registro. El registro con Google no exige escribir nombre ni contraseña;
   conserva la autorización de privacidad cuando la política está publicada.
4. Verificar una cuenta nueva y una cuenta previamente vinculada. Para una cuenta
   existente por correo/contraseña, entrar con contraseña y vincular desde
   Seguridad; la identidad nunca se vincula automáticamente por coincidencia de correo.
5. Adjuntar el nuevo APK a una versión nueva de GitHub. Subir cambios de código
   a `main` **no modifica** los APK ya adjuntos a versiones anteriores.

El error 409 del registro por contraseña conserva el motivo enviado por el
servidor (correo registrado o nombre ocupado). Los códigos de Google distinguen
una cuenta que debe registrarse de una cuenta existente que debe vincularse.

Referencia: https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation
