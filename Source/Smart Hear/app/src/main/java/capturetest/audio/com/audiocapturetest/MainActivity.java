package capturetest.audio.com.audiocapturetest;

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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ddf.minim.effects.BandPass;
import ddf.minim.effects.HighPassSP;
import ddf.minim.effects.LowPassSP;

public class MainActivity extends AppCompatActivity {
    //    private static final int RECORDER_SAMPLERATE = 8000;
//    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
//    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_DEFAULT;
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 8000;
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
    private double[] absNormalizedSignal;
    public int mPeakPos;
    public String savedFileName;
    public float[] floatData;
    BandPass bandpass;
    private SeekBar seekBarForLowPass;
    private SeekBar seekBarForHighPass;
    private SeekBar seekBarForBandPass;
    public int lowFrequencyValue = 0;
    public int highFrequencyValue = 0;
    public int passFilterFlag = 1;
    public int bandPassFrequencyValue = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setButtonHandlers();
        enableButtons(false);
        int maxFrequencyLimit = 15000;
        bufferSize = AudioRecord.getMinBufferSize
                (RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) * 3;
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
        ((Button) findViewById(R.id.btn_Start)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btn_Stop)).setOnClickListener(btnClick);
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
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);

        recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            public void run() {
                // writeAudioDataToFile();

                m_track = new AudioTrack(AudioManager.STREAM_ALARM, RECORDER_SAMPLERATE,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT,
                        bufferSize, AudioTrack.MODE_STREAM);
                PresetReverb pReverb = new PresetReverb(1, 0);
                pReverb.setPreset(PresetReverb.PRESET_SMALLROOM);
                pReverb.setEnabled(true);
                m_track.attachAuxEffect(pReverb.getId());
                m_track.setAuxEffectSendLevel(1.0f);
                NoiseSuppressor.create(m_track.getAudioSessionId());
                m_track.setPlaybackRate(RECORDER_SAMPLERATE);
                int read = 0;
                short shortData[] = new short[bufferSize];
                byte[] data = new byte[bufferSize];
                m_track.play();
                while (isRecording) {
                    read = recorder.read(shortData, 0, bufferSize);
                    if (read > 0) {
                        floatData = shortToFloat(shortData);
                         Log.d("Frequency data",floatData.toString());
//                       bandpass = new BandPass(400, 2, 2500);
//                        bandpass.process( floatData);
                        if (passFilterFlag != 0 && passFilterFlag == 2) {
                            HighPassSP hp = new HighPassSP(highFrequencyValue, RECORDER_SAMPLERATE/2);

                            hp.process(floatData);
                        } else if (passFilterFlag != 0 && passFilterFlag == 1) {
                            //  LowPassFS lp = new LowPassFS(lowFrequencyValue, 1);
                            LowPassSP lp = new LowPassSP(lowFrequencyValue, RECORDER_SAMPLERATE/2);
                            lp.process(floatData);
                        } else if (passFilterFlag != 0 && passFilterFlag == 3) {
//                            BandPass bp = new BandPass(lowFrequencyValue, 2, highFrequencyValue);
//                            bp.process(floatData);
                            float centerFreq = (float)(bandPassFrequencyValue*1.414);

                            bandpass = new BandPass(centerFreq, bandPassFrequencyValue,RECORDER_SAMPLERATE/2);
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


    }

    private void writeAudioDataToFile() {

        byte data[] = new byte[bufferSize];
        short shortData[] = new short[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int read = 0;
        if (null != os) {
            while (isRecording) {
                read = recorder.read(shortData, 0, bufferSize/2);
                //  read = recorder.read(data,0,bufferSize);

                if (read > 0) {
                    // absNormalizedSignal = calculateFFT(data); // --> HERE ^__^
                    floatData = shortToFloat(shortData);
                    bandpass = new BandPass(25000, 2, 44100);
                    bandpass.process(floatData);
                    bandpass.printCoeff();
                }

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(), getFilename());
        // deleteTempFile();
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
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
        //another code

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

        int i = 0;
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
    public void bassBoostEffect(View v)
    {
        BassBoost bass = new BassBoost(0,m_track.getAudioSessionId());
        bass.setEnabled(true);
        BassBoost.Settings bassBoostSettingTemp =bass.getProperties();
        BassBoost.Settings bassBoostSetting = new BassBoost.Settings(bassBoostSettingTemp.toString());
        bass.setProperties(bassBoostSetting);
        bass.setStrength((short) 900);

    }

}
