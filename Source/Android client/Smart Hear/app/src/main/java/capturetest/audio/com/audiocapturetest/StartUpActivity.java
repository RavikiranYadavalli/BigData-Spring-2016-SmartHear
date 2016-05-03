package capturetest.audio.com.audiocapturetest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StartUpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_start_up);

    }
    @Override
    protected void onStart()
    {
        super.onStart();

        startService(new Intent(this,SoundDetectionService.class));
    }

}
