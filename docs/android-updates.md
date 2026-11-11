# Actualizaciones de FinFlow Android

La app consulta en segundo plano el manifiesto de la última versión estable de
[GitHub Releases](https://github.com/LuisJose713/finflow_android/releases).
Al abrirla o volver a ella, busca novedades con un intervalo mínimo de 15 minutos;
también comprueba cada 15 minutos mientras permanece abierta.
También se puede consultar en **Más → Actualizaciones → Buscar actualizaciones**.
Una versión mayor muestra sus cambios y **Descargar actualización** abre la APK
en el navegador. Android confirma la instalación. No se modifica la sesión.

## Publicar una versión compilada en Android Studio

1. Incrementar `versionCode` y actualizar `versionName` en `app/build.gradle.kts`.
2. Generar la APK con la misma clave de firma y el mismo tipo de compilación que
   la versión instalada. Conservar esa clave entre versiones; una firma diferente
   no permite actualizar una instalación existente.
3. Crear un borrador de release desde `main`, con etiqueta `v` + `versionName`
   (por ejemplo `v1.2.3-finflow`). Escribir las novedades en su descripción.
4. Adjuntar **una sola APK firmada** con un nombre sencillo, por ejemplo
   `FinFlow-1.2.3.apk`, y publicar la release como estable y la más reciente.
5. El workflow **Publish Android update notice (existing APK)** comprueba la
   firma y el identificador, lee la versión real de la APK y publica
   `finflow-update.json`. No compila ni genera APKs.
6. Esperar a que termine en verde y comprobar desde una instalación anterior
   con **Buscar actualizaciones**. Si se adjuntó la APK después de publicar,
   ejecutar ese workflow manualmente indicando la etiqueta de la release.

Subir código a `main` no produce una actualización descargable. El aviso se
activa al publicar una APK válida y su manifiesto. No se ofrecen versiones iguales
o anteriores, releases de prueba, descargas externas ni enlaces sin APK.
Sin conexión la app sigue funcionando; la consulta manual permite reintentar.

La instalación que todavía no incluye este mecanismo debe actualizarse
manualmente una primera vez. A partir de ella podrá avisar de versiones futuras.
