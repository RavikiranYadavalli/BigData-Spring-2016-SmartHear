package com.smarthear.audiodetection;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.BassBoost;
import android.media.audiofx.NoiseSuppressor;
import android.media.audiofx.PresetReverb;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import smarthear.com.audiodetection.R;

import ddf.minim.effects.BandPass;
import ddf.minim.effects.HighPassSP;
import ddf.minim.effects.LowPassSP;
import jAudioFeatureExtractor.AudioFeatures.Compactness;
import jAudioFeatureExtractor.AudioFeatures.FeatureExtractor;
import jAudioFeatureExtractor.AudioFeatures.FractionOfLowEnergyWindows;
import jAudioFeatureExtractor.AudioFeatures.MagnitudeSpectrum;
import jAudioFeatureExtractor.AudioFeatures.PeakFinder;
import jAudioFeatureExtractor.AudioFeatures.PowerSpectrum;
import jAudioFeatureExtractor.AudioFeatures.RMS;
import jAudioFeatureExtractor.AudioFeatures.SpectralRolloffPoint;
import jAudioFeatureExtractor.AudioFeatures.ZeroCrossings;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    short[] audioData;
    public AudioTrack m_track;
    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    Intent serviceIntent;
    public float[] floatData;
    int effectFlag = 0;
    private SeekBar seekBarForLowPass;
    private SeekBar seekBarForHighPass;
    private SeekBar seekBarForBandPass;
    private Spinner spinnerEffects;
    public int lowFrequencyValue = 0;
    public int highFrequencyValue = 0;
    public int passFilterFlag = 1;
    public int bandPassFrequencyValue = 0;
    public String sampleFeatureVector ;
    String contextInfo;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setButtonHandlers();
        enableButtons(false);
        int maxFrequencyLimit = 15000;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        stopService();
        spinnerEffects = (Spinner) findViewById(R.id.spinner_Effects);
        // Spinner Drop down elements
        List<String> categories = new ArrayList();
        categories.add("Home");

        categories.add("Class room");

        categories.add("Bass boost");

        categories.add("Outdoor");

        categories.add("Office");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEffects.setOnItemSelectedListener(this);
        // attaching data adapter to spinner
        spinnerEffects.setAdapter(dataAdapter);

        bufferSize = AudioRecord.getMinBufferSize
                (RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        audioData = new short[bufferSize]; //short array that pcm data is put into.
        seekBarForHighPass = (SeekBar) findViewById(R.id.seekBar_highPass);
        seekBarForHighPass.setMax(maxFrequencyLimit);
        seekBarForHighPass.setOnSeekBarChangeListener(this.seekBarEventLister);
        seekBarForBandPass = (SeekBar) findViewById(R.id.seekBar_bandPass);
        seekBarForBandPass.setOnSeekBarChangeListener(this.seekBarEventLister);
        seekBarForBandPass.setMax(maxFrequencyLimit);
        seekBarForLowPass = (SeekBar) findViewById(R.id.seekbar_lowPass);
        seekBarForLowPass.setOnSeekBarChangeListener(this.seekBarEventLister);
        seekBarForLowPass.setMax(maxFrequencyLimit);


    }
//To change the filter pass being selected based on the seekbar value changed on the UI.
    public SeekBar.OnSeekBarChangeListener seekBarEventLister = new SeekBar.OnSeekBarChangeListener() {

        int frequency = 0;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
            frequency = progresValue;
            switch (seekBar.getId()) {
                case R.id.seekbar_lowPass:
                    lowFrequencyValue = progresValue;
                    Log.d("Low freq value: ", Long.toString(lowFrequencyValue));
                    passFilterFlag = 1;
                    break;
                case R.id.seekBar_highPass:
                    highFrequencyValue = progresValue;
                    Log.d("High freq value: ", Long.toString(highFrequencyValue));
                    passFilterFlag = 2;
                    break;
                case R.id.seekBar_bandPass:
                    bandPassFrequencyValue = progresValue;
                    passFilterFlag = 3;
                    Log.d("Band pass freq value: ", Long.toString(bandPassFrequencyValue));
                    break;
                default:
                    lowFrequencyValue = progresValue;
                    passFilterFlag = 1;
                    break;

            }


        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private void setButtonHandlers() {
        findViewById(R.id.btn_Start).setOnClickListener(btnClick);
        findViewById(R.id.btn_Stop).setOnClickListener(btnClick);
    }


    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btn_Start, !isRecording);
        enableButton(R.id.btn_Stop, isRecording);
    }


    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);

        final int[] cntr = {0};
        recorder.startRecording();
        final double[][] featureSamples = new double[7][];
        for (int i = 0; i < featureSamples.length; i++) {
            featureSamples[i] = new double[1000];
        }

        isRecording = true;
//The thread responsible for recording the audio through the Audio recorder. Applies the noise suppresor if supported by the device.
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {

                m_track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT,
                        bufferSize, AudioTrack.MODE_STREAM);
                PresetReverb pReverb = new PresetReverb(1, 0);
                pReverb.setPreset(PresetReverb.PRESET_SMALLROOM);
                pReverb.setEnabled(true);
                m_track.attachAuxEffect(pReverb.getId());
                m_track.setAuxEffectSendLevel(1.0f);

                NoiseSuppressor.create(m_track.getAudioSessionId());
                m_track.setPlaybackRate(RECORDER_SAMPLERATE);
                if (effectFlag != 0) {
                    applyEffect(effectFlag);
                }
                final int[] read = {0};
                short shortData[] = new short[bufferSize];
                m_track.play();
                int index = 0;
                while (isRecording) {
                    read[0] = recorder.read(shortData,0,bufferSize);
                    cntr[0]++;
                    double da[][] = new double[1][shortData.length];
                    //da[0] = calculateFFT(data);
                    for (int j = 0; j < shortData.length; j++) {
                        da[0][j] = (double) shortData[j]/2048 ;
                    }
                    try {
                        //This snippet of code is responsible for the extraction of features from audio file. Jaudio is the library used.
                        FeatureExtractor featureExtractor;

                        featureExtractor = new ZeroCrossings();
                        Log.d("Sample rate : ", String.valueOf(recorder.getSampleRate() ));
                        double sampleRateInDouble = recorder.getSampleRate();
                        double[][] otherFeatures = new double[3][3];
                        double[] zerocrossings;
                        featureExtractor.setWindow(5);
                        zerocrossings = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        featureSamples[0][index] = zerocrossings[0];
                        featureExtractor = new MagnitudeSpectrum();
                        double[] magnitudeSpectrum;
                        magnitudeSpectrum = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        otherFeatures[0] = magnitudeSpectrum;
                        double[] mfcc;
                        mfcc = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanOfMfcc = calculateMean(mfcc);
                        featureSamples[1][index] = meanOfMfcc + 100;
                        double[] lowEnergyWindows;
                        featureExtractor = new FractionOfLowEnergyWindows();
                        lowEnergyWindows = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanLowEnergyWindows = calculateMean(lowEnergyWindows);
                        featureSamples[3][index] = meanLowEnergyWindows;
                        featureExtractor = new PeakFinder();
                        otherFeatures[0] = magnitudeSpectrum;
                        double[] peakFinder = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanPeak = calculateMean(peakFinder);
                        featureSamples[4][index] = meanPeak;
                        featureExtractor = new Compactness();
                        double[] compactness = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanCompact = calculateMean(compactness);
                        featureSamples[6][index] = meanCompact;
                        double[] rms;
                        featureExtractor = new RMS();
                        otherFeatures[0] = null;
                        rms = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanRms = calculateMean(rms);
                        featureSamples[5][index] = meanRms;
                        featureExtractor = new PowerSpectrum();
                        double[] powerSpectrum;
                        powerSpectrum = featureExtractor.extractFeature(da[0],sampleRateInDouble,otherFeatures);
                        otherFeatures[0] = powerSpectrum;
                        featureExtractor = new SpectralRolloffPoint();
                        double[] spectralRollOfPoint = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanSpectralRollOff = calculateMean(spectralRollOfPoint);
                        featureSamples[2][index] = meanSpectralRollOff;
                        index++;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    //This code snippet applies the pass filters on the audio track to process the audio signal through the filters.
                    if (read[0] > 0) {

                        floatData = shortToFloat(shortData);
                        if (passFilterFlag != 0 && passFilterFlag == 2) {
                            HighPassSP hp = new HighPassSP(highFrequencyValue, RECORDER_SAMPLERATE);

                            hp.process(floatData);
                        } else if (passFilterFlag != 0 && passFilterFlag == 1) {
                            LowPassSP lp = new LowPassSP(lowFrequencyValue, RECORDER_SAMPLERATE / 2);
                            lp.process(floatData);
                        } else if (passFilterFlag != 0 && passFilterFlag == 3) {

                            float centerFreq = bandPassFrequencyValue / 2;

                            BandPass bandpass = new BandPass(centerFreq, 50, RECORDER_SAMPLERATE);
                            bandpass.setBandWidth(bandPassFrequencyValue / 2);
                            bandpass.setFreq(bandPassFrequencyValue);
                            bandpass.process(floatData);
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            m_track.write(floatData, 0, bufferSize, AudioTrack.WRITE_NON_BLOCKING);

                        }
                    }
                }

            }
        }, "AudioRecorder Thread");

        recordingThread.start();
        //This code creates a thread that stops the recording at 4 seconds(4000 milliseconds), extracts the features and sends to analysis.
        Handler stopper = new Handler();
        stopper.postDelayed(new Runnable() {
            @Override
            public void run() {
                enableButtons(false);
                stopRecording();
                Log.d("Counter value : ", String.valueOf(cntr[0]));
                Log.d("Stopping thread", "After 10 secs");
                calculateMeanOfSamples(featureSamples);
                analyzeFeatures(sampleFeatureVector);
            }
        }, 4000);



    }
    //This code calculates the mean of samples of features extracted.
    public void calculateMeanOfSamples(double[][] featureSamples) {
        double selectedSamples[] = new double[7];
        sampleFeatureVector ="";
        for (int i = 0; i < featureSamples.length; i++) {

            selectedSamples[i] = calculateMean(featureSamples[i]);
            sampleFeatureVector += selectedSamples[i] + ";";

        }
        Log.d("Zero crossing rate: ", String.valueOf(selectedSamples[0]));
        Log.d("MFCC : ", String.valueOf(selectedSamples[1]));
        Log.d("Spectral rolloff : ", String.valueOf(selectedSamples[2]));
        Log.d("Low energy windows : ", String.valueOf(selectedSamples[3]));
        Log.d("Mean Peak : ", String.valueOf(selectedSamples[4]));
        Log.d("RMS : ", String.valueOf(selectedSamples[5]));
        Log.d("Compactness : ", String.valueOf(selectedSamples[6]));


    }
    //This code sends the extracted features to the spark server through the socket connection.
    public void analyzeFeatures(final String featureVector) {
        if (!featureVector.isEmpty()) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final SocketConnection serverSocket = new SocketConnection();
                    Log.d("Feature vector :", featureVector);
                    Log.d("Context info : ", contextInfo);
                    serverSocket.setData(contextInfo +"_"+ featureVector);
                    String result = serverSocket.run();
                    if(!result.isEmpty())
                    {
                        showNotificationToUser(result);
                    }
                }
            });
        }
    }
    //This code creates a notification based on the result received after the server analysis of the sound.
    public void showNotificationToUser(String predictedClass)
    {
        NotificationCompat.Builder soundNotification = new NotificationCompat.Builder(this);
        soundNotification.setSmallIcon(R.drawable.notification_icon_2);
        soundNotification.setDefaults(Notification.DEFAULT_VIBRATE);
        soundNotification.setContentTitle("Sound alert");
        String textToBeShown = getTextBasedOnClass(predictedClass);
        soundNotification.setContentText(textToBeShown);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(123, soundNotification.build());


    }

    public String getTextBasedOnClass(String predictedClass)
    { String textMessage ="";
        if(!predictedClass.isEmpty())
        {
            switch(predictedClass.toLowerCase())
            {
                case "siren" : textMessage ="A siren seems to set off. Get to the nearest safety point";
                    break;
                case "doorbell": textMessage = "Looks like someone is ringing the door bell";
                    break;
                case "doorknock": textMessage = "Someone is knocking the door";
                    break;
                case "dogbark":textMessage ="Is that the dog thats barking?";
                    break;
                case "traffic": textMessage = "This seems to be a busy place";
                    break;
                case "train" : textMessage = "There is a train passing by";
                    break;
                case "ambulance": textMessage= "Please give way to the emergency service";
                    break;
                case "telephone": textMessage ="Tring tring... The phone is ringing";
                    break;
                case "man" : textMessage ="A person is talking in the class";
                    break;
                case "woman" : textMessage = "A lady is talking in the class";
                    break;
                case "group" : textMessage = "The class seems to be very noisy";
                    break;
                case "applause" : textMessage = "Join the applause";
                    break;
                case "deskbell":textMessage ="Someone needs your assistance";
                    break;
                case "fax" : textMessage = "Looks like you have received a fax";
                    break;
                case "officedoor" : textMessage ="Someone openned the door";
                    break;
                case "phone": textMessage ="You better pick up the phone";
                    break;
                case "printer" : textMessage ="Your print is ready to be collected";
                    break;
                case "vehicle" : textMessage ="A vehicle whizzed past you";
                    break;
                case "police" : textMessage ="A cop is around, hope its not for you.";
                    break;
                case "horn" : textMessage = "Someone seems to be honking you.";
                    break;



            }
        }
        return textMessage;
    }
    //This is to stop the sound detection service that starts the app and redirects to the main activity once a sound is detected.
    public void stopService() {
        stopService(new Intent(getBaseContext(), SoundDetectionService.class));
    }
    //This is to stop the recording of the audio.
    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

    }

    public double calculateMean(double[] arrayOfNumbers) {
        double sum = 0;
        for (int i = 0; i < arrayOfNumbers.length; i++) {
            sum += arrayOfNumbers[i];
        }
        return  sum / arrayOfNumbers.length;
    }


    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_Start: {
                    Log.d("Activity ", "Start Recording");
                    enableButtons(true);
                    startRecording();

                    break;
                }
                case R.id.btn_Stop: {
                    Log.d("Activity ", "Stop Recording");
                    enableButtons(false);
                    stopRecording();
                    break;

                }
            }
        }
    };
    //This code starts the sound detection service on the minimization of the app.
    @Override
    protected void onStop() {
        super.onStop();
        if(serviceIntent==null) {
            serviceIntent = new Intent(this, SoundDetectionService.class);
            startService(serviceIntent);
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private float[] shortToFloat(short[] audio) {
        Log.d("SHORTTOFLOAT", "INSIDE SHORTTOFLOAT");
        float[] converted = new float[audio.length];

        for (int i = 0; i < converted.length; i++) {
            converted[i] = audio[i] / 32768f; /* default range for Android PCM audio buffers) */

        }

        return converted;
    }


    //To apply the bass boost effect on the sound being recorded.
    public void bassBoostEffect() {
        if (m_track != null) {
            BassBoost bass = new BassBoost(0, m_track.getAudioSessionId());
            bass.setEnabled(true);
            BassBoost.Settings bassBoostSettingTemp = bass.getProperties();
            BassBoost.Settings bassBoostSetting = new BassBoost.Settings(bassBoostSettingTemp.toString());
            bass.setProperties(bassBoostSetting);
            bass.setStrength((short) 900);
            Log.d("Bass effect : ", "On");
        } else {
            effectFlag = 3;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ((TextView)parent.getChildAt(0)).setTextColor(Color.rgb(0,0, 0));
        String selectedOption = parent.getItemAtPosition(position).toString();
        int optionId = getSelectedEffectId(selectedOption);
        applyEffect(optionId);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    //This method applies the context information to the features being sent.
    public int getSelectedEffectId(String option) {
        switch (option.toLowerCase()) {
            case "class room":
                contextInfo = "class";
                return 1;
            case "outdoor":
                contextInfo="outdoor";
                return 2;
            case "bass boost":
                return 3;

            case "home":
                contextInfo="home";
                return 4;
            case "office":
                contextInfo="office";
                return 5;
            default:
                return 0;
        }
    }

    public void applyEffect(int option) {
        if (option != 0) {
            switch (option) {
                case 1:
                    applyContextEffects(1);
                    break;

                case 2:
                    applyContextEffects(2);
                    break;
                case 3:
                    bassBoostEffect();
                    break;
                case 4:
                    applyContextEffects(4);
                    break;
                case 5:
                    applyContextEffects(5);
                    break;
                default:
                    break;

            }
        }
    }
    //Based on the context the effect on the filters is being applied here.
    public void applyContextEffects(int id) {
        if (id != 0) {

            switch (id) {
                case 1:
                    seekBarForLowPass.setProgress(0);
                    seekBarForHighPass.setProgress(0);
                    seekBarForBandPass.setProgress(5000);

                    Log.d("Class room mode :", "On");
                    break;
                case 2:
                    seekBarForBandPass.setProgress(0);
                    seekBarForLowPass.setProgress(0);
                    seekBarForHighPass.setProgress(4000);
                    Log.d("Outdoor mode :", "On");
                    break;
                case 4:
                    seekBarForHighPass.setProgress(0);
                    seekBarForBandPass.setProgress(0);
                    seekBarForLowPass.setProgress(10000);
                    Log.d("Home mode :", "On");
                    break;
                case 5:
                    seekBarForHighPass.setProgress(0);
                    seekBarForBandPass.setProgress(0);
                    seekBarForLowPass.setProgress(7000);
                    Log.d("Office mode :", "On");
                    break;
            }
        }
    }

    //Socket for communcation between the android client and the spark server.
    private class SocketConnection {
        int count = 0;
        String message;
        Socket client;
        private Socket clientSocket;
        int cnt;
        String result;
        public void setData(String data) {
            message = "ANALYZE :";
            message += data;
            Log.d("Data sent : ",message);

        }

        public String run() {
            final String hostIp ="10.205.0.125";
            final int portNumber = 9999;

            try {
                client = new Socket(hostIp, portNumber);
                BufferedWriter writer;
                writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                writer.write(message);
                writer.newLine();
                writer.flush();
                writer.close();
                BufferedReader reader;
                String line;
                client = new Socket(hostIp, portNumber);

                reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                line = reader.readLine();
                result = line;
                Log.d("Server response :",line);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            return result;
        }

    }


}