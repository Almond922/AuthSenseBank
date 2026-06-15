package com.authsense.bank;

import android.content.Context;
import android.util.Log;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ModelManager {
    private static final String TAG = "ModelManager";
    private OrtEnvironment env;
    private OrtSession session;
    private double threshold = 0.005; // Default threshold
    private float[] scalerMin = new float[0];
    private float[] scalerMax = new float[0];

    public ModelManager(Context context) {
        Context appContext = context.getApplicationContext();
        try {
            // 1. Load Metadata
            String jsonString = loadJSONFromAsset(appContext, "lstm_cnn_meta.json");
            if (jsonString != null) {
                JSONObject jsonObject = new JSONObject(jsonString);

                // Flexible key check for threshold
                if (jsonObject.has("lstm_cnn_threshold")) {
                    this.threshold = jsonObject.getDouble("lstm_cnn_threshold");
                } else if (jsonObject.has("lstm_ae_threshold")) {
                    this.threshold = jsonObject.getDouble("lstm_ae_threshold");
                }

                // Load Scaler values
                JSONArray minArray = jsonObject.optJSONArray("scaler_min");
                JSONArray maxArray = jsonObject.optJSONArray("scaler_max");

                if (minArray != null && maxArray != null) {
                    scalerMin = new float[minArray.length()];
                    scalerMax = new float[maxArray.length()];
                    for (int i = 0; i < minArray.length(); i++) {
                        scalerMin[i] = (float) minArray.getDouble(i);
                        scalerMax[i] = (float) maxArray.getDouble(i);
                    }
                }
            }

            // 2. Initialize ONNX
            env = OrtEnvironment.getEnvironment();
            
            // Copy the model
            String modelPath = copyAssetToInternalStorage(appContext, "lstm_cnn.onnx", "lstm_cnn.onnx");
            
            // CRITICAL: The .onnx file internally expects its data to be named "lstm_ae.onnx.data"
            try {
                copyAssetToInternalStorage(appContext, "lstm_cnn.onnx.data", "lstm_ae.onnx.data");
            } catch (Exception e) {
                Log.w(TAG, "Data file copy warning: " + e.getMessage());
            }

            if (modelPath != null) {
                session = env.createSession(modelPath);
                Log.d(TAG, "Model loaded successfully. Threshold: " + threshold);
            }

        } catch (Exception e) {
            Log.e(TAG, "Initialization error: " + e.getMessage(), e);
        }
    }

    public float scaleValue(float value, int index) {
        if (scalerMin == null || index >= scalerMin.length || index >= scalerMax.length) return value;
        float denom = scalerMax[index] - scalerMin[index];
        if (denom == 0) return 0;
        return (value - scalerMin[index]) / denom;
    }

    private String copyAssetToInternalStorage(Context context, String assetName, String targetFileName) throws Exception {
        File file = new File(context.getFilesDir(), targetFileName);
        try (InputStream is = context.getAssets().open(assetName);
             FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        }
        return file.getAbsolutePath();
    }

    private String loadJSONFromAsset(Context context, String fileName) {
        try (InputStream is = context.getAssets().open(fileName)) {
            int size = is.available();
            byte[] buffer = new byte[size > 0 ? size : 32768];
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            Log.e(TAG, "Error loading JSON: " + fileName, e);
            return null;
        }
    }

    public OrtSession getSession() { return session; }
    public OrtEnvironment getEnvironment() { return env; }
    public double getThreshold() { return threshold; }
    public void close() throws Exception {
        if (session != null) session.close();
        if (env != null) env.close();
    }
}
