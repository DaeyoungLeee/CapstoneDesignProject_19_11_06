package kr.co.aiotlab.capstonedesignproject.Service;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class FirebaseInstanceIDService extends FirebaseInstanceIdService {
    
    private static final String TAG = "MyFirebaseIDService";

    @Override
    public void onTokenRefresh() {
        
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.e(TAG, "onTokenRefresh: " + token);

        sendRegistrationToServer(token);

    }

    private void sendRegistrationToServer(String token) {

    }
}


