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
import java.net.SocketException;
import java.net.InetAddress;
import java.util.Scanner;

public class colector {

    // Estos son los posibles id's de los sensores
    public static String[] sensorIds = { "sid001", "sid002", "sid003", "sid004", "sid005" };

    // El tiempo en segundos que esperara el colector para recivir la confirmacion
    // antes de hacer un timeout
    public static int confirmationTimeout = 5;

    // La probabiliad de que se simule una perdida de paqueta
    public static double datagramPacketLossChance = 0.05;

    // Determina si se simula el envio de paquetes duplicados
    public static boolean datagramSendDuplicate = false;

    // la variable tendra los datos del sensor a enviar
    public static String sensorDataJSON;

    public static void main(String[] args) {

        // Al iniciar el programa se generan los valores para uno de los sensores
        generateSensorDataJSON();

        try {
            // Se envian estos valores por medio de un Socker UDP al servidor
            sendDatagramSocket();
        } catch (Exception e) {
            System.out.println("Hubo un error al escuchar el mensaje UDP");
        }

    }

    public static void generateSensorDataJSON() {

        // Esta funcion genera valores al azar de los sensores

        JSONObject jsonSensorData = new JSONObject();

        String time;
        String sensorId;
        int sensorValue;

        // Se escoje al azar entre uno de los IDs de los sensores
        sensorId = sensorIds[(int) (Math.random() * 5)];

        // Se genera una lectura al azar entre el 0 y el 99
        sensorValue = (int) (0 + Math.random() * 100);

        // Se guarda el tiempo en que fue generado los datos
        time = Instant.now().toString();

        jsonSensorData.put("sensor", sensorId);
        jsonSensorData.put("fechahoraUTC", time);
        jsonSensorData.put("lectura", sensorValue);

        // Esta informacion se la guarda en un objeto de tipo JSON
        sensorDataJSON = jsonSensorData.toJSONString();

    }

    public static void sendDatagramSocket() throws IOException {

        // Esta funcion envia la lectura de sensor por medio de UDP usando el datagram
        // socket

        DatagramSocket ds = new DatagramSocket();

        // Aqui va la direccion IP del servidor
        InetAddress ip = InetAddress.getLocalHost();

        // Esta variable tendra la secuencia de bytes del mensaje
        byte buf[] = null;

        // Tomamos el JSON de los datos y lo tranformamos a bytes
        buf = sensorDataJSON.getBytes();

        // creamos el paquete a la IP en el puerto 1234
        DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, 1234);

        // Simulamos la probabilidad de perder el paquete, en tal caso el paquete no se
        // envia
        if (Math.random() > datagramPacketLossChance) {
            System.out.println("Enviando paquete al Servidor.");
            ds.send(DpSend);
        } else {

            System.out.println("simulando paquete perdido");
        }

        // Si la opcion de enviar paquetes duplicados esta active, se envia el mismo
        // paquete dos veces
        if (datagramSendDuplicate) {
            ds.send(DpSend);
        }

        // Una vez enviado, llama a escuchar el mesaje de respuesta
        listenConfirmationSocket();

    }

    public static void listenConfirmationSocket() throws IOException {

        // Espera el mensaje de respuesta en el puerto 1235
        DatagramSocket ds = new DatagramSocket(1235);
        byte[] receive = new byte[65535];

        DatagramPacket DpReceive = null;
        // Pone un timeout para la respuesta
        ds.setSoTimeout(confirmationTimeout * 1000);

        while (true) {

            try {

                // Se crea un datagram packet para recibir la informacion
                DpReceive = new DatagramPacket(receive, receive.length);

                // Se recibe la informacion y se muestra el mensaje
                ds.receive(DpReceive);

                System.out.println("Mensaje recibido:-" + data(receive));

                // Se espera 5 segundos antes de volver a enviar otro mensaje
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (Exception ex) {
                    System.out.println(ex);
                }

                try {

                    // Si el mensaje fue recibido, manda a generar unos nuevos valores de lectura de
                    // sensor para enviarlos
                    ds.close();
                    generateSensorDataJSON();
                    sendDatagramSocket();
                } catch (Exception e) {
                    System.out.println("Hubo un error al escuchar el mensaje UDP");
                    System.out.println(e);
                }

                // Se limpia el buffer
                receive = new byte[65535];

            } catch (SocketTimeoutException e) {
                // En caso de ocurrir el timeout, se deja de esperar por el mensaje
                System.out.println("Se llego al timeout " + e);
                ds.close();

                // Se espera 5 segundos despues del timeout para volver a enviar el mismo
                // mensaje
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (Exception ex) {
                    System.out.println(ex);
                }
                // No se llama a generar nuevos datos porque se debe enviar el mismo mensaje
                // hasta recibir el mensaje de confirmacion
                sendDatagramSocket();

            }
        }
    }

    public static StringBuilder data(byte[] a) {
        // Esta funcion convierte los bytes recibidos a un String
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

}
