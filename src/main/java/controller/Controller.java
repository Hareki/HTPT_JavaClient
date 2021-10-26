/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import model.FileInfo;
import model.Message;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

public class Controller {

    private static final ObjectMapper MAPPER;
    private static String API_URL;

    static {
        // API_URL = "http://localhost:8080/HTPTWS/webapi/files/";

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][.SS][.S]")
        ));
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(javaTimeModule);
    }

    public void updateURL(String IP) {
        API_URL = "http://" + IP + ":8080/HTPTWS/webapi/files/";
    }

    public Controller() {
        super();
    }

    public List<FileInfo> getAllFileInfos(){
        // Client client = ClientBuilder.newClient();
        ClientBuilder builder = ClientBuilder.newBuilder()
                .connectTimeout(4000, TimeUnit.MILLISECONDS)
                .readTimeout(4000, TimeUnit.MILLISECONDS);
        Client client = builder.build();

        WebTarget target = client.target(API_URL + "list");

        String listFileInfoJson = target.request(MediaType.APPLICATION_JSON).get(String.class);
        List<FileInfo> fileInfos = null;

        try {
            fileInfos = MAPPER.readValue(listFileInfoJson, new TypeReference<List<FileInfo>>() {
            });
        } catch (JsonProcessingException ex) {
            Logger.getLogger(Controller.class.getName()).log(Level.SEVERE, null, ex);
        }

        return fileInfos;
    }

    public Message uploadFile(String filePath) throws IOException {
        File fileTest = new File(filePath);
        Message message = new Message();
        FileDataBodyPart filePart = new FileDataBodyPart("uploadFile", fileTest);
        try (FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
                MultiPart multiPart = formDataMultiPart.bodyPart(filePart)) {
            Client client = ClientBuilder
                    .newBuilder()
                    .register(MultiPartFeature.class).build();
            WebTarget target = client.target(API_URL + "upload");

            Response response = target
                    .request(MediaType.APPLICATION_JSON_TYPE) // kiểu dữ liệu trả về
                    .post(Entity.entity(multiPart, multiPart.getMediaType())); // gửi yêu cầu tạo 

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                message.setStatus(Message.Status.SUCCESS);
            } else {
                message.setStatus(Message.Status.FAIL);
            }
            message.setMessage(response.readEntity(String.class));
        } catch (Exception e) {
            message.setStatus(Message.Status.FAIL);
            message.setMessage("Có lỗi xảy ra trong quá trình tải lên server!");
            System.err.println(">>> ERROR upload file!");
        }
        return message;
    }

    public Message downloadFile(int id, String filePath) {
        Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
        WebTarget target = client.target(API_URL + "download/" + id);
        Response response = target.request().get();
        Message message = new Message();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            InputStream downloadStream = response.readEntity(InputStream.class);
            if (storeFile(downloadStream, filePath) != null) {
                message.setStatus(Message.Status.SUCCESS);
                message.setMessage("Tải file thành công!");
            } else {
                message.setStatus(Message.Status.FAIL);
                message.setMessage("Tải file thất bại. Đường dẫn file có vấn đề!");
            }
        } else {
            message.setStatus(Message.Status.FAIL);
            message.setMessage(response.readEntity(String.class));
        }
        return message;
    }

    private File storeFile(InputStream downloadStream, String filePath) {
        File downloadFile = new File(Paths.get(filePath).toAbsolutePath().toString());
        try (OutputStream outputStream = new FileOutputStream(downloadFile)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = downloadStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (IOException e) {
            System.err.println(">>>>> Error when storing file: " + e.getMessage());
            downloadFile = null;
        }
        return downloadFile;
    }

    public Message deleteFile(int id) {
        Message message = new Message();
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(API_URL + "delete/" + id);
        Response response = target.request().delete();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            message.setStatus(Message.Status.SUCCESS);
        } else {
            message.setStatus(Message.Status.FAIL);
        }
        message.setMessage(response.readEntity(String.class));
        return message;
    }

}
