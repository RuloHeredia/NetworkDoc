# NetworkDoc

Una aplicación de escritorio desarrollada en Java puro para documentar topologías de red, gestionar VLANs y mantener un inventario seguro de equipos (físicos, VMs en Proxmox, contenedores, etc.). 

Este sistema fue creado originalmente como proyecto para la materia de **Programación Orientada a Objetos**. Esta publicación representa una versión avanzada y extendida respecto a la evaluada en clase.

## Características Principales
* **Gestión de Topología:** Cálculo automático de broadcast mediante operaciones bitwise (*bitwise shifts*).
* **Inventario Detallado:** Registro de Nodos con soporte para MAC, VLAN ID, Tipo de Infraestructura y Estado Operativo.
* **Seguridad Integrada:** Cifrado nativo AES-128 para proteger las credenciales de acceso de los equipos de red.
* **Motor JSON Local:** Persistencia de datos construida desde cero sin necesidad de librerías o bases de datos externas.
* **Exportación:** Generación de reportes de inventario en formato CSV.

## Cómo compilar y ejecutar
Si quieres revisar o modificar el código, solo necesitas tener el JDK instalado:
1. Clona el repositorio: `git clone https://github.com/RuloHeredia/networkDoc.git`
2. Compila los archivos: `javac *.java`
3. Ejecuta el programa: `java NetworkDoc`

## Descargar
Si solo quieres usar la herramienta, ve a la sección de **Releases** a la derecha de esta página y descarga el archivo `NetworkDoc.jar` para ejecutarlo directamente con doble clic.
