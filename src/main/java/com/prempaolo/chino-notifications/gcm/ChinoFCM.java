package com.prempaolo.simplenotification.gcm;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;
import java.util.HashMap;

import io.chino.java.ChinoAPI;
import io.chino.api.common.ChinoApiException;
import io.chino.api.document.Document;
import io.chino.api.user.User;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChinoFCM {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static ChinoFCM instance = null;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private String SERVER_KEY_DOCUMENT_ID = "";
    private String message;
    private ChinoAPI chino;
    private Document documentShared;
    private NotificationCompat.Builder notificationBuilder;

    private ChinoFCM() {

    }

    public void init(ChinoAPI chino, Activity activity, String serverKeyDocumentId){
        getInstance();
        setServerKeyDocumentId(serverKeyDocumentId);
        instance.chino = chino;
        instance.documentShared = null;
        instance.notificationBuilder = null;
        instance.message = null;
        instance.checkPlayServices(activity);
        String token = FirebaseInstanceId.getInstance().getToken();
        new UpdateUser().execute(token);
    }

    private String getServerKeyDocumentId(){
        getInstance();
        return instance.SERVER_KEY_DOCUMENT_ID;
    }

    private void setServerKeyDocumentId(String serverKeyDocumentId){
        getInstance();
        instance.SERVER_KEY_DOCUMENT_ID = serverKeyDocumentId;
    }

    public static ChinoFCM getInstance(){
        if(instance==null){
            instance = new ChinoFCM();
        }
        return instance;
    }

    public void setCustomMessage(String message){
        getInstance();
        instance.message = message;
    }

    public void setCustomNotification(NotificationCompat.Builder notificationBuilder){
        getInstance();
        instance.notificationBuilder = notificationBuilder;
    }

    NotificationCompat.Builder getNotificationBuilder(){
        getInstance();
        return instance.notificationBuilder;
    }

    void setDocumentShared(Document d){
        getInstance();
        instance.documentShared = d;
    }

    public Document getDocumentShared(){
        getInstance();
        return instance.documentShared;
    }

    public ChinoAPI getChino(){
        getInstance();
        return instance.chino;
    }

    public void shareDocument(String documentId, String userId){
        new MakeCall().execute(documentId, userId);
    }


    private String fromUserIDToToken(String userId) throws IOException, ChinoApiException {
        String token = null;
        User u = chino.users.read(userId);
        if (u.getAttributesAsHashMap().get("tokenFCM") != null) {
            token = u.getAttributesAsHashMap().get("tokenFCM").toString();
        }
        if(token==null){
            throw new ChinoApiException("Token not found!");
        } else {
            return token;
        }
    }

    private boolean checkPlayServices(Activity activity) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                activity.finish();
            }
            return false;
        }
        return true;
    }

    private class MakeCall extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            String token = null;
            try {
                token = fromUserIDToToken(params[1]);
            } catch (ChinoApiException | IOException e) {
                e.printStackTrace();
            }
            getInstance();
            if(instance.message==null){
                instance.message="You have a new message!";
            }
            String json = "{ \"data\": {" +
                    "    \"message\": \""+message+"\"," +
                    "\"DOCUMENT_ID\": " + "\"" + params[0] + "\"}," +
                    "  \"to\" : \"" + token + "\"" +
                    "}";
            String server_key = null;
            try {
                server_key = (String)chino.documents.read(SERVER_KEY_DOCUMENT_ID).getContentAsHashMap().get("server-key");
            } catch (ChinoApiException| IOException e) {
                e.printStackTrace();
            }
            if(server_key==null){
                //TODO: gestire errore
                return null;
            }
            RequestBody body = RequestBody.create(JSON, json);
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "key=" + server_key)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                response.body().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class UpdateUser extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                User u = chino.auth.checkUserStatus();
                HashMap<String, Object> attributes = u.getAttributesAsHashMap();
                if(attributes.get("tokenFCM")==null | attributes.get("tokenFCM").equals(""))
                    attributes.put("tokenFCM", params[0]);
                else {
                    attributes.remove("tokenFCM");
                    attributes.put("tokenFCM", params[0]);
                }
                chino.users.update(u.getUserId(), attributes);
            } catch (ChinoApiException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
