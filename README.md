# uees-sd-2020-tarea06
# Giuseppe Carmigniani

## Instalacion

Para ejectuar el programa, es necesario tener el Java JRE y JDK, para lo cual se puede usar los siguientes comandos

```
sudo apt install default-jre
sudo apt install default-jdk
```
## Compilacion

En la maquina del servidor, se debe compilar el archivo servidor.java usando el siguiente comando:

Hay que tomar en cuenta que es necesario usa la libreria json-simple-1.1.1.jar incluida, por lo que se debe pasar su ubicacion como argumento

```
javac -cp '% UBICACION DEL PROYECTO %/uees-sd-2020-tarea06/json-simple-1.1.1.jar' servidor.java -Xlint:unchecked
```
Por ejemplo: 
```
javac -cp '.:/home/carmigniani-sandoval-nodo1/Desktop/uees-sd-2020-tarea06/json-simple-1.1.1.jar' servidor.java -Xlint:unchecked
```

Para el colector, se compila el archivo colector.java usando este comando, igualmente se usa el argumento de la ubicacion de la libreria

```
javac -cp '% UBICACION DEL PROYECTO %/uees-sd-2020-tarea06/json-simple-1.1.1.jar' colector.java -Xlint:unchecked
```

De ser necesario, el verificador.java debe ser modificado ingresando el puerto y la ip del reportero que recibira los mensajes

## Ejecucion

Una vez compilados los archivos java, primero se inicia la maquina del servidor usando el comando
```
java -cp '% UBICACION DEL PROYECTO %/uees-sd-2020-tarea06/json-simple-1.1.1.jar' servidor
```

Ahora se ejecuta el programa del lado del colector, usando el comando
```
java -cp '% UBICACION DEL PROYECTO %/uees-sd-2020-tarea06/json-simple-1.1.1.jar' colector
```

Automaticamente el colector generara lecturas de sensores y enviara las lecturas al servidor para que las procese, el servidor confirmara que las lecturas no esten duplicadas para guardarlas en un registro de cada sensor, y en un registro de transacciones, y enviara un mensaje de respuesta al colector

Ademas el colector tendra un tiempo de timeout para recibir el mensaje de confirmacion, en el cual si se exece, volvera a enviar el mismo mensaje hasta que se guarde. Es posible tambien simular situaciones de atrasos de paquetes y perdidas de paquetes con funcionalidades agregadas al codigo.
## Estructura de carpetas

### Registros
Aqui se guardan los reportes generador por el reportador en formato JSON, el el archivo de transacciones.json que tendra un listado de todas las transacciones generadas
