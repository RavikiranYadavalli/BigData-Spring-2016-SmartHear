package capturetest.audio.com.audiocapturetest;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
//    private static final int RECORDER_SAMPLERATE = 8000;
//    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
//    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_DEFAULT;
    private MediaRecorder recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    public String outputFile;
    // This file is used to record voice
    static final private double EMA_FILTER = 0.6;
    private double mEMA = 0.0;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setButtonHandlers();
        enableButtons(false);
        outputFile = Environment.getExternalStorageDirectory() + "/testRecord.3gp";
    }

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btn_Start)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btn_Stop)).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btn_Start, !isRecording);
        enableButton(R.id.btn_Stop, isRecording);
        enableButton(R.id.btn_Play, true);
    }

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private void startRecording() {

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(outputFile);
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        recorder.start();

    }
    public double getAmplitude() {
        if (recorder != null)
            return  (recorder.getMaxAmplitude()/2700.0);
        else
            return 0;

    }

    public double getAmplitudeEMA() {
        double amp = getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

//    //convert short to byte
//    private byte[] short2byte(short[] sData) {
//        int shortArrsize = sData.length;
//        byte[] bytes = new byte[shortArrsize * 2];
//        for (int i = 0; i < shortArrsize; i++) {
//            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
//            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
//            sData[i] = 0;
//        }
//        return bytes;
//
//    }

//    private void writeAudioDataToFile() {
//        // Write the output audio in byte
//
//        String filePath = "/sdcard/testRecord.3gp";
//        short sData[] = new short[BufferElements2Rec];
//
//        FileOutputStream os = null;
//        try {
//            os = new FileOutputStream(filePath);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        while (isRecording) {
//            // gets the voice output from microphone to byte format
//
//            recorder.read(sData, 0, BufferElements2Rec);
//            System.out.println("Short wirting to file" + sData.toString());
//            try {
//                // // writes the data to file from buffer
//                // // stores the voice buffer
//                byte bData[] = short2byte(sData);
//                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        try {
//            os.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_Start: {
                    enableButtons(true);
                    startRecording();
                    break;
                }
                case R.id.btn_Stop: {
                    enableButtons(false);
                    stopRecording();
                    break;
                }
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void playRecoderedAudio(View v) {
        MediaPlayer m = new MediaPlayer();

        try {
            m.setDataSource(outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            m.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        m.start();
        Toast.makeText(getApplicationContext(), "Playing audio", Toast.LENGTH_LONG).show();
    }


}
