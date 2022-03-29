package demo;

import com.google.api.gax.rpc.ApiException;
import java.io.IOException;
import tfg.IntentDetector;

public final class DialogFlowDemo {
    public static void main(final String[] args) {
        String message = "Greetings!";
        String response;
        try {
            response = IntentDetector.simpleDetect(message);
          } catch (ApiException e) {
            response = "API Exception!";
            e.printStackTrace();
          } catch (IOException e) {
            response = "IO Exception!";
            e.printStackTrace();
          }
        System.out.println(response);
    }
    
}
