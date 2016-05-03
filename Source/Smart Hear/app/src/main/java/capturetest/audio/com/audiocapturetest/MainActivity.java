package capturetest.audio.com.audiocapturetest;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
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
import android.os.Environment;
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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

    private static final int RECORDER_BPP = 8;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    short[] audioData;
    public AudioTrack m_track;
    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    Complex[] fftTempArray;
    Complex[] fftArray;
    int[] bufferData;
    int bytesRecorded;
    Intent serviceIntent;
    private double[] absNormalizedSignal;
    public int mPeakPos;
    public String savedFileName;
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
    private byte[]  buffer;
public String sampleFeatureVector ;
    private static final int TIMER_INTERVAL = 120;
    public int mPeriodInFrames;
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

            // Toast.makeText(getApplicationContext(), "Changing seekbar's progress", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Toast.makeText(getApplicationContext(), "Mode of filter :", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

            // Log.d("SeekBar value", String.valueOf(frequency));
            // Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();

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
        //enableButton(R.id.btn_Play, true);
    }

    private String getFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }
        savedFileName = file.getAbsolutePath() + "/" + System.currentTimeMillis()
                + AUDIO_RECORDER_FILE_EXT_WAV;
        return (savedFileName);
    }


    public void convert() {


    }

    public void calculate() {
        Complex[] fftTempArray = new Complex[bufferSize];
        for (int i = 0; i < bufferSize; i++) {
            fftTempArray[i] = new Complex(audioData[i], 0);
        }
        Complex[] fftArray = FFT.fft(fftTempArray);

        double[] micBufferData = new double[bufferSize];
        final int bytesPerSample = 2;
        final double amplification = 100.0;
        for (int index = 0, floatIndex = 0; index < bytesRecorded - bytesPerSample + 1; index += bytesPerSample, floatIndex++) {
            double sample = 0;
            for (int b = 0; b < bytesPerSample; b++) {
                int v = bufferData[index + b];
                if (b < bytesPerSample - 1 || bytesPerSample == 1) {
                    v &= 0xFF;
                }
                sample += v << (b * 8);
            }
            double sample32 = amplification * (sample / 32768.0);
            micBufferData[floatIndex] = sample32;
        }


    }


    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }


    private void startRecording() {
        ByteBuffer byteBuf = ByteBuffer.allocate(2 * bufferSize);
        mPeriodInFrames = RECORDER_SAMPLERATE * TIMER_INTERVAL / 1000;
        final byte data[] = new byte[bufferSize];
        recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);

        final int[] cntr = {0};
        recorder.startRecording();
        final double[][] featureSamples = new double[7][];
        for (int i = 0; i < featureSamples.length; i++) {
            featureSamples[i] = new double[1000];
        }

        isRecording = true;

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
                //  byte[] data = new byte[bufferSize];
                m_track.play();
                int index = 0;
                while (isRecording) {
                   //read[0] = recorder.read(data, 0, bufferSize);
                    read[0] = recorder.read(shortData,0,bufferSize);
                    cntr[0]++;
                  //  writeAudioDataToFile(data, read[0]);
                   // shortData = byteToShort(data);
                    double da[][] = new double[1][shortData.length];
                    //da[0] = calculateFFT(data);
                    for (int j = 0; j < shortData.length; j++) {
                        da[0][j] = (double) shortData[j]/2048 ;
                    }
                    try {
                        // AudioSamples a = new AudioSamples(da, recorder.getSampleRate(), "test", false);
                        FeatureExtractor featureExtractor;

                        featureExtractor = new ZeroCrossings();
                        //featureExtractor.setWindow(1);
                        Log.d("Sample rate : ", String.valueOf(recorder.getSampleRate() ));
                        double sampleRateInDouble = recorder.getSampleRate();
                        double[][] otherFeatures = new double[3][3];
                        double[] zerocrossings;
                        featureExtractor.setWindow(5);
                        zerocrossings = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        featureSamples[0][index] = zerocrossings[0];
                        featureExtractor = new MagnitudeSpectrum();
                      // featureExtractor.setWindow(5);
                        double[] magnitudeSpectrum;
                       // featureExtractor.setWindow(1600);
                        magnitudeSpectrum = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        otherFeatures[0] = magnitudeSpectrum;
                        double[] mfcc;
                        //featureExtractor = new MFCC();
                       //featureExtractor.setWindow(5);
                        mfcc = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanOfMfcc = calculateMean(mfcc);
                        featureSamples[1][index] = meanOfMfcc + 100;
                        double[] lowEnergyWindows;
                        featureExtractor = new FractionOfLowEnergyWindows();
                     //featureExtractor.setWindow(5);
                        lowEnergyWindows = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanLowEnergyWindows = calculateMean(lowEnergyWindows);
                        featureSamples[3][index] = meanLowEnergyWindows;
                        featureExtractor = new PeakFinder();
                     // featureExtractor.setWindow(5);
                        otherFeatures[0] = magnitudeSpectrum;
                        double[] peakFinder = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanPeak = calculateMean(peakFinder);
                        featureSamples[4][index] = meanPeak;
                        featureExtractor = new Compactness();
                       //featureExtractor.setWindow(5);
                        double[] compactness = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanCompact = calculateMean(compactness);
                        featureSamples[6][index] = meanCompact;
                        //featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double[] rms;
                        featureExtractor = new RMS();
                        //featureExtractor.setWindow(5);
                        otherFeatures[0] = null;
                        rms = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanRms = calculateMean(rms);
                        featureSamples[5][index] = meanRms;
                        featureExtractor = new PowerSpectrum();
                       // featureExtractor.setWindow(5);
                        double[] powerSpectrum;
                       // featureExtractor.setWindow(1600);
                        powerSpectrum = featureExtractor.extractFeature(da[0],sampleRateInDouble,otherFeatures);
                        otherFeatures[0] = powerSpectrum;
                        featureExtractor = new SpectralRolloffPoint();
                        //featureExtractor.setWindow(5);
                      //  featureExtractor.setWindow(1600);
                        double[] spectralRollOfPoint = featureExtractor.extractFeature(da[0], sampleRateInDouble, otherFeatures);
                        double meanSpectralRollOff = calculateMean(spectralRollOfPoint);
                        featureSamples[2][index] = meanSpectralRollOff;
                        index++;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    if (read[0] > 0) {

                        floatData = shortToFloat(shortData);
                        if (passFilterFlag != 0 && passFilterFlag == 2) {
                            HighPassSP hp = new HighPassSP(highFrequencyValue, RECORDER_SAMPLERATE);

                            hp.process(floatData);
                        } else if (passFilterFlag != 0 && passFilterFlag == 1) {
                            //  LowPassFS lp = new LowPassFS(lowFrequencyValue, 1);
                            LowPassSP lp = new LowPassSP(lowFrequencyValue, RECORDER_SAMPLERATE / 2);
                            lp.process(floatData);
                        } else if (passFilterFlag != 0 && passFilterFlag == 3) {

                            float centerFreq = bandPassFrequencyValue / 2;

                            BandPass bandpass = new BandPass(centerFreq, 50, RECORDER_SAMPLERATE);
                            //bandpass.setBandWidth(bandPassFrequencyValue);
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

    public void analyzeFeatures(final String featureVector) {
        if (!featureVector.isEmpty()) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //final SocketConnection serverSocket = new SocketConnection();
                    Log.d("Feature vector :", featureVector);

                   //serverSocket.setData(featureVector);//41.355;37.10349177400677;0.01740234375;0.2366666666666665;
                   // serverSocket.setData("41.355;4.026200965328397;0.01740234375;0.2366666666666665;7.995975753666285E-4;131.39006999868124;2273.4955445886208");
                  String result = "" ;//serverSocket.run();
//                    if(!result.isEmpty())
//                    {
//                        showNotificationToUser(result);
//                    }
                }
            });
        }
    }

    public void showNotificationToUser(String predictedClass)
    {
        NotificationCompat.Builder soundNotification = new NotificationCompat.Builder(this);
        soundNotification.setSmallIcon(R.drawable.notification_icon);
        soundNotification.setDefaults(Notification.DEFAULT_VIBRATE);
        soundNotification.setContentTitle("Sound analyzed");
        soundNotification.setContentText("Your door bell seems to be ringing");
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(123, soundNotification.build());


    }
    public void stopService() {
        stopService(new Intent(getBaseContext(), SoundDetectionService.class));
    }

    private void writeAudioDataToFile(byte[] data, int size){
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
// TODO Auto-generated catch block
            e.printStackTrace();
        }


            try {
                os.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }

//        try {
//            os.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }


    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

       // copyWaveFile(getTempFilename(), getFilename());
        //deleteTempFile();
    }

    public double calculateMean(double[] arrayOfNumbers) {
        double sum = 0;
        for (int i = 0; i < arrayOfNumbers.length; i++) {
            sum += arrayOfNumbers[i];
        }
        return  sum / arrayOfNumbers.length;
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }
    private void writeFileToPhone()
    {
        short nChannels =1;
        short mBitsPersample =16;
        try {
            RandomAccessFile randomAccessWriter = new RandomAccessFile(getFilename(), "rw");
            randomAccessWriter.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
            randomAccessWriter.writeBytes("RIFF");
            randomAccessWriter.writeInt(0); // Final file size not known yet, write 0
            randomAccessWriter.writeBytes("WAVE");
            randomAccessWriter.writeBytes("fmt ");
            randomAccessWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
            randomAccessWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
            randomAccessWriter.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
            randomAccessWriter.writeInt(Integer.reverseBytes(RECORDER_SAMPLERATE)); // Sample rate
            randomAccessWriter.writeInt(Integer.reverseBytes(RECORDER_SAMPLERATE * nChannels * mBitsPersample / 8)); // Byte rate, SampleRate*NumberOfChannels*mBitsPersample/8
            randomAccessWriter.writeShort(Short.reverseBytes((short) (nChannels * mBitsPersample / 8))); // Block align, NumberOfChannels*mBitsPersample/8
            randomAccessWriter.writeShort(Short.reverseBytes(mBitsPersample)); // Bits per sample
            randomAccessWriter.writeBytes("data");
            randomAccessWriter.writeInt(0); // Data chunk size not known yet, write 0
            buffer = new byte[mPeriodInFrames * mBitsPersample / 8 * nChannels];
        }
        catch(Exception ex)
        {

        }
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            Log.d("File size: ", Long.toString(totalDataLen));

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    public double[] calculateFFT(byte[] signal) {
        final int mNumberOfFFTPoints = 1024;
        double mMaxFFTSample;

        double temp;
        Complex[] y;
        Complex[] complexSignal = new Complex[mNumberOfFFTPoints];
        double[] absSignal = new double[mNumberOfFFTPoints / 2];

        for (int i = 0; i < mNumberOfFFTPoints; i++) {
            temp = (double) ((signal[2 * i] & 0xFF) | (signal[2 * i + 1] << 8)) / 32768.0F;
            complexSignal[i] = new Complex(temp, 0.0);
        }

        y = FFT.fft(complexSignal); // --> Here I use FFT class

        mMaxFFTSample = 0.0;
        mPeakPos = 0;
        for (int i = 0; i < (mNumberOfFFTPoints / 2); i++) {
            absSignal[i] = Math.sqrt(Math.pow(y[i].re(), 2) + Math.pow(y[i].im(), 2));
            if (absSignal[i] > mMaxFFTSample) {
                mMaxFFTSample = absSignal[i];
                mPeakPos = i;
            }
        }

        return absSignal;

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
                    // calculate();
                    break;

                }
            }
        }
    };

//    @Override
//    protected void onStop() {
//        super.onStop();
//        if(serviceIntent==null) {
//            serviceIntent = new Intent(this, SoundDetectionService.class);
//            startService(serviceIntent);
//        }
//    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void playRecoderedAudio(View v) {
        m_track = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);
        String filepath = Environment.getExternalStorageDirectory().getAbsolutePath();

        int i;
        byte[] s = new byte[bufferSize];
        try {
            FileInputStream fin = new FileInputStream(savedFileName);
            DataInputStream dis = new DataInputStream(fin);

            m_track.play();
            while ((i = dis.read(s, 0, bufferSize)) > -1) {
                m_track.write(s, 0, i);

            }
            m_track.stop();
            m_track.release();
            dis.close();
            fin.close();

        } catch (FileNotFoundException e) {
            // TODO
            e.printStackTrace();
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private float[] shortToFloat(short[] audio) {
        Log.d("SHORTTOFLOAT", "INSIDE SHORTTOFLOAT");
        float[] converted = new float[audio.length];

        for (int i = 0; i < converted.length; i++) {
            // [-32768,32768] -> [-1,1]
            converted[i] = audio[i] / 32768f; /* default range for Android PCM audio buffers) */

        }

        return converted;
    }

    private float[] byteToFloat(byte[] audio) {
        return shortToFloat(byteToShort(audio));
    }

    public short[] byteToShort(byte[] rawdata) {
        short[] converted = new short[rawdata.length / 2];
        Integer temp;
        for (int i = 0; i < converted.length; i++) {
            // Wave file data are stored in little-endian order
            short lo = rawdata[2 * i];
            short hi = rawdata[2 * i + 1];
            temp = new Integer(((hi & 0xFF) << 8) | (lo & 0xFF));
            converted[i] = temp.shortValue();
        }
        return converted;
    }

    public void bassBoostEffect() {
        if (m_track != null) {
            BassBoost bass = new BassBoost(0, m_track.getAudioSessionId());
            bass.setEnabled(true);
            BassBoost.Settings bassBoostSettingTemp = bass.getProperties();
            BassBoost.Settings bassBoostSetting = new BassBoost.Settings(bassBoostSettingTemp.toString());
            bass.setProperties(bassBoostSetting);
            bass.setStrength((short) 900);
            Log.d("Bass effect : ", "On");
//            Toast message = Toast.makeText(getApplicationContext(),"Bass effect applied",Toast.LENGTH_SHORT);
//            final Toast toast = Toast.makeText(getApplicationContext(), "Bass boost enabled", Toast.LENGTH_SHORT);
//            toast.show();
//
//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    toast.cancel();
//                }
//            }, 2000);
            //  Toast.makeText(getApplicationContext(), "Bass boost enabled", Toast.LENGTH_SHORT).show();
        } else {
            effectFlag = 3;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String selectedOption = parent.getItemAtPosition(position).toString();
        int optionId = getSelectedEffectId(selectedOption);
        applyEffect(optionId);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public int getSelectedEffectId(String option) {
        switch (option.toLowerCase()) {
            case "class room":
                return 1;
            case "outdoor":
                return 2;
            case "bass boost":
                return 3;

            case "home":
                return 4;
            case "office":
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

        }

        public String run() {
            final String hostIp ="10.99.2.114"; //"192.168.0.23";
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
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            result = "Dummy class";
            return result;
        }

    }


}