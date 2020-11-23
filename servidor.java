// Sistermas Distribuidos 
// Giuseppe Carmigniani
// Proyecto - deber 06

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;

import java.io.*;
import java.nio.file.Files;
import java.time.Instant;
import java.nio.file.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class servidor {

    // Se simula una demora en la confirmacion con el minimo y maximo en segundos
    public static int confirmationDelayMin = 1;
    public static int confirmationDelayMax = 5;

    // Se simula una probabilidad de perdida del paquete de la confirmacion
    public static double confirmationPacketLossChance = 0.05;

    public static void main(String[] args) {

        System.out.print("Iniciando servidor");

        try {

            // Se espera a recibir el paquete del colector
            listenDatagramSocket();

        } catch (Exception e) {

            System.out.println("Hubo un error al esperar el mensaje UDP");

        }

    }

    public static void listenDatagramSocket() throws IOException {
        // Se espera el paquete en el puerto 1234
        DatagramSocket ds = new DatagramSocket(1234);
        byte[] receive = new byte[65535];

        DatagramPacket DpReceive = null;
        while (true) {

            // se crea un datagram packet para recibir la informacion
            DpReceive = new DatagramPacket(receive, receive.length);

            ds.receive(DpReceive);

            System.out.println("Recibido:-" + data(receive));

            // Una vez recibido el mensaje de tipo JSON, se hace el decoding para procesarlo
            readSensorData(data(receive).toString());

            // Se limpia el buffer
            receive = new byte[65535];
        }
    }

    // esta funcion convierte los bytes recibidos en un String
    public static StringBuilder data(byte[] a) {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0) {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }

    // Esta funcion toma el String recibido y lo convierte a un objeto JSON
    public static void readSensorData(String encodedJSON) {

        // Se crea un parser para crear el objecto JSONObject
        JSONParser parser = new JSONParser();

        JSONObject sensorData;
        String sensorId;
        long sensorReading;
        String time;

        try {

            sensorData = (JSONObject) parser.parse(encodedJSON);
            sensorId = sensorData.get("sensor").toString();
            sensorReading = (long) sensorData.get("lectura");
            time = sensorData.get("fechahoraUTC").toString();

            // Con los datos ordenados se actualiza los logs de los sensores
            updateSensorLog(sensorId, (int) sensorReading, time);

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public static boolean updateSensorLog(String sensorId, int sensorReading, String time) {

        JSONObject sensorLog;

        // se le agrega la extension .json al nombre del sensor para buscar si existe el
        // archivo de las lecturas de tal sensor
        String filename = sensorId + ".json";

        // se busca si el archivo de registro del sensor ya existe
        File f = new File("./registros/" + filename);
        if (f.exists() && !f.isDirectory()) {
            System.out.println("Se econtro el registro del sensor: " + sensorId);
            System.out.println("Ingresando el nuevo valor de lectura: " + sensorReading);

            sensorLog = readJSON("./registros/" + filename);

            // Para agregar la nueva lectura, es necesario tomar las
            // lecturas como un JSONArray, a la cual se le agrega un nuevo JSONObject con la
            // nueva lectura
            JSONArray sensorLogArray = (JSONArray) sensorLog.get("lecturas");

            if (sensorLogArray.toString().contains(String.valueOf(time))) {

                System.out.println("Ya existe un registro en el sensor: " + sensorId + " , con el tiempo: " + time);
                try {
                    sendConfirmationSocket("Lectura duplicada");
                    saveTransaction("Lectura duplicada", sensorId, sensorReading, time);
                } catch (Exception e) {
                    System.out.print("Hubo un error al enviar el mensaje de respuesta.");
                }
                return false;

            } else {

                JSONObject readingObj = new JSONObject();
                readingObj.put("fechahoraUTC", time);
                readingObj.put("lectura", sensorReading);

                sensorLogArray.add(readingObj);

                sensorLog.put("lecturas", sensorLogArray);

            }

        } else {

            // En caso de que el archivo de registro no existe, se creara el archivo
            // pertinente

            System.out.println("No existe el registro del sensor: " + sensorId);

            // Primero se crea el archivo JSON, y se agregan los datos pertinentes al objeto

            sensorLog = new JSONObject();
            sensorLog.put("sensor", sensorId);

            // Se crea un JSONArray para las lecturas y se agrega la lectura enviada

            JSONArray sensorLogArray = new JSONArray();

            JSONObject lectureObj = new JSONObject();
            lectureObj.put("fechahoraUTC", time);
            lectureObj.put("lectura", sensorReading);

            sensorLogArray.add(lectureObj);
            sensorLog.put("lecturas", sensorLogArray);

        }

        // Una vez creado o modificado el archivo de registro, se utiliza Filewriter
        // para guardarlo en el disco, utilizando el filename que equivale al id del
        // sensor

        try (FileWriter file = new FileWriter("registros/" + filename)) {

            file.write(sensorLog.toJSONString());
            file.flush();

            // Cuando se guarda el archivo, se muestra por consola una confirmacion, ademas
            // de los datos pertinentes como el sensor, la nueva lectura y el tiempo de la
            // lectura

            System.out.println("--- Se ha actualizado el registro exitosamente");
            System.out.println(
                    "Cambios: Sensor: " + sensorId + ", Nueva lectura: " + sensorReading + ", Tiempo: " + time);

            // Una vez guardado el mensaje, se envia el mensaje de respuesta al colector
            sendConfirmationSocket("Lectura guardada");

            // Tambien se guarda la transaccion en un log de transacciones.json
            saveTransaction("Lectura guardada", sensorId, sensorReading, time);
            return true;

        } catch (IOException e) {

            e.printStackTrace();
            return false;

        }
    }

    public static void sendConfirmationSocket(String message) throws IOException {

        // se utiliza el datagram socket para enviar la respuesta
        DatagramSocket ds = new DatagramSocket();

        InetAddress ip = InetAddress.getLocalHost();
        byte buf[] = null;

        buf = message.getBytes();

        DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, 1235);

        // se genera un tiempo de espera al azar entre el minimo y maximo establecido
        int waitTime = (int) (confirmationDelayMin + Math.random() * (confirmationDelayMax - confirmationDelayMin));

        try {
            TimeUnit.SECONDS.sleep(waitTime);
        } catch (Exception ex) {
            System.out.println(ex);
        }

        try {
            // Se simula la probabilidad de la perdida del paquete de la respuesta
            if (Math.random() > confirmationPacketLossChance) {
                ds.send(DpSend);
            }
        } catch (Exception e) {
            System.out.println("Hubo un error al enviar el mensaje UDP");
        }

        ds.send(DpSend);

    }

    // Esta funcion recibe la ubicacion del JSON y retorna un JSONObject
    public static JSONObject readJSON(String url) {

        // JSON parser para crear el JSON a partir del archivo leido
        JSONParser jsonParser = new JSONParser();

        try (FileReader reader = new FileReader(url)) {
            // Se lee el archivo JSON
            Object obj = jsonParser.parse(reader);

            // se crea un JSONObject con el resultado del parser
            JSONObject response = (JSONObject) obj;

            // Se retorna el JSONObject
            return response;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        JSONObject response = new JSONObject();
        return response;

    }

    // Esta funcion guarda todas las transaciones del servidor
    public static void saveTransaction(String message, String sensorId, int sensorReading, String time) {

        JSONObject transactionLog;

        // Se le el archivo de transacciones.json
        String transactionMessage = message;
        transactionLog = readJSON("./registros/transacciones.json");

        JSONArray transactionLogArray = (JSONArray) transactionLog.get("transacciones");

        JSONObject transactionObj = new JSONObject();
        transactionObj.put("sensor", sensorId);
        transactionObj.put("lectura", sensorReading);
        transactionObj.put("fechahoraUTC", time);
        transactionObj.put("mensaje", transactionMessage);

        transactionLogArray.add(transactionObj);
        transactionLog.put("transacciones", transactionLogArray);

        try (FileWriter file = new FileWriter("registros/transacciones.json")) {

            file.write(transactionLog.toJSONString());
            file.flush();

            // Cuando se guarda el archivo, se muestra por consola una confirmacion, ademas
            // de los datos pertinentes como el sensor, la nueva lectura y el tiempo de la
            // lectura

            System.out.println("--- Se ha actualizado el registro de transacciones exitosamente");
            System.out.println("Sensor: " + sensorId + " , Lectura: " + sensorReading + " , Tiempo: " + time
                    + " , Transaccion: " + transactionMessage);

        } catch (IOException e) {

            e.printStackTrace();

        }
    }
}
