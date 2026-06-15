package com.authsense.bank;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtSession;

public class SensorService extends Service implements SensorEventListener, TextToSpeech.OnInitListener {
    private static final String TAG = "AUTH_SENSE_MODEL";
    
    private SensorManager sensorManager;
    private ModelManager modelManager;
    private Vibrator vibrator;
    private TextToSpeech tts;
    private boolean isTtsReady = false;
    
    private float[] lastAccel = new float[3];
    private float[] lastGyro = new float[3];
    
    private List<float[]> dataBuffer = new ArrayList<>();
    private final int WINDOW_SIZE = 300;
    private final int NUM_FEATURES = 6;
    
    private double riskScore = 0;
    private boolean isLocked = false;
    private boolean warningSent = true;

    private static final double RISK_WARNING = 2.0;    
    private static final double RISK_CRITICAL = 5.0;   
    private static final double RISK_INCREMENT = 1.0;
    private static final double RISK_DECAY = 0.2;

    private int criticalAnomalyCount = 0;
    private static final int MAX_CRITICAL_ATTEMPTS = 3;
    
    private BehaviorBaseline behaviorBaseline;
    private KeystrokeTracker keystrokeTracker;
    private String currentUserEmail;
    
    private boolean isCollectingBaseline = false;
    private List<Double> collectionMSEs = new ArrayList<>();
    private List<Double> collectionKeyScores = new ArrayList<>();
    private long learningStartTime = 0;
    private static final long LEARNING_DURATION_MS = 300_000; // 5 MINUTES
    
    // Adaptive Decision Engine Fields
    private boolean isInjuryMode = false;
    private double travelMultiplier = 1.0;
    private double currentVariance = 0.0;
    private List<Double> accelMagnitudes = new ArrayList<>();
    private static final int VARIANCE_WINDOW_SIZE = 100; // 10s window for extreme stability

    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private BroadcastReceiver behaviorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.authsense.bank.KEYSTROKE_EVENT".equals(action)) {
                float pressure = intent.getFloatExtra("pressure", 0.5f);
                if (pressure == 0) pressure = 0.5f;
                keystrokeTracker.recordKeystroke(pressure);

                // Detailed typing pattern logging for honeypot
                SharedPreferences prefs = getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
                if (prefs.getBoolean("is_honeypot", false)) {
                    String pattern = String.format(Locale.US, "⌨️ Typing Pattern: Pressure=%.2f, Mean Interval=%.0fms", 
                                     pressure, keystrokeTracker.getMeanKeystrokeInterval());
                    logToHoneypot(pattern);
                }
            } else if ("com.authsense.bank.INJURY_MODE_TOGGLE".equals(action)) {
                isInjuryMode = intent.getBooleanExtra("active", false);
                Log.i(TAG, "🩹 Injury Mode Toggled: " + (isInjuryMode ? "ENABLED (3.0x Multiplier)" : "DISABLED"));
            }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        currentUserEmail = prefs.getString("user_email", "unknown_user");

        try {
            modelManager = new ModelManager(this);
        } catch (Exception e) {
            Log.e(TAG, "❌ Model error: " + e.getMessage());
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        tts = new TextToSpeech(this, this);
        behaviorBaseline = new BehaviorBaseline(this, currentUserEmail);
        keystrokeTracker = new KeystrokeTracker();
        
        if (!behaviorBaseline.isBaselineComplete()) {
            isCollectingBaseline = true;
            learningStartTime = System.currentTimeMillis();
            mainHandler.post(() -> Toast.makeText(this, "Learning: 5-minute countdown started.", Toast.LENGTH_LONG).show());
        }
        
        Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (accel != null) sensorManager.registerListener(this, accel, 100_000);
        if (gyro != null) sensorManager.registerListener(this, gyro, 100_000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.authsense.bank.KEYSTROKE_EVENT");
        filter.addAction("com.authsense.bank.INJURY_MODE_TOGGLE");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(behaviorReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(behaviorReceiver, filter);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            isTtsReady = true;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            lastAccel = event.values.clone();
            
            // Travel Detection: Magnitude Variance Analysis
            double mag = Math.sqrt(lastAccel[0]*lastAccel[0] + lastAccel[1]*lastAccel[1] + lastAccel[2]*lastAccel[2]);
            accelMagnitudes.add(mag);
            if (accelMagnitudes.size() > VARIANCE_WINDOW_SIZE) {
                accelMagnitudes.remove(0);
                updateTravelState();
            }

            float[] sample = new float[NUM_FEATURES];
            for (int i = 0; i < 3; i++) sample[i] = modelManager.scaleValue(lastAccel[i], i);
            for (int i = 0; i < 3; i++) sample[i + 3] = modelManager.scaleValue(lastGyro[i], i + 3);
            addDataPoint(sample);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            lastGyro = event.values.clone();
        }
    }

    private void updateTravelState() {
        if (accelMagnitudes.size() < VARIANCE_WINDOW_SIZE) return;
        double sum = 0;
        for (double d : accelMagnitudes) sum += d;
        double mean = sum / accelMagnitudes.size();
        double sqDiffSum = 0;
        for (double d : accelMagnitudes) sqDiffSum += Math.pow(d - mean, 2);
        currentVariance = sqDiffSum / accelMagnitudes.size();

        // High-stability thresholds
        // Stationary typing jitter is typically < 4.0
        if (currentVariance < 5.0) {
            travelMultiplier = 1.0; // Normal
        } else if (currentVariance < 15.0) {
            travelMultiplier = 1.8; // Vehicle
        } else {
            travelMultiplier = 2.5; // Walking
        }
    }

    private void addDataPoint(float[] sample) {
        dataBuffer.add(sample);
        if (dataBuffer.size() >= WINDOW_SIZE) {
            runInference();
            for(int i=0; i<50; i++) if(!dataBuffer.isEmpty()) dataBuffer.remove(0);
        }
    }

    private void runInference() {
        if (modelManager == null || modelManager.getSession() == null || isLocked) return;
        try {
            float[][][] inputData = new float[1][WINDOW_SIZE][NUM_FEATURES];
            for (int i = 0; i < WINDOW_SIZE; i++) inputData[0][i] = dataBuffer.get(i);
            OnnxTensor inputTensor = OnnxTensor.createTensor(modelManager.getEnvironment(), inputData);
            OrtSession.Result result = modelManager.getSession().run(Collections.singletonMap("input", inputTensor));
            float[][][] outputData = (float[][][]) result.get(0).getValue();
            double mse = calculateMSE(inputData[0], outputData[0]);

            if (isCollectingBaseline) {
                handleBackgroundCollection(mse);
            } else {
                handleMonitoring(mse);
            }
            inputTensor.close();
            result.close();
        } catch (Exception e) { Log.e(TAG, "Inference error", e); }
    }

    private void handleBackgroundCollection(double mse) {
        long elapsed = System.currentTimeMillis() - learningStartTime;
        long remaining = (LEARNING_DURATION_MS - elapsed) / 1000;
        collectionMSEs.add(mse);
        
        if (keystrokeTracker.hasEnoughData()) {
            double normalizedInterval = Math.min(keystrokeTracker.getMeanKeystrokeInterval(), 1000.0) / 10.0;
            double currentRaw = (normalizedInterval * 0.7) + (keystrokeTracker.getMeanPressure() * 100 * 0.3);
            collectionKeyScores.add(currentRaw);
        }

        Log.i(TAG, String.format(Locale.US, "⏳ Learning: %ds left | MSE: %.4f", Math.max(0, remaining), mse));

        if (elapsed >= LEARNING_DURATION_MS && keystrokeTracker.hasEnoughData()) {
            double meanMSE = collectionMSEs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double mseStdDev = computeStdDev(collectionMSEs, meanMSE);
            
            double meanKeyRaw = collectionKeyScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            List<Double> keyErrors = new ArrayList<>();
            for (Double score : collectionKeyScores) keyErrors.add(Math.abs(score - meanKeyRaw));
            double meanKeyError = keyErrors.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double keyErrorStdDev = computeStdDev(keyErrors, meanKeyError);

            behaviorBaseline.setBaseline(keystrokeTracker, meanMSE, mseStdDev, meanKeyRaw, meanKeyError, keyErrorStdDev);
            isCollectingBaseline = false;
            mainHandler.post(() -> Toast.makeText(this, "🎉 Profile Created!", Toast.LENGTH_LONG).show());
        }
    }

    private void handleMonitoring(double mse) {
        SharedPreferences prefs = getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        boolean isHoneypot = prefs.getBoolean("is_honeypot", false);

        // Apply Adaptive Multipliers
        double injuryMultiplier = isInjuryMode ? 3.0 : 1.0;
        double combinedMultiplier = injuryMultiplier * travelMultiplier;

        double motionThreshold = behaviorBaseline.getMotionThreshold() * combinedMultiplier;
        boolean isMotionAnomaly = mse > motionThreshold;
        
        double keyDeviation = behaviorBaseline.calculateKeystrokeAnomalyScore(keystrokeTracker);
        double keyThreshold = behaviorBaseline.getKeystrokeThreshold() * combinedMultiplier;
        boolean isKeystrokeAnomaly = keystrokeTracker.hasEnoughData() && (keyDeviation > keyThreshold);
        
        // Mode naming for logs
        String modeName = (travelMultiplier == 1.0) ? "Normal" : (travelMultiplier == 1.8 ? "Vehicle" : "Walking");

        Log.d(TAG, String.format(Locale.US, "🛡️ Risk: %.1f | Mode: %s (Var: %.3f) | Mult: %.1fx | MSE: %.4f (Th: %.4f) | KeyDev: %.1f (Th: %.1f)",
                riskScore, modeName, currentVariance, combinedMultiplier, mse, motionThreshold, keyDeviation, keyThreshold));
        if (isMotionAnomaly || isKeystrokeAnomaly) {
            if (isHoneypot) {
                // Silently log attacker behavior instead of alerting
                String logMsg = "⚠️ ATTACKER ANOMALY DETECTED: " + 
                                (isMotionAnomaly ? "Unusual Motion (MSE: " + String.format(Locale.US, "%.4f", mse) + ") " : "") +
                                (isKeystrokeAnomaly ? "Unusual Typing Pattern (Dev: " + String.format(Locale.US, "%.1f", keyDeviation) + ")" : "");
                logToHoneypot(logMsg);
                return;
            }

            riskScore += RISK_INCREMENT;
            
            if (riskScore >= RISK_WARNING && !warningSent) {
                warnUser(false);
            }
            if (riskScore >= RISK_CRITICAL) {
                criticalAnomalyCount++;
                if (criticalAnomalyCount >= MAX_CRITICAL_ATTEMPTS) {
                    lockSystem();
                } else {
                    riskScore = 0;
                    warningSent = false;
                    warnUser(true);
                }
            }
        } else {
            riskScore = Math.max(0, riskScore - RISK_DECAY);
            if (riskScore == 0) warningSent = false;
            
            if (keystrokeTracker.hasEnoughData() && mse < motionThreshold * 1.1) {
                // Only update baseline for REAL users
                if (!isHoneypot) {
                    behaviorBaseline.updateBaseline(keystrokeTracker, mse, 0.005);
                }
            }
        }
    }

    private void logToHoneypot(String action) {
        SharedPreferences prefs = getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        String logKey = "attacker_logs_" + currentUserEmail;
        String currentLogs = prefs.getString(logKey, "");
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new java.util.Date());
        prefs.edit().putString(logKey, currentLogs + "[" + timestamp + "] " + action + "\n").apply();
    }

    private void sendEmailAlert(String subject, String htmlBody) {
        final String senderEmail = "authsensebank@gmail.com";
        final String senderPass = "bguq djnp vuiu nxnb";
        final String senderName = "AuthSense Bank Security";

        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(senderEmail, senderPass);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(senderEmail, senderName));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(currentUserEmail));
                message.setSubject("🚨 " + subject);
                
                String fullHtml = "<html><body style='font-family: \"Segoe UI\", Tahoma, Geneva, Verdana, sans-serif; color: #333; line-height: 1.6; background-color: #f9f9f9; padding: 20px;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 40px; border-radius: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.05); border: 1px solid #e0e0e0;'>" +
                        htmlBody + 
                        "<div style='margin-top: 40px; padding-top: 20px; border-top: 1px solid #eeeeee; font-size: 12px; color: #888;'>" +
                        "User ID: " + currentUserEmail + "<br>" +
                        "Date: " + new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm:ss z", Locale.US).format(new java.util.Date()) +
                        "</div>" +
                        "</div></body></html>";
                
                MimeMultipart multipart = new MimeMultipart("alternative");
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setContent(fullHtml, "text/html; charset=utf-8");
                multipart.addBodyPart(htmlPart);
                message.setContent(multipart);
                
                Transport.send(message);
                Log.i(TAG, "📧 Email alert sent: " + subject);
            } catch (Exception e) {
                Log.e(TAG, "❌ Email Failed: " + e.getMessage());
            }
        }).start();
    }

    private double computeStdDev(List<Double> values, double mean) {
        if (values.size() < 2) return 0;
        double var = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
        return Math.sqrt(var);
    }

    private void warnUser(boolean isUrgent) {
        SharedPreferences prefs = getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        
        if (isUrgent) {
            Log.i(TAG, "🔒 Setting transaction_blocked = true");
            prefs.edit().putBoolean("transaction_blocked", true).commit();
        }

        warningSent = true;
        String title, body;
        if (isUrgent) {
            title = "Final Security Warning";
            body = "<h2 style='color: #d9534f; margin-top: 0;'>Final Security Warning</h2>" +
                   "<p>Hello,</p>" +
                   "<p>Highly suspicious behavior has been detected during your current session. This is your <b>" + 
                   criticalAnomalyCount + " of 3</b> critical warnings.</p>" +
                   "<p>For your protection, outgoing transactions have been restricted, and any further unusual activity will result in an immediate account lock.</p>" +
                   "<p>Please review your active session in the AuthSense app.</p>";
        } else {
            title = "Security Alert: Unusual Activity";
            body = "<h2 style='color: #f0ad4e; margin-top: 0;'>Security Notification</h2>" +
                   "<p>Hello,</p>" +
                   "<p>Our system has identified activity that does not match your typical behavior patterns. We are monitoring the session to ensure your account remains secure.</p>" +
                   "<p>You can continue to use the app, but please be aware that further anomalies may lead to account restrictions.</p>";
        }
        sendEmailAlert(title, body);

        if (vibrator != null) vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        if (isTtsReady && tts != null) {
            String text = isUrgent ? "Critical alert. Account restricted." : "Warning. Unusual behavior detected.";
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "Alert");
        }
        
        Intent intent = new Intent(this, AnomalyActivity.class);
        intent.putExtra("hard_lock", false);
        intent.putExtra("urgent", isUrgent);
        intent.putExtra("attempt_count", criticalAnomalyCount);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void lockSystem() {
        if (isLocked) return;
        isLocked = true;
        
        SharedPreferences prefs = getSharedPreferences("AuthSensePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        // Activate Honeypot for this specific user
        editor.putBoolean("is_honeypot_active_" + currentUserEmail, true);
        // Store current password as 'old' for honeypot login logic
        String currentPass = prefs.getString("pass_" + currentUserEmail, "");
        editor.putString("old_pass_" + currentUserEmail, currentPass);
        
        editor.putBoolean("is_logged_in", false);
        editor.commit();

        // Use custom scheme for more reliable deep linking in dev environment
        String resetLink = "authsense://reset?email=" + currentUserEmail;
        
        String emailBody = "<h2 style='color: #d9534f; margin-top: 0;'>Security Alert: Action Required</h2>" +
            "<p>Dear Customer,</p>" +
            "<p>Our advanced monitoring system has detected unusual activity on your AuthSense Bank account. As a precaution, we have restricted some features of your account.</p>" +
            "<p>To verify your identity and restore full access, you <b>must</b> reset your password using the secure link below:</p>" +
            "<p style='margin: 40px 0;'><a href=\"" + resetLink + "\" style='color: #007bff; text-decoration: underline; font-size: 16px; font-weight: 500;'>Click here to reset your password</a></p>" +
            "<p>If you cannot click the link, please copy and paste this into your browser: " + resetLink + "</p>" +
            "<p>If you did not authorize this action, please contact our security team immediately.</p>" +
            "<p>Thank you for your cooperation.</p>" +
            "<p>Best regards,<br><b>AuthSense Bank Security</b></p>";

        sendEmailAlert("Security Update Required", emailBody);

        Log.e(TAG, "🚨 TRIGGERING HONEYPOT FOR: " + currentUserEmail);
        if (vibrator != null) vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 500, 200, 500}, -1));
        
        // Redirect to Fake Interface immediately
        Intent intent = new Intent(this, FakeMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        stopSelf();
    }

    private double calculateMSE(float[][] original, float[][] reconstructed) {
        double sumError = 0;
        int count = 0;
        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < original[i].length; j++) {
                float diff = original[i][j] - reconstructed[i][j];
                sumError += (diff * diff);
                count++;
            }
        }
        return sumError / count;
    }

    @Override public void onAccuracyChanged(Sensor s, int a) {}
    @Override public IBinder onBind(Intent i) { return null; }
    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        unregisterReceiver(behaviorReceiver);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
