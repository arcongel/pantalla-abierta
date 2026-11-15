# Pantalla Abierta

App Android con dos pestañas:

- **TV en vivo**: los canales de Colombia (nacionales públicos, Caracol, RCN y los
  8 regionales). Cada canal se abre en su página o en su app oficial.
- **Películas y series**: películas y series de dominio público o con licencia
  Creative Commons del Internet Archive, con reproductor propio, más un acceso a
  RTVC Play (cine y series colombianas gratis y públicas).

## Por qué es legal

1. **La app no copia ni retransmite ninguna señal de TV.** No guarda enlaces de
   transmisión. Solo abre las páginas y apps oficiales de cada canal, como lo
   haría un marcador del navegador. Los canales siguen controlando su señal, su
   publicidad y sus restricciones (por ejemplo, Caracol y RCN solo funcionan en
   Colombia).
2. **Solo reproduce videos con licencia libre.** El catálogo filtra el Internet
   Archive y acepta únicamente títulos marcados como dominio público o Creative
   Commons. Cada título muestra su licencia con un enlace para verificarla.
3. **El nombre y el ícono son propios.** No usa logos ni marcas de los canales.
   Los nombres de los canales solo se usan para identificarlos.
4. **No recoge datos.** No tiene cuentas, publicidad ni rastreo. Los favoritos
   se guardan solo en el teléfono.

## Descargar el APK

Cada vez que cambia el código, GitHub compila la app sola (pestaña **Actions**,
unos 5 a 8 minutos). El APK queda en **Releases**, en la columna derecha del
repositorio: descarga `PantallaAbierta.apk`, ábrelo en el teléfono y permite
"instalar apps de origen desconocido" cuando Android lo pida.

Cada compilación usa una llave distinta, así que para instalar una versión
nueva primero desinstala la anterior.

## Cambiar la lista de canales

Edita `app/src/main/assets/channels.json` en GitHub (ícono de lápiz). Al
guardar, GitHub compila un APK nuevo automáticamente. Usa solo direcciones
oficiales de los canales.

## Publicar en Google Play

Para Play Store necesitas firmar con tu propia llave (no la de depuración que
trae este proyecto), una política de privacidad (puede decir que la app no
recoge datos) y declarar en la ficha que la app enlaza a servicios oficiales
de terceros. No uses los logos de los canales en la ficha ni en las capturas.
