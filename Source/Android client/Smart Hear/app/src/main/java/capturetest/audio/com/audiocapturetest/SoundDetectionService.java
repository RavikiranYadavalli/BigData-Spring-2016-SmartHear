package capturetest.audio.com.audiocapturetest;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.widget.Toast;

import java.util.concurrent.ScheduledExecutorService;

public class SoundDetectionService extends Service {
    double amplitudeCutoff = 1000;
    ScheduledExecutorService scheduleTaskExecutor;
    MediaRecorder mRecorder;
    boolean mStartRecording;
    public SoundDetectionService() {
        mStartRecording = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
       return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Toast.makeText(this, "Listening for sound", Toast.LENGTH_LONG).show();

//        scheduleTaskExecutor = Executors.newSingleThreadScheduledExecutor();
//        // This schedule a runnable task every 3 seconds
//        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
//            public void run() {
//               mStartRecording = true;
//                detectSound();
//            }
//        }, 0, 5, TimeUnit.SECONDS);
        detectSound();
        return Service.START_NOT_STICKY;

    }
    public double detectSound()
    {

        double amplitude =0;

            if(mRecorder==null) {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            }
           do {
                try {
                   mRecorder.setOutputFile("/dev/null");
                    mRecorder.prepare();
                    mRecorder.getMaxAmplitude();
                    mRecorder.start();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                }
               amplitude = mRecorder.getMaxAmplitude();
            } while(mStartRecording && amplitude<amplitudeCutoff) ;

            if(amplitude>=amplitudeCutoff)
            {
                stopRecording();
             //   scheduleTaskExecutor.shutdownNow();

                launchApplication();
            }


            return amplitude;


    }
    public void stopRecording()
    {

        if (mRecorder != null) {
            mStartRecording=false;
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;

        }
    }
    public void launchApplication()
    {
        Intent dialogIntent = new Intent(this, MainActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);

    }
      @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Analyzing sound", Toast.LENGTH_LONG).show();
    }


}
